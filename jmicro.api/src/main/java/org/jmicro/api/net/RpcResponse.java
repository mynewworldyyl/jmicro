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

import java.util.Arrays;

import org.jmicro.api.AbstractObjectMapSupport;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:23
 */
public final class RpcResponse extends AbstractObjectMapSupport implements IResponse /*IEncoder,IResponse,*/{
	
	private long id;
	
	private transient Message msg;
	
	private Long reqId;
	
	private Object result;
	
	private boolean isMonitorEnable = false;
	
	private boolean success = true;
	
	
	public RpcResponse(long reqId,Object result){
		this.reqId = reqId;
		this.result = result;
	}
	
	public RpcResponse(long reqId){
		this.reqId = reqId;
	}
	
	public RpcResponse(){
	}
	
	/*@Override
	public void decode(ByteBuffer ois) {
		super.decode(ois);
		this.id = ois.getLong();
		this.reqId = ois.getLong();
		this.success = ois.get()==0?false:true;
		this.result = Decoder.decodeObject(ois);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer bb = super.encode();
		bb.putLong(this.id);
		bb.putLong(this.reqId);
		bb.put(this.success?(byte)1:(byte)0);
		Encoder.encodeObject(bb, result);
		bb.flip();
		return bb;
	}

	@Override
	public ByteBuffer newBuffer() {
		return ByteBuffer.allocate(bufferSize);
	}*/

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public long getId() {
		// TODO Auto-generated method stub
		return id;
	}

	public void setId(long id) {
		this.id=id;
	}

	public Long getRequestId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public boolean isMonitorEnable() {
		return isMonitorEnable;
	}

	public void setMonitorEnable(boolean isMonitorEnable) {
		this.isMonitorEnable = isMonitorEnable;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Long getReqId() {
		return reqId;
	}

	@Override
	public String toString() {
		return "RpcResponse [id=" + id + ", reqId=" + reqId + ", result type=" +result.getClass().getName()  + ", result=" + (result.getClass().isArray()?Arrays.asList((Object[])result).toString():result) + ", isMonitorEnable="
				+ isMonitorEnable + ", success=" + success + "]";
	}
		
}
