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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.cache.ICache;
import org.jmicro.api.cache.ICacheRefresher;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.net.Message;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 实现缓存基本功能：
 * 1. 缓存任意对象,提取时直接取得缓存对象,省去应用序列化和反序列化操作,采用JMICRO高效前缀类型序列化框架实现
 * 2. 有效避免缓存雪崩,随机超时时间
 * 3. 缓存自动刷新,设定定时刷新
 * 4. 有效避免缓存击穿
 * 
 * 缓存数据KEY在单JVM和多JVM下应该有不同的策略，如果要求在单JVM下独立存在，那么KEY应该加入JVM标识，如
 * key = Config.getInstanceName() + "/" + key; 
 * 
 * Config.getInstanceName()取得JVM实例标识，JMICRO启动JVM时生成此标识，并自动确保在整个微服务集群中不存在重复JMV实例标识
 * 
 * 如果多JVM中使用相同缓存，则KEY不需要加JVM标识，但此此种情况下，更新缓存时有可能存在多个JMV实例下同时更新缓存的可能，除非加入分步式锁。
 * 但是因为多JVM实例使用的是同一个数据，所以单个实例和多个实例同时更新缓存一般情况下不会存在问题，如果服务有此依赖并出现问题，应该考虑此问题的可能。
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
@Component(active=true,value="redisCache",side=Constants.SIDE_ANY)
public class RedisCacheImpl implements ICache {

	private static final Logger logger = LoggerFactory.getLogger(RedisCacheImpl.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	@Cfg("/RedisCacheImpl/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private JedisPool jeditPool;
	
	private Random r = new Random(System.currentTimeMillis()/10000);
	
	private Map<String,ICacheRefresher> refreshers = Collections.synchronizedMap(new HashMap<>());
	
	//指定KEY上次直接从源中提取数据时间，避免同小时间片内大批量对不存在的数据请求
	private Map<String,Long> notExistData = Collections.synchronizedMap(new HashMap<>());
	
	@Override
	public boolean put(String key, Object val) {
		
		if(StringUtils.isEmpty(key)) {
			logger.error("Put key cannot be NULL");
			return false;
		}
		
		if(val == null) {
			logger.error("Put value cannot be NULL");
			return false;
		}
		
		byte[] k = ICache.keyData(key);
		if(k == null) {
			return false;
		}
		
		ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(val);
		if(bb == null) {
			logger.error(val.toString() + " encode error");
			return false;
		}
		byte[] value = bb.array();
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 jedis.set(k, value);
			 return true;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
		
	}

	@Override
	public <T> T get(String key) {
		if(StringUtils.isEmpty(key)) {
			logger.error("Get key cannot be NULL");
			return null;
		}
		
		byte[] k = ICache.keyData(key);
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 byte[] val = jedis.get(k);
			if(val != null) {
				//命中缓存,理想情况下,大部份缓存都走到这里返回
				return (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(val), null);
			} else {
				//上次从源读取过相同数据,但是数据不存在,则判断和上次更新时间是否超过0.5秒,是则更新缓存，否则直接返回空
				if(notExistData.containsKey(key)) {
					long interval = System.currentTimeMillis() - notExistData.get(key);
					if(interval < 500) {
						//间隔时间太小,直接返回空,意味数据最大延迟500毫秒
						return null;
					}
					notExistData.remove(key);
				}
				
				//更新缓存并避免缓存击穿
				synchronized(key.intern()) {
					val = jedis.get(k);
					//有可能前面已经有一个线程更新缓存
					if(val == null) {
						//同一时刻只有一个线程能进来更新缓存
						boolean f = update(key);
						if(!f) {
							//数据不存在,缓本次更新时间
							notExistData.put(key, System.currentTimeMillis());
							return null;
						} else {
							val = jedis.get(k);
						}
					}
				}
				
				if(val != null) {
					return (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(val), null);
				}
				return null;
			}
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	private boolean update(String key) {
		
		ICacheRefresher ref = this.refreshers.get(key);
		if(ref == null) {
			return false;
		}
		
		Object val = ref.get(key);
		if(val == null) {
			return false;
		}
		
		return this.put(key, val);
		
	}

	@Override
	public <T> boolean put(String key, T val, long expire) {
		return put(key,val,expire,-1);
	}

	@Override
	public <T> boolean put(String key, T val, long expire, int randomVal) {
		boolean f = this.put(key, val);
		this.expire(key, expire,randomVal);
		return f;
	}

	@Override
	public boolean expire(String key, long expire) {
		if(StringUtils.isEmpty(key)) {
			logger.error("Expire key cannot be NULL");
			return false;
		}
		
		byte[] k = ICache.keyData(key);
		if( k == null) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 jedis.pexpire(k, expire);
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
		return true;
	}

	@Override
	public boolean expire(String key, long expire, int randomVal) {
		if(expire > 0 ) {
			long rv = -1;
			if(randomVal > 0) {
				rv = expire + this.r.nextInt(randomVal);
			} else {
				rv = 0;
			}
			return expire(key,expire+rv);
		}
		return false;
	}
	
	@Override
	public void setReflesher(String key, ICacheRefresher ref) {
		refreshers.put(key, ref);
	}
	

	@Override
	public boolean configRefresh(String key, long timerWithMilliseconds,long expire, int randomVal) {
		
		ICacheRefresher ref = this.refreshers.get(key);
		if(ref == null) {
			logger.error(key + " refresh not exist");
			return false;
		}
		
		//通过旋转时钟刷新缓存
		TimerTicker t = TimerTicker.getTimer(this.timers, timerWithMilliseconds).setOpenDebug(openDebug);
		
		t.addListener(key, (k,rr) -> {
			Object val = ref.get(key);
			if( val != null) {
				put(key,val,expire,randomVal);
			}
		}, r, true);
		
		return true;
	}

	@Override
	public boolean del(String key) {
		if(StringUtils.isEmpty(key)) {
			logger.error("Del key cannot be NULL");
			return false;
		}
		byte[] k = ICache.keyData(key);
		if( k == null) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 Long rst = jedis.del(k);
			this.refreshers.remove(key);
			this.notExistData.remove(key);
			return rst == 1;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
		
	}
}
