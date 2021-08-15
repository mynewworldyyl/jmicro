package cn.jmicro.limit.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.limit.genclient.ILimitDataJMSrv$JMAsyncClient;
import cn.jmicro.api.monitor.IStatisDataSubscribeJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.monitor.StatisDataJRso;
import cn.jmicro.api.monitor.StatisIndexJRso;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(version="0.0.1",external=false)
public class LimitServer implements IStatisDataSubscribeJMSrv {

	private static final Logger logger = LoggerFactory.getLogger(LimitServer.class);
	
	public static void main(String[] args) {
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="subscriberChange")
	private Set<ILimitDataJMSrv$JMAsyncClient> dataReceivers = new HashSet<>();
	
	private Map<String,ILimitDataJMSrv$JMAsyncClient> ins2Limiters = new HashMap<>();
	
	private Map<String,RegEntry> regs = new HashMap<>();
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IDataOperator op;
	
	private StatisIndexJRso[] qpsStatisIndex = new StatisIndexJRso[1];
	
	public void jready() {
		qpsStatisIndex[0] = new StatisIndexJRso();
		qpsStatisIndex[0].setName("qps");
		qpsStatisIndex[0].setNums(new Short[]{MC.MT_SERVER_LIMIT_MESSAGE_PUSH});
		//qpsStatisIndex[0].setDens(REQ_TYPES);
		qpsStatisIndex[0].setDesc("service qps");
		qpsStatisIndex[0].setType(StatisConfigJRso.PREFIX_QPS);
		
		srvManager.addListener((type,siKey,si)->{
			if(type == IListener.ADD) {
				serviceAdd(siKey,si);
			}else if(type == IListener.REMOVE) {
				serviceRemove(siKey,si);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(siKey,si);
			} 
		});
		
		logger.info("Limiter server ready!");
	}
	
	@Override
	@SMethod(needResponse=false)
	public IPromise<Void> onData(StatisDataJRso sc) {
		UniqueServiceMethodKeyJRso k = UniqueServiceMethodKeyJRso.fromKey(sc.getKey());
		Set<UniqueServiceKeyJRso> sis = this.srvManager.getServiceItems(k.getServiceName(), k.getNamespace(), k.getVersion());
		
		sc.setIndex(StatisDataJRso.INS_SIZE, sis.size());
		
		Double qps = (Double)sc.getStatis().get(StatisDataJRso.QPS);
		
		if(sc.containIndex(StatisDataJRso.INS_SIZE)) {
			int insSize = sc.getIndex(StatisDataJRso.INS_SIZE);
			if(insSize > 1) {
				//每个运行实例平均分配QPS
				Double avgQps = qps/insSize;
				sc.setIndex(StatisDataJRso.AVG_QPS, avgQps);
			}
		}
		
		logger.debug("OnData: " + JsonUtils.getIns().toJson(sc));
		
		for(UniqueServiceKeyJRso si : sis) {
			ILimitDataJMSrv$JMAsyncClient r = this.ins2Limiters.get(si.getInstanceName());
			if(r != null) {
				r.onDataJMAsync(sc)
				.fail((code,msg,cxt)->{
					logger.error(sc.getKey() + " err: "+msg);
				});
			}
		}
		return null;
	}
	
