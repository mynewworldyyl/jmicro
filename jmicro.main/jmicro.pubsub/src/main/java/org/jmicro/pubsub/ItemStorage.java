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
package org.jmicro.pubsub;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jmicro.api.cache.ICache;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.common.util.StringUtils;
import org.jmicro.pubsub.PubSubServer.SendItem;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
class ItemStorage {

	private JedisPool pool;
	
	private ICodecFactory codeFactory;
	
	ItemStorage(IObjectFactory of) {
		this.pool = of.get(JedisPool.class);
		codeFactory = of.get(ICodecFactory.class);
		
	}
	
	boolean push(SendItem item) {
		if(item == null) {
			return false;
		}
		
		Jedis j = null; 
		try {
			j = pool.getResource();
			ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(item);
			if(bb != null) {
				j.lpush(ICache.keyData(item.item.getTopic()), bb.array());
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
			return j.llen(ICache.keyData(key));
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
	SendItem pop(String key) {
		if(key == null) {
			return null;
		}
		
		Jedis j = null; 
		try {
			j = pool.getResource();
			byte[] data = j.lpop(ICache.keyData(key));
			SendItem item = (SendItem)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(data),null);
			return item;
		} finally {
			if(j != null) {
				j.close();
			}
		}
	}
	
	List<SendItem> pops(String key,long size) {
		if(key == null || size <=0) {
			return null;
		}
		
		Jedis j = null; 
		try {
			List<SendItem> l = new ArrayList<>();
			
			j = pool.getResource();

			byte[] k = ICache.keyData(key);
			
			for(;size > 0;) {
				byte[] ds = j.lpop(k);
				if(ds == null) {
					break;
				}
				SendItem item = (SendItem)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(ds),null);
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
