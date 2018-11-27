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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public final class Message {
	
	public static final int HEADER_LEN = 17;
	
	public static final byte PROTOCOL_BIN = 0;
	public static final byte PROTOCOL_JSON = 1;
	
	public static final byte PRIORITY_0 = 0;
	public static final byte PRIORITY_1 = 1;
	public static final byte PRIORITY_2 = 2;
	public static final byte PRIORITY_3 = 3;
	public static final byte PRIORITY_4 = 4;
	public static final byte PRIORITY_5 = 5;
	public static final byte PRIORITY_6 = 6;
	public static final byte PRIORITY_7 = 7;
	
	public static final byte PRIORITY_MIN = PRIORITY_0;
	public static final byte PRIORITY_NORMAL = PRIORITY_3;
	public static final byte PRIORITY_MAX = PRIORITY_7;
	
	//1 byte length
	private byte version;
		
	private long msgId;
	
	private long reqId;
	
	private long linkId;
	
	//payload length with byte,4 byte length
	private int len;
	
	// 1 byte
	private byte type;
	
	/**
	 * L: Level
	 * M: Monitorable
	 * S: Stream
	 * N: need Response 
	 * P: protocol 0:bin, 1:json
	 * 
	 *   P L L L M S N
	 *   | | | | | | |
	 * 0 0 0 0 0 0 0 0
	 * @return
	 */
	private byte flag = 0;
	
	//request or response
	//private boolean isReq;
	
	//2 byte length
	//private byte ext;
	
	private Object payload;	
	
	public Message(){}
	
	public boolean isMonitorable() {
		return (flag & Constants.FLAG_MONITORABLE) != 0;
	}
	
	public void setMonitorable(boolean f) {
		flag |= f ? Constants.FLAG_MONITORABLE : 0 ; 
	}
	
	public boolean isStream() {
		return (flag & Constants.FLAG_STREAM) != 0;
	}
	
	public void setStream(boolean f) {
		flag |= f ? Constants.FLAG_STREAM : 0 ; 
	}
	
	public boolean isNeedResponse() {
		return (flag & Constants.FLAG_NEED_RESPONSE) != 0;
	}
	
	public void setNeedResponse(boolean f) {
		flag |= f ? Constants.FLAG_NEED_RESPONSE : 0 ; 
	}
	
	public int getLevel() {
		return (byte)((flag >>> 3) & 0x07);
	}
	
	public void setLevel(int l) {
		if(l > PRIORITY_7 || l < PRIORITY_0) {
			 new CommonException("Invalid priority: "+l);
		}
		this.flag = (byte)((l << 3) | this.flag);
	}
	
	public byte getProtocol() {
		return (byte)((flag >>> 7) & 0x01);
	}

	public void setProtocol(byte protocol) {
		if(protocol == PROTOCOL_BIN || protocol == PROTOCOL_JSON) {
			this.flag = (byte)((protocol << 7) | this.flag);
		}else {
			 new CommonException("Invalid protocol: "+protocol);
		}
	}
	
	
	public void decode(ByteBuffer b) {
		
		this.flag = b.get();
		if(this.getProtocol() == PROTOCOL_BIN) {
			//ByteBuffer b = ByteBuffer.wrap(data);
			this.len = readUnsignedShort(b);
			if(b.remaining() < len){
				throw new CommonException("Message len not valid");
			}
			
			this.setVersion(b.get());
			
			//read type
			this.setType(b.get());
			this.setId(readUnsignedInt(b));
			this.setReqId(readUnsignedInt(b));
			this.setLinkId(readUnsignedInt(b));
		    
			if(len > 0){
				byte[] payload = new byte[len];
				b.get(payload, 0, len);
				this.setPayload(ByteBuffer.wrap(payload));
			}else {
				this.setPayload(null);
			}
		} else if(this.getProtocol() == PROTOCOL_JSON) {
			try {
				String json = new String(b.array(),1,b.remaining(),Constants.CHARSET);
				Message msg = JsonUtils.getIns().fromJson(json, Message.class);
				this.msgId = msg.msgId;
				this.reqId = msg.reqId;
				this.linkId = msg.linkId;
				this.len = msg.len;
				this.version = msg.version;
				this.type = msg.type;
				this.flag = msg.flag;
				this.payload = msg.payload;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			throw new CommonException("Invalid protocol: "+ this.getProtocol());
		}

	}
	
	public ByteBuffer encode() {
		ByteBuffer b =  null;
		if(this.getProtocol() == PROTOCOL_BIN) {
			
			ByteBuffer data = (ByteBuffer)this.getPayload();
			int len = 0;
			if(data == null){
				b =  ByteBuffer.allocate(Message.HEADER_LEN);
				len = 0;
			} else {
				b = ByteBuffer.allocate(data.remaining() + Message.HEADER_LEN);
				len = data.remaining();
			}
			
			b.put(this.flag);
			
			writeUnsignedShort(b, len);
			//b.putShort((short)0);
			
			b.put(this.version);
			
			//writeUnsignedShort(b, this.type);
			b.put(this.type);
			
			writeUnsignedInt(b, this.msgId);
			//b.putLong(this.msgId);
			
			writeUnsignedInt(b, this.reqId);
			//b.putLong(this.reqId);
			
			writeUnsignedInt(b, this.linkId);
			//b.putLong(this.linkId);
			
			if(data != null){
				b.put(data);
			}
			
		} else {
			String json = JsonUtils.getIns().toJson(this);
			try {
				byte data[] = json.getBytes(Constants.CHARSET);
				b = ByteBuffer.allocate(data.length+1);
				b.put(PROTOCOL_JSON);
				b.put(data,0,data.length);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		b.flip();

		return b;
	}
	
	public static ByteBuffer readMessage(ByteBuffer cache){
		
		//当前写的位置，也就是可读的数据长度
		int totalLen = cache.position();
		if(totalLen < Message.HEADER_LEN) {
			//可读的数据长度小于头部长度
			return null;
		}
		
		//保存写数据位置
		int pos = cache.position();
		cache.position(1);
		//读数据长度
		int len = Message.readUnsignedShort(cache);
		//还原写数据公位置
		cache.position(pos);
		
		if(totalLen < len+Message.HEADER_LEN){
			//还不能构成一个足够长度的数据包
			return null;
		}
		
		//准备读数据
		cache.flip();
		
		ByteBuffer body = ByteBuffer.allocate(len+Message.HEADER_LEN);
		body.put(cache);
		body.flip();
		
		//准备下一次读
		/**
		  System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
	      position(remaining());
	      limit(capacity());
	      discardMark();
	      return this;
		 */
		//将剩余数移移到缓存开始位置，position定位在数据长度位置，处于写状态
		cache.compact();
		//b.position(b.limit());
		//cache.limit(cache.capacity());
		
		return body;
	}
	
	public static void writeUnsignedShort(ByteBuffer b,int v) {
		byte data = (byte)((v >>> 8) & 0xFF);
		b.put(data);
		data = (byte)((v >>> 0) & 0xFF);
		b.put(data);
	}
	
	 public static int readUnsignedShort(ByteBuffer b) {
		int firstByte = (0xFF & ((int)b.get()));
		int secondByte = (0xFF & ((int)b.get()));
		char anUnsignedShort  = (char) (firstByte << 8 | secondByte);
        return anUnsignedShort;
	 }
	
	public static void writeUnsignedByte(ByteBuffer b,short v) {
		byte vv = (byte)((v >>> 0) & 0xFF);
		b.put(vv);
	}
	
	public static short readUnsignedByte(ByteBuffer b) {
		short vv = (short) (b.get() & 0xff);
	    return vv;
	}
    
    public static long readUnsignedInt(ByteBuffer b) {
    	int firstByte = (0xFF & ((int)b.get()));
    	int secondByte = (0xFF & ((int)b.get()));
    	int thirdByte = (0xFF & ((int)b.get()));
    	int fourthByte = (0xFF & ((int)b.get()));
 	    long anUnsignedInt  = 
 	    		((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte))
 	    		& 0xFFFFFFFFL;
 	    return anUnsignedInt;
    }
    
    public static void writeUnsignedInt(ByteBuffer b,long v) {
		b.put((byte)((v >>> 24)&0xFF));
		b.put((byte)((v >>> 16)&0xFF));
		b.put((byte)((v >>> 8)&0xFF));
		b.put((byte)((v >>> 0)&0xFF));
	}
    
	public long getId() {
		return this.msgId;
	}

	public void setId(long id) {
		this.msgId = id;
	}

	public byte getVersion() {
		return version;
	}
	public void setVersion(byte version) {
		this.version = version;
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}
	
	public Object getPayload() {
		return payload;
	}
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}
}
