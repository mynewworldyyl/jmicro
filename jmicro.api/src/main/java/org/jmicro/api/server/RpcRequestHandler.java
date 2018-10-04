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
package org.jmicro.api.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.annotation.Handler;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.servicemanager.ServiceLoader;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Handler(Constants.DEFAULT_HANDLER)
public class RpcRequestHandler extends AbstractHandler implements IRequestHandler {

	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Override
	public IResponse onRequest(IRequest request) {
		Object obj = serviceLoader.getService(request.getServiceName()
				,request.getNamespace(),request.getVersion());
		
		Object[] args = request.getArgs();
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			parameterTypes[i] = args[i].getClass();
		}
		
		RpcResponse resp = null;
		try {
			Method m = obj.getClass().getMethod(request.getMethod(), parameterTypes);
			if(m != null) {
				Object result = m.invoke(obj, args);
				resp = new RpcResponse(request.getRequestId(),result);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RpcException(request,"",e);
		}
		return resp;
	}

}
