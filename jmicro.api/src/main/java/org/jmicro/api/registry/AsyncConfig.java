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

import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.annotation.SO;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月17日
 */
@SO
final public class AsyncConfig {
	
	//超时时做异步调用
	public static final String ASYNC_TIMEOUT="timeout";
	//熔断时做异步调用
	public static final String ASYNC_BREAK="break";
	//直接做异步调用
	public static final String 	ASYNC_DIRECT="direct";
	//不能做异步调用
	public static final String 	ASYNC_DISABLE="";
	
	private boolean enable = false;
	
	//要调用的目标方法名称
	private String forMethod;
	
	//异步条件
	private String condition;
	
	//异步调用服务名称
	private String serviceName;
	
	//异步调用服务名称空间
	private String namespace; 
	
	//异步调用服务版本
	private String version;
	
	//异步调用服务方法名称，参数即是目标方法（forMethod）的返回值
	private String method;
	
	//要调用的目标方法参数,此参数可选
	private String paramStr;
	
	//异步方法上下文，回调时回传
	private Map<String,Object> context = new HashMap<>();
	
	public AsyncConfig() {
		
	}
	
	public AsyncConfig(boolean enable,String forMethod,String condition) {
		this.enable = enable;
		this.forMethod = forMethod;
		this.condition = condition;
	}
	
	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
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
	
	public String getForMethod() {
		return forMethod;
	}

	public void setForMethod(String forMethod) {
		this.forMethod = forMethod;
	}

	public String getParamStr() {
		return paramStr;
	}

	public void setParamStr(String paramStr) {
		this.paramStr = paramStr;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public void from(AsyncConfig r) {
		this.setEnable(r.isEnable());
		this.setCondition(r.getCondition());
		this.setMethod(r.getMethod());
		this.setNamespace(r.getNamespace());
		this.setServiceName(r.getServiceName());
		this.setVersion(r.getVersion());
		this.setForMethod(r.getForMethod());
		this.setParamStr(r.getParamStr());
		this.context.putAll(r.context);
	}

	@Override
	public String toString() {
		return "AsyncConfig [enable=" + enable + ", forMethod=" + forMethod + ", condition=" + condition
				+ ", serviceName=" + serviceName + ", namespace=" + namespace + ", version=" + version + ", method="
				+ method + ", paramStr=" + paramStr + "]";
	}
	
}
