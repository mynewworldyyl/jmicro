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
package org.jmicro.api.net;

import java.util.Map;

import org.jmicro.api.AbstractObjectMapSupport;
import org.jmicro.api.server.IRequest;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:03
 */
public final class RpcRequest extends AbstractObjectMapSupport implements IRequest{
	
	private String serviceName;
	
	private String method;
	
	private Object[] args;
	
	private String namespace;
	
	private String version;
	
	private String impl;
	
	private String transport;
	
	private transient ISession session;
	
	protected Long reqId = -1L;
	
	private boolean isMonitorEnable = false;
	
	private transient Message msg;
	
	private transient boolean success = false;
	
	private transient boolean finish = false;
	
	public RpcRequest(){}
	
	public Long getMsgId(){
		if(this.msg != null){
			return this.msg.getId();
		}
		//super.getFloat("", 33F);
		return -1l;
	}
	public boolean isFinish() {
		return finish;
	}
	
	public boolean needResponse(){
		return (this.msg.getFlag() & Constants.FLAG_NEED_RESPONSE) != 0;
	}
	
	public boolean isStream(){
		return (this.msg.getFlag() & Constants.FLAG_STREAM) != 0;
	}

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean isSuccess) {
		this.success = isSuccess;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public ISession getSession() {
		return session;
	}

	public void setSession(ISession session) {
		this.session = session;
	}
	
	public void setRequestId(Long reqId) {
		this.reqId = reqId;
	}

	@Override
	public Long getRequestId() {
		return this.reqId;
	}
	
	@Override
	public int hashCode() {
		return reqId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof RpcRequest)) {
			return false;
		}
		return reqId == ((RpcRequest)obj).reqId;
	}

	public boolean isMonitorEnable() {
		return isMonitorEnable;
	}

	public void setMonitorEnable(boolean isMonitorEnable) {
		this.isMonitorEnable = isMonitorEnable;
	}

	@Override
	public Map<String, Object> getRequestParams() {
		return this.getParams();
	}
	
	public String getImpl() {
		return impl;
	}

	public void setImpl(String impl) {
		this.impl = impl;
	}

	public void setVersion(String version){
		this.version=version;
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

	public void Namespace(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}
}