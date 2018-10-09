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

import java.lang.reflect.Method;

import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.servicemanager.ServiceLoader;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:38
 */
public interface IInterceptor {

	public static Method getMethod(ServiceLoader sl ,IRequest req){
		Object obj = sl.getService(req.getServiceName()
				,req.getNamespace(),req.getVersion());
		
		Object[] args = req.getArgs();
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			parameterTypes[i] = args[i].getClass();
		}
		
		try {
			Method m = ProxyObject.getTargetCls(obj.getClass()).getMethod(req.getMethod(), parameterTypes);
			return m;
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			throw new RpcException(req,"",e);
		}
	}
	
	public static boolean isNeedResponse(ServiceLoader sl ,IRequest req){
		Method m = getMethod(sl,req);
		if(m == null || !m.isAnnotationPresent(SMethod.class)){
			return true;
		}else {
			SMethod sm = m.getAnnotation(SMethod.class);
			return sm.noNeedResponse() == 0;
		}
	}
	
	IResponse intercept(IRequestHandler handler,IRequest req) throws RpcException;
}
