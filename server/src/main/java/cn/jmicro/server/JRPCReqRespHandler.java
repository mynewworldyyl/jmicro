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
package cn.jmicro.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.InterceptorManager;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 请求响应式RPC请求
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:50:36
 */
@Component(active=true,value="JRPCReqRespHandler",side=Constants.SIDE_PROVIDER)
public class JRPCReqRespHandler implements IMessageHandler{

	public static final Byte TYPE = Constants.MSG_TYPE_REQ_JRPC;
	
	private static final Class<?> TAG = JRPCReqRespHandler.class;
	
	private static final Logger logger = LoggerFactory.getLogger(JRPCReqRespHandler.class);
	
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
	
	@Inject
	private IAccountService accountManager;
	
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
	    	
	    	//req1为内部类访问
	    	final RpcRequest req1 = ICodecFactory.decode(this.codeFactory,msg.getPayload(),
					RpcRequest.class,msg.getUpProtocol());
	    	
	    	req = req1;
			req.setSession(s);
			req.setMsg(msg);
			
	    	JMicroContext.config(req1,serviceLoader,registry);
	    	
	    	if(msg.isMonitorable()) {
				SF.netIoRead(TAG.getName(),MC.MT_SERVER_JRPC_GET_REQUEST, msg.getLen());
			}
	    	
			resp.setReqId(msg.getReqId());
			resp.setMsg(msg);
			resp.setSuccess(true);
			//resp.setId(idGenerator.getLongId(IResponse.class));
			
			if(msg.isDebugMode()) {
				msg.setId(idGenerator.getLongId(Message.class));
				msg.setInstanceName(Config.getInstanceName());
			}
	    	
	    	ActInfo ai = null;
			
			if(req1.getParams().containsKey(JMicroContext.LOGIN_KEY)) {
				String lk = (String)req1.getParams().get(JMicroContext.LOGIN_KEY);
				if(StringUtils.isNotEmpty(lk)) {
					ai = this.accountManager.getAccount(lk);
					if(ai == null) {
						ServerError se = new ServerError(ServerError.SE_INVLID_LOGIN_KEY,"Invalid login key!");
						resp.setResult(se);
						resp.setSuccess(false);
						msg.setPayload(ICodecFactory.encode(codeFactory, resp, msg.getUpProtocol()));
						SF.eventLog(MC.MT_INVALID_LOGIN_INFO,MC.LOG_ERROR, TAG,se.toString());
						s.write(msg);
						return;
					}else {
						JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
						JMicroContext.get().setAccount(ai);;
					}
				}
			}
			
	    	if(msg.isDebugMode()) {
	    		JMicroContext.get().appendCurUseTime("Server end decode req",true);
    		}
	    	
			if(!msg.isNeedResponse()){
				//无需返回值
				//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			if(openDebug) {
        			//logger.info("Not need response req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
        		}
				interceptorManger.handleRequest(req);
				//SF.doSubmit(MonitorConstant.SERVER_REQ_OK, req,resp,null);
				if(msg.isMonitorable()) {
					SF.netIoRead(TAG.getName(),MC.MT_SERVER_JRPC_RESPONSE_SUCCESS, 0);
				}
				return;
			}
			
			//下面处理需要返回值的RPC
			//msg.setReqId(req.getRequestId());
			//msg.setSessionId(req.getSession().getId());
			msg.setVersion(req.getMsg().getVersion());
				
			//同步响应
			resp = (RpcResponse)interceptorManger.handleRequest(req);
			if(resp == null){
				//返回空值情况处理
				resp = new RpcResponse(req.getRequestId(),null);
				resp.setSuccess(true);
			}
			msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getUpProtocol()));
			//请求类型码比响应类型码大1，
			msg.setType((byte)(msg.getType()+1));
			
			//响应消息
			s.write(msg);

			if(msg.isMonitorable()) {
				SF.netIoRead(TAG.getName(),MC.MT_SERVER_JRPC_RESPONSE_SUCCESS, msg.getLen());
			}
			
		} catch (Throwable e) {
			//返回错误
			SF.eventLog(MC.MT_SERVER_ERROR,MC.LOG_ERROR, TAG,"JRPCReq error",e);
			logger.error("JRPCReq error: ",e);
			if(needResp && req != null ){
				//返回错误
				resp = new RpcResponse(req.getRequestId(),new ServerError(0,e.getMessage()));
				resp.setSuccess(false);
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getUpProtocol()));
				msg.setType((byte)(msg.getType()+1));
				msg.setInstanceName(Config.getInstanceName());
				msg.setTime(System.currentTimeMillis());
				s.write(msg);
			}
			s.close(true);
		} finally {
			if(JMicroContext.get().isMonitorable()) {
				MRpcItem item = JMicroContext.get().getMRpcItem();
				item.setReq(req);
				item.setResp(resp);
				item.setReqId(req.getRequestId());
				item.setLinkId(msg.getLinkId());
			}
		}
	}

}
