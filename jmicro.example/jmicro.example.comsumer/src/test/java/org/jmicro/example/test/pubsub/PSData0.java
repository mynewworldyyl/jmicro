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
package org.jmicro.example.test.pubsub;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.codec.ISerializeObject;
import org.jmicro.api.registry.UniqueServiceMethodKey;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:43
 */
public final class PSData0 implements Serializable, ISerializeObject {

	// 1: 队列消息, 只要一个消费者成功消费消息即为成功，并且只能一个消费者成功消费消息
	// 0:订阅消息，可以有多个消费者消费消息
	public static final byte FLAG_QUEUE = 1 << 0;

	public static final byte FLAG_PUBSUB = 0 << 0;

	// 1右移1位，异步方法，决定回调方法的参数类型为消息通知的返回值
	public static final byte FLAG_ASYNC_METHOD = 1 << 1;
	// 1右移两位，消息回调通知，决定回调方法的参数类型为消息通知的返回值分别为 消息发送状态码，消息ID，消息上下文透传
	public static final byte FLAG_MESSAGE_CALLBACK = 1 << 2;

	// 1: 如果是订阅类消息,必须全部成功消费才算成功，否则对于失败
	// 0:只需要确保其中一个消费者成功消费消息即可认为消息发送成功，即使别的消费者消费失败，也不会重发消息
	// public static final byte FLAG_SUCCESS_ALL = 1<<1;

	private static final long serialVersionUID = 389875668374730999L;

	private Map<String, Object> context = new HashMap<>();

	// 消息ID,唯一标识一个消息
	private long id = 0;

	private byte flag = 0;

	private String topic;

	private Object data;

	// 消息发送结果回调的RPC方法，用于消息服务器给发送者回调
	private UniqueServiceMethodKey callback = null;

	// 客户端发送失败次数，用于重发计数，如果消息失败次数到达一定量，将消息丢弃，并调用localCallback（如果存在）通知调用者，
	private transient int failCnt = 0;

	public static byte flag(byte... fs) {
		byte fl = 0;
		for (byte f : fs) {
			fl |= f;
		}
		return fl;
	}

	public byte getFlag() {
		return flag;
	}

	public void setFlag(byte flag) {
		this.flag = flag;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void put(String key, Object v) {
		this.context.put(key, v);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) this.context.get(key);
	}

