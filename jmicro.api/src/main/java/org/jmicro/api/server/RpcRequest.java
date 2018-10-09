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
package org.jmicro.api.server;

import java.nio.ByteBuffer;

import org.jmicro.api.AbstractRpcProtocolMessage;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:03
 */
public class RpcRequest extends AbstractRpcProtocolMessage implements IRequest{
	
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
		this.reqId = ois.getLong();
		this.isMonitorEnable = ois.get() >= 1;
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer oos = super.encode();
		oos.putLong(this.reqId);
		oos.put(this.isMonitorEnable?(byte)1:(byte)0);
		oos.flip();
		return oos;
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
	
}