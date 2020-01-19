
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
package org.jmicro.gateway;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 *
 * @author Yulei Ye
 */
@Component(side = Constants.SIDE_PROVIDER)
public class ApiRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRequestMessageHandler.class);
	private static final Class<?> TAG = ApiRequestMessageHandler.class;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private IObjectFactory objFactory;
	
	@Cfg("/ApiRequestMessageHandler/openDebug")
	private boolean openDebug = false;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_API_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		ApiRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequest.class, msg.getProtocol());
		
		ApiResponse resp = new ApiResponse();
		Object result = null;
		Object srv = JMicro.getObjectFactory().getRemoteServie(req.getServiceName(), 
				req.getNamespace(), req.getVersion(),null);
		
		msg.setType(Constants.MSG_TYPE_API_RESP);
		resp.setReqId(req.getReqId());
		resp.setMsg(msg);
		resp.setSuccess(true);
		resp.setId(idGenerator.getLongId(ApiResponse.class));
		
		//long lid = JMicroContext.lid();

		JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, session.localHost());
		JMicroContext.get().setParam(JMicroContext.LOCAL_PORT, session.localPort()+"");
		JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, session.remoteHost());
		JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, session.remotePort()+"");
		
		JMicroContext.get().mergeParams(req.getParams());
		
		if(srv != null){
			Class<?>[] clazzes = null;
			if(req.getArgs() != null && req.getArgs().length > 0){
				clazzes = new Class<?>[req.getArgs().length];
				for(int index = 0; index < req.getArgs().length; index++){
					clazzes[index] = req.getArgs()[index].getClass();
				}
			} else {
				clazzes = new Class<?>[0];
			}
			
			try {
				AbstractClientServiceProxy proxy = (AbstractClientServiceProxy)srv;
				ServiceItem si = proxy.getItem();
				if(si == null) {
					SF.doRequestLog(MonitorConstant.LOG_ERROR, TAG, req, null," service not found");
					throw new CommonException("Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] not found");
				}
				ServiceMethod sm = si.getMethod(req.getMethod(), clazzes);
				if(sm == null) {
					SF.doRequestLog(MonitorConstant.LOG_ERROR, TAG, req, null," service method not found");
					throw new CommonException("Service mehtod ["+req.getServiceName()+"] method ["+req.getMethod()+"] not found");
				}
				
				Method m = srv.getClass().getMethod(req.getMethod(), clazzes);
				
				JMicroContext.get().configMonitor(sm.getMonitorEnable(), si.getMonitorEnable());
				
				if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
					SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG, req, null," got request");
				}
				
				if(!sm.isNeedResponse()) {
					result = m.invoke(srv, req.getArgs());
					if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
						SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG, req, null," no need response");
					}
					return;
				}
				
				if(sm.isStream()) {
					final JMicroContext jc = JMicroContext.get();
					IMessageCallback<Object> msgReceiver = (rst)->{
						if(session.isClose()) {
							return false;
						}
						JMicroContext.get().mergeParams(jc);
						resp.setSuccess(true);
						resp.setResult(rst);
						resp.setId(idGenerator.getLongId(ApiResponse.class));
						msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
						session.write(msg);
						if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
							SF.doResponseLog(MonitorConstant.LOG_DEBUG,TAG, resp, null," Api gateway stream response");
						}
						return true;
					};
					JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_CLIENT, msgReceiver);
					result = m.invoke(srv, req.getArgs());
					// 返回确认包
					resp.setResult(result);
					if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
						SF.doResponseLog(MonitorConstant.LOG_DEBUG, TAG, resp, null," Api gateway stream comfirm response",
								result!=null ? result.toString():"");
					}
					session.write(msg);
				} else {
					
					result = m.invoke(srv, req.getArgs());
					
					resp.setResult(result);
					msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
					if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
						SF.doResponseLog(MonitorConstant.LOG_DEBUG, TAG, resp, null," one response");
					}
					session.write(msg);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | CommonException e) {
				logger.error("",e);
				result = new ServerError(0,e.getMessage());
				resp.setSuccess(false);
				resp.setResult(result);
				SF.doResponseLog(MonitorConstant.LOG_ERROR, TAG, resp, e," service error");
			}
		} else {
			resp.setSuccess(false);
			resp.setResult(result);
			msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
			SF.doResponseLog(MonitorConstant.LOG_ERROR, TAG, resp, null," service instance not found");
			session.write(msg);
		}
	}

}
