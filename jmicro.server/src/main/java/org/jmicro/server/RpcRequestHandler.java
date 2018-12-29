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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.net.AbstractHandler;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_HANDLER,lazy=false,active=true,side=Constants.SIDE_PROVIDER)
public class RpcRequestHandler extends AbstractHandler implements IRequestHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
	
	@Override
	public IResponse onRequest(IRequest request) {
		Object obj = JMicroContext.get().getObject(Constants.SERVICE_OBJ_KEY, null);
		RpcResponse resp = null;
		try {
			Method m = ServiceLoader.getServiceMethod(obj, request);
			if(m != null) {
				/*if(m.getName().equals("publishData")) {
					logger.debug("debug info");;
				}*/
				Object result = m.invoke(obj, request.getArgs());
				resp = new RpcResponse(request.getRequestId(),result);
				resp.setMonitorEnable(request.isMonitorEnable());
				resp.setSuccess(true);
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RpcException(request,"",e);
		}
		return resp;
	}

}
