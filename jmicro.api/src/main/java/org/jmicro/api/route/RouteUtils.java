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
import org.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:35:34
 */
public class RouteUtils {

	private RouteUtils() {}
	
	public static boolean filterByClient(String key, String val) {
		String ctxVal = getCtxParam(key);
		if(StringUtils.isEmpty(ctxVal) || StringUtils.isEmpty(key) || StringUtils.isEmpty(val)) {
			return false;
		}
		return !ctxVal.equals(val);
	}

	public static  boolean isEmpty(Set<RouteRule> rs) {
		return rs == null || rs.isEmpty();
	}
	
	public static  boolean isFilter(String ruleVal,String val) {
		return !StringUtils.isEmpty(ruleVal) && !ruleVal.equals(val);
	}
	
	public static  boolean filterByClientTag(RouteEndpoint ep) {
		if(StringUtils.isEmpty(ep.getTagKey()) || StringUtils.isEmpty(ep.getTagVal())) {
			return false;
		}
		
		String ctxTagValue = getCtxParam(ep.getTagKey());
		if(StringUtils.isEmpty(ctxTagValue)) {
			return false;
		}
		return !ctxTagValue.equals(ep.getTagVal());
	}
	
	public static  boolean filterByClientIpPort(RouteEndpoint ep) {
		String clientIp = getCtxParam(JMicroContext.CLIENT_IP);
		Integer clientPort = JMicroContext.get().getInt(JMicroContext.CLIENT_PORT,0);
		if(clientPort == null || StringUtils.isEmpty(ep.getIpPort()) || StringUtils.isEmpty(clientIp)) {
			return false;
		}
		return !(clientIp+":"+clientPort).equals(ep.getIpPort());
	}
	
	public static String getCtxParam(String key) {
		return JMicroContext.get().getString(key, "");
	}
	
	public static RouteRule maxPriorityRule(Set<RouteRule> rs) {
		if(isEmpty(rs)) {
			return null;
		}
		Iterator<RouteRule> ite = rs.iterator();
		RouteRule r = ite.next();
		while(ite.hasNext()) {
			RouteRule rr = ite.next();
			if(r.getPriority() > rr.getPriority()) {
				r = rr;
			}
		}
		return r;
	}
}