	public void encode(java.io.DataOutput __buffer) throws java.io.IOException {
		PSData0 __obj = this;
		org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput) __buffer;
		org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
		java.util.Map __val5 = __obj.context;
		byte flag5 = 0;
		int flagIndex5 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val5 == null) {
			flag5 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex5, flag5);
		} else { // block0
			Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val5.getClass());
			if (c == null) {
				flag5 |= org.jmicro.common.Constants.TYPE_VAL;
				__buffer.writeUTF(__val5.getClass().getName());
			} else {
				__buffer.writeShort(c.intValue());
			}
			int size = __val5.size();
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeKeyEvery = false;
				boolean writeValEvery = false;
				if (java.lang.String.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.String.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.String.class))) {
					flag5 |= org.jmicro.common.Constants.GENERICTYPEFINAL; // 能从泛型中能获取到足够的列表元素类型信息
				} else { // block2
					boolean sameKeyElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val5.keySet());// 是否是同种类型的对象
					boolean isKeyFinal = org.jmicro.agent.SerializeProxyFactory
							.seriaFinalClass(__val5.keySet().iterator().next().getClass());
					boolean hasNullKeyElt = org.jmicro.agent.SerializeProxyFactory.collHasNullElement(__val5.keySet());// 是否有空元素的KEY
					if (sameKeyElt && isKeyFinal && !hasNullKeyElt) { // block3
						Class cls = __val5.keySet().iterator().next().getClass();
						flag5 |= org.jmicro.common.Constants.HEADER_ELETMENT;
						writeKeyEvery = false;
						Short c5 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);
						if (c5 == null) {
							flag5 |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(cls.getName());
						} //
						else {
							__buffer.writeShort(c5.intValue());
						}
					} // block3
					else { // block4
						writeKeyEvery = true;
					} // block4
				} // block2
				if (java.lang.Object.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Object.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Object.class))) {
					flag5 |= org.jmicro.common.Constants.EXT0;
				} else { // block2
					boolean sameValElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val5.values());
					boolean isValFinal = org.jmicro.agent.SerializeProxyFactory
							.seriaFinalClass(__val5.values().toArray());
					boolean hasNullValElt = org.jmicro.agent.SerializeProxyFactory.collHasNullElement(__val5.values());// 是否有空元素的值
					if (sameValElt && isValFinal && !hasNullValElt) { // block3
						flag5 |= org.jmicro.common.Constants.EXT1;// 首值编码
						writeValEvery = false;
						Class cls = __val5.values().iterator().next().getClass();
						Short c5 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);
						if (c5 == null) {
							flag5 |= org.jmicro.common.Constants.SIZE_NOT_ZERO; // 字符串类型名编码
							__buffer.writeUTF(cls.getName());
						} //
						else {
							__buffer.writeShort(c5.intValue());
						}
					} // block3
					else { // block4
						writeValEvery = true;
					} // block4
				} // block2
				java.util.Iterator ite = __val5.keySet().iterator();
				while (ite.hasNext()) { // loop block5
					Object key = ite.next();
					Object val = __val5.get(key);
					if (writeKeyEvery) {
						if (key == null) {
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc5 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(key.getClass());
						if (cc5 == null) {
							org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(key.getClass().getName());
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(key.getClass().getName());
						} else {
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc5.intValue());
						}
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, key);
					if (writeValEvery) {
						if (val == null) {
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc5 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(val.getClass());
						if (cc5 == null) {
							org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(val.getClass().getName());
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(val.getClass().getName());
						} else {
							__buffer.writeByte(org.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc5.intValue());
						}
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, val);
				} // end for loop block5
				out.write(flagIndex5, flag5);
			} // end if block1
		} // end else block0

		long __val6 = __obj.id;
		out.writeLong(__val6);

		byte __val7 = __obj.flag;
		out.writeByte(__val7);

		java.lang.String __val8 = __obj.topic;
		out.writeUTF(__val8 == null ? "" : __val8);

		java.lang.Object __val9 = __obj.data;
		byte flag9 = 0;
		int flagIndex9 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val9 == null) {
			flag9 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex9, flag9);
		} else { // block0
			__coder.encode(__buffer, __val9, java.lang.Object.class, null);
			out.write(flagIndex9, flag9);
		} // end else block0

		org.jmicro.api.registry.UniqueServiceMethodKey __val10 = __obj.callback;
		byte flag10 = 0;
		int flagIndex10 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val10 == null) {
			flag10 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex10, flag10);
		} else { // block0
			java.lang.Object __o10 = __val10;
			((org.jmicro.api.codec.ISerializeObject) __o10).encode(__buffer);
			out.write(flagIndex10, flag10);
		} // end else block0

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		PSData0 __obj = this;
		org.jmicro.api.codec.JDataInput in = (org.jmicro.api.codec.JDataInput) __buffer;
		java.util.Map __val5;
		byte flagName5 = __buffer.readByte();
		__val5 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName5)) {
			__val5 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			String clsName = null;
			short c = 0;
			if (0 != (org.jmicro.common.Constants.TYPE_VAL & flagName5)) {
				clsName = __buffer.readUTF();
			} else {
				c = __buffer.readShort();
			}
			if (__obj.context == null) { // block0
				Class cls = null;
				if (clsName != null) {
					cls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName);
				} else {
					cls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
				}
				__val5 = (java.util.Map) org.jmicro.agent.SerializeProxyFactory.newInstance(cls);
				__obj.context = __val5;
			} // block0
			else { // block1
				__val5 = __obj.context;
			} // block1
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readKeyEvery = false;
				boolean readValEvery = false;
				Class keyEleCls = null;
				String keyClsName = null;
				c = 0;
				if (0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & flagName5)) { // blockgenic 从头部中读取类型信息
					if (0 != (org.jmicro.common.Constants.HEADER_ELETMENT & flagName5)) {
						readKeyEvery = false;
						if (0 != (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName5)) {
							c = __buffer.readShort();
							keyEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							keyClsName = __buffer.readUTF();
							keyEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(keyClsName);
						}
					}
				} // blockgenic
				else {
					keyEleCls = java.lang.String.class;
					readKeyEvery = false;
				}
				Class valEleCls = null;
				String valClsName = null;
				c = 0;
				if (0 == (org.jmicro.common.Constants.EXT0 & flagName5)) { // blockgenic 不能从泛型参数获取类型信息
					if (0 != (org.jmicro.common.Constants.EXT1 & flagName5)) {
						readValEvery = false;
						if (0 != (org.jmicro.common.Constants.SIZE_NOT_ZERO & flagName5)) {
							valClsName = __buffer.readUTF();
							valEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(valClsName);
						} else {
							c = __buffer.readShort();
							valEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						}
					} else {
						readValEvery = true;
					}
				} // blockgenic
				else {
					valEleCls = java.lang.Object.class;
					readValEvery = false;
				}
				int cnt = 0;
				while (cnt < size) { // block5
					++cnt;
					if (readKeyEvery) { // block6
						short prefixCode = __buffer.readByte();
						if (prefixCode == org.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT) {
							c = __buffer.readShort();
							keyEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							java.lang.String cn = __buffer.readUTF();
							keyEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(cn);
						}
					} // block6
					Object key = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer, keyEleCls);
					if (readValEvery) { // block6
						short prefixCode = __buffer.readByte();
						if (prefixCode == org.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT) {
							c = __buffer.readShort();
							valEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							java.lang.String cn = __buffer.readUTF();
							valEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(cn);
						}
					} // block6
					Object val = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer, valEleCls);
					if (key != null) { // block7
						__val5.put(key, val);
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.context = __val5;

		long __val6;
		__val6 = in.readLong();
		__obj.id = __val6;

		byte __val7;
		__val7 = in.readByte();
		__obj.flag = __val7;

		java.lang.String __val8;
		__val8 = __buffer.readUTF();
		__obj.topic = __val8;

		java.lang.Object __val9;
		byte flagName9 = __buffer.readByte();
		__val9 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName9)) {
			__val9 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();

			__val9 = (java.lang.Object) __coder.decode(__buffer, java.lang.Object.class, null);
		} // block0
		__obj.data = __val9;

		org.jmicro.api.registry.UniqueServiceMethodKey __val10;
		byte flagName10 = __buffer.readByte();
		__val10 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName10)) {
			__val10 = null;
		} else { // block0
			__val10 = new org.jmicro.api.registry.UniqueServiceMethodKey();
			((org.jmicro.api.codec.ISerializeObject) (Object) __val10).decode(__buffer);
		} // block0
		__obj.callback = __val10;

		return;
	}

}
