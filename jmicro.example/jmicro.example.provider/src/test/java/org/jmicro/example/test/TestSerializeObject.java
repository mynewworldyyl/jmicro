package org.jmicro.example.test;

import java.io.Serializable;

import org.jmicro.api.codec.ISerializeObject;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;

public class TestSerializeObject implements Serializable, ISerializeObject {

	private String serviceName;

	private String method;

	private Object[] args = new Object[] { 1, "string" };

	private String namespace;

	private String version;

	private String impl;

	private String transport;

	protected Long reqId = -1L;

	private transient ISession session;

	private transient boolean isMonitorEnable = false;

	private transient Message msg;

	private transient boolean success = false;

	private transient boolean finish = false;

	public void encode(java.io.DataOutput __buffer) throws java.io.IOException {
		TestSerializeObject __obj = this;
		org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput) __buffer;
		org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
		java.lang.String __val0 = __obj.serviceName;
		out.writeUTF(__val0);

		java.lang.String __val1 = __obj.method;
		out.writeUTF(__val1);

		java.lang.Object[] __val2 = __obj.args;
		byte flag2 = 0;
		int flagIndex2 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val2 == null) {
			flag2 |= org.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex2, flag2);
		} else { // block0
			int size = __val2.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				if (java.lang.Object.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Object.class.getModifiers())
						|| org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Object.class))) {
					flag2 |= org.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameArrayTypeEles(__val2);
					boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val2[0].getClass());
					if (sameElt && isFinal) { // block3
						flag2 |= org.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c2 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val2[0].getClass());
						if (c2 == null) {
							flag2 |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE;
							__buffer.writeUTF(__val2[0].getClass().getName());
						} //
						else {
							__buffer.writeShort(c2.intValue());
						}
					} // block3
					else { // block4
						writeEvery = true;
					} // block4
				} // block2
				for (int i = 0; i < size; i++) { // loop block5
					Object v = __val2[i];
					if (writeEvery) {
						Short cc2 = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass());
						__buffer.writeShort(cc2.intValue());
					}
					org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, v);
				} // end for loop block5
				out.write(flagIndex2, flag2);
			} // end if block1
		} // end else block0

		java.lang.String __val3 = __obj.namespace;
		out.writeUTF(__val3);

		java.lang.String __val4 = __obj.version;
		out.writeUTF(__val4);

		java.lang.String __val5 = __obj.impl;
		out.writeUTF(__val5);

		java.lang.String __val6 = __obj.transport;
		out.writeUTF(__val6);

		java.lang.Long __val7 = __obj.reqId;
		out.writeLong(__val7);

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		TestSerializeObject __obj = this;
		org.jmicro.api.codec.JDataInput in = (org.jmicro.api.codec.JDataInput) __buffer;
		java.lang.String __val0;
		__val0 = __buffer.readUTF();
		__obj.serviceName = __val0;

		java.lang.String __val1;
		__val1 = __buffer.readUTF();
		__obj.method = __val1;

		java.lang.Object[] __val2;
		byte flagName2 = __buffer.readByte();
		__val2 = null;
		if (0 != (org.jmicro.common.Constants.NULL_VAL & flagName2)) {
			__val2 = null;
		} else { // block0
			org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			String clsName = null;
			short c = 0;
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & flagName2)) { // blockgenic
					if (0 != (org.jmicro.common.Constants.HEADER_ELETMENT & flagName2)) {
						readEvery = false;
						if (0 != (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName2)) {
							c = __buffer.readShort();
							eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName);
						}
					}
				} // blockgenic
				else {
					eleCls = java.lang.Object.class;
					readEvery = false;
				}
				__val2 = new java.lang.Object[size];
				for (int i = 0; i < size; i++) { // block5
					if (readEvery) { // block6
						c = __buffer.readShort();
						eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
					} // block6
					java.lang.Object elt = (java.lang.Object) org.jmicro.agent.SerializeProxyFactory
							.decodeListElement(__buffer, eleCls);
					if (elt != null) { // block7
						__val2[i] = elt;
					} // block7
				} // block5
			} // block2
		} // block0
		__obj.args = __val2;

		java.lang.String __val3;
		__val3 = __buffer.readUTF();
		__obj.namespace = __val3;

		java.lang.String __val4;
		__val4 = __buffer.readUTF();
		__obj.version = __val4;

		java.lang.String __val5;
		__val5 = __buffer.readUTF();
		__obj.impl = __val5;

		java.lang.String __val6;
		__val6 = __buffer.readUTF();
		__obj.transport = __val6;

		java.lang.Long __val7;
		__val7 = new java.lang.Long(in.readLong());
		__obj.reqId = __val7;

		return;
	}


}
