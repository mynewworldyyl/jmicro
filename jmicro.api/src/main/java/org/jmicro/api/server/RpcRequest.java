package org.jmicro.api.server;

import java.nio.ByteBuffer;

import org.jmicro.api.AbstractRpcProtocolMessage;

public class RpcRequest extends AbstractRpcProtocolMessage implements IRequest{
	
	private transient ISession session;
	
	protected Long reqId=-1L;
	
	private transient Message msg;
	
	private transient boolean success = false;
	
	private transient boolean finish = false;
	
	public RpcRequest(){}
	
	@Override
	public void decode(ByteBuffer ois) {
		this.reqId = ois.getLong();
		super.decode(ois);
	}

	@Override
	public void encode(ByteBuffer oos) {
		oos.putLong(this.reqId);
		super.encode(oos);
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean isSuccess) {
		this.success = isSuccess;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public ISession getSession() {
		return session;
	}

	public void setSession(ISession session) {
		this.session = session;
	}
	
	public void setRequestId(Long reqId) {
		this.reqId = reqId;
	}

	@Override
	public Long getRequestId() {
		return this.reqId;
	}
	
	@Override
	public int hashCode() {
		return reqId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof RpcRequest)) {
			return false;
		}
		return reqId == ((RpcRequest)obj).reqId;
	}
	
}