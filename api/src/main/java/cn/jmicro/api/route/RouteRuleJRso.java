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

import cn.jmicro.api.annotation.SO;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午9:32:20
 */
@SO
public final class RouteRuleJRso implements Comparable<RouteRuleJRso> {
	
	public static final String TYPE_FROM_IP_ROUTER = "ipRouter";
	
	public static final String TYPE_FROM_TAG_ROUTER = "tagRouter";
	
	public static final String TYPE_FROM_SERVICE_ROUTER = "serviceRouter";
	
	public static final String TYPE_FROM_ACCOUNT_ROUTER = "accountRouter";
	
	public static final String TYPE_FROM_INSTANCE_ROUTER = "instanceRouter";
	
	public static final String TYPE_FROM_INSTANCE_PREFIX_ROUTER = "instancePrefixRouter";
	
	public static final String TYPE_FROM_GUEST_ROUTER = "guestRouter";
	
	public static final String TYPE_FROM_NONLOGIN_ROUTER = "notLoginRouter";
	
	public static final String TYPE_TARGET_INSTANCE_PREFIX = "instancePrefix";
	
	public static final String TYPE_TARGET_INSTANCE_NAME = "instanceName";
	
	public static final String TYPE_TARGET_IPPORT = "ipPort";
	
	private int clientId;
	
	private String forIns;
	
	//just a ID used to tag in ZK path, not others
	private int uniqueId;

	//group rule in rule editor
	//private String group = "Default";
	
	/**
	 * 此Rule是否启用
	 */
	private boolean enable = false;
	
	/**
	 * 优先级，值越小，优先级越高
	 */
	private int priority;
	
	private FromRouteEndpointJRso from;
	
	private String targetType;
	
    private String targetVal;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int updatedBy;
	
	private int createdBy;
	
	public RouteRuleJRso() {}
	
	@Override
	public int compareTo(RouteRuleJRso o) {
		return o.priority == this.priority?0:o.priority>this.priority?-1:1;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int hashCode() {
		return this.uniqueId;
	}

	public FromRouteEndpointJRso getFrom() {
		return from;
	}

	public void setFrom(FromRouteEndpointJRso from) {
		this.from = from;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof RouteRuleJRso)) {
			return false;
		}
		RouteRuleJRso rr = (RouteRuleJRso)obj;
		return rr.getUniqueId() == this.uniqueId;
	}

	public String value() {
		return JsonUtils.getIns().toJson(this);
	}

	public int getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(int uniqueId) {
		this.uniqueId = uniqueId;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(long updatedTime) {
		this.updatedTime = updatedTime;
	}

	public int getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(int updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getForIns() {
		return forIns;
	}

	public void setForIns(String forIns) {
		this.forIns = forIns;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getTargetVal() {
		return targetVal;
	}

	public void setTargetVal(String targetVal) {
		this.targetVal = targetVal;
	}
	
}
