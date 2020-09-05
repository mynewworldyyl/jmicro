package cn.jmicro.example.test.pubsub;

import java.util.Map;

import cn.jmicro.api.AbstractObjectMapSupport;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;

public class RpcRequest0 extends AbstractObjectMapSupport implements IRequest {

	private String serviceName;

	private String method;

	private Object[] args;

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

	public RpcRequest0() {
	}

	public void encode(java.io.DataOutput __buffer) throws java.io.IOException {
		RpcRequest0 __obj = this;
		cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput) __buffer;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();
		java.lang.String __val0 = __obj.serviceName;
		out.writeUTF(__val0 == null ? "" : __val0);

		java.lang.String __val1 = __obj.method;
		out.writeUTF(__val1 == null ? "" : __val1);

		java.lang.Object[] __val2 = __obj.args;
		if (__val2 == null) {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);
		} else {
			out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY);
			__coder.encode(__buffer, __val2, java.lang.Object[].class, null);
		}

		java.lang.String __val3 = __obj.namespace;
		out.writeUTF(__val3 == null ? "" : __val3);

		java.lang.String __val4 = __obj.version;
		out.writeUTF(__val4 == null ? "" : __val4);

		java.lang.String __val5 = __obj.impl;
		out.writeUTF(__val5 == null ? "" : __val5);

		java.lang.String __val6 = __obj.transport;
		out.writeUTF(__val6 == null ? "" : __val6);

		long __val7 = __obj.reqId;
		out.writeLong(__val7);

	}

	public void decode(java.io.DataInput __buffer) throws java.io.IOException {
		RpcRequest0 __obj = this;
		cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();

		cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput) __buffer;
		java.lang.String __val0;
		__val0 = __buffer.readUTF();

		__obj.serviceName = __val0;

		java.lang.String __val1;
		__val1 = __buffer.readUTF();

		__obj.method = __val1;

		java.lang.Object[] __val2;
		if (in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) {
			__val2 = null;
		} else {
			__val2 = (java.lang.Object[]) __coder.decode(__buffer, java.lang.Object[].class, null);
		}
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

		long __val7;
		__val7 = in.readLong();

		__obj.reqId = __val7;

		return;
	}

	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public void setSession(ISession session) {
		this.session = session;
	}

	public void setMonitorEnable(boolean isMonitorEnable) {
		this.isMonitorEnable = isMonitorEnable;
	}

	@Override
	public ISession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getReqParentId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTransport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNamespace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setImpl(String impl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object[] getArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getRequestId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRequestId(long reqId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSuccess(boolean isSuccess) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isFinish() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFinish(boolean finish) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isMonitorEnable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Long getMsgId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getRequestParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLogLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPacketSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
}
