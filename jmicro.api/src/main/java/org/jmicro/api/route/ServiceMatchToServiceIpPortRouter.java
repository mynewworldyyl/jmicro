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

import java.util.Iterator;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value="serviceRouter",lazy=false)
public class ServiceMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	private final static Logger logger = LoggerFactory.getLogger(ServiceMatchToServiceIpPortRouter.class);
	
	@Inject
	private RuleManager ruleManager;
	
	@Override
	public RouteRule getRoute() {
		 Set<RouteRule> filterRules = ruleManager.getRouteRulesByType(IRouter.TYPE_CLIENT_SERVICE_MATCH);
		if(filterRules == null || filterRules.isEmpty()) {
			return null;
		}
		
		Iterator<RouteRule> ite = filterRules.iterator();
		while(ite.hasNext()) {
			RouteRule rr = ite.next();
			if(StringUtils.isEmpty(rr.getFrom().getMethod()) 
				&&StringUtils.isEmpty(rr.getFrom().getServiceName()) 
				&&StringUtils.isEmpty(rr.getFrom().getNamespace()) 
				&&StringUtils.isEmpty(rr.getFrom().getVersion()) ) {
				ite.remove();
				logger.error("Invalid rule: {}",JsonUtils.getIns().toJson(rr));
				//无效规则
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(this.filterByClient(ite, JMicroContext.CLIENT_METHOD, rr.getFrom().getMethod())) {
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(this.filterByClient(ite, JMicroContext.CLIENT_SERVICE, rr.getFrom().getServiceName())) {
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(this.filterByClient(ite, JMicroContext.CLIENT_NAMESPACE, rr.getFrom().getNamespace())) {
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(this.filterByClient(ite, JMicroContext.CLIENT_VERSION, rr.getFrom().getVersion())) {
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
		}	
		return RouteUtils.maxPriorityRule(filterRules);
	}

	@Override
	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName, String method, Class<?>[] args,
			String namespace, String version, String transport) {
		return filterServicesByIpPort(rule,services,transport);
	}

}
