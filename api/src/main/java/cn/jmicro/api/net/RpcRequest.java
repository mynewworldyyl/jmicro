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
package cn.jmicro.api.net;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.monitor.v1.MonitorConstant;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:03
 */
@SO
public final class RpcRequest implements IRequest{
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	
	private String serviceName;
	
	private String method;
	
	private Object[] args;
	
	private String namespace;
	
	private String version;
	
	private String impl;
	
	private String transport;
	
	protected long reqId = -1L;
	
	private transient ISession session;
	
	private transient boolean isMonitorEnable = false;
	
	private transient Message msg;
	
	private transient boolean success = false;
	
	private transient boolean finish = false;
	
	public RpcRequest(){}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.params.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	public int getLogLevel() {
		if(msg != null) {
			return msg.getLogLevel();
		}
		return MonitorConstant.LOG_NO;
	}
	
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
		return this.msg.isNeedResponse();
	}
	
	/*public boolean isStream(){
		return this.msg.isStream();
	}*/

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
	
	@Override
	public void setRequestId(long reqId) {
		this.reqId = reqId;
	}

	@Override
	public long getRequestId() {
		return this.reqId;
	}
	
	@Override
	public int hashCode() {
		return (int)reqId;
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
	public Map<String,Object> getParams(){
		return this.params;
	}
	
	//public abstract ByteBuffer newBuffer();
	
	public Integer getInt(String key,int defautl){
		return this.getParam(key,defautl);
	}
	
	public String getString(String key,String defautl){
		return this.getParam(key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return this.getParam(key,defautl);
	}
	
	public Float getFloat(String key,Float defautl){
		return this.getParam(key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return this.getParam(key,defautl);
	}
	
	public Object getObject(String key,Object defautl){
		return this.getParam(key,defautl);
	}
	
	public void putObject(String key,Object value){
		 this.params.put(key, value);
	}
	@Override
	public String toString() {
		return "RpcRequest [serviceName=" + serviceName + ", method=" + method + ", args=" + Arrays.toString(args)
				+ ", namespace=" + namespace + ", version=" + version + ", impl=" + impl + ", transport=" + transport
				+ ", reqId=" + reqId + "]";
	}
	
}