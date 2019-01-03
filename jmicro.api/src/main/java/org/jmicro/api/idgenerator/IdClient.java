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
package org.jmicro.api.idgenerator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月9日 下午5:23:34
 */
@Component(value="idClient",side=Constants.SIDE_ANY,level=3)
public class IdClient implements IIdGenerator{

	public static final int CACHE_SIZE = 1;
	private volatile Map<String,Queue<Object>> cache = new HashMap<>();
	
	@Reference(namespace="idServer",version="0.0.1",handler="specailInvocationHandler")
	private IIdServer idServer;
	
	@Override
	public Long[] getLongIds(String idType, int num) {
		return (Long[])getFromServer(Long.class,idType, num);
	}

	private Object getFromServer(Class<?> insType,String idType, int num) {

		ServiceItem si = JMicroContext.get().getParam(Constants.SERVICE_ITEM_KEY,null);
		if(si != null && "org.jmicro.api.idgenerator.IIdServer".equals(si.getKey().getServiceName())) {
			/*
			 * IIdServer本身的RPC也要ID，此种情况直接从ZK取，不做RPC，否则会陷入死循坏
			 */
			UniqueIdGenerator localUidGenerator = JMicro.getObjectFactory().getByName("uniqueIdGenerator");
			if(insType == Integer.class) {
				return localUidGenerator.getIntIds(idType, num);
			}else if(insType == Long.class) {
				return localUidGenerator.getLongIds(idType, num);
			}else if(insType == String.class) {
				return localUidGenerator.getStringIds(idType, num);
			}else {
				throw new CommonException("Id type ["+ insType.getName()+"] not support for ["+idType+"]");
			}
		}
		
		Map<String,Queue<Object>> lc = cache;
		Object ids = Array.newInstance(insType, num);
		
		if(lc.containsKey(idType) && lc.get(idType).size() >= num) {
			Queue<Object> q = lc.get(idType);
			synchronized(q) {
				if(q.size() >= num) {
					for(int i=0; i < num;i++) {
						Array.set(ids, i, q.poll());
					}
					return ids;
				}
			}
		}

		synchronized(idType) {
			//如果有多个线程同时等待在锁上,会多次从服务器请求多批ID并放到缓存中，
			Object[] set = null;
			if(insType == Long.class) {
				set = idServer.getLongIds(idType, num*20);
			}else if(insType == Integer.class) {
				set = idServer.getIntIds(idType, num*2);
			}else if(insType == String.class) {
				set = idServer.getStringIds(idType, num*2);
			}
			
			Queue<Object> l = lc.get(idType);
			if(l == null) {
				//只有第一个线程会走到这里
				l = new ConcurrentLinkedQueue<Object>();
				lc.put(idType, l);
			}
			l.addAll(Arrays.asList(set));

			for(int i=0; i < num;i++) {
				Array.set(ids, i, l.poll());
			}
			return ids;
		}
	}

	@Override
	public String[] getStringIds(String idType, int num) {
		return (String[])getFromServer(String.class,idType, num);
	}

	@Override
	public Integer[] getIntIds(String idType, int num) {
		return (Integer[])getFromServer(Integer.class,idType, num);
	}

	@Override
	public Long getLongId(String idType) {
		return getLongIds(idType, 1)[0];
	}

	@Override
	public String getStringId(String idType) {
		return getStringIds(idType, 1)[0];
	}

	@Override
	public Integer getIntId(String idType) {
		return getIntIds(idType, 1)[0];
	}
	
}
