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
import org.jmicro.api.mng.ReportData;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v2.IMonitorDataSubscriber;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.registry.BreakRule;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
public class BreakerManager implements ITickerAction{
	
	private final static Logger logger = LoggerFactory.getLogger(BreakerManager.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private final Map<String,ServiceMethod> breakableMethods = new ConcurrentHashMap<>();
	
	@Cfg("/BreakerManager/openDebug")
	private boolean openDebug = false;
	
	@Reference(namespace="rpcStatisMonitor", version="0.0.1",required=false)
	private IMonitorDataSubscriber dataServer;
	
	private AbstractClientServiceProxy ds;
	
	@Inject
	private ServiceManager srvManager;
	
	public void init(){
		
	}
	
	@JMethod("ready")
	public void ready(){
		
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
	@Override
	public void act(String key, Object attachement) {
		
		if(!ds.isUsable()) {
			logger.warn("Monitor data server is not ready for breaker check  {}",key);
			return;
		}
		
		ServiceMethod sm = (ServiceMethod)attachement;
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
			
			ServiceMethod oldSm = breakableMethods.get(key);

			BreakRule br = sm.getBreakingRule();
			
			long interval = TimeUtils.getMilliseconds(br.getCheckInterval(), sm.getBaseTimeUnit());
			
			if(oldSm == null) {
				if(!br.isEnable()) {
					continue;
				} else {
					boolean isMonitorable = sm.getMonitorEnable() == 1 ? true:(sm.getMonitorEnable() == 0 ? false : (item.getMonitorEnable() == 1?true:false));
					if(isMonitorable) {
						breakableMethods.put(key, sm);
						TimerTicker.getTimer(timers,interval).addListener(key, this, sm);
					}
				}
			} else {
				boolean isMonitorable = sm.getMonitorEnable() == 1 ? true:(sm.getMonitorEnable() == 0 ? false : (item.getMonitorEnable() == 1?true:false));
				if(!br.isEnable() || !isMonitorable) { //br.isEnable() == true, 则 oldSm不可能等于NULL
					//变为不可熔断
					breakableMethods.remove(key);
					long inter = TimeUtils.getMilliseconds(oldSm.getBreakingRule().getCheckInterval(), oldSm.getBaseTimeUnit());
					TimerTicker.getTimer(timers,inter).removeListener(key, true);
				} else if(!oldSm.getBreakingRule().equals(sm.getBreakingRule())){
					long inter = TimeUtils.getMilliseconds(oldSm.getBreakingRule().getCheckInterval(), oldSm.getBaseTimeUnit());
					TimerTicker.getTimer(timers,inter).removeListener(key, true);
					breakableMethods.put(key, sm);
					TimerTicker.getTimer(timers,interval).addListener(key, this, sm);
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
				breakableMethods.put(key, sm);
				long interval = TimeUtils.getMilliseconds(br.getCheckInterval(), sm.getBaseTimeUnit());
				TimerTicker.getTimer(timers,interval).addListener(key, this, sm);
			}
		}
	}
	
}
