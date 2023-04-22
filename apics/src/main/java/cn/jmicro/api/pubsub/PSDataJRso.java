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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.IDStrategy;
import lombok.Data;
import lombok.Serial;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:43
 */
@Serial
@IDStrategy(100)
@Data
public class PSDataJRso implements Serializable{
	
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
	
	//消息上下文
	private Map<String,Object> cxt = null;
	
	//消息ID,唯一标识一个消息
	private long id = 0;
	
	//标志
	private byte flag = 0;
	
	//消息类型，可以用于逻辑分发，相当于一个方法标识编码
	private byte type = 0;
	
	//主题
	private String topic;
	
	//源租户
	private int srcClientId;
	
	//消息来源
	private Integer fr;
	
	//消息发送给谁
	private Integer to;
	
	//消息数据
	private Object data;
	
	//消息发送结果回调的RPC方法，用于消息服务器给发送者回调
	private String callback = null;
	
	//本地回调
	private transient ILocalCallback localCallback;
	
	//客户端发送失败次数，用于重发计数，如果消息失败次数到达一定量，将消息丢弃，并调用localCallback（如果存在）通知调用者，
	private transient int failCnt = 0;
	
	//延迟多久发送,单位是秒
	private byte delay=0;
	
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
