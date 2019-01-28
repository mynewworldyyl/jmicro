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
@Component(value="tagRouter",lazy=false)
public class TagMatchToServiceIpPortRouter extends AbstractRouter  implements IRouter {

	private final static Logger logger = LoggerFactory.getLogger(TagMatchToServiceIpPortRouter.class);
	
	@Inject
	private RuleManager ruleManager;
	
	@Override
	public RouteRule getRoute() {
		 Set<RouteRule> rules = ruleManager.getRouteRulesByType(IRouter.TYPE_CONTEXT_PARAMS_MATCH);
		if(rules == null || rules.isEmpty()) {
			return null;
		}
		Iterator<RouteRule> ite = rules.iterator();
		while(ite.hasNext()) {
			RouteRule r = ite.next();
			if(StringUtils.isEmpty(r.getFrom().getTagKey()) || StringUtils.isEmpty(r.getFrom().getTagVal())) {
				logger.error("Invalid rule: {}",JsonUtils.getIns().toJson(r));
				ite.remove();
				if(rules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			String ctxVal = RouteUtils.getCtxParam(r.getFrom().getTagKey());
			if(StringUtils.isEmpty(ctxVal)) {
				ite.remove();
				if(rules.isEmpty()) {
					return null;
				}
				continue;
			}
			
			if(!ctxVal.equals(r.getFrom().getTagVal() )) {
				ite.remove();
				if(rules.isEmpty()) {
					return null;
				}
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
