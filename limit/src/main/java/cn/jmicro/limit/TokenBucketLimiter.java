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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.limitspeed.ILimiter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.IRequest;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-下午1:51:29
 */
@Component(lazy=false,value="tokenBucketLimiter")
public class TokenBucketLimiter extends AbstractLimiter implements ILimiter{

	private final static Logger logger = LoggerFactory.getLogger(TokenBucketLimiter.class);
	
	private static final Class<?> TAG = TokenBucketLimiter.class;
	
	private Map<String,ITokenBucket> buckets = new HashMap<>();
	
	@Override
	public boolean enter(IRequest req) {
		int speed = this.getSpeed(req);
		if(speed <=0){
			return true;
		}
		String key = this.serviceKey(req);
		if(!this.buckets.containsKey(key)){
			this.buckets.put(key, new TokenBucket(this.getSpeedUnit(req)));
		}
		
		ITokenBucket b = this.buckets.get(key);
		//System.out.println("Speed:"+ speed);
		b.updateSpeed(speed);
		//logger.debug("TokenBucketLimiter apply reqID: " + req.getRequestId());
		int rst = b.applyToken(1);
		if(rst < 0) {
			String errMsg = "speed:"+speed+",key:"+key;
			SF.eventLog(MC.MT_SERVICE_SPEED_LIMIT, MC.LOG_WARN, TAG, errMsg);
			logger.info(errMsg);
			return false;
		} else {
			return true;
		}
	}
}
