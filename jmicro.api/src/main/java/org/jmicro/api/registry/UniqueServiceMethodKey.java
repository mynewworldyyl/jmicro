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

import org.jmicro.common.CommonException;
import org.jmicro.common.util.ReflectUtils;

/**
 * 在服务标识基础上加上方法签名
 *  {@link ServiceItem}
 * @author Yulei Ye
 * @date 2018年12月2日 下午11:22:50
 */
public final class UniqueServiceMethodKey {

	public static final String SEP = UniqueServiceKey.SEP;
	public static final String PSEP = ",";
	
	private UniqueServiceKey usk = new UniqueServiceKey();
	
	private String method;
	private String paramsStr;
	
	public static String paramsStr(String[] clazzes) {
		if(clazzes == null || clazzes.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int offset = clazzes.length - 1;
		for(int i = 0; i < offset; i++){
		    sb.append(clazzes[i]).append(PSEP);
		}
		sb.append(clazzes[offset]);
		return sb.toString();
	}
	
	public static String paramsStr(Class<?>[] args) {
		if(args == null || args.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int offset = args.length - 1;
		for(int i = 0; i < offset; i++){
		    sb.append(ReflectUtils.getFullClassName(args[i])).append(PSEP);
		}
		sb.append(ReflectUtils.getFullClassName(args[offset]));
		return sb.toString();
	}
	
	public static String paramsStr(Object[] args) {
		if(args == null || args.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int offset = args.length - 1;
		for(int i = 0; i < offset; i++){
		    sb.append(ReflectUtils.getFullClassName(args[i].getClass())).append(PSEP);
		}
		sb.append(ReflectUtils.getFullClassName(args[offset].getClass()));
		return sb.toString();
	}
	
	
	public String toKey(boolean ins,boolean host,boolean port) {
		StringBuilder sb = new StringBuilder(usk.toKey(ins, host, port));
		sb.append(SEP).append(this.method).append(SEP);
		sb.append(this.paramsStr);
		return sb.toString();
	}
	
	public static UniqueServiceKey fromKey(String[] strs) {
		if(strs.length < 3 ) {
			throw new CommonException("Invalid unique service method key: " + strs);
		}
		UniqueServiceKey usk = new UniqueServiceKey();
		
		int idx = -1;
		
		usk.setServiceName(strs[++idx]);
		usk.setNamespace(strs[++idx]); 
		usk.setVersion(strs[++idx]);
		
		if(strs.length > 3) {
			usk.setInstanceName(strs[++idx]);
		}
		
		if(strs.length > 4) {
			usk.setHost(strs[++idx]);
		}
		
		if(strs.length > 5) {
			usk.setPort(Integer.parseInt(strs[++idx]));
		}
		
		return usk;
	}
	
	public static UniqueServiceMethodKey fromKey(String key) {
		String[] strs = key.split(SEP);
		if(strs.length < 3 ) {
			throw new CommonException("Invalid unique service method key: " + key);
		}
		UniqueServiceMethodKey usk = new UniqueServiceMethodKey();
		UniqueServiceKey srvUsk = fromKey(strs);
		usk.setUsk(srvUsk);
		
		int idx = 5;
		if(strs.length > 6) {
			usk.setMethod(strs[++idx]);;
		}
		
		if(strs.length > 7) {
			usk.setParamsStr(strs[++idx]);
		}
		
		return usk;
	}
	
	public String getServiceName() {
		return this.getUsk().getServiceName();
	}
	public void setServiceName(String serviceName) {
		this.getUsk().setServiceName(serviceName);
	}
	public String getNamespace() {
		return this.getUsk().getNamespace();
	}
	public void setNamespace(String namespace) {
		this.getUsk().setNamespace(namespace);
	}
	public String getVersion() {
		return this.getUsk().getVersion();
	}
	public void setVersion(String version) {
		this.getUsk().setVersion(version);
	}
	public String getInstanceName() {
		return this.getUsk().getInstanceName();
	}
	public void setInstanceName(String instanceName) {
		this.getUsk().setInstanceName(instanceName);
	}
	public String getHost() {
		return this.getUsk().getHost();
	}
	public void setHost(String host) {
		this.getUsk().setHost(host);
	}
	public int getPort() {
		return this.getUsk().getPort();
	}
	
	public void setPort(int port) {
		this.getUsk().setPort(port);
	}
	
	public UniqueServiceKey getUsk() {
		return usk;
	}

	public void setUsk(UniqueServiceKey usk) {
		this.usk = usk;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getParamsStr() {
		return paramsStr;
	}

	public void setParamsStr(String paramsStr) {
		this.paramsStr = paramsStr;
	}

	public String toString() {
		return toKey(true,true,true);
	}
	
	@Override
	public int hashCode() {
		return this.toKey(true,true,true).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof UniqueServiceMethodKey)) {
			return false;
		}
		return hashCode() == obj.hashCode();
	}

	@Override
	protected UniqueServiceMethodKey clone() throws CloneNotSupportedException {
		return (UniqueServiceMethodKey) super.clone();
	}
	
}
