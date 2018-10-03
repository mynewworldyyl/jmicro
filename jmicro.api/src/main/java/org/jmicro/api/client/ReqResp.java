package org.jmicro.api.client;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.Message;

public class ReqResp {

	public Message msg;
	public IRequest req;
	public IResponseHandler handler;
	private int retryCnt = 0;
	public ReqResp(Message msg,IRequest req,IResponseHandler handler,int retryCnt){
		this.msg = msg;
		this.req = req;
		this.handler = handler;
		this.retryCnt = retryCnt;
	}
	
}
