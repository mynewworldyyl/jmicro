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
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.Constants;

import com.google.common.util.concurrent.RateLimiter;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-下午5:44:19
 */
@Component(value="gavaLimiter")
public class GuavaBaseLimiter  implements ILimiter{

	private Map<String,RateLimiter> rateLimiter = new ConcurrentHashMap<>();
	
	@Override
	public boolean apply(IRequest req) {
		String key = this.key(req);
		
		ServiceMethod sm = (ServiceMethod)JMicroContext.get()
				.getObject(Constants.SERVICE_METHOD_KEY, null);
		
		ServiceItem item = (ServiceItem)JMicroContext.get()
				.getObject(Constants.SERVICE_ITEM_KEY, null);
		
		RateLimiter rl = rateLimiter.get(key);
		if(rl == null) {
			key = key.intern();
			synchronized(key){
				rl = rateLimiter.get(key);
				if(rl == null) {
					float maxSpeed = -1;
					if(sm != null){
						maxSpeed = sm.getMaxSpeed();
					}
					
					if(maxSpeed == -1 && item != null){
						maxSpeed = item.getMaxSpeed();
					}
					
					if(maxSpeed > 0) {
						rl = RateLimiter.create(sm.getMaxSpeed());
						this.rateLimiter.put(key, rl);
					}else {
						//不限速
						return true;
					}
				}
				
			}
		}
		
		int timeout = sm.getTimeout();
		if(timeout < 0){
			timeout = item.getTimeout();
		}
		
		if(timeout > 0){
			return rl.tryAcquire(1, timeout, TimeUnit.MILLISECONDS);
		}else {
			rl.acquire(1);
		}
		
		return true;
	}
	
	private String key(IRequest req){
		String key = UniqueServiceMethodKey.paramsStr(req.getArgs());
		//key = key + ServiceItem.serviceName(req.getServiceName(), req.getNamespace(), req.getVersion());
		key = key + req.getMethod() + req.getImpl();
		return key;
	}

}
