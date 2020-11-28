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
package cn.jmicro.limit;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.limitspeed.ILimiter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.AccountRelatedStatis;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:11:58
 */
//@Component(lazy=false,value="limiterName")
public class DefaultSpeedLimiter extends AbstractLimiter implements ILimiter{
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultSpeedLimiter.class);
	
	private static final Class<?> TAG = DefaultSpeedLimiter.class;
	
	private static final String Act_SM_SEPERATOR = ":";
	
	@Inject
	private AccountRelatedStatis actStatisManager;

	@Override
	public boolean enter(IRequest req) {
		
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		
		if(sm.getMaxSpeed() <= 0) {
			return true;
		}
		
		String key = counterKey(sm,req);
		
		ServiceCounter sc =  this.actStatisManager.getCounter(key);
		
		sc.increment(MC.MT_REQ_START);
		
		double qps = sc.getQps(TimeUnit.SECONDS,MC.MT_REQ_START);
		
		sc.setLastActiveTime(TimeUtils.getCurTime());
		
		if(qps > sm.getMaxSpeed()){
			logger.info("{} cur qps:{},{} maxSpeed:{}",key,qps,sm.getKey().toKey(false, false, false),sm.getMaxSpeed());
			return false;
		}
		
		return true;
	}

	private String counterKey(ServiceMethod sm,IRequest req) {
		String key = "";
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai == null) {
			key = req.getSession().remoteHost();
		}else {
			key = ai.getActName();
		}
		//账号+RPC方法KEY
		return key + Act_SM_SEPERATOR + sm.getKey().toKey(false, false, false);
	}

	@Override
	public void end(IRequest req) {
		
	}
	
}
