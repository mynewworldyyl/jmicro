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
package org.jmicro.breaker.api;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.codec.JDataInput;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.api.mng.ReportData;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.monitor.v2.IMonitorDataSubscriber;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.BreakRule;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceInvokeManager;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Base64Utils;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
public class BreakerManager{
	
	private static final String TAG = BreakerManager.class.getName();
	private final static Logger logger = LoggerFactory.getLogger(BreakerManager.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private final Map<String,BreakerReg> breakableMethods = new ConcurrentHashMap<>();
	
	@Cfg("/BreakerManager/openDebug")
	private boolean openDebug = false;
	
	@Reference(namespace="rpcStatisMonitor", version="0.0.1",required=false)
	private IMonitorDataSubscriber dataServer;
	
	private AbstractClientServiceProxy ds;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private ServiceInvokeManager invokeManager;
	
	private ITickerAction<ServiceMethod> doTestImpl = null;
	
	private ITickerAction<ServiceMethod> breakerChecker = null;
	
	public void init(){
		
	}
	
	@JMethod("ready")
	public void ready(){
		
		doTestImpl = this::doTestService;
		breakerChecker = this::breakerChecker;
		
		ds = (AbstractClientServiceProxy)((Object)dataServer);
		
		srvManager.addListener((type,item)->{
			if(type == IListener.ADD) {
				serviceAdd(item);
			}else if(type == IListener.REMOVE) {
				serviceRemove(item);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(item);
			} 
		});
	}

	/**
	 * 按指定时间间隔，调用已经熔断的服务方法，直到熔断器关闭
	 */
	public void breakerChecker(String key, ServiceMethod sm) {
		
		if(!ds.isUsable()) {
			logger.warn("Monitor data server is not ready for breaker check  {}",key);
			return;
		}
		
		BreakRule rule = sm.getBreakingRule();

		if(sm.isBreaking()) {
			//已经熔断,算成功率,判断是否关闭熔断器
			
			ReportData rd = dataServer.getData(key,new Short[] {MonitorConstant.REQ_SUCCESS},new String[] {MonitorConstant.PREFIX_CUR_PERCENT});
			
			if(rd.getCurPercent() == null) {
				logger.warn("Monitor data not found  {}",key);
				return;
			}
			
			if(rd.getCurPercent()[0] > rule.getPercent()) {
				if(this.openDebug) {
					logger.info("Close breaker for service {}, success rate {}",key,rd.getCurPercent()[0]);
				}
				sm.setBreaking(false);
				srvManager.breakService(sm);
				
				long interval = TimeUtils.getMilliseconds(rule.getBreakTimeInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).removeListener(key+"/dotesting", true);
				SF.breakService("close", sm, "" + rd.getCurPercent()[0]);
			}
		} else {
			//没有熔断,判断是否需要熔断
			ReportData rd = dataServer.getData(key,new Short[] {MonitorConstant.STATIS_FAIL_PERCENT},new String[] {MonitorConstant.PREFIX_CUR_PERCENT});
			
			if(rd.getCurPercent() == null) {
				logger.info("Monitor data not found  {}",key);
				return;
			}
			
			if(rd.getCurPercent()[0] > rule.getPercent()) {
				logger.warn("Break down service {}, fail rate {}",key,rd.getCurPercent()[0]);
				sm.setBreaking(true);
				srvManager.breakService(sm);
				
				long interval = TimeUtils.getMilliseconds(rule.getBreakTimeInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).addListener(key+"/dotesting", doTestImpl, sm);
				
				SF.breakService("open", sm, ""+rd.getCurPercent()[0]);
			}
		}
	}	
	
	private void serviceDataChange(ServiceItem item) {

		Set<ServiceMethod> sms = item.getMethods();
		if(sms == null || sms.isEmpty()) {
			return;
		}
		
		Iterator<ServiceMethod> ite = sms.iterator();
		while(ite.hasNext()) {
			ServiceMethod sm = ite.next();
			String key = sm.getKey().toKey(true, true, true);
			
			BreakerReg oldSm = breakableMethods.get(key);

			BreakRule br = sm.getBreakingRule();
			
			long interval = TimeUtils.getMilliseconds(br.getCheckInterval(), sm.getBaseTimeUnit());
			
			if(oldSm == null) {
				if(!br.isEnable()) {
					continue;
				} else {
					boolean isMonitorable = sm.getMonitorEnable() == 1 ? true:(sm.getMonitorEnable() == 0 ? false : (item.getMonitorEnable() == 1?true:false));
					if(isMonitorable) {
						BreakerReg reg = new BreakerReg(key,sm,item);
						breakableMethods.put(key, reg);
						TimerTicker.getTimer(timers,interval).addListener(key, breakerChecker, sm);
					}
				}
			} else {
				boolean isMonitorable = sm.getMonitorEnable() == 1 ? true:(sm.getMonitorEnable() == 0 ? false : (item.getMonitorEnable() == 1?true:false));
				if(!br.isEnable() || !isMonitorable) { //br.isEnable() == true, 则 oldSm不可能等于NULL
					//变为不可熔断
					breakableMethods.remove(key);
					long inter = TimeUtils.getMilliseconds(oldSm.sm.getBreakingRule().getCheckInterval(), oldSm.sm.getBaseTimeUnit());
					TimerTicker.getTimer(timers,inter).removeListener(key, true);
				} else if(!oldSm.sm.getBreakingRule().equals(sm.getBreakingRule())){
					long inter = TimeUtils.getMilliseconds(oldSm.sm.getBreakingRule().getCheckInterval(), oldSm.sm.getBaseTimeUnit());
					TimerTicker.getTimer(timers,inter).removeListener(key, true);
					BreakerReg reg = new BreakerReg(key,sm,item);
					breakableMethods.put(key, reg);
					TimerTicker.getTimer(timers,interval).addListener(key, breakerChecker, sm);
				}
			}
		}
	}

	private void serviceRemove(ServiceItem item) {
		Set<ServiceMethod> sms = item.getMethods();
		if(sms == null || sms.isEmpty()) {
			return;
		}
		
		Iterator<ServiceMethod> ite = sms.iterator();
		while(ite.hasNext()) {
			ServiceMethod sm = ite.next();
			
			BreakRule br = sm.getBreakingRule();
			if(br == null || !br.isEnable() ) {
				continue;
			}
			
			String key = sm.getKey().toKey(true, true, true);
			if(breakableMethods.containsKey(key)) {
				long interval = TimeUtils.getMilliseconds(br.getCheckInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).removeListener(key, true);
				breakableMethods.remove(key);
			}
		}
	}

	private void serviceAdd(ServiceItem item) {
		Set<ServiceMethod> sms = item.getMethods();
		if(sms == null || sms.isEmpty()) {
			return;
		}
		
		Iterator<ServiceMethod> ite = sms.iterator();
		while(ite.hasNext()) {
			ServiceMethod sm = ite.next();
			BreakRule br = sm.getBreakingRule();
			if(br == null || !br.isEnable() ) {
				continue;
			}
			boolean isMonitorable = sm.getMonitorEnable() == 1 ? true:(sm.getMonitorEnable() == 0 ? false : (item.getMonitorEnable() == 1?true:false));
			if(isMonitorable) {
				String key = sm.getKey().toKey(true, true, true);
				BreakerReg reg = new BreakerReg(key,sm,item);
				breakableMethods.put(key, reg);
				long interval = TimeUtils.getMilliseconds(br.getCheckInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).addListener(key, breakerChecker, sm);
				
				if(sm.isBreaking()) {
					long inte = TimeUtils.getMilliseconds(br.getBreakTimeInterval(), sm.getBaseTimeUnit());
					TimerTicker.getTimer(timers,inte).addListener(key+"/dotesting", doTestImpl, sm);
				}
			}
		}
	}
	
	/**
	 * 按指定时间间隔，调用已经熔断的服务方法，直到熔断器关闭
	 */
	public void doTestService(String key, ServiceMethod sm) {
		
		if(!sm.isBreaking()) {
			long interval = TimeUtils.getMilliseconds(sm.getBreakingRule().getCheckInterval(), sm.getBaseTimeUnit());
			TimerTicker.getTimer(timers,interval).removeListener(key,false);
			return;
		}
		
		String mkey = sm.getKey().toKey(true, true, true);
		BreakerReg reg = this.breakableMethods.get(mkey);
		
		Object[] args = null;
		if(reg.testArgs  == null && StringUtils.isEmpty(sm.getTestingArgs())) {
			args = new Object[0];
		} else if(reg.testArgs  == null) {
			try {
				byte[] data = Base64Utils.decode(sm.getTestingArgs().getBytes(Constants.CHARSET));
				JDataInput ji = new JDataInput(ByteBuffer.wrap(data));
				args = (Object[])TypeCoderFactory.getDefaultCoder().decode(ji, null, null);
				reg.testArgs = args;
			} catch (UnsupportedEncodingException e) {
				logger.error("",e);
				throw new CommonException("Invalid testint args:"+sm.getTestingArgs()+ " for: "+mkey,e);
			}
		}
		
		try {
			this.invokeManager.callDirect(reg.si,sm, reg.testArgs);
		} catch (Throwable e) {
			logger.error("doTestService error: "+key,e);
		}
		
	}
	
	private class BreakerReg{
		private String mkey;
		private ServiceMethod sm;
		private Object[] testArgs;
		private ServiceItem si = null;
		
		public BreakerReg(String mkey,ServiceMethod sm,ServiceItem item) {
			this.mkey = mkey;
			this.si = item;
			this.sm = sm;
		}
	}
	
}
