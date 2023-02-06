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
package cn.jmicro.api.gateway;

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import lombok.Serial;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:02
 *
 */
//@Serial
public final class ApiResponseJRso implements IResp {
	private Long id = -1L;
	private transient Message msg = null;
	private Long reqId =  -1L;
	private Object result = null;
	private boolean success = true;

	public ByteBuffer encode() {
		JDataOutput jo = new JDataOutput(512);
		try {
			jo.writeLong(id);
			jo.writeLong(reqId);
			jo.writeBoolean(this.success);
			
			byte[] data = null;
			
			if(result.getClass() ==  new byte[0].getClass()) {
				data = (byte[]) this.result;
			} else if(result instanceof ByteBuffer) {
				ByteBuffer bb = (ByteBuffer)result;
				data = new byte[bb.remaining()];
				bb.get(data, 0, data.length);
			} else {
				String json = JsonUtils.getIns().toJson(this.result);
				data = json.getBytes(Constants.CHARSET);
			}
			
			jo.write(data);
			
		} catch (IOException e) {
			throw new CommonException("encode error: ",e);
		}
		return jo.getBuf();
	}
	
	public void decode(ByteBuffer buf) {
		JDataInput ji = new JDataInput(buf);
		try {
			
			this.id = ji.readLong();
			this.reqId = ji.readLong();
			this.success = ji.readBoolean();
			
			byte[] data = null;
			
			if(result.getClass() ==  new byte[0].getClass()) {
				data = (byte[]) this.result;
			} else if(result instanceof ByteBuffer) {
				ByteBuffer bb = (ByteBuffer)result;
				data = new byte[bb.remaining()];
				bb.get(data, 0, data.length);
			} else {
				String json = JsonUtils.getIns().toJson(this.result);
				data = json.getBytes(Constants.CHARSET);
			}
			
		} catch (IOException e) {
			throw new CommonException("encode error: ",e);
		}
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	public Long getReqId() {
		return reqId;
	}
	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}
	public Object result() {
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
}
