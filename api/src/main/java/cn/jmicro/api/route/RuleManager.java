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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.security.PermissionManager;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:35:21
 */
@Component(lazy=false)
public class RuleManager {
	
	private static final String RULE_DIR = Config.getRaftBasePath("") + "/routeRules/" + Config.getInstancePrefix();
	
	private RaftNodeDataListener<RouteRuleJRso> rndl = null;
	 
	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private RouterManager rm;
	
	//private volatile Map<Integer,RouteRule> rules = Collections.synchronizedMap(new HashMap<>());
	
	private volatile Map<String,List<RouteRuleJRso>> mapRules = Collections.synchronizedMap(new HashMap<>());
	
	private IRaftListener<RouteRuleJRso> ruleInfoListener = (type, node, ci) -> {
		if(ci == null) {
			ci = getRuleById(Integer.parseInt(node));
		}
		
		if(ci == null) {
			throw new NullPointerException();
		}
		
		if(!PermissionManager.checkClientPermission(Config.getClientId(), ci.getClientId())) {
			return;
		}
		if(type == IListener.ADD) {
			ruleAdd(ci);
		} else if (type == IListener.REMOVE) {
			ruleRemove(ci);
		} else if (type == IListener.DATA_CHANGE) {
			RouteRuleJRso orr = this.getRuleById(ci.getUniqueId());
			if(orr != null) {
				ruleRemove(orr);
			}
			if(ci.isEnable()) {
				ruleAdd(ci);
			}
		}
	};
	
	private void ruleRemove(RouteRuleJRso ci) {
		if(ci.isEnable()) {
			List<RouteRuleJRso> set = mapRules.get(ci.getFrom().getType());
			if(set != null && set.contains(ci)) {
				synchronized(set) {
					set.remove(ci);
				}
			}
		}
	}

	private RouteRuleJRso getRuleById(int node) {
		
		for(String key : mapRules.keySet()) {
			List<RouteRuleJRso> set = mapRules.get(key);
			if(set == null || set.isEmpty()) {
				continue;
			}
			synchronized(set) {
				for(RouteRuleJRso rr : set) {
					if(rr.getUniqueId() == node) {
						return rr;
					}
				}
			}
		}
		
		return null;
		
	}

	private void ruleAdd(RouteRuleJRso ci) {
		if(ci.isEnable()) {
			List<RouteRuleJRso> set = mapRules.get(ci.getFrom().getType());
			if(set == null) {
				set = new ArrayList<>();
				mapRules.put(ci.getFrom().getType(), set);
			}
			synchronized(set) {
				set.add(ci);
				Collections.sort(set);
			}
		}
	}

	public List<RouteRuleJRso> getRouteRulesByType(String type){
		if(mapRules.containsKey(type)) {
			return Collections.unmodifiableList(mapRules.get(type));
		}else {
			return Collections.EMPTY_LIST;
		}
		
	}
	
	List<RouteRuleJRso> getRouteRules(){
		List<RouteRuleJRso> set = new ArrayList<>();
		for(String t : rm.getRouterTypes()) {
			if(mapRules.containsKey(t)) {
				set.addAll(mapRules.get(t));
			}
		}
		Collections.sort(set);
		return set;
	}
	
	public void ready() {
		rndl = new RaftNodeDataListener<>(this.dataOperator,RULE_DIR,RouteRuleJRso.class,false);
    	rndl.addListener(ruleInfoListener);
	}
	
}
