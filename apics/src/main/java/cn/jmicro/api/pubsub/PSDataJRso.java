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
package cn.jmicro.api.pubsub;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import lombok.Data;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:43
 */
//@Serial
@IDStrategy(100)
@Data
public class PSDataJRso implements Serializable, ISerializeObject{
	
	private static final long serialVersionUID = 389875668374730999L;
	
	public static final String MSG_TYPE = "__msgType";
	
	public static final String SRC_PSDATA_ID = "_spid_";
	
	public static final byte FLAG_DEFALUT = 0;
	
	// 1: 队列消息, 只要一个消费者成功消费消息即为成功，并且只能一个消费者成功消费消息
	// 0:订阅消息，可以有多个消费者消费消息
	public static final byte FLAG_QUEUE = 1<<0;
	
	public static final byte FLAG_PUBSUB = 0<<0;
	
	//1右移1位，异步方法，决定回调方法的参数类型为消息通知的返回值
	public static final byte FLAG_ASYNC_METHOD = 1<<1;
	//1右移两位，消息回调通知，决定回调方法的参数类型为消息通知的返回值分别为 消息发送状态码，消息ID，消息上下文透传
	public static final byte FLAG_MESSAGE_CALLBACK = 1<<2;
	
	public static final byte FLAG_PERSIST = 1<<3;
	
	public static final byte FLAG_CALLBACK_TOPIC = 1<<4;
	
	public static final byte FLAG_CALLBACK_METHOD = 0<<4;
	
	//第5，6两位一起表示data字段的编码类型
	public static final byte FLAG_DATA_TYPE = 5;
	
	public static final byte FLAG_DATA_STRING = 0;
	public static final byte FLAG_DATA_BIN = 1;
	public static final byte FLAG_DATA_JSON = 2;
	public static final byte FLAG_DATA_NONE = 3;
	
	//1:  如果是订阅类消息,必须全部成功消费才算成功，否则对于失败
	//0:只需要确保其中一个消费者成功消费消息即可认为消息发送成功，即使别的消费者消费失败，也不会重发消息
	//public static final byte FLAG_SUCCESS_ALL = 1<<1;
	
	public static final byte RESULT_SUCCCESS = 0;
	
	public static final int PUB_OK = RESULT_SUCCCESS;
	//无消息服务可用,需要启动消息服务
	public static final int PUB_SERVER_NOT_AVAILABALE = -1;
	//消息队列已经满了,客户端可以重发,或等待一会再重发
	public static final int PUB_SERVER_DISCARD = -2;
	//消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
	public static final int PUB_SERVER_BUSUY = -3;
	
	public static final int PUB_TOPIC_INVALID= -4;
	
	public static final int PUB_ITEM_IS_NULL= -5;
	
	public static final int PUB_TOPIC_IS_NULL= -6;
	
	//消息服务器不可用
	public static final byte RESULT_FAIL_SERVER_DISABLE = -5;
	
	//发送给消息订阅者失败
	public static final byte RESULT_FAIL_DISPATCH = -6;
	
	//回调结果通知失败
	public static final byte RESULT_FAIL_CALLBACK = -7;
	
	public static final byte INVALID_ITEM_COUNT = -8;
	
	//数据标志
	private byte dataFlag = 0;
	
	//标志
	private byte flag = 0;
	
	//消息来源，发送消息的账号ID
	private Integer fr;
	
	//消息ID,唯一标识一个消息
	private long id = 0; //标志位 0
	
	//消息类型，可以用于逻辑分发，相当于一个方法标识编码
	private byte type = 0; //标志位 1
	
	//主题
	private String topic; //标志位 2
	
	//源租户
	private int srcClientId; //标志位 3
	
	//消息发送给谁
	private Integer to; //标志位 4
	
	//延迟多久发送,单位是秒
	private byte delay=0; //标志位 5
		
	//消息数据
	private Object data; //标志位 6
	
	//消息上下文
	private Map<String,Object> cxt = null; //标志位 7
		
	//消息发送结果回调的RPC方法，用于消息服务器给发送者回调
	private String callback = null; //标志位 8

	//本地回调
	private transient ILocalCallback localCallback;
	
	//客户端发送失败次数，用于重发计数，如果消息失败次数到达一定量，将消息丢弃，并调用localCallback（如果存在）通知调用者，
	private transient int failCnt = 0;
	
