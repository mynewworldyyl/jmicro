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

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.common.CommonException;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年3月9日
 */
public final class AsyncRpcException extends CommonException {
	 
	private static final long serialVersionUID = 134328923L;
	
	private IRequest req = null;

	public AsyncRpcException(IRequest req,String cause){
		super(cause);
		this.req=req;
	}
	
	public AsyncRpcException(IRequest req,IResponse resp){
		super("fail");
		this.req = req;
	}
	
	public AsyncRpcException(IRequest req,RespJRso se){
		this(req,"code:"+se.getCode()+":msg"+se.getMsg());
	}
	
	public AsyncRpcException(IRequest req,Throwable exp){
		super(exp.getMessage());
		this.req= req;
	}

	public IRequest getReq() {
		return req;
	}

	public void setReq(IRequest req) {
		this.req = req;
	}


}
