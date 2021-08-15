/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.breaker.api;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.TypeCoderFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.IStatisDataSubscribeJMSrv;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorStatisConfigManager;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.monitor.StatisDataJRso;
import cn.jmicro.api.monitor.StatisIndexJRso;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
@Service(version="0.0.1",showFront=false)
public class BreakerManager implements IStatisDataSubscribeJMSrv{
	
	private static final Short[] REQ_FAIL_TYPES = new Short[]{MC.MT_CLIENT_RESPONSE_SERVER_ERROR,MC.MT_REQ_TIMEOUT_FAIL,MC.MT_REQ_ERROR};
	private static final Short[] REQ_TYPES = new Short[] {MC.MT_REQ_START};
	private static final Short[] REQ_SUCCESS_TYPES = new Short[] {MC.MT_REQ_SUCCESS,MC.MT_SERVICE_ERROR};

	private static final String TAG = BreakerManager.class.getName();
	private final static Logger logger = LoggerFactory.getLogger(BreakerManager.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	//private final Map<String,BreakerReg> breakableMethods = new ConcurrentHashMap<>();
	
	@Cfg("/BreakerManager/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private MonitorStatisConfigManager mc;
	
	private ITickerAction<CheckerVo> doTestImpl = null;
	
	//private ITickerAction<ServiceMethod> breakerChecker = null;
	
	private Map<String,Integer> srvMt2ConfigIds = new HashMap<>();
	
	private StatisIndexJRso[] statisIndex = new StatisIndexJRso[2];
	
	public void init(){}
	
	public void jready(){
		doTestImpl = this::doTestService;
		//breakerChecker = this::breakerChecker;
		
		statisIndex[0] = new StatisIndexJRso();
		statisIndex[0].setName("fp");
		statisIndex[0].setNums(REQ_FAIL_TYPES);
		statisIndex[0].setDens(REQ_TYPES);
		statisIndex[0].setDesc("rpc fail percent");
		statisIndex[0].setType(StatisConfigJRso.PREFIX_CUR_PERCENT);
		
		statisIndex[1] = new StatisIndexJRso();
		statisIndex[1].setName("sp");
		statisIndex[1].setNums(REQ_SUCCESS_TYPES);
		statisIndex[1].setDens(REQ_TYPES);
		statisIndex[1].setDesc("rpc success percent");
		statisIndex[1].setType(StatisConfigJRso.PREFIX_CUR_PERCENT);
		
		srvManager.addListener((type,siKey,si)->{
			if(type == IListener.ADD) {
				serviceAdd(siKey,si);
			}else if(type == IListener.REMOVE) {
				serviceRemove(siKey,si);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(siKey,si);
			} 
		});
	}

	private void serviceDataChange(UniqueServiceKeyJRso itemKey,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(itemKey.fullStringKey());
		}
		
		for(ServiceMethodJRso sm : item.getMethods()) {
			String smKey = sm.getKey().methodID();
			if(!srvMt2ConfigIds.containsKey(smKey)) {
				if(sm.getBreakingRule().isEnable()) {
					createStatisConfig(sm,true);
				}
			} else {
				if(!sm.getBreakingRule().isEnable()) {
					srvMt2ConfigIds.remove(smKey);
				}
			}
		}
	}

	private void serviceRemove(UniqueServiceKeyJRso itemKey,ServiceItemJRso item) {
		//ServiceItemJRso item = this.srvManager.getServiceByKey(itemKey.fullStringKey());
		for(ServiceMethodJRso sm : item.getMethods()) {
			if(sm.getBreakingRule().isEnable()) {
				srvMt2ConfigIds.remove(sm.getKey().methodID());
			}
		}
	}

	private void serviceAdd(UniqueServiceKeyJRso itemKey,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(itemKey.fullStringKey());
		}
		for(ServiceMethodJRso sm : item.getMethods()) {
			if(sm.getBreakingRule().isEnable()) {
				createStatisConfig(sm,true);
			}
		}
	}

