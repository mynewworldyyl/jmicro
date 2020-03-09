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
package org.jmicro.client.specail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.exception.TimeoutException;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.registry.Server;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.client.RpcClientRequestHandler;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:47
 */
@Component(value="specailInvocationHandler", side=Constants.SIDE_COMSUMER)
public class SpecailInvocationHandler implements InvocationHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(SpecailInvocationHandler.class);
	
	@Cfg(value="/SpecailInvocationHandler/openDebug",defGlobal=false)
	private boolean openDebug=false;
	
	@Inject(value=Constants.DEFAULT_CLIENT_HANDLER)
	private RpcClientRequestHandler rpcHandler;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	public SpecailInvocationHandler(){}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, Config.getHost());
		JMicroContext.get().setParam(Constants.CLIENT_REF_METHOD, method);
		
		AbstractClientServiceProxy po = (AbstractClientServiceProxy)proxy;
		String methodName = method.getName();
		if(method.getDeclaringClass() == Object.class) {
		   throw new CommonException("Invalid invoke ["
				   +method.getDeclaringClass().getName()+"] for method [ "+methodName+"]");
		}

		ServiceItem poItem = po.getItem();
		if(poItem == null){
			throw new CommonException("cls["+method.getDeclaringClass().getName()+"] method ["+method.getName()+"] service not found");
		}
		
		ServiceMethod sm = poItem.getMethod(methodName, args);
		if(sm == null){
			throw new CommonException("cls["+method.getDeclaringClass().getName()+"] method ["+method.getName()+"] method not found");
		}
		
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, poItem);
		//JMicroContext.setSrvLoggable();
		
		JMicroContext.get().setObject(Constants.PROXY, po);
		
		RpcRequest req = new RpcRequest();
        req.setMethod(method.getName());
        req.setServiceName(poItem.getKey().getServiceName());
        req.setNamespace(poItem.getKey().getNamespace());
        req.setVersion(poItem.getKey().getVersion());
        req.setArgs(args);
        req.setRequestId(idGenerator.getLongId(IRequest.class));
        req.setTransport(Constants.TRANSPORT_NETTY);
        req.setImpl(poItem.getImpl());
        
        if(openDebug) {
			logger.debug("Item:{}",poItem.getKey().toKey(true, true, true));
		}
        
        //RpcResponse resp = doRequest(req,po);
        
        IResponse resp = this.rpcHandler.onRequest(req);
        return resp == null ? null :resp.getResult();
	
	}

}
