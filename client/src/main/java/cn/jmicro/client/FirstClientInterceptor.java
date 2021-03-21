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
package cn.jmicro.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.limitspeed.ILimiter;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月27日 下午10:44:30
 */
@Component(value=Constants.FIRST_CLIENT_INTERCEPTOR, side=Constants.SIDE_COMSUMER)
@Interceptor
public class FirstClientInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(FirstClientInterceptor.class);
	
	@Cfg(value ="/defaultLimiterName", required=false, changeListener="limiterName")
	private String defaultLimiterName="gavaLimiter";
	
	@Cfg("/respBufferSize")
	private int respBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	private ILimiter limiter = null;
	
	public FirstClientInterceptor() {}
	
	public void init() {
		limiterName("defaultLimiterName");
	}
	
	public void limiterName(String fieldName){
		if(fieldName == null || "".equals(fieldName.trim())){
			return;
		}
		
		if(fieldName.trim().equals("defaultLimiterName")){
			limiter = EnterMain.getObjectFactory().getByName(defaultLimiterName);
		}
	}
	
	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		
		//logger.debug(Constants.FIRST_CLIENT_INTERCEPTOR + " before");
		
		/*if(limiter != null){
			boolean r = limiter.apply(req);
			if(!r){
				logger.warn("Limit exceep, forbidon this request");
				return doFastFail(req,null);
			}
		}*/
		
		return handler.onRequest(req);
		
		//logger.debug(Constants.FIRST_CLIENT_INTERCEPTOR + " after");
		
		//return resp;
	}

	public static IPromise<Object> doFastFail(IRequest req,Throwable e,int code) {
		PromiseImpl<Object> p = new PromiseImpl<>();
		p.setFail(code, e.getLocalizedMessage());
		p.done();
		return p;
	}
}
