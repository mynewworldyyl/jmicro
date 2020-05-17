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
package cn.jmicro.example.test.pubsub;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.monitor.v1.MonitorConstant;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.ServiceMethod;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
public final class SubmitItem {

	// 消息类型
	private short type = -1;

	private long linkId;

	private String tag = null;

	private String instanceName = null;

	private String localHost = null;
	private String localPort = null;
	private String remoteHost = null;
	private String remotePort = null;

	private Object[] others = new Object[0];

	private String desc;

	private Message msg = null;

	private IReq req = null;

	private IResp resp = null;

	private ServiceMethod sm = null;

	private long time = 0;

	private byte level = MonitorConstant.LOG_DEBUG;

	private transient Throwable ex = null;

	private String exMsg;

	private transient boolean canCache = false;

	public void reset() {
		type = -1;

		linkId = 0;

		localHost = null;
		localPort = null;
		remoteHost = null;
		remotePort = null;
		instanceName = null;

		others = new String[0];
		msg = null;
		req = null;
		resp = null;
		desc = null;

		canCache = false;

		tag = null;

	}

	public SubmitItem() {
		this.time = System.currentTimeMillis();
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getLocalPort() {
		return localPort;
	}

	public void setLocalPort(String port) {
		this.localPort = port;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public String getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(String remotePort) {
		this.remotePort = remotePort;
	}

	public void setLinkId(Long linkId) {
		this.linkId = linkId;
	}

	public void appendOther(String msg) {
		String[] arr = new String[this.others.length + 1];
		System.arraycopy(this.others, 0, arr, 0, this.others.length);
		arr[arr.length - 1] = msg;
		this.others = arr;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}

	public Object[] getOthers() {
		return others;
	}

	public void setOthers(Object[] others) {
		this.others = others;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public IReq getReq() {
		return req;
	}

	public void setReq(IReq req) {
		this.req = req;
	}

	public IResp getResp() {
		return resp;
	}

	public void setResp(IResp resp) {
		this.resp = resp;
	}

	public String getLocalHost() {
		return localHost;
	}

	public void setLocalHost(String host) {
		this.localHost = host;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public ServiceMethod getSm() {
		return sm;
	}

	public void setSm(ServiceMethod sm) {
		this.sm = sm;
	}

	public boolean isCanCache() {
		return canCache;
	}

	public void setCanCache(boolean canCache) {
		this.canCache = canCache;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public Throwable getEx() {
		return ex;
	}

	public void setEx(Throwable ex) {
		this.ex = ex;
		if (ex != null) {
			this.exMsg = ex.getMessage();
		}
	}

	public String getExMsg() {
		return exMsg;
	}

	public void encode(java.io.DataOutput __buffer) throws java.io.IOException {
		SubmitItem __obj = this;
		cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput) __buffer;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
		short __val0 = __obj.type;
		out.writeShort(__val0);

		long __val1 = __obj.linkId;
		out.writeLong(__val1);

		java.lang.String __val2 = __obj.tag;
		out.writeUTF(__val2 == null ? "" : __val2);

		java.lang.String __val3 = __obj.instanceName;
		out.writeUTF(__val3 == null ? "" : __val3);

		java.lang.String __val4 = __obj.localHost;
		out.writeUTF(__val4 == null ? "" : __val4);

		java.lang.String __val5 = __obj.localPort;
		out.writeUTF(__val5 == null ? "" : __val5);

		java.lang.String __val6 = __obj.remoteHost;
		out.writeUTF(__val6 == null ? "" : __val6);

		java.lang.String __val7 = __obj.remotePort;
		out.writeUTF(__val7 == null ? "" : __val7);

		java.lang.Object[] __val8 = __obj.others;
		byte flag8 = 0;
		int flagIndex8 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val8 == null) {
			flag8 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex8, flag8);
		} else { // block0
			int size = __val8.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				if (java.lang.Object.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Object.class.getModifiers())
						|| cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Object.class))) {
					flag8 |= cn.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = cn.jmicro.agent.SerializeProxyFactory.sameArrayTypeEles(__val8);
					boolean isFinal = cn.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val8);
					if (sameElt && isFinal) { // block3
						flag8 |= cn.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c8 = cn.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val8[0].getClass());
						if (c8 == null) {
							flag8 |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(__val8[0].getClass().getName());
						} //
						else {
							__buffer.writeShort(c8.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				for (int i = 0; i < size; i++) { // loop block5
					Object v = __val8[i];
					if (writeEvery) {
						if (v == null) {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
							continue;
						}
						Short cc8 = cn.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass());
						if (cc8 == null) {
							cn.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(v.getClass().getName());
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
							__buffer.writeUTF(v.getClass().getName());
						} else {
							__buffer.writeByte(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_SHORT);
							__buffer.writeShort(cc8.intValue());
						}
					}
					cn.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex8, flag8);
			} // end if block1
		} // end else block0

		java.lang.String __val9 = __obj.desc;
		out.writeUTF(__val9 == null ? "" : __val9);

		cn.jmicro.api.net.Message __val10 = __obj.msg;
		byte flag10 = 0;
		int flagIndex10 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val10 == null) {
			flag10 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex10, flag10);
		} else { // block0
			__coder.encode(__buffer, __val10, cn.jmicro.api.net.Message.class, null);

			out.write(flagIndex10, flag10);
		} // end else block0

		cn.jmicro.api.net.IReq __val11 = __obj.req;
		byte flag11 = 0;
		int flagIndex11 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val11 == null) {
			flag11 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex11, flag11);
		} else { // block0
			__coder.encode(__buffer, __val11, cn.jmicro.api.net.IReq.class, null);

			out.write(flagIndex11, flag11);
		} // end else block0

		cn.jmicro.api.net.IResp __val12 = __obj.resp;
		byte flag12 = 0;
		int flagIndex12 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val12 == null) {
			flag12 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex12, flag12);
		} else { // block0
			__coder.encode(__buffer, __val12, cn.jmicro.api.net.IResp.class, null);

			out.write(flagIndex12, flag12);
		} // end else block0

		Object __val13 = __obj.sm;
		byte flag13 = 0;
		int flagIndex13 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val13 == null) {
			flag13 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex13, flag13);
		} else { // block0
			((cn.jmicro.api.codec.ISerializeObject) __val13).encode(__buffer);
			out.write(flagIndex13, flag13);
		} // end else block0

		long __val14 = __obj.time;
		out.writeLong(__val14);

		byte __val15 = __obj.level;
		out.writeByte(__val15);

		java.lang.String __val17 = __obj.exMsg;
		out.writeUTF(__val17 == null ? "" : __val17);

	}
}
