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
package cn.jmicro.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.loadbalance.ISelector;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.route.RouterManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:03:10
 */
@Component(Constants.DEFAULT_SELECTOR)
public class RoundBalance implements ISelector{

	@Inject(required=true,value=Constants.DEFAULT_REGISTRY)
	private IRegistry registry;
	
	@Inject
	private RouterManager routerManager;
	
	private Map<String,Integer> indexMap = new ConcurrentHashMap<>();
	
	public ServiceItem getService(String srvName,String method,/*Class<?>[] args,*/String namespace,String version,
			String transport) {
		
		//客户端指定了服务实例，不需要做路由
		ServiceItem dsi = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
		if(dsi != null) {
			return dsi;
		}
		
		Set<ServiceItem> srvItems = registry.getServices(srvName,method,/*args,*/namespace,version,transport);
		if(srvItems == null || srvItems.isEmpty()) {
			return null;
		}
		
		long curTime = TimeUtils.getCurTime();
		Iterator<ServiceItem> ite = srvItems.iterator();
		for(;ite.hasNext();) {
			//加载时间小于5秒的服务不使用
			ServiceItem si = ite.next();
			if(/*si.getLoadTime() - si.getCreatedTime() > 300000 ||  */curTime - si.getLoadTime() < 5000) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if(routerManager != null) {
			srvItems = this.routerManager.doRoute(srvItems, srvName, method,/* args, */namespace, version, transport);
		}
		
		if(srvItems == null || srvItems.isEmpty()) {
			return null;
		}
		
		if(srvItems.size() == 1) {
			return srvItems.iterator().next();
		}
		
		String srvKey = curIndex(srvName,namespace,version,method);
		
		return doBalance(srvItems,srvKey);
	
	}

	private String curIndex(String srvName, String namespace, String version, String method) {
		return srvName+UniqueServiceKey.SEP+namespace+UniqueServiceKey.SEP+version+UniqueServiceKey.SEP+method;
	}

	private ServiceItem doBalance(Set<ServiceItem> srvItems,String srvKey) {
		ServiceItem[] arr = new ServiceItem[srvItems.size()];
		srvItems.toArray(arr);
		
		if(!this.indexMap.containsKey(srvKey)) {
			indexMap.put(srvKey, 0);
		}
		
		int idx = indexMap.get(srvKey);
		
		idx %= arr.length;
		
		indexMap.put(srvKey, idx+1);
		
		return arr[idx];
	}

}
