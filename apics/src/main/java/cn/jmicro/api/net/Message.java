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
package cn.jmicro.api.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.codec.DecoderConstant;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Messge format:
 * 
 * +++ 2 bytes （flag） +++  2 or 4 bytes （len） +++ 1 byte （type） +++ 4 bytes （extra flag） +++ 2 bytes extra data len +++ extra data +++ payload data +++
 
 * a. 2 bytes flag: 固定2字节不变
 * b. 2 or 4 bytes len 根据Message.FLAG_LENGTH_INT值确定是2字节还是4字节，1表示4字节，0表示两字节，len的值等于 4（如果存在，extra flag长度）+
 * 2（如果存在，extra长度） + extra data长度（如果存在） + data长度
 
 	如FLAG_EXTRA=1,则包含以下信息
 * c. 4 bytes （extra flag） 
 * d. 2 bytes extra data len  表示附加数据长度
 * e. extra data
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
@IDStrategy(100)
@Getter
@Setter
public final class Message {
	
	public static final int HEADER_LEN = 13; // 2(flag)+2(data len with short)+1(type)
	
	public static final int EXT_HEADER_LEN = 2; //附加数据头部长度
	
	//public static final int SEC_LEN = 128;
	
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
	
	public static final int MAX_SHORT_VALUE = Short.MAX_VALUE;
	
	public static final short MAX_BYTE_VALUE = Byte.MAX_VALUE;
	
	public static final long MAX_INT_VALUE = 1024*1024*10;//10M
	
	//public static final long MAX_LONG_VALUE = Long.MAX_VALUE*2;
	
	//public static final byte MSG_VERSION = (byte)1;
	
	//长度字段类型，1表示整数，0表示短整数
    public static final short FLAG_LENGTH_INT = 1 << 0;
    
	public static final short FLAG_UP_PROTOCOL = 1<<1;
	public static final short FLAG_DOWN_PROTOCOL = 1 << 2;
	
	//可监控消息
	public static final short FLAG_MONITORABLE = 1 << 3;
	
	//包含Extra数据
	public static final short FLAG_EXTRA = 1 << 4;
	
	//来自外网消息，由网关转发到内网
	public static final short FLAG_OUT_MESSAGE = 1 << 5;
	
	public static final short FLAG_ERROR = 1 << 6;
	
	//需要响应的请求  or down message is error
	public static final short FLAG_FORCE_RESP_JSON = 1 << 7;
	
	public static final short FLAG_RESP_TYPE = 11;
	
	public static final short FLAG_LOG_LEVEL = 13;
	
	/****************  extra constants flag   *********************/
	
	//调试模式
	public static final int EXTRA_FLAG_DEBUG_MODE = 1 << 0;
		
	public static final int EXTRA_FLAG_PRIORITY = 1; 
		
	//DUMP上行数据
	public static final int EXTRA_FLAG_DUMP_UP = 1 << 3;
	
	//DUMP下行数据
	public static final int EXTRA_FLAG_DUMP_DOWN = 1 << 4;
	
	//加密参数 0：没加密，1：加密
	public static final int EXTRA_FLAG_UP_SSL = 1 << 5;
	
	//是否签名
	public static final int EXTRA_FLAG_DOWN_SSL = 1 << 6;
	
	public static final int EXTRA_FLAG_IS_SEC = 1 <<8;
	
	//是否签名： 0:无签名； 1：有签名
	public static final int EXTRA_FLAG_IS_SIGN = 1 << 9;
		
	//加密方式： 0:对称加密，1：RSA 非对称加密
	public static final int EXTRA_FLAG_ENC_TYPE = 1 << 10;
	
	public static final int EXTRA_FLAG_RPC_MCODE = 1 << 11;
	
	public static final int EXTRA_FLAG_SECTET_VERSION = 1 << 12;
	
	//是否包含实例ID
	public static final int EXTRA_FLAG_INS_ID = 1 << 13;
	
	public static final Byte EXTRA_KEY_LINKID = -127;
	public static final Byte EXTRA_KEY_INSID = -126;
	public static final Byte EXTRA_KEY_TIME = -125;
	public static final Byte EXTRA_KEY_SM_CODE = -124;
	public static final Byte EXTRA_KEY_SM_NAME = -123;
	public static final Byte EXTRA_KEY_SIGN = -122;
	public static final Byte EXTRA_KEY_SALT = -121;
	public static final Byte EXTRA_KEY_SEC = -120;
	public static final Byte EXTRA_KEY_LOGIN_KEY = -119;
	
