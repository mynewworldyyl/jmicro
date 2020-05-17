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
package cn.jmicro.pubsub;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.common.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
class ItemStorage<T> {

	private JedisPool pool;
	
	private ICodecFactory codeFactory;
	
	private String prefix;
	
	ItemStorage(IObjectFactory of,String prefix) {
		this.pool = of.get(JedisPool.class);
		codeFactory = of.get(ICodecFactory.class);
		this.prefix = prefix;
	}
	
	boolean push(String key,T[] items,int pos, int len) {
		if(items == null) {
			return false;
		}
		
		for(int i = pos; i < len; i++) {
			push(key,items[i]);
		}
		
		return true;
	}
	
	boolean push(String key,T item) {
		if(item == null) {
			return false;
		}
		
		Jedis j = null; 
		try {
			j = pool.getResource();
			ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(item);
			if(bb != null) {
				j.lpush(ICache.keyData(this.prefix+key), bb.array());
				return true;
			} else {
				return false;
			}
			
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
	long len(String key) {
		if(StringUtils.isEmpty(key)) {
			return 0;
		}
		
		Jedis j = null; 
		try {
			j = pool.getResource();
			return j.llen(ICache.keyData(this.prefix + key));
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
	T pop(String key) {
		if(key == null) {
			return null;
		}
		
		Jedis j = null; 
		try {
			j = pool.getResource();
			byte[] data = j.lpop(ICache.keyData(this.prefix+key));
			T item = (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(data),null);
			return item;
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
	List<T> pops(String key,long size) {
		if(key == null || size <=0) {
			return null;
		}
		
		Jedis j = null; 
		try {
			List<T> l = new ArrayList<>();
			
			j = pool.getResource();

			byte[] k = ICache.keyData(this.prefix+key);
			
			for(;size > 0;) {
				byte[] ds = j.lpop(k);
				if(ds == null) {
					break;
				}
				T item = (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(ds),null);
				l.add(item);
			}
			return l;
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
}
