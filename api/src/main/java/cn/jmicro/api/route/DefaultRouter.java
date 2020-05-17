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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.ServiceItem;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:21:12
 *
 */
@Component(active=false,value="defaultRouteManagerImpl")
public class DefaultRouter  extends AbstractRouter implements IRouter{

	@Inject
	private RuleManager ruleManager;
	
	@Override
	public RouteRule getRouteRule() {
		Set<RouteRule> rules = this.ruleManager.getRouteRules();
		if(RouteUtils.isEmpty(rules)) {
			return null;
		}
		
		Set<RouteRule> rs = filterRuleSource(rules);
		if(RouteUtils.isEmpty(rs)) {
			return null;
		}
		
		return RouteUtils.maxPriorityRule(rs);
	}

	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName, String method, Class<?>[] args,
			String namespace, String version, String transport) {
		
		Set<RouteRule> rules = this.ruleManager.getRouteRules();
		Set<RouteRule> rs = filterRuleSource(rules);
		if(RouteUtils.isEmpty(rs)) {
			return services;
		}
		
		rs = filterRuleTarget(rs,srvName,namespace,version,method);
		if(RouteUtils.isEmpty(rs)) {
			return services;
		}
		
		Iterator<RouteRule> ite = rs.iterator();
		RouteRule r = ite.next();
		while(ite.hasNext()) {
			RouteRule rr = ite.next();
			if(r.getPriority() > rr.getPriority()) {
				r = rr;
			}
		}
		
		Set<ServiceItem> srvs = new HashSet<>();
		srvs.addAll(services);
		Iterator<ServiceItem> site = srvs.iterator();
		while(site.hasNext()) {
			ServiceItem si = site.next();
			if(doFilterService(si,r)) {
				site.remove();
			}
		}
		
		return services;
	}
	
	private boolean doFilterService(ServiceItem si, RouteRule r) {
	    
		return false;
	}

	private Set<RouteRule> filterRuleTarget(Set<RouteRule> rules, String srvName
			, String namespace, String version,String method) {
		
		Set<RouteRule> filterRules =  new HashSet<>();
		filterRules.addAll(rules);
		
		Iterator<RouteRule> ite = filterRules.iterator();
		
		while(ite.hasNext()) {
			RouteRule rr = ite.next();
			//filter by target
			if(RouteUtils.isFilter(rr.getTo().getMethod(), method)) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.isFilter(rr.getTo().getServiceName(), srvName)) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.isFilter(rr.getTo().getNamespace(), namespace)) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.isFilter(rr.getTo().getVersion(), version)) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
		}
		
		return filterRules;
	}

	private Set<RouteRule> filterRuleSource(Set<RouteRule> rules) {
		
		Set<RouteRule> filterRules =  new HashSet<>();
		filterRules.addAll(rules);
		
		Iterator<RouteRule> ite = filterRules.iterator();
		while(ite.hasNext()) {
			RouteRule rr = ite.next();
			
			//filter client
			if(RouteUtils.filterByClientIpPort(rr.getFrom())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.filterByClientTag(rr.getFrom())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.filterByClient(JMicroContext.CLIENT_METHOD,rr.getFrom().getMethod())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.filterByClient(JMicroContext.CLIENT_SERVICE,rr.getFrom().getServiceName())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.filterByClient(JMicroContext.CLIENT_NAMESPACE,rr.getFrom().getNamespace())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(RouteUtils.filterByClient(JMicroContext.CLIENT_VERSION,rr.getFrom().getVersion())) {
				ite.remove();
				if(filterRules.isEmpty()) {
					return null;
				}
				continue;
			}
		}
		
		return filterRules;
	}


}
