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

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.common.util.TimeUtils;
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
	
	private static final Integer[] TYPES = new Integer[] {MonitorConstant.CLIENT_REQ_OK};
	
	@JMethod("init")
	public void init(){
	}
	
	private Map<String,ServiceCounter> limiterData = new ConcurrentHashMap<>();
	
	@Override
	public boolean apply(IRequest req) {
		
		//not support method override
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm.getMaxSpeed() <= 0) {
			return true;
		}
		
		ServiceCounter sc =  null;
		String key = sm.getKey().toKey(true, true, true);
		
		if(!limiterData.containsKey(key)){
			key = key.intern();
			synchronized(key) {
				if(!limiterData.containsKey(key)){
					sc =  new ServiceCounter("key", TYPES,2,2,TimeUnit.SECONDS);
					this.limiterData.put(key, sc);
				}else {
					sc = limiterData.get(key);
				}
			}
		} else {
			sc = limiterData.get(key);
		}
		
		double qps = sc.getQpsWithEx(MonitorConstant.CLIENT_REQ_OK, TimeUnit.SECONDS);
		//logger.info("{} qps:{}",sm.getKey().getMethod(),qps);
		
		if(qps > sm.getMaxSpeed()){
			SF.doSubmit(MonitorConstant.SERVER_REQ_LIMIT_OK, req,null,"");
			logger.info("key:{},qps:{},maxQps:{}",key,qps,sm.getMaxSpeed());
			return false;
		}
		
		sc.incrementWithEx(MonitorConstant.CLIENT_REQ_OK);
		
		return true;
	}
	


}
