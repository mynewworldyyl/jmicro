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
import java.util.Iterator;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.registry.Server;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value="ipMatchToServiceIpPortRouter")
public class IpMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	@Inject
	private RuleManager ruleManager;
	
	private Set<RouteRule> rules = null;
	
	@Override
	public RouteRule getRoute() {
		rules = ruleManager.getRouteRulesByType(IRouter.TYPE_IP_TO_IP);
		if(rules == null || rules.isEmpty()) {
			return null;
		}
		
		String clientIp = JMicroContext.get().getString(JMicroContext.CLIENT_IP, null);
		if(StringUtils.isEmpty(clientIp)) {
			return null;
		}
		
		String clientPort = JMicroContext.get().getString(JMicroContext.CLIENT_PORT, null);
		if(StringUtils.isEmpty(clientPort)) {
			return null;
		}
		
		Iterator<RouteRule> ite = this.rules.iterator();
		while(ite.hasNext()) {
			RouteRule r = ite.next();
			if(StringUtils.isEmpty(r.getFrom().getIpPort())) {
				continue;
			}
			if(r.getFrom().getIpPort().startsWith(clientIp+":"+clientPort)) {
				return r;
			}
		}	
		return null;
	}

	@Override
	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName, String method, Class<?>[] args,
			String namespace, String version, String transport) {
		return filterServicesByIpPort(rule,services,transport);
	}

}
