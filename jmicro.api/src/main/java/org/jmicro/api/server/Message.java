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

import org.jmicro.api.IDable;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.url.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public class Message implements IEncodable,IDecodable,IDable{

	public static final int HEADER_LEN=33;
	
	public static final byte MSG_REQ_TYPE_RESP=1;
	
	public static final byte MSG_REQ_TYPE_REQ=2;
	
	public static final byte PROTOCOL_TYPE_BEGIN=1;
	public static final byte PROTOCOL_TYPE_END=2;
	
	public static final byte PROTOCOL_TYPE_REQ_ER=3;
	public static final byte PROTOCOL_TYPE_RESP_ER=4;
	
	public static final byte[] VERSION = {0,0,1};
	public static final String VERSION_STR = "0.0.1";
	
	private Long msgId;
	
	private Long reqId;
	
	private Long sessionId;
	
	//payload length with byte,4 byte length
	private int len;
		
	//3 byte length
	private String version;
	// 1 byte
	private byte type;
	
	//request or response
	private boolean isReq;
	
	//2 byte length
	private byte ext;
	
	private byte[] payload;	
	
	public Message(){
	}
	
	@Override
	public void decode(byte[] data) {
		
		ByteBuffer b = ByteBuffer.wrap(data);
		this.len = b.getInt();
		if(data.length-Message.HEADER_LEN < len){
			throw new CommonException("Message len not valid");
		}
		byte[] vb = new byte[3];
		b.get(vb, 0, 3);
		this.setVersion(vb[0]+"."+vb[1]+"."+vb[2]);
		
		//read type
		this.setType(b.get());
		
		this.setReq(b.get() == Message.MSG_REQ_TYPE_REQ);
		this.setId(b.getLong());
		this.setReqId(b.getLong());
		this.setSessionId(b.getLong());
		
		byte[] payload = new byte[len];
		b.get(payload, 0, len);
		this.setPayload(payload);
	
	}
	
	@Override
	public byte[] encode() {
		byte[] data = this.getPayload();
		ByteBuffer b = ByteBuffer.allocate(data.length + Message.HEADER_LEN);
		
		b.putInt(data.length);
		byte[] vd = null;
		if(StringUtils.isEmpty(getVersion())){
			vd = new byte[] {0,0,0};
		}else {
			String[] vs = this.version.split("\\.");
			vd = new byte[3];
			vd[0]=Byte.parseByte(vs[0]);
			vd[1]=Byte.parseByte(vs[1]);
			vd[2]=Byte.parseByte(vs[2]);
		}
		b.put(vd);
		b.put(this.getType());
		b.put(this.isReq()?Message.MSG_REQ_TYPE_REQ:Message.MSG_REQ_TYPE_RESP);
		b.putLong(this.getId());
		b.putLong(this.reqId);
		b.putLong(this.sessionId);	
		b.put(data);
		b.flip();
		return b.array();
	}
	
	@Override
	public long getId() {
		return this.msgId;
	}

	@Override
	public void setId(long id) {
		this.msgId = id;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}
	public byte getExt() {
		return ext;
	}
	public void setExt(byte ext) {
		this.ext = ext;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public boolean isReq() {
		return isReq;
	}
	public void setReq(boolean isReq) {
		this.isReq = isReq;
	}
	public Long getSessionId() {
		return sessionId;
	}
	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}
	
}
