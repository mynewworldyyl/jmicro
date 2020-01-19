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
package org.jmicro.api.registry;

import org.jmicro.api.annotation.SO;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月17日
 */
@SO
final public class AsyncConfig {
	
	private boolean enable = false;
	
	private String[] condition;
	
	//异步调用服务名称
	private String serviceName;
	
	//异步调用服务名称空间
	private String namespace; 
	
	//异步调用服务版本
	private String version;
	
	//异步调用服务方法名称，参数即是目标方法的返回值
	private String method;

	public String[] getCondition() {
		return condition;
	}

	public void setCondition(String[] condition) {
		this.condition = condition;
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

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public void from(AsyncConfig r) {
		this.setEnable(r.isEnable());
		this.setCondition(r.getCondition());
		this.setMethod(r.getMethod());
		this.setNamespace(r.getNamespace());
		this.setServiceName(r.getServiceName());
		this.setVersion(r.getVersion());
	}
	
}
