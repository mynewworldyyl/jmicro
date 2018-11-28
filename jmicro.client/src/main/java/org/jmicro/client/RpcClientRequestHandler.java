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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.exception.TimeoutException;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.AbstractHandler;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.registry.Server;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_CLIENT_HANDLER,side=Constants.SIDE_COMSUMER)
public class RpcClientRequestHandler extends AbstractHandler implements IRequestHandler, IMessageHandler {
	
    private final static Logger logger = LoggerFactory.getLogger(RpcClientRequestHandler.class);
	
	private static final Class<?> TAG = RpcClientRequestHandler.class;
	
	private volatile Map<Long,IResponseHandler> waitForResponse = new ConcurrentHashMap<>();
	
	@Cfg("/ServiceInvocationHandler/openDebug")
	private boolean openDebug;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Cfg("/respBufferSize")
	private int respBufferSize  = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Override
	public IResponse onRequest(IRequest request) {
		AbstractClientServiceProxy proxy =  (AbstractClientServiceProxy)JMicroContext.get().getObject(Constants.PROXY, null);
		RpcResponse resp = null;
		try {
			resp = doRequest(request,proxy);
		} catch (SecurityException | IllegalArgumentException  e) {
			throw new RpcException(request,"",e);
		}
		return resp;
	}

	private RpcResponse doRequest(IRequest req, AbstractClientServiceProxy proxy) {
        
        ServerError se = null;
        		
        ServiceItem si = null;
        ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
        
        int retryCnt = -1;
        int interval = -1;
        int timeout = -1;
        boolean isFistLoop = true;
        
        long lid = JMicroContext.lid(idGenerator);
        
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setProtocol(Message.PROTOCOL_BIN);
		msg.setReqId(req.getRequestId());
		msg.setLinkId(lid);
		msg.setVersion(Constants.MSG_VERSION);
		msg.setLevel(Message.PRIORITY_NORMAL);
		
        do {
        	
			//此方法可能抛出FusingException
        	si = selector.getService(req.getServiceName(),req.getMethod(),req.getArgs(),req.getNamespace(),
        			req.getVersion(), Constants.TRANSPORT_NETTY);
        	
        	if(si == null) {
        		SF.doSubmit(MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req,null);
    			throw new CommonException("Service [" + req.getServiceName() + "] not found!");
    		}
        	
        	Server s = si.getServer(Constants.TRANSPORT_NETTY);
        	
        	JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, s.getHost());
    		JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, s.getPort()+"");
    		
        	if(isFistLoop){
        		
        		retryCnt = sm.getRetryCnt();
        		if(retryCnt < 0){
        			retryCnt = si.getRetryCnt();
        		}
        		interval = sm.getRetryInterval();
    			if(interval < 0){
    				interval = si.getRetryInterval();
    			}
    			timeout = sm.getTimeout();
				if(timeout < 0){
					timeout = si.getTimeout();
				}
				
				msg.setStream(sm.stream);
	    		msg.setNeedResponse(sm.needResponse);
	    		
	    		int f = sm.getMonitorEnable() == 1 ? Constants.FLAG_MONITORABLE : 
	    			(si.getMonitorEnable()== 1?Constants.FLAG_MONITORABLE:0);
	    		msg.setMonitorable(f == 1);
	    		
	    		//msg.setLevel(Message.PRIORITY_NORMAL);
	    		
        	}
    		
    		if(isFistLoop){
    			SF.doSubmit(MonitorConstant.CLIENT_REQ_BEGIN, req,null);
    		}
    	    
    	    IClientSession session = this.sessionManager.getOrConnect(s.getHost(), s.getPort());
    	    //req.setSession(session);
    		
    		msg.setId(this.idGenerator.getLongId(Message.class));
    		msg.setPayload(ICodecFactory.encode(this.codecFactory,req,msg.getProtocol()));
    		
    		if(this.openDebug) {
    			SF.doRequestLog(MonitorConstant.DEBUG,lid,TAG,req,null," do request");
    		}
    		
    		session.write(msg);
    		
    		if(!sm.needResponse && !msg.isStream()) {
    			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			//this.sessionManager.write(msg, null,retryCnt);
    			if(this.openDebug) {
    				SF.doServiceLog(MonitorConstant.DEBUG,TAG,lid,si.serviceName(), si.getNamespace(),
    						si.getVersion(),req.getMethod(), req.getArgs(),null, " no need response and return");
        		}
    			return null;
    		}
    		
