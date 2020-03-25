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

import org.jmicro.common.CommonException;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午9:32:20
 */
public final class RouteRule {
	
	//just a ID used to tag in ZK path, not others
	private String uniqueId;

	private String type;
	
	//group rule in rule editor
	private String group = "Default";
	
	/**
	 * 此Rule是否启用
	 */
	private boolean enable = true;
	
	/**
	 * 优先级，值越小，优先级越高
	 */
	private int priority;
	
	private RouteEndpoint from;
	
	private RouteEndpoint to;
	
	public RouteRule() {
	}
	
	public void check() {
		if(StringUtils.isEmpty(this.getType())) {
			throw new CommonException("RouteRule ID cannot be null");
		}
	}
	
	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enable ? 1231 : 1237);
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + priority;
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public RouteEndpoint getFrom() {
		return from;
	}

	public void setFrom(RouteEndpoint from) {
		this.from = from;
	}

	public RouteEndpoint getTo() {
		return to;
	}

	public void setTo(RouteEndpoint to) {
		this.to = to;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RouteRule other = (RouteRule) obj;
		
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}
	
	

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("from=").append(this.from)
		.append(",to=").append(this.to)
		.append(",type=").append(this.type);
		return sb.toString();
	}
	
	public String value() {
		return JsonUtils.getIns().toJson(this);
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	
}