	//public static final Byte EXTRA_KEY_ARRAY = -116;
	public static final Byte EXTRA_KEY_FLAG = -118;
	
	public static final Byte EXTRA_KEY_MSG_ID = -117;
	
	//rpc method name
	public static final Byte EXTRA_KEY_METHOD = 127;
	public static final Byte EXTRA_KEY_EXT0 = 126;
	public static final Byte EXTRA_KEY_EXT1 = 125;
	public static final Byte EXTRA_KEY_EXT2 = 124;
	public static final Byte EXTRA_KEY_EXT3 = 123;
	
	public static final byte MSG_TYPE_PINGPONG = 0;//默认请求响应模式
	
	public static final byte MSG_TYPE_NO_RESP = 1;//单向模式
	
	public static final byte MSG_TYPE_MANY_RESP = 2;//多个响应模式，如消息订阅
	
	/****************  extra constants flag   *********************/
	
	//0B00111000 5---3
	//public static final short FLAG_LEVEL = 0X38;
	
	//是否启用服务级log
	//public static final short FLAG_LOGGABLE = 1 << 3;
	private transient long startTime = -1;
	
	//此消息所占字节数，用于记录流量
	private transient int len = -1;
	
	//1 byte length
	//private byte version;
	
	//payload length with byte,4 byte length
	//private int len;
	
	/**
	 * 0        S:       data length type 0:short 1:int
	 * 1        UPR:     up protocol  0: bin,  1: json 
	 * 2        DPR:     down protocol 0: bin, 1: json 
	 * 3        M:       Monitorable
	 * 4        Extra    Contain extradata
	 * 5        Innet    message from outer network
	 * 6        
	 * 7       
	 * 8       
	 * 9        
	 * 10
	 * 11，12   Resp type  MSG_TYPE_PINGPONG，MSG_TYPE_NO_RESP，MSG_TYPE_MANY_RESP
	 * 13 14 15 LLL      Log level
	 * @return
	 */
	private int flag = 0;
	
	// 1 byte
	private byte type;
		
	//2 byte length
	//private byte ext;
	
	//normal message ID	or JRPC request ID
	private long msgId;
	
	private Object payload;
	
	private Map<Byte,Object> extraMap;
	
	//*****************extra data begin******************//
	
	/**
	 * 0        dm:       is development mode EXTRA_FLAG_DEBUG_MODE = 1 << 1;
	 * 1,2      PP:       Message priority   EXTRA_FLAG_PRIORITY
	 * 3        up:       dump up stream data
	 * 4        do:       dump down stream data
	 * 5 	    US        上行SSL  0:no encrypt 1:encrypt
	 * 6        DS        下行SSL  0:no encrypt 1:encrypt
	 * 7        
	 * 8        MK        RPC方法编码       
	 * 9        SV        对称密钥版本
	 * 10       SE        密码
	 * 11       SI        是否有签名值 0：无，1：有
	 * 12       ENT       encrypt type 0:对称加密，1：RSA 非对称加密
	 * 13
	             ENT SI  SE  WE MK SV  DS   US   DO   UP  P    P   dm   
	 |    |   |   |  |   |   |  |  |   |    |    |    |   |    |   |
     15  14  13  12  11  10  9  8  7   6    5    4    3   2    1   0
     
	 |    |   |   |  |   |   |    |   |   |    |    |    |   |    |   |
     31  30  29  28  27  26  25   24  23  22   21   20   19  18   17  16
     
	 * 
	 * @return
	 */
	private transient int extrFlag = 0;
	
	//附加数居
	private transient ByteBuffer extra = null;
		
	//非对称加密签名
	//private transient String signData;
	
	//对称加密盐值
	//private transient  byte[] saltData;
	
	//对称加密密钥,i 
	//private transient  byte[] secData;
	
	//private long msgId;
	//private transient  long linkId;
	
	//消息发送时间
	//private transient  long time;
	
	//private String instanceName;
	//private transient  String method;
	
	//rpc method code
	//private transient  int smKeyCode;
	
	//instance ID for jvm
	//private transient  int insId;
		
	//****************extra data begin*******************//
	
	public Message(){}
	
