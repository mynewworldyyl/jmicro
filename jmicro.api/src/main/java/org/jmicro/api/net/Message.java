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
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public class Message implements IEncodable,IDecodable,IDable{

	public static final int HEADER_LEN=34;
	
/*	public static final byte MSG_REQ_TYPE_RESP=1;
	
	public static final byte MSG_REQ_TYPE_REQ=2;
	
	public static final byte PROTOCOL_TYPE_BEGIN=1;
	public static final byte PROTOCOL_TYPE_END=2;
	
	public static final byte PROTOCOL_TYPE_REQ_ER=3;
	public static final byte PROTOCOL_TYPE_RESP_ER=4;*/
	
	//public static final short MSG_TYPE_ZERO = 0x0000;
	
	public static final short MSG_TYPE_REQ_JRPC = 0x0001; //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
	public static final short MSG_TYPE_RRESP_JRPC = 0x0002;//返回端返回IResponse
	
	//public static final short MSG_TYPE_SERVER_ASYNC_MESSAGE = 0x0003; //异步消息请求，服务器处理
	//public static final short MSG_TYPE_RRESP_RAW = 0x0004;//纯二进制数据响应
	
	public static final short MSG_TYPE_REQ_RAW = 0x0004; //纯二进制数据请求
	public static final short MSG_TYPE_RRESP_RAW = 0x0005;//纯二进制数据响应
	
	public static final short MSG_TYPE_ASYNC_REQ = 0x0006; //异步请求，不需求等待响应返回
	public static final short MSG_TYPE_ASYNC_RESP = 0x0007; //异步响应，通过回调用返回
	
	//public static final short MSG_TYPE_SERVER_ERR = 0x7FFE;
	//public static final short MSG_TYPE_ALL = 0x7FFF;
	
	public static final short MSG_TYPE_HEARBEAT_REQ = 0x7FFC; //心跳请求
	public static final short MSG_TYPE_HEARBEAT_RESP = 0x7FFD;//心跳响应
	
	public static final byte[] VERSION = {0,0,1};
	public static final String VERSION_STR = "0.0.1";
	
	//public static final byte FLAG_ASYNC = 1<<0;
	
	public static final byte FLAG_NEED_RESPONSE = 1<<1;
	
	public static final byte FLAG_STREAM = 1<<2;
	
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
			b =  ByteBuffer.allocate(Message.HEADER_LEN);
			b.putInt(0);
		} else {
			b = ByteBuffer.allocate(data.remaining() + Message.HEADER_LEN);
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
