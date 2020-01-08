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
package org.jmicro.redis;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.cache.lock.ILocker;
import org.jmicro.api.cache.lock.ILockerManager;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
@Component(active=true,value="redisDLockerManager",side=Constants.SIDE_ANY)
public class RedisBaseDistributeLockerManager implements ILockerManager {

	@Inject
	private JedisPool pool;
	
	@Override
	public ILocker getLocker(String resource) {
		if(StringUtils.isEmpty(resource)) {
			return null;
		}
		return new LockerImpl(resource);
	}
	
	class LockerImpl implements ILocker {
		
		private static final String NX = "NX";
		private static final String EX = "EX";
		private static final String PX = "PX";
		
		private String resource;
		
		private boolean lockStat = false;
		
		protected LockerImpl(String resource) {
			this.resource = resource;
		}

		/**
		 *最长持锁时间10分钟
		 */
		@Override
		public boolean tryLock(int checkIntervalWithMillisenconds) {
			//最长持锁时间10分钟
			return tryLock(checkIntervalWithMillisenconds,60000);
		}

		@Override
		public boolean tryLock(int checkIntervalWithMillisenconds, long timeoutWithMillisenconds) {
			if(lockStat) {
				//同一个锁实例，在未解锁前，不能重复上锁，但是解锁之后，可以再锁
				throw new CommonException(resource + " have been lock with Object:" + toString());
			}
			if(checkIntervalWithMillisenconds <= 0) {
				checkIntervalWithMillisenconds = 10;
			}
			
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				String rst = jedis.set(resource, "dd",NX,EX,timeoutWithMillisenconds);
				long inv = timeoutWithMillisenconds;
				while(!"OK".equals(rst)) {
					try {
						Thread.sleep(checkIntervalWithMillisenconds);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					inv -= checkIntervalWithMillisenconds;
					if(inv > 0) {
						rst = jedis.set(resource, "ddd",NX,EX,timeoutWithMillisenconds);
					}
				}
				
				lockStat = "OK".equals(rst);
				
				return lockStat;
			}finally{
				if(jedis != null) {
					jedis.close();
				}
			}
		}

		@Override
		public boolean unLock() {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				Long rst = jedis.del(resource);
				lockStat = rst != 1;
				return !lockStat;
			}finally{
				if(jedis != null) {
					jedis.close();
				}
			}
		}
	}
}



