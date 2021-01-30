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
import cn.jmicro.api.limit.genclient.ILimitData$JMAsyncClient;
import cn.jmicro.api.monitor.IStatisDataSubscribe;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.api.monitor.StatisIndex;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(version="0.0.1",external=false)
public class LimitServer implements IStatisDataSubscribe {

	private static final Logger logger = LoggerFactory.getLogger(LimitServer.class);
	
	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="subscriberChange")
	private Set<ILimitData$JMAsyncClient> dataReceivers = new HashSet<>();
	
	private Map<String,ILimitData$JMAsyncClient> ins2Limiters = new HashMap<>();
	
	private Map<String,RegEntry> regs = new HashMap<>();
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IDataOperator op;
	
	private StatisIndex[] qpsStatisIndex = new StatisIndex[1];
	
	public void ready() {
		qpsStatisIndex[0] = new StatisIndex();
		qpsStatisIndex[0].setName("qps");
		qpsStatisIndex[0].setNums(new Short[]{MC.MT_SERVER_LIMIT_MESSAGE_PUSH});
		//qpsStatisIndex[0].setDens(REQ_TYPES);
		qpsStatisIndex[0].setDesc("service qps");
		qpsStatisIndex[0].setType(StatisConfig.PREFIX_QPS);
		
		srvManager.addListener((type,item)->{
			if(type == IListener.ADD) {
				serviceAdd(item);
			}else if(type == IListener.REMOVE) {
				serviceRemove(item);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(item);
			} 
		});
		
		logger.info("Limiter server ready!");
	}
	
	@Override
	@SMethod(needResponse=false)
	public IPromise<Void> onData(StatisData sc) {
		UniqueServiceMethodKey k = UniqueServiceMethodKey.fromKey(sc.getKey());
		Set<ServiceItem> sis = this.srvManager.getServiceItems(k.getServiceName(), k.getNamespace(), k.getVersion());
		sc.setIndex(StatisData.INS_SIZE, sis.size());
		
		Double qps = (Double)sc.getStatis().get(StatisData.QPS);
		
		if(sc.containIndex(StatisData.INS_SIZE)) {
			int insSize = sc.getIndex(StatisData.INS_SIZE);
			if(insSize > 1) {
				//每个运行实例平均分配QPS
				Double avgQps = qps/insSize;
				sc.setIndex(StatisData.AVG_QPS, avgQps);
			}
		}
		
		logger.debug("OnData: " + JsonUtils.getIns().toJson(sc));
		
		for(ServiceItem si : sis) {
			ILimitData$JMAsyncClient r = this.ins2Limiters.get(si.getKey().getInstanceName());
			if(r != null) {
				r.onDataJMAsync(sc)
				.fail((code,msg,cxt)->{
					logger.error(sc.getKey() + " err: "+msg);
				});
			}
		}
		return null;
	}
	
	private void serviceDataChange(ServiceItem item) {
		for(ServiceMethod sm : item.getMethods()) {
			String key = sm.getKey().toKey(false, false, false);
			if(regs.containsKey(key)) {
				RegEntry re = regs.get(key);
				if(sm.getMaxSpeed() <= 0) {
					if(re.smInsCount >1) {
						re.smInsCount--;
					} else {
						//服务完全删除，无需再监控
						regs.remove(key);
						String path = StatisConfig.STATIS_CONFIG_ROOT + "/" + re.cid;
						op.deleteNode(path);
					}
				}
			} else if(sm.getMaxSpeed() > 0){
				createStatisConfig(sm);
			}
		}
	}

	private void serviceRemove(ServiceItem item) {
		for(ServiceMethod sm : item.getMethods()) {
			String key = sm.getKey().toKey(false, false, false);
			if(regs.containsKey(key)) {
				RegEntry re = regs.get(key);
				if(re.smInsCount >1) {
					re.smInsCount--;
				} else {
					//服务完全删除，无需再监控
					regs.remove(key);
					String path = StatisConfig.STATIS_CONFIG_ROOT + "/" + re.cid;
					op.deleteNode(path);
				}
			}
		}
	}

	private void serviceAdd(ServiceItem item) {
		for(ServiceMethod sm : item.getMethods()) {
			if(sm.getMaxSpeed() > 0) {
				createStatisConfig(sm);
			}
		}
	}
	
	private void createStatisConfig(ServiceMethod sm) {
		
		if(sm.getLimitType() != Constants.LIMIT_TYPE_SS) {
			return;
		}
		
		String key = sm.getKey().toKey(false,false,false);
		if(this.regs.containsKey(key)) {
			RegEntry re = regs.get(key);
			re.smInsCount += 1;
			re.sm = sm;
			return;
		}
		
		StatisConfig sc = new StatisConfig();
		sc.setId(idGenerator.getIntId(StatisConfig.class));
		
		sc.setByType(StatisConfig.BY_TYPE_SERVICE_METHOD);
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
		
		sc.setToType(StatisConfig.TO_TYPE_SERVICE_METHOD);
		
		StringBuilder sb = new StringBuilder();
		sb.append(UniqueServiceKey.serviceName(IStatisDataSubscribe.class.getName(),"limitServer", "*"));
		sb.append("########")
		.append("onData").append(UniqueServiceKey.SEP);
		
		sc.setToParams(sb.toString());
		
		sc.setCounterTimeout(1*60);
		sc.setTimeUnit(StatisConfig.UNIT_SE);
		sc.setTimeCnt(1);
		sc.setEnable(true);
		
		sc.setStatisIndexs(this.qpsStatisIndex);
		sc.setCreatedBy(Config.getClientId());
		
		String path = StatisConfig.STATIS_CONFIG_ROOT + "/" + sc.getId();
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
		ServiceItem si = srv.getItem();
		String insName = si.getKey().getInstanceName();
		if(type == IListener.ADD) {
			ILimitData$JMAsyncClient djm = (ILimitData$JMAsyncClient)srv;
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
		private ServiceMethod sm;
		private int smInsCount;
	}
}
