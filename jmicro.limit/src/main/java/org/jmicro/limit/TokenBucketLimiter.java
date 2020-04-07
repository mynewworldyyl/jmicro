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

import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.monitor.v1.IMonitorDataSubmiter;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.net.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-下午1:51:29
 */
@Component(lazy=false,value="tokenBucketLimiter")
public class TokenBucketLimiter extends AbstractLimiter implements ILimiter{

	private final static Logger logger = LoggerFactory.getLogger(TokenBucketLimiter.class);
	
	private static final String TAG = TokenBucketLimiter.class.getName();
	
	private Map<String,ITokenBucket> buckets = new HashMap<>();
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
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
			SF.limit(TAG);
			return false;
		} else {
			return true;
		}
	}
}
