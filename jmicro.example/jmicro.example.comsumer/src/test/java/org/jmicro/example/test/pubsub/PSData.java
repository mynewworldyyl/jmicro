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

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:43
 */
public final class PSData implements Serializable, ISerializeObject {

	// 1: 队列消息, 只要一个消费者成功消费消息即为成功，并且只能一个消费者成功消费消息
	// 0:订阅消息，可以有多个消费者消费消息
	public static final byte FLAG_QUEUE = 1 << 0;

	public static final byte FLAG_PUBSUB = 0 << 0;

	public static final byte FLAG_ASYNC_METHOD = 1 << 1;

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

	public void encode(java.io.DataOutput __buffer, Object obj) throws java.io.IOException {
		PSData __obj = this;
		org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput) __buffer;
		org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
		java.util.Map __val4 = __obj.context;
		byte flag4 = 0;
		int flagIndex4 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val4 == null) {
			flag4 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex4, flag4);
		} else { // block0
			Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val4.getClass());
			if (c == null) {
				flag4 |= org.jmicro.common.Constants.TYPE_VAL;
				__buffer.writeUTF(__val4.getClass().getName());
			} else {
				__buffer.writeShort(c.intValue());
			}
			int size = __val4.size();
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeKeyEvery = false;
				boolean writeValEvery = false;
				if (java.lang.String.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.String.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.String.class))) {
					flag4 |= org.jmicro.common.Constants.GENERICTYPEFINAL; // 能从泛型中能获取到足够的列表元素类型信息
				} else { // block2
					boolean sameKeyElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val4.keySet());// 是否是同种类型的对象
					boolean isKeyFinal = org.jmicro.agent.SerializeProxyFactory
							.seriaFinalClass(__val4.keySet().iterator().next().getClass());
					if (sameKeyElt && isKeyFinal) { // block3
						Class cls = __val4.keySet().iterator().next().getClass();
						flag4 |= org.jmicro.common.Constants.HEADER_ELETMENT;
						writeKeyEvery = false;
						Short c4 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);
						if (c4 == null) {
							flag4 |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(cls.getName());
						} //
						else {
							__buffer.writeShort(c4.intValue());
						}
					} // block3
					else { // block4
						writeKeyEvery = true;
					} // block4
				} // block2
				if (java.lang.Object.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Object.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Object.class))) {
					flag4 |= org.jmicro.common.Constants.EXT0;
				} else { // block2
					boolean sameValElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val4.values());
					boolean isValFinal = org.jmicro.agent.SerializeProxyFactory
							.seriaFinalClass(__val4.values().toArray());
					if (sameValElt && isValFinal) { // block3
						flag4 |= org.jmicro.common.Constants.EXT1;// 首值编码
						writeValEvery = false;
						Class cls = __val4.values().iterator().next().getClass();
						Short c4 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);
						if (c4 == null) {
							flag4 |= org.jmicro.common.Constants.SIZE_NOT_ZERO; // 字符串类型名编码
							__buffer.writeUTF(cls.getName());
						} //
						else {
							__buffer.writeShort(c4.intValue());
						}
					} // block3
					else { // block4
						writeValEvery = true;
					} // block4
				} // block2
				java.util.Iterator ite = __val4.keySet().iterator();
				while (ite.hasNext()) { // loop block5
					Object key = ite.next();
					Object val = __val4.get(key);
					if (writeKeyEvery) {
						Short cc4 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(key.getClass());
						if (cc4 == null)
							org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(key.getClass().getName());
						else
							__buffer.writeShort(cc4.intValue());
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, key);
					if (writeValEvery) {
						Short cc4 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(val.getClass());
						if (cc4 == null)
							org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(val.getClass().getName());
						else
							__buffer.writeShort(cc4.intValue());
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, val);
				} // end for loop block5
				out.write(flagIndex4, flag4);
			} // end if block1
		} // end else block0

		long __val5 = __obj.id;
		out.writeLong(__val5);

		byte __val6 = __obj.flag;
		out.writeByte(__val6);

		java.lang.String __val7 = __obj.topic;
		out.writeUTF(__val7);

		java.lang.Object __val8 = __obj.data;
		byte flag8 = 0;
		int flagIndex8 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val8 == null) {
			flag8 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex8, flag8);
		} else { // block0
			__coder.encode(__buffer, __val8, java.lang.Object.class, null);

			out.write(flagIndex8, flag8);
		} // end else block0

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		PSData __obj = this;
		org.jmicro.api.codec.JDataInput in = (org.jmicro.api.codec.JDataInput) __buffer;
		java.util.Map __val4;
		byte flagName4 = __buffer.readByte();
		__val4 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName4)) {
			__val4 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			String clsName = null;
			short c = 0;
			if (0 != (org.jmicro.common.Constants.TYPE_VAL & flagName4)) {
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
				__val4 = (java.util.Map) org.jmicro.agent.SerializeProxyFactory.newInstance(cls);
				__obj.context = __val4;
			} // block0
			else { // block1
				__val4 = __obj.context;
			} // block1
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readKeyEvery = false;
				boolean readValEvery = false;
				Class keyEleCls = null;
				String keyClsName = null;
				c = 0;
				if (0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & flagName4)) { // blockgenic 从头部中读取类型信息
					if (0 != (org.jmicro.common.Constants.HEADER_ELETMENT & flagName4)) {
						readKeyEvery = false;
						if (0 != (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName4)) {
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
				if (0 == (org.jmicro.common.Constants.EXT0 & flagName4)) { // blockgenic 不能从泛型参数获取类型信息
					if (0 != (org.jmicro.common.Constants.EXT1 & flagName4)) {
						readValEvery = false;
						if (0 != (org.jmicro.common.Constants.SIZE_NOT_ZERO & flagName4)) {
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
						c = __buffer.readShort();
						keyEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
					} // block6
					Object key = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer, keyEleCls);
					if (readValEvery) { // block6
						c = __buffer.readShort();
						valEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
					} // block6
					Object val = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer, valEleCls);
					if (key != null) { // block7
						__val4.put(key, val);
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.context = __val4;

		long __val5;
		__val5 = in.readLong();
		__obj.id = __val5;

		byte __val6;
		__val6 = in.readByte();
		__obj.flag = __val6;

		java.lang.String __val7;
		__val7 = __buffer.readUTF();
		__obj.topic = __val7;

		java.lang.Object __val8;
		byte flagName8 = __buffer.readByte();
		__val8 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName8)) {
			__val8 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();

			__val8 = (java.lang.Object) __coder.decode(__buffer, java.lang.Object.class, null);
		} // block0
		__obj.data = __val8;

		return;
	}

}
