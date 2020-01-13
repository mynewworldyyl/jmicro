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
import org.jmicro.api.cache.ICache;
import org.jmicro.api.persist.IByteDataStorage;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
@Component(active=true,value="redisBaseByteDataStorage",side=Constants.SIDE_ANY)
public class RedisBaseByteDataStorage implements IByteDataStorage {

	private static final Logger logger = LoggerFactory.getLogger(RedisBaseByteDataStorage.class);
	
	@Inject
	private JedisPool jeditPool;
	
	@Override
	public boolean save(String key, byte[] data) {
		Jedis j = null;
		try {
			j = jeditPool.getResource();
			byte[] k = ICache.keyData(key);
			j.set(k, data);
			return true;
		}catch(Throwable e) {
			logger.error("save",e);
			return false;
		}finally {
			if(j != null) {
				j.close();
			}
		}
	}

	@Override
	public byte[] get(String key) {
		Jedis j = null;
		try {
			j = jeditPool.getResource();
			byte[] k = ICache.keyData(key);
			return j.get(k);
		}finally {
			if(j != null) {
				j.close();
			}
		}
	}

}
