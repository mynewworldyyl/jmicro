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
package org.jmicro.api.route;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.registry.ServiceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(value="routerManager")
public class RouterManager {

	private final static Logger logger = LoggerFactory.getLogger(RouterManager.class);
	
	@Inject
	private Map<String,IRouter> routers = new HashMap<>();
	
	@Cfg("/RouterManager/routerSort")
	private String[] routerSorts = {"tagRouter","serviceRouter","ipRouter"};
	
	public Set<ServiceItem> doRoute(Set<ServiceItem> services,String srvName,String method,Class<?>[] args
			,String namespace,String version,String transport){
		if(routers.isEmpty()) {
			return services;
		}
		Map<String,IRouter> rs = this.routers;
		for(String key: routerSorts) {
			IRouter r = rs.get(key);
			if(r != null) {
				RouteRule rr = r.getRoute();
				if(rr != null) {
					return r.doRoute(rr, services, srvName, method, args, namespace, version, transport);
				}
			}else {
				logger.error("Router {} not defined",key);
			}
		}
		return services;
	}
}
