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

import org.jmicro.api.IDable;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public class Message implements IEncodable,IDecodable,IDable{
	
	private long msgId;
	
	private long reqId;
	
	private long sessionId;
	
	//payload length with byte,4 byte length
	private int len;
		
	//3 byte length
	private String version;
	// 1 byte
	private short type;
	
	private byte flag;
	
	//request or response
	//private boolean isReq;
	
	//2 byte length
	//private byte ext;
	
	private ByteBuffer payload;	
	
	public Message(){
	}
	
	@Override
	public void decode(ByteBuffer b) {
		
		//ByteBuffer b = ByteBuffer.wrap(data);
		this.len = b.getInt();
		if(b.remaining() < len){
			throw new CommonException("Message len not valid");
		}
		byte[] vb = new byte[3];
		b.get(vb, 0, 3);
		this.setVersion(vb[0]+"."+vb[1]+"."+vb[2]);
		
		//read type
		this.setType(b.getShort());
		this.setId(b.getLong());
		this.setReqId(b.getLong());
		this.setSessionId(b.getLong());
	    this.setFlag(b.get());
	    
		if(len > 0){
			byte[] payload = new byte[len];
			b.get(payload, 0, len);
			this.setPayload(ByteBuffer.wrap(payload));
		}else {
			this.setPayload(null);
		}
	
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = this.getPayload();
		ByteBuffer b =  null;
		if(data == null){
			b =  ByteBuffer.allocate(Constants.HEADER_LEN);
			b.putInt(0);
		} else {
			b = ByteBuffer.allocate(data.remaining() + Constants.HEADER_LEN);
			b.putInt(data.remaining());
		}
		
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
		b.putShort(this.getType());
		b.putLong(this.getId());
		b.putLong(this.reqId);
		b.putLong(this.sessionId);
		b.put(this.flag);
		
		if(data != null){
			b.put(data);
		}
		
		b.flip();
		
		return b;
	}
	
	@Override
	public long getId() {
		return this.msgId;
	}

	public byte getFlag() {
		return flag;
	}

	public void setFlag(byte flag) {
		this.flag = flag;
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
	public short getType() {
		return type;
	}
	public void setType(short type) {
		this.type = type;
	}
	
	public ByteBuffer getPayload() {
		return payload;
	}
	public void setPayload(ByteBuffer payload) {
		this.payload = payload;
	}
	
	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public long getSessionId() {
		return sessionId;
	}
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}
	
}
