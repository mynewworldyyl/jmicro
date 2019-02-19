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
package org.jmicro.client;

import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.route.RouterManager;
import org.jmicro.common.Constants;
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
	
	private int next = 0;
	
	@SuppressWarnings("null")
	@Override
	public ServiceItem getService(String srvName,String method,Class<?>[] args,String namespace,String version,
			String transport) {
		
		ServiceItem dsi = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
		if(dsi != null) {
			return dsi;
		}
		
		Set<ServiceItem> srvItems = registry.getServices(srvName,method,args,namespace,version,transport);
		if(srvItems == null || srvItems.isEmpty()) {
			return null;
		}
		
		if(routerManager != null) {
			srvItems = this.routerManager.doRoute(srvItems, srvName, method, args, namespace, version, transport);
		}
		
		if(srvItems == null || srvItems.isEmpty()) {
			return null;
		}
		
		if(srvItems.size() == 1) {
			return srvItems.iterator().next();
		}
		
		return doBalance(srvItems);
	
	}

	private ServiceItem doBalance(Set<ServiceItem> srvItems) {
		ServiceItem[] arr = new ServiceItem[srvItems.size()];
		srvItems.toArray(arr);
		int next = this.next++%arr.length;
		return arr[next];
	}

	@Override
	public ServiceItem getService(String srvName, String method, Object[] args,String namespace,String version,
			String transport) {
		Class<?>[] clazzes = null;
		if(args != null && args.length > 0){
			int i = 0;
			clazzes = new Class<?>[args.length];
			for(Object a : args){
				clazzes[i++] = a.getClass();
			}
		} else {
			clazzes = new Class<?>[0];
		}
		return this.getService(srvName, method, clazzes,namespace,version,transport);
	}
	
}
