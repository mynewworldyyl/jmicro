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

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午10:27:53
 */
@SO
public class FromRouteEndpoint {
	
	private String type;
	
    private String serviceName;
	
	private String namespace;
	
	private String version;
	
	private String method;
	
	private String tagKey;
	
	private String val;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	
	public String getTagKey() {
		return tagKey;
	}

	public void setTagKey(String tagKey) {
		this.tagKey = tagKey;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof RouteRule)) {
			return false;
		}
		RouteRule rr = (RouteRule)obj;
		return this.toString().equals(rr.toString());
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}


}
