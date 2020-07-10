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

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.client.InvocationHandler;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.monitor.MonitorClient;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.InterceptorManager;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

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
	private MonitorClient monitor;
	
	public ServiceInvocationHandler(){}
	
	
	public Object invoke(Object proxy, String methodName, Object[] args){
		
		Object obj = null;
		RpcRequest req = null;
		IResponse resp = null;
		try {
			
			JMicroContext cxt = JMicroContext.get();
			
			ServiceItem si = cxt.getParam(Constants.SERVICE_ITEM_KEY, null);
			
			req = new RpcRequest();
			req.setMethod(methodName);
			req.setServiceName(si.getKey().getServiceName());
			req.setNamespace(si.getKey().getNamespace());
			req.setVersion(si.getKey().getVersion());
			req.setArgs(args);
			req.setRequestId(idGenerator.getLongId(IRequest.class));
			req.setTransport(Constants.TRANSPORT_NETTY);
			req.setImpl(si.getImpl());
			req.putObject(JMicroContext.LOGIN_KEY, cxt.getString(JMicroContext.LOGIN_KEY, null));
			req.setReqParentId(cxt.getLong(JMicroContext.REQ_PARENT_ID, 0L));
			
			if(!JMicroContext.existLinkId() ) {
				//新建一个RPC链路开始
				JMicroContext.lid();
				cxt.setParam(Constants.NEW_LINKID, true);
				if(JMicroContext.get().isMonitorable()) {
					SF.eventLog(MC.MT_LINK_START, MC.LOG_NO, TAG, null);
				}
			} else {
				cxt.setParam(Constants.NEW_LINKID, false);
			}
			
			if(JMicroContext.get().isMonitorable()) {
				MRpcItem mi = cxt.getMRpcItem();
				mi.setReq(req);
				mi.setReqId(req.getRequestId());
				mi.setLinkId(JMicroContext.lid());
				mi.setReqParentId(req.getReqParentId());
			}
			
			if(JMicroContext.get().isDebug()) {
				JMicroContext.get().getDebugLog()
				.append(methodName)
				.append(",reqID:").append(req.getRequestId())
				.append(",linkId:").append(JMicroContext.lid());
			}
			
			resp = this.intManager.handleRequest(req);
			
			obj = resp == null ? null : resp.getResult();
		} catch(Throwable ex) {
			if(ex instanceof CommonException) {
				throw ex;
			} else {
				logger.error("ServiceInvocationHandler error:",ex);
				throw new RpcException(req,ex);
			}
		} finally {
			if(!JMicroContext.get().isAsync()) {
				if(JMicroContext.get().getObject(Constants.NEW_LINKID,null) != null &&
						JMicroContext.get().getBoolean(Constants.NEW_LINKID,false) ) {
					//RPC链路结束
					SF.eventLog(MC.MT_LINK_END, MC.LOG_NO, TAG, null);
					JMicroContext.get().removeParam(Constants.NEW_LINKID);
				}
				JMicroContext.get().debugLog(0);
				JMicroContext.get().submitMRpcItem(monitor);
			}
			
		}
       /* if("intrest".equals(method.getName())) {
        	//代码仅用于测试
        	logger.debug("result type:{},value:{}",obj.getClass().getName(),obj.toString());
        }*/
		
        return obj;
	
	}
	
}
