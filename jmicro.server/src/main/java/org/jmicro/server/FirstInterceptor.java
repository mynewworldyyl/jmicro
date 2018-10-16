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
package org.jmicro.server;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.server.AbstractInterceptor;
import org.jmicro.api.server.IInterceptor;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IRequestHandler;
import org.jmicro.api.server.IResponse;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:30
 */
@Component(Constants.FIRST_INTERCEPTOR)
@Interceptor
public class FirstInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(FirstInterceptor.class);
	
	@Cfg(value ="/defaultLimiterName", required=false, changeListener="limiterName")
	private String defaultLimiterName="gavaLimiter";
	
	@Cfg("/respBufferSize")
	private int respBufferSize;
	
	private ILimiter limiter=null;
	
	public FirstInterceptor() {}
	
	public void init() {
		limiterName("defaultLimiterName");
	}
	
	public void limiterName(String fieldName){
		if(fieldName == null || "".equals(fieldName.trim())){
			return;
		}
		
		if(fieldName.trim().equals("defaultLimiterName")){
			limiter = ComponentManager.getObjectFactory().getByName(defaultLimiterName);
		}
	}
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		if(limiter != null){
			boolean r = limiter.apply(req);
			if(!r){
				logger.warn("Limit exceep, forbidon this request");
				return fastFail(req);
			}
		}
		IResponse resp = handler.onRequest(req);
		return resp;
	}

	private IResponse fastFail(IRequest req) {
		ServerError se = new ServerError();
		se.setErrorCode(ServerError.SE_LIMITER);
		se.setMsg("");
		return new RpcResponse(req.getRequestId(),se);
	}
}
