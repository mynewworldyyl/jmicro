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

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月3日 下午1:46:17
 */
@Component(value=Constants.DEFAULT_IDGENERATOR, level=2,side = Constants.SIDE_PROVIDER)
@Service(namespace="RedisBaseIdServer", version="0.0.1")
public class JMicroRedisBaseIdGenerator implements IIdServer {
	
	private static final String ID_IDR = Constants.CFG_ROOT + "/id/";
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=RedisBaseIdServer",
				 "-Dserver=true",
				 "-Dorg.jmicro.api.idgenerator.IIdServer=uniqueIdGenerator"});
		 Utils.getIns().waitForShutdown();
	}
	
	@Inject(required=true)
	private Jedis redis;
	
	public void init(){
		
	}
	
	public Integer[] getIntIds(String idKey, int num) {
		return null;
	}
	
	public Long[] getLongIds(String idKey, int num) {
		return null;
	}
	
	public String[] getStringIds(String idKey, int num) {
		return null;
	}
	
	@Override
	public Long getLongId(String idKey) {
		return 1L;
	}

	@Override
	public String getStringId(String idType) {
		return null;
	}

	@Override
	public Integer getIntId(String idKey) {
		return 0;
	}
}
