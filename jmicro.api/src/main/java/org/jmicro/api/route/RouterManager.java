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

import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.registry.ServiceItem;

@Component(value="routerManager")
public class RouterManager {

	@Inject
	private Set<IRouter> routers = new HashSet<>();
	
	public Set<ServiceItem> doRoute(Set<ServiceItem> services,String srvName,String method,Class<?>[] args
			,String namespace,String version,String transport){
		if(routers.isEmpty()) {
			return services;
		}
		Set<IRouter> rs = this.routers;
		for(IRouter r: rs) {
			RouteRule rr = r.getRoute();
			if(rr != null) {
				return r.doRoute(rr, services, srvName, method, args, namespace, version, transport);
			}
		}
		return services;
	}
}
