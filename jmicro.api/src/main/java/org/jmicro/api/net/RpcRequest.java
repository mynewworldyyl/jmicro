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

import java.nio.ByteBuffer;
import java.util.Map;

import org.jmicro.api.AbstractObjectMapSupport;
import org.jmicro.api.server.IRequest;
import org.jmicro.common.Constants;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:03
 */
public class RpcRequest extends AbstractObjectMapSupport implements IRequest{
	
	protected String serviceName;
	
	protected String method;
	
	protected Object[] args;
	
	protected String namespace;
	
	protected String version;
	
	protected String impl;
	
	private transient ISession session;
	
	protected Long reqId = -1L;
	
	private boolean isMonitorEnable = false;
	
	private transient Message msg;
	
	private transient boolean success = false;
	
	private transient boolean finish = false;
	
	public RpcRequest(){}
	
	@Override
	public void decode(ByteBuffer ois) {
		super.decode(ois);
		this.version = Decoder.decodeObject(ois);
		this.serviceName = Decoder.decodeObject(ois);
		this.method = Decoder.decodeObject(ois);
		this.namespace = Decoder.decodeObject(ois);
		this.impl = Decoder.decodeObject(ois);
		this.args = Decoder.decodeObject(ois);// (Object[])ois.readObject();
		this.reqId = ois.getLong();
		this.isMonitorEnable = ois.get() >= 1;
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer oos = super.encode();
		Encoder.encodeObject(oos, this.version);
		Encoder.encodeObject(oos, this.serviceName);
		Encoder.encodeObject(oos, this.method);
		Encoder.encodeObject(oos, this.namespace);
		Encoder.encodeObject(oos, this.impl);
		Encoder.encodeObject(oos, this.args);
		oos.putLong(this.reqId);
		oos.put(this.isMonitorEnable?(byte)1:(byte)0);
		oos.flip();
		return oos;
	}

	@Override
	public ByteBuffer newBuffer() {
		return ByteBuffer.allocate(4069);
	}
	
	public Long getMsgId(){
		if(this.msg != null){
			return this.msg.getId();
		}
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