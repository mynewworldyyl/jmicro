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

import org.jmicro.api.AbstractObjectMapSupport;
import org.jmicro.api.IDable;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.codec.IEncodable;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:23
 */
public class RpcResponse extends AbstractObjectMapSupport implements IEncodable,IResponse,IDable{

	private long id;
	
	private transient Message msg;
	
	private Long reqId;
	
	private Object result;
	
	private boolean isMonitorEnable = false;
	
	public RpcResponse() {}
	
	public RpcResponse(long reqId,Object result){
		this.reqId = reqId;
		this.result = result;
	}
	
	public RpcResponse(long reqId){
		this.reqId = reqId;
	}
	
	@Override
	public void decode(ByteBuffer ois) {
		this.id = ois.getLong();
		this.reqId = ois.getLong();
		this.result = Decoder.decodeObject(ois);
	}

	@Override
	public void encode(ByteBuffer oos) {
		oos.putLong(this.id);
		oos.putLong(this.reqId);
		Encoder.encodeObject(oos, result);
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
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
		
}