    		if(msg.isStream()){
    			String key = req.getRequestId()+"";
    			if(session.getParam(key) != null) {
    				String errMsg = "Failure Callback have been exists reqID："+key;
    				SF.doServiceLog(MonitorConstant.ERROR,TAG,lid,si.serviceName(), si.getNamespace(),
    						si.getVersion(),req.getMethod(), req.getArgs(),null, errMsg);
    				throw new CommonException(errMsg);
    			}
    			session.putParam(key,JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK_CLIENT, null));
    		}
    		
    		//保存返回结果
    		final Map<String,Object> result = new HashMap<>();
    		if(isFistLoop){
    			//超时重试不需要重复注册监听器
    			waitForResponse.put(req.getRequestId(), (message)->{
    				result.put("msg", message);
    				//在请求响应之间做同步
    				synchronized(req) {
        				req.notify();
        			}
    			});
    		}
    		
    		isFistLoop = false;
    		
    		synchronized(req) {
    			try {
    				if(timeout > 0){
    					req.wait(timeout);
    				}else {
    					req.wait();
    				}
    			} catch (InterruptedException e) {
    				logger.error("timeout: ",e);
    			}
    		}
    		
    		Message respMsg = (Message)result.get("msg");
    		result.clear();
    		RpcResponse resp = null;
    		if(respMsg != null){
    			if(respMsg.getPayload() != null){
    				resp = ICodecFactory.decode(this.codecFactory,respMsg.getPayload(),
    						RpcResponse.class,msg.getProtocol());
    				resp.setMsg(respMsg);
    				//req.setMsg(msg);
        			if(this.openDebug) {
        				SF.doResponseLog(MonitorConstant.DEBUG,lid,TAG,resp,null,"reqID ["+resp.getReqId()+"] response");
            		}
    			}else {
        			SF.doMessageLog(MonitorConstant.ERROR,TAG,respMsg,null,"reqID ["+resp.getReqId()+"] response");
        		}
    		} else {
    			//肯定是超时了
    			SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT, req, null);
    		}
    		
    		if(resp != null && resp.isSuccess() && !(resp.getResult() instanceof ServerError)) {
    			if(!msg.isStream()) {
    				//同步请求成功，直接返回
        			SF.doSubmit(MonitorConstant.CLIENT_REQ_OK, req, resp,null);
        			req.setFinish(true);
        			waitForResponse.remove(req.getRequestId());
        			return resp;
    			} else {
    				//异步请求
    				//异步请求，收到一个确认包
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS, req, resp,null);
        			req.setFinish(true);
        			waitForResponse.remove(req.getRequestId());
        			return resp;
    			}
    		}
    		
    		//下面是此次请求失败,进入重试处理过程
    		StringBuffer sb = new StringBuffer();
			if(se!= null){
				sb.append(se.toString());
			}
			sb.append(" host[").append(s.getHost()).append("] port [").append(s.getPort())
			.append("] service[").append(si.getServiceName())
			.append("] method[").append(sm.getMethodName())
			.append("] param[").append(sm.getMethodParamTypes());
    		
    		if(resp == null){
    			if(retryCnt > 0){
    				sb.append("] do retry: ").append(retryCnt);
    			} else {
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL, req, null);
    				sb.append("] timeout request and stop retry: ").append(retryCnt);
    				throw new TimeoutException(req,sb.toString());
    			}
    			SF.doRequestLog(MonitorConstant.ERROR,msg.getLinkId(),TAG,req,null,sb.toString());
    		
    			if(interval > 0 && retryCnt > 0){
    				try {
    					//超时重试间隔
    					Thread.sleep(si.getRetryInterval());
    				} catch (InterruptedException e) {
    					logger.error("Sleep exceptoin ",e);
    				}
    				SF.doRequestLog(MonitorConstant.WARN,lid,TAG,req,null," do retry");
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_RETRY, req, resp,null);
    				continue;//重试循环
    			}
    			
    		}else if(resp.getResult() instanceof ServerError){
				//服务器已经发生错误，是否需要重试
				 se = (ServerError)resp.getResult();
				 //logger.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR, req, null);
				 SF.doResponseLog(MonitorConstant.ERROR,lid,TAG,resp,null,se.toString());
				 throw new RpcException(req,sb.toString());
			} else if(!resp.isSuccess()){
				 //服务器正常逻辑处理错误，不需要重试，直接失败
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR, req, resp,null);
				 SF.doResponseLog(MonitorConstant.ERROR,lid,TAG,resp,null);
			     throw new RpcException(req,sb.toString());
			}
    		//代码不应该走到这里，如果走到这里，说明系统还有问题
    		throw new CommonException(sb.toString());
    		
        }while(retryCnt-- > 0);
        throw new CommonException("Service:"+req.getServiceName()+", Method: "+req.getMethod()+", Params: "+req.getArgs());
	}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_RRESP_JRPC;
	}

	@Override
	public void onMessage(ISession session,Message msg) {
		//receive response
		IResponseHandler handler = waitForResponse.get(msg.getReqId());
		if(this.openDebug) {
			SF.doMessageLog(MonitorConstant.DEBUG,TAG,msg,null," receive message");
		}
		if(handler!= null){
			handler.onResponse(msg);
		} else {
			SF.doMessageLog(MonitorConstant.ERROR,TAG,msg,null," handler not found");
			logger.error("msdId:"+msg.getId()+",reqId:"+msg.getReqId()+",linkId:"+msg.getLinkId()+" IGNORE");
		}
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
}
