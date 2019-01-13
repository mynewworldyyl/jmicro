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
package org.jmicro.idgenerator;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.idgenerator.IIdServer;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月3日 下午1:46:17
 */
@Component(value=Constants.DEFAULT_IDGENERATOR, level=2)
//@Service(namespace="RedisBaseIdServer", version="0.0.1")
public class JMicroRedisBaseIdGenerator implements IIdServer {
	
	private static final String ID_IDR = Constants.CFG_ROOT + "/id/";
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=RedisBaseIdServer",
				 "-Dserver=true",
				 "-Dorg.jmicro.api.idgenerator.IIdServer=uniqueIdGenerator"});
		 Utils.getIns().waitForShutdown();
	}
	
	/*@Inject(required=true)
	private Jedis redis;*/
	
	@Inject(required=true)
	private JedisPool pool;
	
	private String luaScript = null;
	
	public JMicroRedisBaseIdGenerator() {
		StringBuilder sb = new StringBuilder();
		sb.append("local k = KEYS[1];\n");
		sb.append("local cnt = ARGV[1];\n");
		sb.append("local val = tonumber(redis.call('incrby', k, cnt));\n");
		sb.append("return val;\n");
		luaScript = sb.toString();
	}
	
	public void init(){	
	}
	
	public Integer[] getIntIds(String idKey, int num) {
		Jedis r = pool.getResource();
		try {
			int endId = Integer.parseInt(r.eval(luaScript, 1, idKey,num+"").toString());
			Integer[] ids = new Integer[num];
			int oriId = endId - num;
			for(int i = 0; i < num; i++) {
				ids[i] = oriId+i;
			}
			return ids;
		}finally {
			r.close();
		}
		
	}
	
	public Long[] getLongIds(String idKey, int num) {
		Jedis r = pool.getResource();
		try {
			long endId = Long.parseLong(r.eval(luaScript, 1, idKey,num+"").toString());
			Long[] ids = new Long[num];
			long oriId = endId - num;
			for(int i = 0; i < num; i++) {
				ids[i] = oriId+i;
			}
			return ids;
		}finally {
			r.close();
		}
		
	}
	
	public String[] getStringIds(String idKey, int num) {
		Jedis r = pool.getResource();
		try {
			long endId = Long.parseLong(r.eval(luaScript, 1, idKey,num+"").toString());
			String[] ids = new String[num];
			long oriId = endId - num;
			for(int i = 0; i < num; i++) {
				ids[i] = new Long(oriId+i).toString();
			}
			return ids;
		}finally {
			r.close();
		}
		
	}
	
	@Override
	public Long getLongId(String idKey) {
		return getLongIds(idKey,1)[0];
	}

	@Override
	public String getStringId(String idKey) {
		return getStringIds(idKey,1)[0];
	}

	@Override
	public Integer getIntId(String idKey) {
		return getIntIds(idKey,1)[0];
	}
}
