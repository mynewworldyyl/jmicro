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

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月27日 下午10:40:08
 */
@Component(value=Constants.LAST_CLIENT_INTERCEPTOR, side=Constants.SIDE_COMSUMER)
@Interceptor
public class LastClientInterceptor extends AbstractInterceptor implements IInterceptor {

	private final static Logger logger = LoggerFactory.getLogger(LastClientInterceptor.class);
	
	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest request) throws RpcException {
		//logger.debug(Constants.LAST_CLIENT_INTERCEPTOR + " before");
		return handler.onRequest(request);
		//logger.debug(Constants.LAST_CLIENT_INTERCEPTOR + " after");
		//return resp;
	}
	
	
}
