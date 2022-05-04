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
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:21:55
 *
 */
@SO
public final class ApiRequestJRso implements IReq {
	
	private Map<String,Object> params = new HashMap<>();
	
	//private String serviceName = "";
	
	//private String namespace = "";
	
	//private String version = "";
	
	//private String method = "";
	
	private Object[] args = null;
	
	private Long reqId = -1L;
	
	//private int impCode = 0;
	//private transient ServiceMethod sm;
	
	private transient Message msg = null;
	
	public ByteBuffer encode() {
		JDataOutput jo = new JDataOutput(1024);
		
		try {
			jo.writeLong(this.reqId);
			
			jo.writeInt(params.size());
			
			for(Map.Entry<String, Object> e : params.entrySet()) {
				jo.writeUTF(e.getKey());
				if(e.getValue() == null) {
					jo.writeUTF("");
				} else {
					jo.writeUTF(JsonUtils.getIns().toJson(e.getValue()));
				}
			}
			
			if(args == null || args.length == 0) {
				jo.writeInt(0);
			}else {
				jo.writeInt(args.length);
				for(Object a : args) {
					if(a == null) {
						throw new CommonException("Argument cannot be null");
					}
					
					Class<?> cls = a.getClass();
					
					if(cls == String.class) {
						jo.writeUTF(a.toString());
					}else if(cls == Boolean.class || cls == Boolean.TYPE) {
						Boolean b = (Boolean)a;
						if(b) {
							jo.writeByte(1);
						}else {
							jo.writeByte(0);
						}
					}else if(cls == Byte.class || cls == Byte.TYPE 
							|| cls == Short.class || cls == Short.TYPE
							|| cls == Integer.class || cls == Integer.TYPE
							|| cls == Long.class || cls == Long.TYPE
							|| cls == Double.class || cls == Double.TYPE
							|| cls == Float.class || cls == Float.TYPE
							){
						jo.writeLong(Long.parseLong(a.toString()));
					}else if(cls == new byte[0].getClass()) {
						byte[] data = (byte[])a;
						jo.writeInt(data.length);
						jo.write(data, 0, data.length);
					}else if(cls == ByteBuffer.class) {
						ByteBuffer data = (ByteBuffer)a;
						jo.writeInt(data.remaining());
						jo.write(data.array(), data.position(), data.remaining());
					}
				}
			}
		} catch (IOException e) {
			throw new CommonException("",e);
		}
		
		return jo.getBuf();
	}
	
	public Map<String, Object> getParams() {
		return params;
	}
	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
	
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	
	public Long getReqId() {
		return reqId;
	}
	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
	
}
