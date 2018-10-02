package org.jmicro.api.server;

import java.nio.ByteBuffer;

import org.jmicro.api.AbstractObjectMapSupport;
import org.jmicro.api.IDable;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.codec.IEncodable;

public class RpcResponse extends AbstractObjectMapSupport implements IEncodable,IResponse,IDable{

	private long id;
	
	private transient Message msg;
	
	private Long reqId;
	
	private Object result;
	
	public RpcResponse() {}
	
	public RpcResponse(long reqId,Object result){
		this.reqId = reqId;
		this.result = result;
	}
	
	public RpcResponse(long reqId){
		this.reqId = reqId;
	}
	
	@Override
	public void decode(ByteBuffer ois) {
		this.id = ois.getLong();
		this.reqId = ois.getLong();
		this.result = Decoder.decodeObject(ois);
	}

	@Override
	public void encode(ByteBuffer oos) {
		oos.putLong(this.id);
		oos.putLong(this.reqId);
		Encoder.encodeObject(oos, result);
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public void setId(long id) {
		this.id=id;
	}

	public Long getRequestId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
		
}
