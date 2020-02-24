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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.JDataInput;
import org.jmicro.api.codec.JDataOutput;
import org.jmicro.api.codec.OnePrefixTypeEncoder;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public final class Message {
	
	public static final int HEADER_LEN = 10;
	
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
	
	public static final byte MSG_VERSION = (byte)1;
	
	public static final byte FLAG_PROTOCOL = 1<<0;
	
	//调试模式
	public static final byte FLAG_DEBUG_MODE = 1<<1;
	
	//需要响应的请求
	public static final byte FLAG_NEED_RESPONSE = 1<<2;
	
	//0B00111000 5---3
	public static final byte FLAG_LEVEL = 0X38;
	
	//异步消息
	public static final byte FLAG_STREAM = 1<<6;
	
	//DUMP上行数据
	public static final byte FLAG0_DUMP_UP = 1<<0;
	//DUMP下行数据
	public static final byte FLAG0_DUMP_DOWN = 1<<1;
	
	//可监控消息
	public static final byte FLAG0_MONITORABLE = 1<<2;
	
	//是否启用服务级log
	public static final short FLAG0_LOGGABLE = 1 << 3;
	
	private  transient long startTime = -1;
	
	//1 byte length
	private byte version;
		
	private long reqId;
	
	//payload length with byte,4 byte length
	//private int len;
	
	// 1 byte
	private byte type;
	
	/**
	 * dm: is development mode
	 * S: Stream
	 * N: need Response 
	 * P: protocol 0:bin, 1:json
	 * LLL: Message priority 
	 * 
	 *   S L L  L  N dm P
	 * | | | |  |  |  | |
	 * 7 6 5 4  3  2  1 0
	 * @return
	 */
	private byte flag = 0;
	
	/**
	 * up: dump up stream data
	 * do: dump down stream data
	 * M: Monitorable
	 * L: 开发日志上发 
	
	 * 
	 *         L M  do up
	 * | | | | | |  |  |
	 * 7 6 5 4 3 2  1  0
	 * @return
	 */
	private byte flag0 = 0;
	
	//request or response
	//private boolean isReq;
	
	//2 byte length
	//private byte ext;
	
	private Object payload;	
	
	
	//*****************development mode field begin******************//
	private long msgId;
	private long linkId;
	private long time;
	private String instanceName;
	private String method;
	
	//****************development mode field end*******************//
	
	public Message(){}
	
	public static boolean is(byte flag, byte mask) {
		return (flag & mask) != 0;
	}
	
	public static boolean is(byte flag, short mask) {
		return (flag & mask) != 0;
	}
	
	public boolean isDumpUpStream() {
		return is(flag0,FLAG0_DUMP_UP);
	}
	
	public void setDumpUpStream(boolean f) {
		flag0 |= f ? FLAG0_DUMP_UP : 0 ; 
	}
	
	public boolean isDumpDownStream() {
		return is(flag0,FLAG0_DUMP_DOWN);
	}
	
	public void setDumpDownStream(boolean f) {
		flag0 |= f ? Message.FLAG0_DUMP_DOWN : 0 ; 
	}
	
	public boolean isLoggable() {
		return is(flag0,FLAG0_LOGGABLE);
	}
	
	public void setLoggable(boolean f) {
		flag0 |= f ? FLAG0_LOGGABLE : 0 ; 
	}
	
	public boolean isDebugMode() {
		return is(flag,FLAG_DEBUG_MODE);
	}
	
	public void setDebugMode(boolean f) {
		flag |= f ? FLAG_DEBUG_MODE : 0 ; 
	}
	
	public boolean isMonitorable() {
		return is(flag0,FLAG0_MONITORABLE);
	}
	
	public void setMonitorable(boolean f) {
		flag0 |= f ? FLAG0_MONITORABLE : 0 ; 
	}
	
	/*public boolean isStream() {
		return is(flag,Message.FLAG_STREAM);
	}
	
	public void setStream(boolean f) {
		flag |= f ? Message.FLAG_STREAM : 0 ; 
	}*/
	
	public boolean isNeedResponse() {
		return is(flag,FLAG_NEED_RESPONSE);
	}
	
	public void setNeedResponse(boolean f) {
		flag |= f ? FLAG_NEED_RESPONSE : 0 ; 
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
	
	public  static byte getProtocolByFlag(byte flag) {
		return (byte)(flag & 0x01);
	}
	
	public byte getProtocol() {
		return getProtocolByFlag(this.flag);
	}

	public void setProtocol(byte protocol) {
		if(protocol == PROTOCOL_BIN || protocol == PROTOCOL_JSON) {
			this.flag = (byte)( protocol | (this.flag & 0xFE));
		}else {
			 new CommonException("Invalid protocol: "+protocol);
		}
	}
	
	public static Message decode(ByteBuffer b) {
		Message msg = null;
		byte flag = b.get();
		if(getProtocolByFlag(flag) == PROTOCOL_BIN) {
			msg = new Message();
			msg.flag =  flag;
			//ByteBuffer b = ByteBuffer.wrap(data);
			int len = readUnsignedShort(b);
			if(b.remaining() < len){
				throw new CommonException("Message len not valid");
			}
			
			msg.setVersion(b.get());
			
			//read type
			msg.setType(b.get());
			msg.setReqId(readUnsignedInt(b));
			msg.flag0 = b.get();
			
			if(msg.isDebugMode()) {
				//读取测试数据头部
				msg.setId(readUnsignedInt(b));
				msg.setLinkId(readUnsignedInt(b));
				msg.setTime(b.getLong());
				msg.setInstanceName(JDataInput.readString(b));
				msg.setMethod(JDataInput.readString(b));
				//减去测试数据头部长度
				len -= JDataOutput.encodeStringLen(msg.getInstanceName());
				len -= JDataOutput.encodeStringLen(msg.getMethod());
				len -= 16; //time
			}
		    
			if(len > 0){
				byte[] payload = new byte[len];
				b.get(payload, 0, len);
				msg.setPayload(ByteBuffer.wrap(payload));
			}else {
				msg.setPayload(null);
			}
		} else if(getProtocolByFlag(flag) == PROTOCOL_JSON) {
			try {
				String json = new String(b.array(),1,b.remaining(),Constants.CHARSET);
				msg = JsonUtils.getIns().fromJson(json, Message.class);
				msg.msgId = msg.msgId;
				msg.reqId = msg.reqId;
				msg.linkId = msg.linkId;
				msg.version = msg.version;
				msg.type = msg.type;
				msg.flag = msg.flag;
				msg.payload = msg.payload;
				msg.flag0 = msg.flag0;
				msg.instanceName = msg.instanceName;
				msg.method = msg.method;
				msg.time = msg.time;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			throw new CommonException("Invalid protocol: "+ msg.getProtocol());
		}
		return msg;
	}
	
	public ByteBuffer encode() {
		ByteBuffer b =  null;
		
		boolean debug = this.isDebugMode();
		if(this.getProtocol() == PROTOCOL_BIN) {
			
			ByteBuffer data = (ByteBuffer)this.getPayload();
			int len = 0;
			if(data == null){
				len = 0;
			} else {
				len = data.remaining();
			}
			
			if(debug) {
				len += JDataOutput.encodeStringLen(instanceName);
				len += JDataOutput.encodeStringLen(method);
				//3个整数ID的长度，3*4=12
				len += 16; //time
			}
			
			b = ByteBuffer.allocate(len + Message.HEADER_LEN);
			
			b.put(this.flag);
			
			writeUnsignedShort(b, len);
			//b.putShort((short)0);
			
			b.put(this.version);
			
			//writeUnsignedShort(b, this.type);
			b.put(this.type);
			
			writeUnsignedInt(b, this.reqId);
			//b.putLong(this.linkId);
			b.put(this.flag0);
			
			if(debug) {
				writeUnsignedInt(b, this.msgId);
				//b.putLong(this.msgId);
				writeUnsignedInt(b, this.linkId);
				//b.putLong(this.reqId);
				//writeUnsignedInt(b, this.time);
				b.putLong(this.time);
				
				try {
					JDataOutput.writeString(b, this.instanceName);
					JDataOutput.writeString(b, this.method);
				} catch (IOException e) {
					throw new CommonException("",e);
				}
				
				//OnePrefixTypeEncoder.encodeString(b, this.instanceName);
				//OnePrefixTypeEncoder.encodeString(b, this.method);
			}
			
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
	
	public static Message readMessage(ByteBuffer cache){
		
		//保存读数据位置
		int pos = cache.position();
		
		//数据总长是否可构建一个包的最小长度
		int totalLen = cache.remaining();
		if(totalLen < Message.HEADER_LEN) {
			//可读的数据长度小于头部长度
			return null;
		}
		
		//取第一个字节标志位
		//byte f = cache.get();
				
		//取第二，第三个字节 数据长度
		cache.position(pos+1);
		int len = Message.readUnsignedShort(cache);
		//还原读数据公位置
		cache.position(pos);
				
		if(totalLen < len + Message.HEADER_LEN){
			//还不能构成一个足够长度的数据包
			return null;
		}
		
		byte[] data = new byte[len+Message.HEADER_LEN];
		//从缓存中读一个包,cache的position往前推
		cache.get(data, 0, len+Message.HEADER_LEN);
		
		return Message.decode(ByteBuffer.wrap(data));
        
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

	public byte getFlag() {
		return flag;
	}

	public byte getFlag0() {
		return flag0;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	@Override
	public String toString() {
		return "Message [version=" + version + ", msgId=" + msgId + ", reqId=" + reqId + ", linkId=" + linkId 
				+ ", type=" + type + ", flag=" + Integer.toHexString(flag) + ", flag0=" + Integer.toHexString(flag0) 
				+ ", payload=" + payload + ", time="+ time 
				+ ", devMode=" + this.isDebugMode() + ", monitorable="+ this.isMonitorable() 
				+ ", needresp="+ this.isNeedResponse()
				+ ", upstream=" + this.isDumpUpStream() + ", downstream="+ this.isDumpDownStream() 
				+ ", instanceName=" + instanceName + ", method=" + method + "]";
	}

	
}