	private static Map<Byte,Object> decodeExtra(ByteBuffer extra) {
		if(extra == null || extra.remaining() == 0) return null;
		
		Map<Byte,Object> ed = new HashMap<>();
		
		JDataInput b = new JDataInput(extra);
		try {
			while(b.remaining() > 0) {
				Byte k = b.readByte();
				Object v = decodeVal(b,k);
				ed.put(k, v);
			}
			return ed;
		} catch (IOException e) {
			throw new CommonException("decodeExtra error:" + ed.toString() + ", Extra: " + extra);
		}
	}
	
	public static Object decodeVal(JDataInput b,Byte k) throws IOException {
		byte type = b.readByte();
		
		if(type == DecoderConstant.PREFIX_TYPE_NULL) {
			return null;
		}else if(DecoderConstant.PREFIX_TYPE_LIST == type){
			int len = b.readUnsignedShort();
			if(len == 0) {
				return new byte[0];
			}
			byte[] arr = new byte[len];
			b.readFully(arr, 0, len);
			return arr;
		}else if(type == DecoderConstant.PREFIX_TYPE_INT){
			return b.readInt();
		}else if(DecoderConstant.PREFIX_TYPE_BYTE == type){
			return b.readByte();
		}else if(DecoderConstant.PREFIX_TYPE_SHORTT == type){
			return b.readUnsignedShort();
		}else if(DecoderConstant.PREFIX_TYPE_LONG == type){
			return b.readLong();
		}else if(DecoderConstant.PREFIX_TYPE_FLOAT == type){
			return b.readFloat();
		}else if(DecoderConstant.PREFIX_TYPE_DOUBLE == type){
			return b.readDouble();
		}else if(DecoderConstant.PREFIX_TYPE_BOOLEAN == type){
			return b.readBoolean();
		}else if(DecoderConstant.PREFIX_TYPE_CHAR == type){
			return b.readChar();
		}else if(DecoderConstant.PREFIX_TYPE_STRING == type){
			return JDataInput.readString(b);
			/*int len = b.readUnsignedShort();
			if(len == 0) {
				return "";
			}
			byte[] arr = new byte[len];
			b.readFully(arr, 0, len);
			return new String(arr,0,len,Constants.CHARSET);*/
		} else {
			throw new CommonException("not support header type: " + type+", key: " + k);
		}
	}

	private static ByteBuffer encodeExtra(Map<Byte, Object> extras) {
		if(extras == null || extras.isEmpty()) return null;
		
		JDataOutput b = new JDataOutput(64);
		for(Map.Entry<Byte, Object> e : extras.entrySet()) {
			try {
				b.writeByte(e.getKey());
				encodeVal(b,e.getValue());
			} catch (IOException e1) {
				throw new CommonException("encodeExtra key: " + e.getKey() +",val"+  e.getValue(),e1);
			}
		}
		return b.getBuf();
	}
	
