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
import java.util.List;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.Server;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:36:24
 */
public abstract class AbstractRouter implements IRouter{

	@Inject
	protected RuleManager ruleManager;
	
	private String type;
	
	public AbstractRouter(String type) {
		this.type = type;
	}
	
	protected boolean filterByClient(Iterator<RouteRule> ite, String key, String val) {
		if(StringUtils.isEmpty(val)) {
			return false; 
		}
		String ctxVal = JMicroContext.get().getString(key, null);
		if(StringUtils.isEmpty(ctxVal) || StringUtils.isEmpty(key) || StringUtils.isEmpty(val)
				|| !ctxVal.equals(val) ) {
			ite.remove();
			return true;
		}
		return false;
	}
	
	protected HashSet<ServiceItem> filterServicesByTarget(RouteRule rule, Set<ServiceItem> services,String transport) {
	   
		HashSet<ServiceItem> items = new HashSet<>();
		
		String ipPort = rule.getTargetVal();
		
		switch(rule.getTargetType()) {
		case RouteRule.TYPE_TARGET_INSTANCE_NAME:
			for(ServiceItem si : services) {
				if(si.getKey().getInstanceName().equals(ipPort)) {
					items.add(si);
				}
			}
			break;
		case RouteRule.TYPE_TARGET_INSTANCE_PREFIX:
			for(ServiceItem si : services) {
				if(si.getKey().getInstanceName().startsWith(ipPort)) {
					items.add(si);
				}
			}
			break;
		case RouteRule.TYPE_TARGET_IPPORT:
			for(ServiceItem si : services) {
				Server s = si.getServer(transport);
				if(s == null) {
					continue;
				}
				String sipPort = s.getHost()+":"+s.getPort();
				if(sipPort.startsWith(ipPort)) {
					items.add(si);
				}
			}
			break;
		}
		
		return items;
	}
	
	@Override
	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName,
			String method,/* Class<?>[] args,*/String namespace, String version, String transport) {
		return filterServicesByTarget(rule,services,transport);
	}
	
	@Override
	public RouteRule getRouteRule() {
		List<RouteRule> rules = ruleManager.getRouteRulesByType(this.type);
		if(rules == null || rules.isEmpty()) {
			return null;
		}
		
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		UniqueServiceMethodKey key = sm.getKey();
		
		Iterator<RouteRule> ite = rules.iterator();
		while(ite.hasNext()) {
			RouteRule r = ite.next();
			
			if(!key.getServiceName().equals(r.getFrom().getServiceName())) {
				continue;
			}
			
			String val = r.getFrom().getMethod();
			if(!Utils.isEmpty(val) && !key.getMethod().contains(val)) {
				continue;
			}
			
			val = r.getFrom().getNamespace();
			if(!Utils.isEmpty(val) && !key.getNamespace().contains(val)) {
				continue;
			}
			
			val = r.getFrom().getVersion();
			if(!Utils.isEmpty(val) && !key.getVersion().contains(val)) {
				continue;
			}
			
			if(this.accept(r)) {
				return r;
			}
		}	
		return null;
	}
	
	protected abstract boolean accept(RouteRule r);
}
