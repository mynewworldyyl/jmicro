package org.jmicro.api.exception;

import org.jmicro.api.server.IRequest;

public class RpcException extends CommonException {
	 
	private static final long serialVersionUID = 134328923L;
	
	private IRequest req = null;

	public RpcException(IRequest req,String cause){
		super(cause);
		this.req=req;
	}
	
	public RpcException(IRequest req,String cause,Throwable exp){
		super(cause,exp);
		this.req = req;
	}
	
	public RpcException(IRequest req,String key,String cause){
		this(req,key,cause,null);
	}
	
	public RpcException(IRequest req,String key,String cause,Throwable exp){
		super(key,cause,exp);
		this.req= req;
	}

	public IRequest getReq() {
		return req;
	}

	public void setReq(IRequest req) {
		this.req = req;
	}


}
