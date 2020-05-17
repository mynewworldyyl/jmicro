package cn.jmicro.example.test;

import java.io.Serializable;

import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;

public class RpcRequestVo implements Serializable, ISerializeObject {

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
		RpcRequestVo __obj = this;
		cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput) __buffer;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
		java.lang.String __val0 = __obj.serviceName;
		out.writeUTF(__val0);

		java.lang.String __val1 = __obj.method;
		out.writeUTF(__val1);

		java.lang.Object[] __val2 = __obj.args;
		byte flag2 = 0;
		int flagIndex2 = out.position();
		__buffer.writeByte(0); // forward one byte
		if (__val2 == null) {
			flag2 |= cn.jmicro.common.Constants.NULL_VAL;
			out.write(flagIndex2, flag2);
		} else { // block0
			int size = __val2.length;
			__buffer.writeShort(size);
			if (size > 0) { // if block1
				boolean writeEvery = false;
				if (java.lang.Object.class != null && (java.lang.reflect.Modifier
						.isFinal(java.lang.Object.class.getModifiers())
						|| cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(java.lang.Object.class))) {
					flag2 |= cn.jmicro.common.Constants.GENERICTYPEFINAL;
				} else { // block2
					boolean sameElt = cn.jmicro.agent.SerializeProxyFactory.sameArrayTypeEles(__val2);
					boolean isFinal = cn.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val2[0].getClass());
					if (sameElt && isFinal) { // block3
						flag2 |= cn.jmicro.common.Constants.HEADER_ELETMENT;
						writeEvery = false;
						Short c2 = cn.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val2[0].getClass());
						if (c2 == null) {
							flag2 |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
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
						Short cc2 = cn.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass());
						__buffer.writeShort(cc2.intValue());
					}
					cn.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer, v);
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
		RpcRequestVo __obj = this;
		cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput) __buffer;
		java.lang.String __val0;
		__val0 = __buffer.readUTF();
		__obj.serviceName = __val0;

		java.lang.String __val1;
		__val1 = __buffer.readUTF();
		__obj.method = __val1;

		java.lang.Object[] __val2;
		byte flagName2 = __buffer.readByte();
		__val2 = null;
		if (0 != (cn.jmicro.common.Constants.NULL_VAL & flagName2)) {
			__val2 = null;
		} else { // block0
			cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();
			String clsName = null;
			short c = 0;
			int size = __buffer.readShort();
			if (size > 0) { // block2
				boolean readEvery = true;
				Class eleCls = null;
				clsName = null;
				c = 0;
				if (0 == (cn.jmicro.common.Constants.GENERICTYPEFINAL & flagName2)) { // blockgenic
					if (0 != (cn.jmicro.common.Constants.HEADER_ELETMENT & flagName2)) {
						readEvery = false;
						if (0 == (cn.jmicro.common.Constants.ELEMENT_TYPE_CODE & flagName2)) {
							c = __buffer.readShort();
							eleCls = cn.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
						} else {
							clsName = __buffer.readUTF();
							eleCls = cn.jmicro.agent.SerializeProxyFactory.loadClazz(clsName);
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
						eleCls = cn.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c));
					} // block6
					java.lang.Object elt = (java.lang.Object) cn.jmicro.agent.SerializeProxyFactory
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

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getImpl() {
		return impl;
	}

	public void setImpl(String impl) {
		this.impl = impl;
	}

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public ISession getSession() {
		return session;
	}

	public void setSession(ISession session) {
		this.session = session;
	}

	public boolean isMonitorEnable() {
		return isMonitorEnable;
	}

	public void setMonitorEnable(boolean isMonitorEnable) {
		this.isMonitorEnable = isMonitorEnable;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

}