	@Override
	public void encode(DataOutput out1) throws IOException{
		
		JDataOutput out = (JDataOutput)out1;
		
		if(id != 0) setDataFlag(0);
		if(type != 0) setDataFlag(1);
		if(Utils.isNotEmpty(this.topic)) setDataFlag(2);
		if(srcClientId != 0) setDataFlag(3);
		if(to != 0) setDataFlag(4);
		if(Utils.isNotEmpty(this.callback)) setDataFlag(5);
		if(data != null) setDataFlag(6);
		if(this.cxt != null) setDataFlag(7);
		if(delay != 0) this.dataFlag = (byte)(-this.dataFlag);
		
		out.writeByte(this.dataFlag);
		out.writeByte(this.flag);
		out.writeInt(this.fr);
		
		if(id != 0) {
			out.writeLong(this.id);
			setDataFlag(0);
		}
		
		if(type != 0) {
			out.writeByte(this.type);
			setDataFlag(1);
		}
		
		if(Utils.isNotEmpty(this.topic)) {
			out.writeUTF(this.topic);
			setDataFlag(2);
		}
		
		if(srcClientId != 0) {
			out.writeInt(this.srcClientId);
			setDataFlag(3);
		}
		
		if(to != 0) {
			out.writeInt(this.to);
			setDataFlag(4);
		}
		
		if(Utils.isNotEmpty(this.callback)) {
			out.writeUTF(this.callback);
			setDataFlag(5);
		}
		
		if(data != null) {
			if(FLAG_DATA_BIN == this.getDataType()) {
				//依赖于接口实现数据编码，服务提供方和使用方需要协商好数据编码和解码方式
				if(data instanceof ISerializeObject) {
					((ISerializeObject)data).encode(out);
				} else {
					throw new CommonException(data.getClass().getName() +" not implement "+ISerializeObject.class.getName());
				}
			}else if(FLAG_DATA_STRING == this.getDataType()){
				out.writeUTF(this.data.toString());
			}else if(FLAG_DATA_JSON== this.getDataType()){
				out.writeUTF(JsonUtils.getIns().toJson(this.data));
			} else {
				//对几种基本数据类型做编码
				Message.encodeVal(out, this.data);
			}
			setDataFlag(6);
		}
		
		if(this.cxt != null) {
			encodeExtra(out,this.cxt);
			setDataFlag(7);
		}
		
		if(delay != 0) {
			out.writeByte(this.delay);
			this.dataFlag = (byte)(-this.dataFlag);
		}
		
	}
	
	private void encodeExtra(JDataOutput b, Map<String, Object> extras) {
		try {
			b.writeByte((byte)extras.size());//如果大于127，写入在小是负数，解码端需要做转换，参数Message.decodeExtra
		} catch (IOException e2) {
			throw new CommonException("encodeExtra extra size error");
		}
		
		for(Map.Entry<String, Object> e : extras.entrySet()) {
			try {
				b.writeUTF(e.getKey());
				Message.encodeVal(b,e.getValue());
			} catch (IOException e1) {
				throw new CommonException("encodeExtra key: " + e.getKey() +",val"+  e.getValue(),e1);
			}
		}
	}
	
	private void setDataFlag(int idx) {
		this.dataFlag |= 1 << idx;
	}
	
	/*private void clearDataFlag(int idx) {
		this.dataFlag &= ~(1 << idx);
	}*/
	
	private boolean isDataFlag(int idx) {
		return (this.dataFlag & (1 << idx)) != 0;
	}
	
	public byte getDataType() {
		return (byte)((flag >>> FLAG_DATA_TYPE) & 0x03);
	}

	public void setDataType(int v) {
		if(v < 0 || v > 6) {
			 new CommonException("Invalid data type: "+v);
		}
		this.flag = (byte)((v << FLAG_DATA_TYPE) | this.flag);
	}