	private static void encodeVal(JDataOutput b, Object v) throws IOException {
		if(v == null) {
			b.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
			return;
		}
		Class<?> cls = v.getClass();
		
		if(cls.isArray()){
			if(!(cls.getComponentType() == Byte.class || cls.getComponentType() == Byte.TYPE)) {
				throw new CommonException("Only support byte array not: " + cls.getName());
			}
			b.writeByte(DecoderConstant.PREFIX_TYPE_LIST);
			byte[] arr = (byte[])v;
			b.writeUnsignedShort(arr.length);
			b.write(arr);
		}else if(cls == String.class) {
			b.writeByte(DecoderConstant.PREFIX_TYPE_STRING);
			String str = v.toString();
			JDataOutput.writeString(b, str);
			/*b.writeByte(DecoderConstant.PREFIX_TYPE_STRING);
			String str = v.toString();
			if("".equals(str)){
				b.writeUnsignedShort(0);
				return;
			}
		    try {
				byte[] data = str.getBytes(Constants.CHARSET);
				b.writeUnsignedShort(data.length);
				b.write(data);
			} catch (UnsupportedEncodingException e) {
				throw new CommonException("Invalid: "+str,e);
			}*/
		}else if(cls == int.class || cls == Integer.class || cls == Integer.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_INT);
			b.writeInt((Integer)v);
		}else if(cls == byte.class || cls == Byte.class || cls == Byte.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BYTE);
			b.writeByte((Byte)v);
		}else if(cls == short.class || cls == Short.class || cls == Short.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_SHORTT);
			b.writeUnsignedShort((Short)v);
		}else if(cls == long.class || cls == Long.class || cls == Long.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_LONG);
			b.writeLong((Long)v);
		}else if(cls == float.class || cls == Float.class || cls == Float.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BYTE);
			b.writeFloat((Byte)v);
		}else if(cls == double.class || cls == Double.class || cls == Double.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_DOUBLE);
			b.writeDouble((Double)v);
		}else if(cls == boolean.class || cls == Boolean.class || cls == Boolean.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BOOLEAN);
			b.writeByte((Boolean)v?(byte)1:(byte)0);
		}else if(cls == char.class || cls == Character.class || cls == Character.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_CHAR);
			b.writeChar((Character)v);
		} else {
			throw new CommonException("not support header type for val: " + v);
		}
	}

	public static Message decode(JDataInput b) {
		try {
			Message msg = new Message();
			//第0,1个字节
			int flag = b.readUnsignedShort();

			msg.flag =  flag;
			//ByteBuffer b = ByteBuffer.wrap(data);
			int len = 0;
			if(msg.isLengthInt()) {
				len = b.readInt();
			} else {
				len = b.readUnsignedShort(); // len = 数据长度 + 附加数据长度
			}
			if(b.remaining() < len){
				throw new CommonException("Message len not valid");
			}
			
			//第3个字节
			//msg.setVersion(b.readByte());
			
			//read type
			//第4个字节
			msg.setType(b.readByte());
			msg.setMsgId(b.readLong());
			
			if(msg.isReadExtra()) {
				int elen = b.readUnsignedShort();
				byte[] edata = new byte[elen];
				b.readFully(edata,0,elen);
				msg.extra = ByteBuffer.wrap(edata);
				len = len - Message.EXT_HEADER_LEN - elen;
				msg.extraMap = decodeExtra(msg.extra);
				msg.setLen(len + elen);
				if(msg.extraMap.containsKey(EXTRA_KEY_FLAG)) {
					msg.extrFlag = (Integer)msg.extraMap.get(EXTRA_KEY_FLAG);
				}
			} else {
				msg.setLen(len);
			}
			
			if(len > 0){
				byte[] payload = new byte[len];
				b.readFully(payload,0,len);
				msg.setPayload(ByteBuffer.wrap(payload));
			} else {
				msg.setPayload(null);
			}
			
			return msg;
		} catch (IOException e) {
			throw new CommonException("error",e);
		}
	}
	
	
	public ByteBuffer encode() {
		
		JDataOutput b = new JDataOutput(512);
		
		//boolean debug = this.isDebugMode();

		ByteBuffer data = null;
		if(this.getPayload() instanceof ByteBuffer) {
			data = (ByteBuffer)this.getPayload();
		} else {
			String json = JsonUtils.getIns().toJson(this.getPayload());
			try {
				data = ByteBuffer.wrap(json.getBytes(Constants.CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		data.mark();
		
		int len = 0;//数据长度 + 附加数据长度,不包括头部长度
		if(data != null){
			len = data.remaining();
		}
		
		if(this.extrFlag != 0) {
			this.putExtra(EXTRA_KEY_FLAG, extrFlag);
		}
		
		if(this.isWriteExtra()) {
			extra = encodeExtra(extraMap);
			if(extra.remaining() > 4092) {
				throw new CommonException("Too long extra: " + extraMap.toString());
			}
			len += extra.remaining()+ Message.EXT_HEADER_LEN;
			this.setExtra(true);
		}
		
		//第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度
		if(len <= MAX_SHORT_VALUE) {
			this.setLengthType(false);
		} else if(len < MAX_INT_VALUE){
			this.setLengthType(true);
		} else {
			throw new CommonException("Data length too long than :"+MAX_INT_VALUE+", but value "+len);
		}
		
		try {
			//第0,1,2,3个字节，标志头
			//b.put(this.flag);
			//b.writeShort(this.flag);
			Message.writeUnsignedShort(b, this.flag);
			
			if(len <= MAX_SHORT_VALUE) {
				//第2，3个字节 ,len = 数据长度 + 测试模式时附加数据长度
				b.writeUnsignedShort(len);
			}else if(len < Integer.MAX_VALUE){
				//消息内内容最大长度为MAX_VALUE 2,3,4,5
				b.writeInt(len);
			} else {
				throw new CommonException("Max int value is :"+ Integer.MAX_VALUE+", but value "+len);
			}
			
			//b.putShort((short)0);
			
			//第3个字节
			//b.put(this.version);
			//b.writeByte(this.method);
			
			//第4个字节
			//writeUnsignedShort(b, this.type);
			//b.put(this.type);
			b.writeByte(this.type);
			
			b.writeLong(this.msgId);
			
			if(this.isWriteExtra()) {
				//b.writeInt(this.extrFlag);
				b.writeUnsignedShort(this.extra.remaining());
				b.write(this.extra);
			}
			
			if(data != null){
				//b.put(data);
				b.write(data);
				data.reset();
			}
			
			//b.flip();
			ByteBuffer bb = b.getBuf();
			this.len = bb.limit();
			return bb;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Message readMessage(ByteBuffer cache){
		
		//保存读数据位置
		int pos = cache.position();
		
		//数据总长是否可构建一个包的最小长度
		int totalLen = cache.remaining();
		if(totalLen < Message.HEADER_LEN) {
			//可读的数据长度小于最少头部长度
			return null;
		}
		
		//取第一个字节标志位
		int f = Message.readUnsignedShort(cache);
		int len = 0;
		int headerLen = Message.HEADER_LEN;
		//取第二，第三个字节 数据长度
	    if(is(f,FLAG_LENGTH_INT)) {
	    	//数据长度不可能起过整数的最大值
	    	//len = cache.getInt();
	    	len = cache.getInt();
	    	
	    	//还原读数据公位置
			cache.position(pos);
			/*if(len > (Integer.MAX_VALUE-10000) || len < 0) {
	    		throw new CommonException("Got invalid message len: " + len + ",flag: " + f+",buf: " + cache.toString());
	    	}*/
			
			headerLen += 2;  //int型比默认short多2字节
	    	if(totalLen < len + headerLen){
				//还不能构成一个足够长度的数据包
				return null;
			}
	    } else {
	    	len = Message.readUnsignedShort(cache);
	    	//还原读数据公位置
			cache.position(pos);
	    	if(totalLen < len + headerLen){
				//还不能构成一个足够长度的数据包
				return null;
			}
	    }
		
		byte[] data = new byte[len + headerLen];
		//从缓存中读一个包,cache的position往前推
		cache.get(data, 0, len + headerLen);
		
		return Message.decode(new JDataInput(ByteBuffer.wrap(data)));
        
	}
	
	public static boolean is(int flag, int mask) {
		return (flag & mask) != 0;
	}
	
	public static boolean is(short flag, short mask) {
		return (flag & mask) != 0;
	}
	
	public static int set(boolean isTrue,int f,int mask) {
		return isTrue ?(f |= mask) : (f &= ~mask);
	}
	
	public static short set(boolean isTrue,short f,short mask) {
		return isTrue ?(f |= mask) : (f &= ~mask);
	}
	
	public <T> T getExtra(Byte key) {
		return this.extraMap != null ? (T)this.extraMap.get(key):null;
	}
	
	public void putExtra(Byte key,Object val) {
		if(this.extraMap == null) {
			this.extraMap = new HashMap<>();
		}
		this.extraMap.put(key, val);
	}
	
	public boolean isWriteExtra() {
		return this.extraMap != null && !extraMap.isEmpty();
	}

	public boolean isReadExtra() {
		return is(flag,FLAG_EXTRA);
	}
	
	public void setExtra(boolean f) {
		flag = set(f,flag,FLAG_EXTRA);
	}
	
	public boolean isUpSsl() {
		return is(this.extrFlag,EXTRA_FLAG_UP_SSL);
	}
	
	public void setUpSsl(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_UP_SSL);
	}
	
	public boolean isDownSsl() {
		return is(extrFlag,EXTRA_FLAG_DOWN_SSL);
	}
	
	public void setDownSsl(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_DOWN_SSL);
	}
	
	public boolean isRsaEnc() {
		return is(extrFlag,EXTRA_FLAG_ENC_TYPE);
	}
	
	public void setEncType(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_ENC_TYPE);
	}
	
	public boolean isSecretVersion() {
		return is(extrFlag,EXTRA_FLAG_SECTET_VERSION);
	}
	
	public void setSecretVersion(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_SECTET_VERSION);
	}
	
	public boolean isSign() {
		return is(extrFlag,EXTRA_FLAG_IS_SIGN);
	}
	
	public void setSign(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_IS_SIGN);
	}
	
	public boolean isSec() {
		return is(extrFlag, EXTRA_FLAG_IS_SEC);
	}
	
	public void setSec(boolean f) {
		extrFlag = set(f,extrFlag, EXTRA_FLAG_IS_SEC);
	}
	
	/*public boolean isFromWeb() {
		return is(extrFlag, EXTRA_FLAG_IS_FROM_WEB);
	}
	
	public void setFromWeb(boolean f) {
		extrFlag = set(f,extrFlag, EXTRA_FLAG_IS_FROM_WEB);
	}*/
	
	public boolean isRpcMk() {
		return is(extrFlag, EXTRA_FLAG_RPC_MCODE);
	}
	
	public void setRpcMk(boolean f) {
		extrFlag = set(f,extrFlag, EXTRA_FLAG_RPC_MCODE);
	}
	
	public boolean isDumpUpStream() {
		return is(extrFlag,EXTRA_FLAG_DUMP_UP);
	}
	
	public void setDumpUpStream(boolean f) {
		//flag0 |= f ? FLAG0_DUMP_UP : 0 ; 
		extrFlag = set(f,extrFlag,EXTRA_FLAG_DUMP_UP);
	}
	
	public boolean isDumpDownStream() {
		return is(extrFlag,EXTRA_FLAG_DUMP_DOWN);
	}
	
	public void setDumpDownStream(boolean f) {
		extrFlag = set(f,extrFlag,EXTRA_FLAG_DUMP_DOWN);
	}
	
	public boolean isLoggable() {
		return this.getLogLevel() > 0;
	}
	
	public boolean isDebugMode() {
		return is(extrFlag,EXTRA_FLAG_DEBUG_MODE);
	}
	
	public void setDebugMode(boolean f) {
		this.extrFlag = set(f,extrFlag,EXTRA_FLAG_DEBUG_MODE);
	}
	
	public boolean isMonitorable() {
		return is(this.flag,FLAG_MONITORABLE);
	}
	
	public void setMonitorable(boolean f) {
		flag = set(f,flag,FLAG_MONITORABLE);
	}
	
	public boolean isError() {
		return is(flag,FLAG_ERROR);
	}
	
	public void setError(boolean f) {
		flag = set(f,flag,FLAG_ERROR);
	}
	
	public boolean isOuterMessage() {
		return is(flag,FLAG_OUT_MESSAGE);
	}
	
	public void setOuterMessage(boolean f) {
		flag = set(f,flag,FLAG_OUT_MESSAGE);
	}
	
	public boolean isForce2Json() {
		return is(flag,FLAG_FORCE_RESP_JSON);
	}
	
	public void setForce2Json(boolean f) {
		flag = set(f,flag,FLAG_FORCE_RESP_JSON);
	}
	
	public boolean isNeedResponse() {
		int rt = getRespType();
		return rt != MSG_TYPE_NO_RESP;
	}
	
	public boolean isPubsubMessage() {
		int rt = getRespType();
		return rt == MSG_TYPE_MANY_RESP;
	}
	
	public boolean isPingPong() {
		int rt = getRespType();
		return rt != MSG_TYPE_PINGPONG;
	}
	
	/**
	 * @param f true 表示整数，false表示短整数
	 */
	public void setLengthType(boolean f) {
		//flag |= f ? FLAG_LENGTH_INT : 0 ; 
		flag = set(f,flag,FLAG_LENGTH_INT);
	}
	
	public boolean isLengthInt() {
		return is(flag,FLAG_LENGTH_INT);
	}
	
	public int getPriority() {
		return (byte)((extrFlag >>> EXTRA_FLAG_PRIORITY) & 0x03);
	}
	
	public void setPriority(int l) {
		if(l > PRIORITY_3 || l < PRIORITY_0) {
			 new CommonException("Invalid priority: "+l);
		}
		this.extrFlag = (l << EXTRA_FLAG_PRIORITY) | this.extrFlag;
	}
	
	public byte getLogLevel() {
		return (byte)((flag >>> FLAG_LOG_LEVEL) & 0x07);
	}
	//000 001 010 011 100 101 110 111
	public void setLogLevel(int v) {
		if(v < 0 || v > 6) {
			 new CommonException("Invalid Log level: "+v);
		}
		this.flag = (short)((v << FLAG_LOG_LEVEL) | this.flag);
	}
	
	public byte getRespType() {
		return (byte)((flag >>> FLAG_RESP_TYPE) & 0x03);
	}
	
	public void setRespType(int v) {
		if(v < 0 || v > 3) {
			 new CommonException("Invalid message response type: "+v);
		}
		this.flag = (short)((v << FLAG_RESP_TYPE)|this.flag);
	}
	
	public byte getUpProtocol() {
		return is(this.flag,FLAG_UP_PROTOCOL)?(byte)1:0;
	}

	public void setUpProtocol(byte protocol) {
		//flag |= protocol == PROTOCOL_JSON ? FLAG_UP_PROTOCOL : 0 ; 
		flag = set(protocol == PROTOCOL_JSON,flag,FLAG_UP_PROTOCOL);
	}
	
	public byte getDownProtocol() {
		return is(this.flag,FLAG_DOWN_PROTOCOL)?(byte)1:0;
	}

	public void setDownProtocol(byte protocol) {
		//flag |= protocol == PROTOCOL_JSON ? FLAG_DOWN_PROTOCOL : 0 ;
		flag = set(protocol == PROTOCOL_JSON,flag,FLAG_DOWN_PROTOCOL);
	}
	
	public static void writeUnsignedShort(ByteBuffer b,int v) {
		/*if(v > MAX_SHORT_VALUE) {
    		throw new CommonException("Max short value is :"+MAX_SHORT_VALUE+", but value "+v);
    	}*/
		byte data = (byte)((v >> 8) & 0xFF);
		b.put(data);
		data = (byte)((v >> 0) & 0xFF);
		b.put(data);
	}
	
	public static void writeUnsignedShort(JDataOutput b,int v) {
		/*if(v > MAX_SHORT_VALUE) {
    		throw new CommonException("Max short value is :"+MAX_SHORT_VALUE+", but value "+v);
    	}*/
		try {
			byte data = (byte)((v >> 8) & 0xFF);
			b.writeByte(data);
			data = (byte)((v >> 0) & 0xFF);
			b.writeByte(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	 public static int readUnsignedShort(ByteBuffer b) {
		int firstByte = (0xFF & ((int)b.get()));
		int secondByte = (0xFF & ((int)b.get()));
		int anUnsignedShort  = (int) (firstByte << 8 | secondByte);
        return anUnsignedShort;
	 }
	 
	 public static long readUnsignedLong(ByteBuffer b) {
			int firstByte = (0xFF & ((int)b.get()));
			int secondByte = (0xFF & ((int)b.get()));
			int thirdByte = (0xFF & ((int)b.get()));
			int fourByte = (0xFF & ((int)b.get()));
			
			int fiveByte = (0xFF & ((int)b.get()));
			int sixByte = (0xFF & ((int)b.get()));
			int sevenByte = (0xFF & ((int)b.get()));
			int eigthByte = (0xFF & ((int)b.get()));
			
			int anUnsignedShort  = (int) (
					firstByte << 56 | secondByte<<48
					| thirdByte << 40 | fourByte<<32
					| fiveByte << 24 | sixByte<<16
					| sevenByte << 8 | eigthByte
					);
	        return anUnsignedShort;
	}
	 
	 public static void wiriteUnsignedLong(ByteBuffer b,long val) {
			
		    b.put((byte)(0xFF & (val >> 56)));
			b.put((byte)(0xFF & (val >> 48)));
			b.put((byte)(0xFF & (val >> 40)));
			b.put((byte)(0xFF & (val >> 32)));
			
			b.put((byte)(0xFF & (val >> 24)));
			b.put((byte)(0xFF & (val >> 16)));
			b.put((byte)(0xFF & (val >> 8)));
			b.put((byte)(0xFF & (val >> 0)));
			
			return;
	}
	
	public static void writeUnsignedByte(ByteBuffer b,short v) {
		if(v > MAX_BYTE_VALUE) {
    		throw new CommonException("Max byte value is :"+MAX_BYTE_VALUE+", but value "+v);
    	}
		byte vv = (byte)((v >> 0) & 0xFF);
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
		int fourByte = (0xFF & ((int)b.get()));
		
		int anUnsignedShort  = (int) (
				firstByte << 56 | secondByte<<48
				| thirdByte << 40 | fourByte<<32
				);
        return anUnsignedShort;
    	
    	/*int b = buf.get() & 0xff;
		int n = b & 0x7f;
		if (b > 0x7f) {
			b = buf.get() & 0xff;
			n ^= (b & 0x7f) << 7;
			if (b > 0x7f) {
				b = buf.get() & 0xff;
				n ^= (b & 0x7f) << 14;
				if (b > 0x7f) {
					b = buf.get() & 0xff;
					n ^= (b & 0x7f) << 21;
					if (b > 0x7f) {
						b = buf.get() & 0xff;
						n ^= (b & 0x7f) << 28;
						if (b > 0x7f) {
							throw new CommonException("Invalid int encoding");
						}
					}
				}
			}
		}
		return (n >>> 1) ^ -(n & 1);*/
		
    }
    
    public static void writeUnsignedInt(ByteBuffer b,long v) {
    	if(v > MAX_INT_VALUE) {
    		throw new CommonException("Max int value is :"+MAX_INT_VALUE+", but value "+v);
    	}
		b.put((byte)((v >>> 24)&0xFF));
		b.put((byte)((v >>> 16)&0xFF));
		b.put((byte)((v >>> 8)&0xFF));
		b.put((byte)((v >>> 0)&0xFF));
		
		/*n = (n << 1) ^ (n >> 31);
		if ((n & ~0x7F) != 0) {
			buf.put((byte) ((n | 0x80) & 0xFF));
			n >>>= 7;
			if (n > 0x7F) {
				buf.put((byte) ((n | 0x80) & 0xFF));
				n >>>= 7;
				if (n > 0x7F) {
					buf.put((byte) ((n | 0x80) & 0xFF));
					n >>>= 7;
					if (n > 0x7F) {
						buf.put((byte) ((n | 0x80) & 0xFF));
						n >>>= 7;
					}
				}
			}
		}
		buf.put((byte) n);*/
	}

	public void setInsId(int insId) {
		if(insId < 0) {
			insId = -insId;
		}
		this.putExtra(EXTRA_KEY_INSID, insId);
	}
	
	public Integer getInsId() {
		Integer v = this.getExtra(EXTRA_KEY_INSID);
		return v == null ? 0:v;
	}
	
	public void setSignData(String data) {
		this.putExtra(EXTRA_KEY_SIGN, data);
	}
	
	public String getSignData() {
		return this.getExtra(EXTRA_KEY_SIGN);
	}
	
	public void setLinkId(Long insId) {
		this.putExtra(EXTRA_KEY_LINKID, insId);
	}
	
	public Long getLinkId() {
		Long v = this.getExtra(EXTRA_KEY_LINKID);
		return v == null ? 0L : v;
	}
	
	public void setSaltData(byte[] data) {
		this.putExtra(EXTRA_KEY_SALT, data);
	}
	
	public byte[] getSaltData() {
		return this.getExtra(EXTRA_KEY_SALT);
	}
	
	public void setSecData(byte[] data) {
		this.putExtra(EXTRA_KEY_SEC, data);
	}
	
	public byte[] getSecData() {
		return this.getExtra(EXTRA_KEY_SEC);
	}

	public void setSmKeyCode(Integer code) {
		this.putExtra(EXTRA_KEY_SM_CODE, code);
	}
	
	public Integer getSmKeyCode() {
		Integer v = this.getExtra(EXTRA_KEY_SM_CODE);
		return v == null? 0:v;
	}
	
	public void setMethod(String method) {
		this.putExtra(EXTRA_KEY_SM_NAME, method);
	}
	
	public String getMethod() {
		return this.getExtra(EXTRA_KEY_SM_NAME);
	}
	
	public void setTime(Long time) {
		this.putExtra(EXTRA_KEY_TIME, time);
	}
	
	public Long getTime() {
		Long v = this.getExtra(EXTRA_KEY_TIME);
		return v == null ? 0L:v;
	}

	@Override
	public String toString() {
		return "Message [flag=" + flag + ", type=" + type + ", msgId=" + msgId + ", payload=" + payload + ", extraMap="
				+ extraMap + "]";
	}
	
}
