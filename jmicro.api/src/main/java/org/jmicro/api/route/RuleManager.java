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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:35:21
 */
@Component(lazy=false)
public class RuleManager {
	
	private static final String RULE_DIR = Config.BASE_DIR+"/routeRules";

	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Cfg(value ="/RuleManager/routerSort",defGlobal=true,changeListener="addRouteType")
	private String[] routerTypes = {"tagRouter","serviceRouter","ipRouter"};
	
	
	private volatile Map<String,Set<RouteRule>> mapRules = new HashMap<>();
	
	private IDataListener dataListener = (path,data)-> {
		RouteRule rule = JsonUtils.getIns().fromJson(data, RouteRule.class);
		rule.check();
		Map<String,Set<RouteRule>> rs = mapRules;
		if(rule.isEnable()) {
			rs.get(rule.getType()).add(rule);
		}
	};
	
	public void addRouteType(String type) {
		if(!mapRules.containsKey(type)) {
			mapRules.put(type,  new HashSet<RouteRule>());
		}
	}
	
	public Set<RouteRule> getRouteRulesByType(String type){
		return mapRules.get(type);
	}
	
	public Set<RouteRule> getRouteRules(){
		Set<RouteRule> set = new HashSet<>();
		for(String t : routerTypes) {
			if(mapRules.containsKey(t)) {
				mapRules.put(t, this.mapRules.get(t));
			}
		}
		return set;
	}
	
	public void init() {
		
		for(String t : routerTypes) {
			if(!mapRules.containsKey(t)) {
				mapRules.put(t, new HashSet<RouteRule>());
			}
		}
		
		Set<String> children = dataOperator.getChildren(RULE_DIR,true);
		update(children);
	}
	
	private void update(Set<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		for(String c : children) {
			RouteRule rule = updateOne(c);
			if(rule.isEnable()) {
				mapRules.get(rule.getType()).add(rule);
			}
		}
	}

	private RouteRule updateOne(String c) {
		String p = RULE_DIR+"/"+c;
		String data = this.dataOperator.getData(p);
		RouteRule rule = JsonUtils.getIns().fromJson(data, RouteRule.class);
		rule.check();
		watchRule(p);
		return rule;
	}

	private void watchRule(String p) {
		this.dataOperator.addDataListener(p, dataListener);
	}

	public void addOrUpdate(RouteRule rule) {
		if(StringUtils.isEmpty(rule.getUniqueId())) {
			rule.setUniqueId(idGenerator.getStringId(RouteRule.class));
		}
		String path = RULE_DIR + "/" + rule.getUniqueId();
		dataOperator.createNode(path, rule.value(), false);
	}

	public String[] getRouterTypes() {
		return routerTypes;
	}
	
}
