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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:35:34
 */
public class RouteUtils {

	private RouteUtils() {}
	
	public static boolean filterByClient(String key, String val) {
		String ctxVal = JMicroContext.get().getParam(key, null);
		if(StringUtils.isEmpty(ctxVal) || StringUtils.isEmpty(key) || StringUtils.isEmpty(val)) {
			return false;
		}
		return !ctxVal.equals(val);
	}

	public static  boolean isEmpty(Collection<RouteRuleJRso> rs) {
		return rs == null || rs.isEmpty();
	}
	
	public static  boolean isFilter(String ruleVal,String val) {
		return !StringUtils.isEmpty(ruleVal) && !ruleVal.equals(val);
	}
	
	public static  boolean filterByClientTag(FromRouteEndpointJRso ep) {
		if(StringUtils.isEmpty(ep.getTagKey()) || StringUtils.isEmpty(ep.getVal())) {
			return false;
		}
		
		String ctxTagValue = JMicroContext.get().getParam(ep.getTagKey(), null);
		if(StringUtils.isEmpty(ctxTagValue)) {
			return false;
		}
		return !ctxTagValue.equals(ep.getVal());
	}
	
	public static  boolean filterByClientIpPort(FromRouteEndpointJRso ep) {
		String clientIp = JMicroContext.get().getString(JMicroContext.REMOTE_HOST, "");
		//getCtxParam(JMicroContext.REMOTE_HOST);
		Integer clientPort = JMicroContext.get().getInt(JMicroContext.LOCAL_PORT,0);
		if(clientPort == null || StringUtils.isEmpty(ep.getVal()) || StringUtils.isEmpty(clientIp)) {
			return false;
		}
		return !(clientIp+":"+clientPort).equals(ep.getVal());
	}
	
	public static RouteRuleJRso maxPriorityRule(List<RouteRuleJRso> rs) {
		if(isEmpty(rs)) {
			return null;
		}
		if(rs.size() == 1) {
			return rs.iterator().next();
		}
		Iterator<RouteRuleJRso> ite = rs.iterator();
		RouteRuleJRso r = ite.next();
		while(ite.hasNext()) {
			RouteRuleJRso rr = ite.next();
			if(r.getPriority() > rr.getPriority()) {
				r = rr;
			}
		}
		return r;
	}
}
