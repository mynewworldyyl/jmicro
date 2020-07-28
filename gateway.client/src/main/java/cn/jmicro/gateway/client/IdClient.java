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
package cn.jmicro.gateway.client;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月9日 下午5:23:34
 */
public class IdClient{

	public static final int CACHE_SIZE = 1;
	
	private static final AtomicLong ID = new AtomicLong();
	
	public Long getLongId(String idType) {
		return ID.incrementAndGet();
	}

	public String getStringId(String idType) {
		return getLongId(idType).toString();
	}

	public Integer getIntId(String idType) {
		return getLongId(idType).intValue();
	}
	
/*	private volatile Map<String,Queue<Object>> cache = new HashMap<>();
	
	private ApiGatewayClient agc;
	
	public IdClient(ApiGatewayClient apiGatewayClient) {
		agc = apiGatewayClient;
	}
	
	public Long[] getLongIds(String idType, int num) {
		return (Long[])getFromServer(Long.class,idType, num);
	}

	private Object getFromServer(Class<?> insType,String idType, int num) {
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
				set = agc.getIds(idType, num*20, IdRequest.LOng);// idServer.getLongIds(idType, num*20);
			}else if(insType == Integer.class) {
				set = agc.getIds(idType, num*2, IdRequest.INteger);//idServer.getIntIds(idType, num*2);
			}else if(insType == String.class) {
				set = agc.getIds(idType, num*2, IdRequest.STring);//idServer.getStringIds(idType, num*2);
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

	public String[] getStringIds(String idType, int num) {
		return (String[])getFromServer(String.class,idType, num);
	}

	public Integer[] getIntIds(String idType, int num) {
		return (Integer[])getFromServer(Integer.class,idType, num);
	}

	public Long getLongId(String idType) {
		return getLongIds(idType, 1)[0];
	}

	public String getStringId(String idType) {
		return getStringIds(idType, 1)[0];
	}

	public Integer getIntId(String idType) {
		return getIntIds(idType, 1)[0];
	}*/
	
}
