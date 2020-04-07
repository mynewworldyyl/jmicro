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
package org.jmicro.limit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.monitor.v1.ServiceCounter;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:11:58
 */
@Component(lazy=false,value="limiterName")
public class DefaultSpeedLimiter extends AbstractLimiter implements ILimiter{
	
	static final Logger logger = LoggerFactory.getLogger(DefaultSpeedLimiter.class);
	private static final String TAG = DefaultSpeedLimiter.class.getName();
	
	private static final Short[] TYPES = new Short[] {MonitorConstant.REQ_START};
	
	private Map<String,AtomicInteger> als = new ConcurrentHashMap<>();
	
	private Map<String,ServiceCounter> limiterData = new ConcurrentHashMap<>();
	
	@JMethod("init")
	public void init(){
	}
	
	@Override
	public boolean enter(IRequest req) {
		
		//not support method override
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm.getMaxSpeed() <= 0) {
			return true;
		}
		
		ServiceCounter sc =  null;
		AtomicInteger al = null;
		
		String key = sm.getKey().toKey(true, true, true);
		
		if(!limiterData.containsKey(key)){
			key = key.intern();
			synchronized(key) {
				if(!limiterData.containsKey(key)){
					sc =  new ServiceCounter("key", TYPES,2,2,TimeUnit.SECONDS);
					this.limiterData.put(key, sc);
					al = new AtomicInteger(0);
					this.als.put(key, al);
				} else {
					sc = limiterData.get(key);
					al = this.als.get(key);
				}
			}
		} else {
			sc = limiterData.get(key);
			al = this.als.get(key);
		}
		
		double qps = sc.getQpsWithEx(MonitorConstant.REQ_START, TimeUnit.SECONDS);
		//logger.info("qps:{},key:{}",qps,sm.getKey().getMethod());
		
		if(qps > sm.getMaxSpeed()){
			
			int cnt = al.get()+1;
			int needWaitTime = (int)((1000.0*cnt)/ sm.getMaxSpeed());
			
			if(needWaitTime >= sm.getTimeout()) {
				SF.limit(TAG);
				logger.info("qps:{},maxQps:{},key:{}",qps,sm.getMaxSpeed(),key);
				return false;
			} 
			
			if(needWaitTime > 0) {
				try {
					Thread.sleep(needWaitTime);
				} catch (InterruptedException e) {
					logger.error("Limit wait timeout error",e);
				}
			}
			
			logger.info("wait:{},cnt:{},maxSpeed:{}",needWaitTime,cnt,sm.getMaxSpeed());
			
		}
		
		//logger.info("apply cnt:{}",al.incrementAndGet());
		
		sc.incrementWithEx(MonitorConstant.REQ_END);
		
		return true;
	}

	@Override
	public void end(IRequest req) {
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm.getMaxSpeed() <= 0) {
			return;
		}
		String key = sm.getKey().toKey(true, true, true);
		AtomicInteger al = this.als.get(key);
		if(al != null) {
			int v = al.decrementAndGet();
			//logger.info("END cnt:{}",v);
			if(v <= 0) {
				al.set(0);
			}
		}
	}
	
}