	@Override
	public void decode(DataInput out1) throws IOException {
		
		JDataInput in = (JDataInput)out1;
		
		this.dataFlag = in.readByte();
		this.flag = in.readByte();
		this.fr = in.readInt();
		
		if(isDataFlag(0)) {
			this.id  = in.readLong();
		}
		
		if(isDataFlag(1)) {
			this.type  = in.readByte();
		}
		
		if(isDataFlag(2)) {
			this.topic  = in.readUTF();
		}
		
		if(isDataFlag(3)) {
			this.srcClientId  = in.readInt();
		}
		
		if(isDataFlag(4)) {
			this.to  = in.readInt();
		}
		
		if(isDataFlag(5)) {
			this.callback  = in.readUTF();
		}
		
		if(isDataFlag(6)) {
			if(FLAG_DATA_BIN == this.getDataType()) {
				//依赖于接口实现数据编码，服务提供方和使用方需要协商好数据编码和解码方式
				if(data instanceof ISerializeObject) {
					((ISerializeObject)this).decode(in);
				} else {
					throw new CommonException(data.getClass().getName() +" not implement "+ISerializeObject.class.getName());
				}
			}else if(FLAG_DATA_STRING == this.getDataType()){
				this.data = in.readUTF();
			}else if(FLAG_DATA_JSON== this.getDataType()){
				String json = in.readUTF();
				this.data = json;
				//out.writeUTF(JsonUtils.getIns().toJson(this.data));
			} else {
				//对几种基本数据类型做解码
				this.data = Message.decodeVal(in);
			}
		}
		
		if(isDataFlag(7)) {
			this.cxt  = decodeExtra(in);
		}
		
		if(this.dataFlag < 0) {
			this.delay  = in.readByte();
		}
	}
	
	private Map<String,Object> decodeExtra(JDataInput b) {
		
		Map<String,Object> ed = new HashMap<>();
		
		try {
			
			int eleNum = b.readByte(); //extra元素个数
			if(eleNum < 0) {
				eleNum += 256; //参考encode方法说明
			}
			
			if(eleNum == 0) return null;
			while(eleNum > 0) {
				String k = b.readUTF();
				Object v = Message.decodeVal(b);
				ed.put(k, v);
				eleNum--;
			}
			return ed;
		} catch (IOException e) {
			throw new CommonException("decodeExtra error:" + ed.toString() + " IOException: " + e.getMessage());
		}
	}

	
	public static boolean is(byte flag, byte mask) {
		return (flag & mask) != 0;
	}
	
	public static byte set(boolean isTrue,byte f,byte mask) {
		return isTrue ?(f |= mask) : (f &= ~mask);
	}
	
	public boolean isPersist() {
		return is(this.flag,FLAG_PERSIST);
	}
	
	public void setPersist(boolean f) {
		this.flag = set(f,this.flag,FLAG_PERSIST);
	}
	
	public void queue() {
		this.flag = set(true,this.flag,FLAG_QUEUE);
	}
	
	public void pubsub() {
		this.flag = set(false,this.flag,FLAG_PUBSUB);
	}
	
	public void callbackTopic() {
		this.flag = set(true,this.flag,FLAG_CALLBACK_TOPIC);
	}
	
	public void callbackMethod() {
		this.flag = set(false,this.flag,FLAG_CALLBACK_METHOD);
	}
	
	public boolean isCallbackTopic() {
		return is(this.flag,FLAG_CALLBACK_TOPIC);
	}
	
	public boolean isCallbackMethod() {
		return !is(this.flag,FLAG_CALLBACK_TOPIC);
	}
	
	public boolean isQueue() {
		return is(this.flag,FLAG_QUEUE);
	}
	
	public boolean isPubsub() {
		return !is(this.flag,FLAG_QUEUE);
	}
	
	public static byte flag(byte ...fs) {
		byte fl = 0;
		for(byte f : fs) {
			fl |= f;
		}
		return fl; 
	}

	public void mergeContext(Map<String,Object> cxt) {
		initContext();
		this.cxt.putAll(cxt);
	}
	
	private void initContext() {
		if(this.cxt == null) {
			this.cxt = new HashMap<>();
		}
	}

	public Map<String, Object> getContext() {
		return cxt;
	}

	public void setContext(Map<String, Object> cxt) {
		if(cxt == null) {
			return;
		}
		this.cxt = cxt;
	}

	public void put(String key,Object v) {
		initContext();
		this.cxt.put(key, v);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		if(cxt == null) {
			return null;
		}
		return (T) this.cxt.get(key);
	}

	@Override
	public int hashCode() {
		return new Long(this.id).intValue();
	}

	@Override
	public boolean equals(Object obj) {
		return this.id == ((PSDataJRso)obj).getId();
	}

}
