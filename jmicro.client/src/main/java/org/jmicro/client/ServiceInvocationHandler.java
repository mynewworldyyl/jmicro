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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.monitor.v2.MRpcItem;
import org.jmicro.api.monitor.v2.MonitorManager;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.InterceptorManager;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:47
 */
@Component(value=Constants.DEFAULT_INVOCATION_HANDLER, lazy=false, side=Constants.SIDE_COMSUMER)
public class ServiceInvocationHandler implements InvocationHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceInvocationHandler.class);
	
	private static final Class<?> TAG = ServiceInvocationHandler.class;
	
	@Cfg("/ServiceInvocationHandler/openDebug")
	private boolean openDebug;
	
	@Inject(required=true)
	private InterceptorManager intManager;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private MonitorManager monitor;
	
	public ServiceInvocationHandler(){}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		Object obj = null;
		RpcRequest req = null;
		IResponse resp = null;
		try {
			
			JMicroContext cxt = JMicroContext.get();
			
			ServiceItem si = cxt.getParam(Constants.SERVICE_ITEM_KEY, null);
			
			req = new RpcRequest();
			req.setMethod(method.getName());
			req.setServiceName(si.getKey().getServiceName());
			req.setNamespace(si.getKey().getNamespace());
			req.setVersion(si.getKey().getVersion());
			req.setArgs(args);
			req.setRequestId(idGenerator.getLongId(IRequest.class));
			req.setTransport(Constants.TRANSPORT_NETTY);
			req.setImpl(si.getImpl());
			
			if(!JMicroContext.existLinkId() ) {
				//新建一个RPC链路开始
				JMicroContext.lid();
				cxt.setParam(Constants.NEW_LINKID, true);
				if(JMicroContext.get().isMonitorable()) {
					SF.linkStart(TAG.getName(),req);
					MRpcItem mi = cxt.getMRpcItem();
					mi.setReq(req);
					mi.setReqId(req.getRequestId());
					mi.setLinkId(JMicroContext.lid());
				}
			} else {
				cxt.setParam(Constants.NEW_LINKID, false);
			}
			
			if(JMicroContext.get().isDebug()) {
				JMicroContext.get().getDebugLog()
				.append(method.getName())
				.append(",reqID:").append(req.getRequestId())
				.append(",linkId:").append(JMicroContext.lid());
			}
			
			resp = this.intManager.handleRequest(req);
			
			obj = resp == null ? null :resp.getResult();
		} catch(Throwable ex) {
			if(ex instanceof CommonException) {
				throw ex;
			} else {
				logger.error("ServiceInvocationHandler error:",ex);
				throw new RpcException(req,ex);
			}
		} finally {
			if(JMicroContext.get().getObject(Constants.NEW_LINKID,null) != null &&
					JMicroContext.get().getBoolean(Constants.NEW_LINKID,false) ) {
				//RPC链路结束
				SF.linkEnd(TAG.getName(),resp);
				JMicroContext.get().removeParam(Constants.NEW_LINKID);
			}
			JMicroContext.get().debugLog(0);
			JMicroContext.get().submitMRpcItem(monitor);
		}
       /* if("intrest".equals(method.getName())) {
        	//代码仅用于测试
        	logger.debug("result type:{},value:{}",obj.getClass().getName(),obj.toString());
        }*/
		
        return obj;
	
	}
	
}
