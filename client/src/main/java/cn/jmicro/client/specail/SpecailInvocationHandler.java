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
package cn.jmicro.client.specail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.client.InvocationHandler;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.client.RpcClientRequestHandler;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

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
	public Object invoke(Object proxy, String methodName, Object[] args) {
		
		JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, Config.getHost());
		JMicroContext.get().setParam(Constants.CLIENT_REF_METHOD, methodName);
		
		AbstractClientServiceProxyHolder po = (AbstractClientServiceProxyHolder)proxy;

		ServiceItem poItem = po.getHolder().getItem();
		if(poItem == null){
			throw new CommonException("cls["+proxy.getClass().getName()+"] method ["+methodName+"] service not found");
		}
		
		ServiceMethod sm = poItem.getMethod(methodName, args);
		if(sm == null){
			throw new CommonException("cls["+proxy.getClass().getName()+"] method ["+methodName+"] method not found");
		}
		
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, poItem);
		//JMicroContext.setSrvLoggable();
		
		JMicroContext.get().setObject(Constants.PROXY, po);
		
		RpcRequest req = new RpcRequest();
        req.setMethod(methodName);
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
