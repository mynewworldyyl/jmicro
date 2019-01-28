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

	private String type;
	
	private String id;
	
	private RouteEndpoint from;
	
	private RouteEndpoint to;
	
	/**
	 * 此Rule是否启用
	 */
	private boolean enable = true;
	
	/**
	 * 优先级，值越小，优先级越高
	 */
	private int priority;
	
	public RouteRule() {
	}
	
	public RouteRule(String id) {
		this.id = id;
	}
	
	public void check() {
		if(StringUtils.isEmpty(this.getId())) {
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
		return this.toString().hashCode();
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof RouteRule)) {
			return false;
		}
		RouteRule rr = (RouteRule)obj;
		return this.getId().equals(rr.getId());
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("from=").append(this.from)
		.append(",to=").append(this.to)
		.append(",id=").append(this.id)
		.append(",type=").append(this.type);
		return sb.toString();
	}
	
	public String key() {
		return this.getId();
	}
	
	public String value() {
		return JsonUtils.getIns().toJson(this);
	}
	
	
}