	private void createStatisConfig(ServiceMethodJRso sm, boolean isFp) {
		
		String key = sm.getKey().methodID();
		if(this.srvMt2ConfigIds.containsKey(key)) {
			return;
		}
		
		StatisConfigJRso sc = new StatisConfigJRso();
		sc.setId(idGenerator.getIntId(StatisConfigJRso.class));
		srvMt2ConfigIds.put(key, sc.getId());
		
		sc.setByType(StatisConfigJRso.BY_TYPE_SERVICE_METHOD);
		sc.setByKey(key);
		
		sc.setExpStr("fp>" + sm.getBreakingRule().getPercent());
		sc.setExpStr1("sp>" + sm.getBreakingRule().getPercent());
		
		sc.setToType(StatisConfigJRso.TO_TYPE_SERVICE_METHOD);
		
		StringBuilder sb = new StringBuilder();
		sb.append(UniqueServiceKeyJRso.serviceName(IStatisDataSubscribeJMSrv.class.getName(),"breaker", "*"));
		sb.append(UniqueServiceKeyJRso.SEP).append(UniqueServiceKeyJRso.SEP)
		.append(UniqueServiceKeyJRso.SEP).append(UniqueServiceKeyJRso.SEP)
		.append("onData").append(UniqueServiceKeyJRso.SEP);
		
		sc.setToParams(sb.toString());
		
		sc.setCounterTimeout(1*60);
		sc.setTimeUnit(StatisConfigJRso.UNIT_SE);
		sc.setTimeCnt(1);
		sc.setEnable(true);
		
		sc.setStatisIndexs(statisIndex);
		
		sc.setCreatedBy(Config.getClientId());
		
		String path = StatisConfigJRso.STATIS_CONFIG_ROOT + "/" + sc.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(sc), true);//服务停止时，配置将消失
		
	}
	
	private ServiceMethodJRso getServiceMethodBreakRule(String sn,String ns,String ver,String method) {
		Set<UniqueServiceKeyJRso> itemKeys = this.reg.getServices(sn, ns, ver);
		ServiceItemJRso item = null;
		long lasttime = Long.MAX_VALUE;
		for(UniqueServiceKeyJRso siKey : itemKeys) {
			//以最先创建的熔断配置为准
			ServiceItemJRso si = this.srvManager.getServiceByKey(siKey.fullStringKey());
			
			if(si != null && si.getCreatedTime() < lasttime) {
				lasttime = si.getCreatedTime();
				item = si;
			}
		}
		
		if(item != null) {
			return item.getMethod(method);
		}
		
		return null;
	}

	/**
	 * 按指定时间间隔，调用已经熔断的服务方法，直到熔断器关闭
	 */
	public void breakerChecker(CheckerVo vo) {
		
		ServiceMethodJRso sm = vo.sm;
		if(sm == null) {
			logger.warn("Break rule not found for: ",vo.sd.getKey());
			return;
		}

		String key = vo.sd.getKey();
		
		if(sm.isBreaking()) {
			//已经熔断,算成功率,判断是否关闭熔断器
			Double sp = vo.sd.getIndex("sp");
			
			if(sp == null) {
				//logger.warn("Monitor data not found  {}",key);
				return;
			}
			
			if(sp > sm.getBreakingRule().getPercent()) {
				if(this.openDebug) {
					logger.info("Close breaker for service {}, success rate {}",key,sp);
				}
				
				updateBreaker(vo,false);
				
				long interval = TimeUtils.getMilliseconds(sm.getBreakingRule().getBreakTimeInterval(), 
						sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).removeListener(key, true);
				LG.breakService(MC.LOG_WARN,TAG, sm, "close breaker for: " + key + " sp: " + sp);
				
				//切换回计算失败率
				/*if(!updateStaticIndex(vo.sd.getCid(),true,sm.getBreakingRule().getPercent())) {
					String msg = "Fail to update statis config for "+key;;
					LG.logWithNonRpcContext(MC.LOG_ERROR,TAG, msg,null);
					logger.error(msg);
				}*/
			}
		} else {
			
			Double fp = vo.sd.getIndex("fp");
			if(fp == null) {
				logger.info("Monitor data not found  {}",key);
				return;
			}
			
			if(fp > sm.getBreakingRule().getPercent()) {
				sm.setBreaking(true);
				//切换到计算成功率
				/*if(!updateStaticIndex(vo.sd.getCid(),false,sm.getBreakingRule().getPercent())) {
					//切失败失败，不熔断服务
					String msg = "Break down service "+key+", fail rate: " +fp;
					LG.logWithNonRpcContext(MC.LOG_ERROR,TAG, msg,null);
					logger.error(msg);
					return;
				}*/
				
				logger.warn("Break down service {}, fail rate {}",key,fp);
				
				updateBreaker(vo,true);
				
				long interval = TimeUtils.getMilliseconds(sm.getBreakingRule().getBreakTimeInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).addListener(key, vo, doTestImpl);
				
				LG.breakService(MC.LOG_WARN,TAG, sm, "Open breaker for fp: "+fp);
			}
		}
	}	
	
	private void updateBreaker(CheckerVo vo,boolean breakStatus) {
		UniqueServiceMethodKeyJRso smKey = vo.sm.getKey();
		Set<UniqueServiceKeyJRso> items = this.reg.getServices(smKey.getServiceName(), smKey.getNamespace(), smKey.getVersion());
		for(UniqueServiceKeyJRso siKey : items) {
			//以最先创建的熔断配置为准
			ServiceItemJRso si = this.srvManager.getServiceByKey(siKey.fullStringKey());
			ServiceMethodJRso sm = si.getMethod(smKey.getMethod());
			if(sm != null) {
				sm.setBreaking(breakStatus);
				this.srvManager.breakService(sm.getKey());
			}
		}
	}

	/*private boolean updateStaticIndex(Integer cid, boolean isFp,int percent) {
		StatisConfig sc = mc.getConfig(cid);
		if(sc != null) {
			if(isFp) {
				sc.setExpStr("fp>"+percent);
			}else {
				sc.setExpStr("sp>"+percent);
			}
			
			sc.setStatisIndexs(isFp?fpStatisIndex:spStatisIndex);
			String path = StatisConfig.STATIS_CONFIG_ROOT + "/" + sc.getId();
			op.setData(path, JsonUtils.getIns().toJson(sc));
			return true;
		}
		return false;
	}*/
	
	private void removeChecker(CheckerVo vo) {
		long interval = TimeUtils.getMilliseconds(vo.sm.getBreakingRule().getCheckInterval(), vo.sm.getBaseTimeUnit());
		TimerTicker.getTimer(timers,interval).removeListener(vo.sm.getKey().fullStringKey(),false);
	}
	
	/**
	 * 按指定时间间隔，调用已经熔断的服务方法，直到熔断器关闭
	 */
	public void doTestService(String key,CheckerVo vo) {
		
		if(!vo.sm.isBreaking()) {
			removeChecker(vo);
			return;
		}
		
		Object[] args = vo.args;
		
		if(args == null) {
			if(Utils.isEmpty(vo.sm.getTestingArgs())) {
				vo.args = args = new Object[0];
			}else {
				if(vo.sm.getTestingArgs().startsWith("[")) {
					List<String> list = JsonUtils.getIns().getStringValueList(vo.sm.getTestingArgs(), false);
					Class<?>[] clses = vo.sm.getKey().getParameterClasses();
					vo.args = args = new Object[clses.length];
					for(int i = 0; i < clses.length; i++) {
						vo.args[i] = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(list.get(i)), clses[i]);
					}
				}else {
					try {
						byte[] data = Base64Utils.decode(vo.sm.getTestingArgs().getBytes(Constants.CHARSET));
						JDataInput ji = new JDataInput(ByteBuffer.wrap(data));
						vo.args = args = (Object[])TypeCoderFactory.getIns().getDefaultCoder().decode(ji, null, null);
					} catch (UnsupportedEncodingException e) {
						logger.error("",e);
						throw new CommonException("Invalid testint args:"+vo.sm.getTestingArgs()
						+ " for: "+vo.sm.getKey().fullStringKey(),e);
					}
				}
				
				
			}
		}
		
		UniqueServiceMethodKeyJRso smKey = vo.sm.getKey();
		
		if(vo.srv == null) {
			
			if(!reg.isExists(smKey.getServiceName(), smKey.getNamespace(),smKey.getVersion())) {
				//服务还不在在，可能后续上线，这里只发一个警告
				String msg2 = "Now config service ["+smKey.getServiceName() 
				+"##"+smKey.getNamespace()+"##"+ smKey.getVersion()+"] not found";
				logger.warn(msg2);
				LG.logWithNonRpcContext(MC.LOG_WARN, BreakerManager.class, msg2,MC.MT_DEFAULT,true);
				removeChecker(vo);
				return;
			}
			
			Object srv = of.getRemoteServie(smKey.getServiceName(), smKey.getNamespace(),smKey.getVersion(),null);
			if(srv == null) {
				String msg2 = "Fail to create service proxy ["+smKey.getServiceName() +"##"+smKey.getNamespace()+"##"+ smKey.getVersion()+"] not found";
				logger.warn(msg2);
				LG.logWithNonRpcContext(MC.LOG_WARN, BreakerManager.class, msg2,MC.MT_DEFAULT,true);
				removeChecker(vo);
				return;
			}
			
			vo.srv = srv;
		}
		
		try {
			/*if(StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD == vo.sd.getType()) {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, vo.si);
			}*/
			JMicroContext.get().setParam(Constants.BREAKER_TEST_CONTEXT, true);
			PromiseUtils.callService(vo.srv, smKey.getMethod(), null, args)
			.fail((code,msg,cxt)->{
				//logger.error("Notify fail: " + smKey.toKey(true, true, true));
			});
			JMicroContext.get().removeParam((Constants.BREAKER_TEST_CONTEXT));
			/*if(StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD == vo.sd.getType()) {
				JMicroContext.get().removeParam((Constants.DIRECT_SERVICE_ITEM));
			}*/
		} catch (Throwable e) {
			logger.error("doTestService error: "+vo.sm.getKey().fullStringKey(),e);
		}
		
	}
	
	@Override
	public IPromise<Void> onData(StatisDataJRso sd) {
		
		if(!this.srvMt2ConfigIds.containsKey(sd.getKey())) {
			this.srvMt2ConfigIds.put(sd.getKey(), sd.getCid());
		}
		
		String[] arr = sd.getKey().split(UniqueServiceKeyJRso.SEP);
		
		String method = arr[UniqueServiceKeyJRso.INDEX_METHOD];
		String sn = arr[UniqueServiceKeyJRso.INDEX_SN];
		String ns = arr[UniqueServiceKeyJRso.INDEX_NS];
		String ver = arr[UniqueServiceKeyJRso.INDEX_VER];
		
		ServiceMethodJRso sm = this.getServiceMethodBreakRule(sn,ns,ver,method);
		
		CheckerVo vo = new CheckerVo(sd,sm);
		
		breakerChecker(vo);
		
		return null;
	}
	
	private class CheckerVo {
		private CheckerVo(StatisDataJRso sd,ServiceMethodJRso sm) {
			this.sm = sm;
			this.sd = sd;
		}
		private ServiceMethodJRso sm;
		private StatisDataJRso sd;
		
		private Object[] args;
		private Object srv;
	}

}
