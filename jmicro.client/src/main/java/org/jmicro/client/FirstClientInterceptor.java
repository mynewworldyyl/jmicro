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
package org.jmicro.client;

import java.lang.reflect.Method;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.AbstractInterceptor;
import org.jmicro.api.net.IInterceptor;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			limiter = JMicro.getObjectFactory().getByName(defaultLimiterName);
		}
	}
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		
		logger.debug(Constants.FIRST_CLIENT_INTERCEPTOR + " before");
		
		if(limiter != null){
			boolean r = limiter.apply(req);
			if(!r){
				logger.warn("Limit exceep, forbidon this request");
				return doFastFail(req,null);
			}
		}
		
		AbstractClientServiceProxy proxy =  (AbstractClientServiceProxy)JMicroContext.get().getObject(Constants.PROXY, null);
		ServiceMethod sm = null;
		String t = ServiceMethod.methodParamsKey(req.getArgs());
		
		for(ServiceMethod m : proxy.getItem().getMethods()){
			if(m.getMethodName().equals(req.getMethod()) 
					&& m.getMethodParamTypes().equals(t)){
				sm = m;
				break;
			}
		}
		
		if(sm == null){
			SF.doSubmit(MonitorConstant.CLIENT_REQ_METHOD_NOT_FOUND, req,null);
			return doFastFail(req,null);
		}
		
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, proxy.getItem());
		
		IResponse resp = handler.onRequest(req);
		
		logger.debug(Constants.FIRST_CLIENT_INTERCEPTOR + " after");
		
		return resp;
	}

	public static IResponse doFastFail(IRequest req,Throwable e) {
		//ServiceItem si = JMicroContext.get().getParam(Constants.SERVICE_ITEM_KEY, null);
		
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(!sm.isBreakable()) {
			//不支持熔断
			throw new RpcException(req,"",e);
		}
		
		RpcResponse resp = new RpcResponse();
		resp.setSuccess(true);
		resp.setReqId(req.getRequestId());
		resp.setMonitorEnable(req.isMonitorEnable());
		
		Method method = JMicroContext.get().getParam(Constants.CLIENT_REF_METHOD, null);
		Class<?> cls = method.getReturnType();
		if(cls == Void.class) {
			resp.setResult(null);
		} else if(!StringUtils.isEmpty(sm.getFailResponse())) {
			//Object result = JsonUtils.getIns().fromJson(sm.getFailResponse(), cls);
			Object v = Utils.getIns().getValue(cls,sm.getFailResponse(),cls);
			resp.setResult(v);
		} else {
			//参数不对，继续抛出异常
			throw new RpcException(req,"",e);
		}
		return resp;
	}
}
