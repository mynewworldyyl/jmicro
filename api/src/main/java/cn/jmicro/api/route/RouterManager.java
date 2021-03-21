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
package cn.jmicro.api.route;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:21:27
 *
 */
@Component(value="routerManager",lazy=false)
public class RouterManager {

	private final static Logger logger = LoggerFactory.getLogger(RouterManager.class);
	
	//@Cfg(value ="/RuleManager/routerSort",defGlobal=true/*,changeListener="addRouteType"*/)
	private String[] routerTypes = {
			RouteRule.TYPE_FROM_NONLOGIN_ROUTER,
			RouteRule.TYPE_FROM_GUEST_ROUTER,
			RouteRule.TYPE_FROM_ACCOUNT_ROUTER,
			RouteRule.TYPE_FROM_INSTANCE_ROUTER,
			RouteRule.TYPE_FROM_INSTANCE_PREFIX_ROUTER,
			/*RouteRule.TYPE_FROM_SERVICE_ROUTER,*/
			RouteRule.TYPE_FROM_TAG_ROUTER,
			RouteRule.TYPE_FROM_IP_ROUTER,
	};
	
	@Inject
	private volatile Map<String,IRouter> routers = new HashMap<>();
	
	@Inject
	private RuleManager ruleManager;
	
	public Set<ServiceItem> doRoute(Set<ServiceItem> services,String srvName,String method/*,Class<?>[] args*/
			,String namespace,String version,String transport){
		
		String routerSort = JMicroContext.get().getParam(Constants.ROUTER_TYPE, null);
		
		if(StringUtils.isNotEmpty(routerSort)) {
			routerSort = routerSort.trim();
			IRouter r = routers.get(routerSort);
			RouteRule ru = r.getRouteRule();
			if(r != null && ru != null) {
				return r.doRoute(ru, services, srvName, method, /*args, */namespace, version, transport);
			} else {
				//logger.error("Router {} not defined, try to use by default config",routerSort);
				throw new CommonException("Router ["+routerSort+"] not defined");
			}
		}
		
		for(String key: routerTypes) {
			IRouter r = this.routers.get(key);
			if(r != null) {
				RouteRule rr = r.getRouteRule();
				if(rr != null) {
					return r.doRoute(rr, services, srvName, method,/* args, */namespace, version, transport);
				}
			}
		}
		return services;
	}
	
	public String[] getRouterTypes() {
		return routerTypes;
	}
	
}
