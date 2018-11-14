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
@Component(value="ipRouter")
public class IpMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	private final static Logger logger = LoggerFactory.getLogger(IpMatchToServiceIpPortRouter.class);
	
	@Inject
	private RuleManager ruleManager;
	
	@Override
	public RouteRule getRoute() {
		Set<RouteRule> rules = ruleManager.getRouteRulesByType(IRouter.TYPE_IP_TO_IP);
		if(rules == null || rules.isEmpty()) {
			return null;
		}
		
		String clientIp = JMicroContext.get().getString(JMicroContext.CLIENT_IP, null);
		if(StringUtils.isEmpty(clientIp)) {
			return null;
		}
		
		/*int clientPort = JMicroContext.get().getInt(JMicroContext.CLIENT_PORT, 0);
		if(clientPort == 0) {
			return null;
		}*/
		
		Iterator<RouteRule> ite = rules.iterator();
		while(ite.hasNext()) {
			RouteRule r = ite.next();
			if(StringUtils.isEmpty(r.getFrom().getIpPort())) {
				//规则定义的源IP和端口是NULL，无效
				logger.error("Invalid rule: {}",JsonUtils.getIns().toJson(r));
				ite.remove();
				if(rules.isEmpty()) {
					return null;
				}
				continue;
			}
			if(!r.getFrom().getIpPort().startsWith(clientIp)) {
				//规则定义的源IP和端口不匹配当前请求客户端
				ite.remove();
				continue;
			}
		}
		return RouteUtils.maxPriorityRule(rules);
	}

	@Override
	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName, String method, Class<?>[] args,
			String namespace, String version, String transport) {
		return filterServicesByIpPort(rule,services,transport);
	}

}
