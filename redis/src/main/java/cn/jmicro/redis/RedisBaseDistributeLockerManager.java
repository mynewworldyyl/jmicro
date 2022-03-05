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
package cn.jmicro.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.lock.ILocker;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.config.Config;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
@Component(active=true,value="redisDLockerManager",side=Constants.SIDE_ANY)
public class RedisBaseDistributeLockerManager implements ILockerManager {

	private static final ThreadLocal<Map<String,AtomicInteger>> tl = new ThreadLocal<Map<String,AtomicInteger>>();
	
	@Inject
	private JedisPool pool;
	
	private String prefix;
	
	public void jready() {
		prefix = Config.getRaftBasePath("")+"/dl";
	}
	
	@Override
	public ILocker getLocker(String resource) {
		if(StringUtils.isEmpty(resource)) {
			return null;
		}
		return new LockerImpl(prefix+"/"+resource);
	}
	
	class LockerImpl implements ILocker {
		
		private static final String NX = "NX";
		private static final String EX = "EX";
		private static final String PX = "PX";
		
		private String resource;
		
		//private boolean lockStat = false;
		
		protected LockerImpl(String resource) {
			this.resource = resource;
		}

		/**
		 *最长持锁时间10分钟
		 */
		@Override
		public boolean tryLock(int checkIntervalWithMillisenconds) {
			//最长持锁时间1分钟
			return tryLock(checkIntervalWithMillisenconds,60000);
		}

		@Override
		public boolean tryLock(int checkIntervalWithMillisenconds, long timeoutWithMillisenconds) {
			/*if(lockStat) {
				//同一个锁实例，在未解锁前，不能重复上锁，但是解锁之后，可以再锁
				throw new CommonException(resource + " have been lock with Object:" + toString());
			}*/
			
			AtomicInteger cnt = getLockCnt(resource);
			if(cnt.get() > 0) {
				//同一个线程对同一个资源取锁
				//lockStat = true;
				cnt.incrementAndGet();
				return true;
			}
			
			if(checkIntervalWithMillisenconds <= 0) {
				checkIntervalWithMillisenconds = 10;
			}
			
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				SetParams sp = new SetParams();
				sp.nx().px(timeoutWithMillisenconds);
				String rst = jedis.set(resource, "dd",sp);
				long inv = timeoutWithMillisenconds;
				while(!"OK".equals(rst)) {
					try {
						Thread.sleep(checkIntervalWithMillisenconds);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					inv -= checkIntervalWithMillisenconds;
					if(inv > 0) {
						SetParams spe = new SetParams();
						spe.nx().ex(timeoutWithMillisenconds);
						rst = jedis.set(resource, "dd",spe);
					 }else {
						break;
					}
					
					if("OK".equals(rst)) {
						break;
					}
				}
				
				boolean lockStat = "OK".equals(rst);
				
				if(lockStat) {
					cnt.incrementAndGet();
				}
				
				return lockStat;
			}finally{
				if(jedis != null) {
					jedis.close();
				}
			}
		}

		@Override
		public boolean unLock() {
			/*if(!lockStat) {
				throw new CommonException(resource + " not in lock status" + toString());
			}
			*/
			Jedis jedis = null;
			try {
				AtomicInteger cnt = getLockCnt(resource);
				if(cnt.get() == 0) {
					return true;
				}
				int c = cnt.decrementAndGet();
				if(c < 0) {
					c = 0;
					cnt.set(0);
				}
				if(c == 0) {
					jedis = pool.getResource();
					Long rst = jedis.del(resource);
					//lockStat = rst == 1;
					/*if(lockStat) {
						tl.get().remove(resource);
					}*/
					return rst == 1;
				} else {
					return true;
				}
				
			}finally{
				if(jedis != null) {
					jedis.close();
				}
			}
		}
	}
	
	private static AtomicInteger getLockCnt(String resource) {
		Map<String,AtomicInteger> m = tl.get();
		if(m == null) {
			m = new HashMap<>();
			m.put(resource, new AtomicInteger(0));
			tl.set(m);
		}
		
		AtomicInteger ac = m.get(resource);
		if(ac == null) {
			m.put(resource, ac = new AtomicInteger(0));
		}
		
		return m.get(resource);
		
	}
}



