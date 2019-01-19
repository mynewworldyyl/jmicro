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

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.debug.LogUtil;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.IWriteCallback;
import org.jmicro.api.net.InterceptorManager;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求响应式RPC请求
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:50:36
 */
@Component(active=true,value="JRPCReqRespHandler",side=Constants.SIDE_PROVIDER)
public class JRPCReqRespHandler implements IMessageHandler{

	public static final Byte TYPE = Constants.MSG_TYPE_REQ_JRPC;
	
	private static final Class<?> TAG = JRPCReqRespHandler.class;
	
	static final Logger logger = LoggerFactory.getLogger(JRPCReqRespHandler.class);
	
	@Inject
	private InterceptorManager interceptorManger;
	
	@Cfg("/JRPCReqRespHandler/openDebug")
	private boolean openDebug=false;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=true)
	private IRegistry registry = null;
	
	@Override
	public Byte type() {
		return TYPE;
	}

	@Override
	public void onMessage(ISession s, Message msg) {
		   
		RpcRequest req = null;
		boolean needResp = true;
		RpcResponse resp =  new RpcResponse();
		
	    try {
	    	
			resp.setReqId(msg.getReqId());
			resp.setMsg(msg);
			resp.setSuccess(true);
			resp.setId(idGenerator.getLongId(IResponse.class));
			
			if(msg.isDebugMode()) {
				msg.setId(idGenerator.getLongId(Message.class));
				msg.setInstanceName(Config.getInstanceName());
				msg.setTime(System.currentTimeMillis());
			}
			
	    	//req1为内部类访问
	    	final RpcRequest req1 = ICodecFactory.decode(this.codeFactory,msg.getPayload(),
					RpcRequest.class,msg.getProtocol());
			req = req1;
			req.setSession(s);
			req.setMsg(msg);
			
			JMicroContext.config(req1,serviceLoader,registry);
			
			if(!msg.isNeedResponse()){
				interceptorManger.handleRequest(req);
				SF.doSubmit(MonitorConstant.SERVER_REQ_OK, req,resp,null);
				return;
			}

			msg.setReqId(req.getRequestId());
			//msg.setSessionId(req.getSession().getId());
			msg.setVersion(req.getMsg().getVersion());
				
			if(msg.isLoggable()){
				SF.doRequestLog(MonitorConstant.LOG_DEBUG,msg.getLinkId(), TAG, req,null,"got REQUEST");
			}
			
			if(req.isStream()){
				
				IWriteCallback callback = new IWriteCallback(){
					@Override
					public boolean send(Object message) {
						if(s.isClose()){
							s.close(true);
							return false;
						}
						RpcResponse resp = new RpcResponse(req1.getRequestId(),message);
						resp.setId(idGenerator.getLongId(IResponse.class));
						resp.setSuccess(true);
						//返回结果包
						msg.setId(idGenerator.getLongId(Message.class));
						msg.setPayload(codeFactory.getEncoder(msg.getProtocol()).encode(resp));
						msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
						
						if(msg.isLoggable()) {
							SF.doResponseLog(MonitorConstant.LOG_DEBUG, msg.getLinkId(), TAG, resp,null,"STREAM",resp.getId()+"");
						}
						s.write(msg);
						return true;
					}
				};
				
				JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_SERVICE, callback);
				JMicroContext jc = JMicroContext.get();
				
				/*
				SuspendableRunnable r = () -> {
					JMicroContext.get().mergeParams(jc);
					interceptorManger.handleProvider(req1);
				};
				//异步响应
				new Fiber<Void>(r).start();*/
				
				Runnable run = ()->{
					JMicroContext.get().mergeParams(jc);
					interceptorManger.handleRequest(req1);
				};
				
				//异步响应
				new Thread(run).start();
				
				//直接返回一个确认包
				resp = new RpcResponse(req.getRequestId(),null);
				resp.setSuccess(true);
				
				msg.setType((byte)(msg.getType()+1));
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getProtocol()));
				
				if(msg.isLoggable()) {
					SF.doResponseLog(MonitorConstant.LOG_DEBUG,msg.getLinkId(),TAG, resp,null,"STREAM Confirm");
				}
				
				s.write(msg);
			
			} else {
				//同步响应
				resp = (RpcResponse)interceptorManger.handleRequest(req);
				if(resp == null){
					resp = new RpcResponse(req.getRequestId(),null);
					resp.setSuccess(true);
				}
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getProtocol()));
				msg.setType((byte)(msg.getType()+1));
				
				if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
					SF.doResponseLog(MonitorConstant.LOG_DEBUG,msg.getLinkId(), TAG, resp,null);
				}
				s.write(msg);
			}
			SF.doSubmit(MonitorConstant.SERVER_REQ_OK, req,resp,null);
		} catch (Throwable e) {
			SF.doMessageLog(MonitorConstant.LOG_ERROR, TAG, msg,e);
			SF.doSubmit(MonitorConstant.SERVER_REQ_ERROR, req,resp,null);
			logger.error("reqHandler error: ",e);
			if(needResp && req != null ){
				resp = new RpcResponse(req.getRequestId(),new ServerError(0,e.getMessage()));
				resp.setSuccess(false);
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getProtocol()));
				msg.setType((byte)(msg.getType()+1));
				msg.setInstanceName(Config.getInstanceName());
				msg.setTime(System.currentTimeMillis());
				s.write(msg);
			}
			
			s.close(true);
		}
	}

}
