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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:35:21
 */
@Component(lazy=false)
public class RuleManager {
	
	private static final String RULE_DIR = Constants.CFG_ROOT+"/"+Constants.DEFAULT_PREFIX+"/routeRules";

	@Inject
	private IDataOperator dataOperator;
	
	private volatile Map<String,RouteRule> mapRules = new HashMap<>();
	
	private IDataListener dataListener = (path,data)-> {
		RouteRule rule = JsonUtils.getIns().fromJson(data, RouteRule.class);
		rule.check();
		Map<String,RouteRule> rs = mapRules;
		if(rule.isEnable()) {
			rs.put(rule.key(), rule);
		}
	};
	
	public Set<RouteRule> getRouteRulesByType(String type){
		Iterator<RouteRule> rules = this.mapRules.values().iterator();
		Set<RouteRule> set = new HashSet<>();
		while(rules.hasNext()) {
			RouteRule r = rules.next();
			if(type.equals(r.getType())) {
				set.add(r);
			}
		}
		return set;
	}
	
	public Set<RouteRule> getRouteRules(){
		Map<String,RouteRule> mapRules = this.mapRules;
		Set<RouteRule> set = new HashSet<>();
		set.addAll(mapRules.values());
		return set;
	}
	
	public void init() {
		Set<String> children = dataOperator.getChildren(RULE_DIR,true);
		update(children);
	}
	
	private void update(Set<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		Map<String,RouteRule> rs = new HashMap<>();
		for(String c : children) {
			RouteRule rule = updateOne(c);
			if(rule.isEnable()) {
				rs.put(rule.key(), rule);
			}
		}
		this.mapRules = rs;
	}

	private RouteRule updateOne(String c) {
		String p = RULE_DIR+"/"+c;
		String data = this.dataOperator.getData(p);
		RouteRule rule = JsonUtils.getIns().fromJson(data, RouteRule.class);
		rule.setId(c);
		rule.check();
		watchRule(p);
		return rule;
	}

	private void watchRule(String p) {
		this.dataOperator.addDataListener(p, dataListener);
	}

	public void addOrUpdate(RouteRule rule) {
		String path = RULE_DIR + "/" + rule.key();
		dataOperator.createNode(path, rule.value(), false);
	}
	
}
