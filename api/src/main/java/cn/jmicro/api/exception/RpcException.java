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
package cn.jmicro.api.exception;

import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.common.CommonException;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:02:42
 */
public final class RpcException extends CommonException {
	 
	private static final long serialVersionUID = 134328923L;
	
	private IRequest req = null;
	
	private IResponse resp = null;

	public RpcException(IRequest req,String cause){
		super(cause);
		this.req=req;
	}
	
	public RpcException(IRequest req,IResponse resp){
		super("fail");
		this.req = req;
		this.resp = resp;
	}
	
	public RpcException(IRequest req,ServerError se){
		this(req,"code:"+se.getErrorCode()+":msg"+se.getMsg());
	}
	
	public RpcException(IRequest req,Throwable exp){
		super("fail",exp);
		this.req= req;
	}

	public IRequest getReq() {
		return req;
	}

	public void setReq(IRequest req) {
		this.req = req;
	}

	public IResponse getResp() {
		return resp;
	}

	public void setResp(IResponse resp) {
		this.resp = resp;
	}

	@Override
	public String toString() {
		String msg = super.toString();
		StringBuffer sb = new StringBuffer(msg);
		if(req != null) {
			sb.append("reqID:").append(req.getRequestId())
			.append(", service: ").append(req.getServiceName())
			.append(", namespace: ").append(req.getNamespace())
			.append(", version: ").append(req.getVersion())
			.append(", method: ").append(req.getMethod())
			.append(", args: ").append(req.getArgs());
		}
		
		if(resp!= null) {
			sb.append(", resp result: ").append(resp.getResult());
		}
		
		return sb.toString();
		
	}

}