	private void serviceDataChange(UniqueServiceKeyJRso siKey,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(siKey.fullStringKey());
		}
		for(ServiceMethodJRso sm : item.getMethods()) {
			String key = sm.getKey().methodID();
			if(regs.containsKey(key)) {
				RegEntry re = regs.get(key);
				if(sm.getMaxSpeed() <= 0) {
					if(re.smInsCount >1) {
						re.smInsCount--;
					} else {
						//服务完全删除，无需再监控
						regs.remove(key);
						String path = StatisConfigJRso.STATIS_CONFIG_ROOT + "/" + re.cid;
						op.deleteNode(path);
					}
				}
			} else if(sm.getMaxSpeed() > 0){
				createStatisConfig(sm);
			}
		}
	}

	private void serviceRemove(UniqueServiceKeyJRso siKey,ServiceItemJRso item) {
		for(ServiceMethodJRso sm : item.getMethods()) {
			String key = sm.getKey().methodID();
			if(regs.containsKey(key)) {
				RegEntry re = regs.get(key);
				if(re.smInsCount >1) {
					re.smInsCount--;
				} else {
					//服务完全删除，无需再监控
					regs.remove(key);
					String path = StatisConfigJRso.STATIS_CONFIG_ROOT + "/" + re.cid;
					op.deleteNode(path);
				}
			}
		}
	}

	private void serviceAdd(UniqueServiceKeyJRso siKey,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(siKey.fullStringKey());
		}
		for(ServiceMethodJRso sm : item.getMethods()) {
			if(sm.getMaxSpeed() > 0) {
				createStatisConfig(sm);
			}
		}
	}
	
	private void createStatisConfig(ServiceMethodJRso sm) {
		
		if(sm.getLimitType() != Constants.LIMIT_TYPE_SS) {
			return;
		}
		
		String key = sm.getKey().methodID();
		if(this.regs.containsKey(key)) {
			RegEntry re = regs.get(key);
			re.smInsCount += 1;
			re.sm = sm;
			return;
		}
		
		StatisConfigJRso sc = new StatisConfigJRso();
		sc.setId(idGenerator.getIntId(StatisConfigJRso.class));
		
		sc.setByType(StatisConfigJRso.BY_TYPE_SERVICE_METHOD);
		sc.setByKey(key);
		
		if(sm.getMaxSpeed() > 10) {
			sc.setExpStr("qps>"+sm.getMaxSpeed());//达到qps的80%通知目标
		} else {
			sc.setExpStr("qps>"+sm.getMaxSpeed());
		}
		
		if(sm.getMaxSpeed() > 10) {
			sc.setExpStr1("qps<"+sm.getMaxSpeed());//达到qps的80%通知目标
		} else {
			sc.setExpStr1("qps<"+sm.getMaxSpeed());
		}
		
		sc.setToType(StatisConfigJRso.TO_TYPE_SERVICE_METHOD);
		
		StringBuilder sb = new StringBuilder();
		sb.append(UniqueServiceKeyJRso.serviceName(IStatisDataSubscribeJMSrv.class.getName(),"limitServer", "*"));
		sb.append("########")
		.append("onData").append(UniqueServiceKeyJRso.SEP);
		
		sc.setToParams(sb.toString());
		
		sc.setCounterTimeout(1*60);
		sc.setTimeUnit(StatisConfigJRso.UNIT_SE);
		sc.setTimeCnt(1);
		sc.setEnable(true);
		
		sc.setStatisIndexs(this.qpsStatisIndex);
		sc.setCreatedBy(Config.getClientId());
		
		String path = StatisConfigJRso.STATIS_CONFIG_ROOT + "/" + sc.getId();
		//服务停止时，配置将消失
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(sc), true);
		
		RegEntry re = new RegEntry();
		re.cid = sc.getId();
		re.smKey = key;
		re.sm = sm;
		re.smInsCount = 1;
		this.regs.put(key, re);
	}

	public void subscriberChange(AbstractClientServiceProxyHolder srv,int type) {
		ServiceItemJRso si = srv.getItem();
		String insName = si.getKey().getInstanceName();
		if(type == IListener.ADD) {
			ILimitDataJMSrv$JMAsyncClient djm = (ILimitDataJMSrv$JMAsyncClient)srv;
			if(!this.ins2Limiters.containsKey(insName)) {
				this.ins2Limiters.put(insName, djm);
			}
		}else if(type == IListener.REMOVE) {
			if(this.ins2Limiters.containsKey(insName)) {
				this.ins2Limiters.remove(insName, srv);
			}
		}
	}
	
	private class RegEntry{
		private int cid;
		private String smKey;
		private ServiceMethodJRso sm;
		private int smInsCount;
	}
}
