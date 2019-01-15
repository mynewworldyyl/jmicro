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
import java.util.concurrent.TimeUnit;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.exception.TimeoutException;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.monitor.ServiceCounter;
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
import org.jmicro.api.timer.TimerTicker;
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
	
	@Cfg("/RpcClientRequestHandler/openDebug")
	private boolean openDebug=false;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Cfg("/respBufferSize")
	private int respBufferSize  = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	//测试统计模式使用
	//@Cfg(value="/RpcClientRequestHandler/clientStatis",defGlobal=false)
	//private boolean clientStatis=false;
	
	//private ServiceCounter counter = null;
	
	public void init() {
		/*if(clientStatis) {
			counter = new ServiceCounter("RpcClientRequestHandler",
					AbstractMonitorDataSubscriber.YTPES,10,2,TimeUnit.SECONDS);
			TimerTicker.getDefault(2000L).addListener("RpcClientRequestHandler", (key,att)->{
				System.out.println("======================================================");
				logger.debug("总请求:{}, 总响应:{},QPS:{}/S",
						counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BEGIN)
						,counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,MonitorConstant.CLIENT_REQ_OK,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)
						,counter.getAvg(TimeUnit.SECONDS,MonitorConstant.CLIENT_REQ_OK)
						);
			}, null);
		}*/
	}
	
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
        
        long lid = JMicroContext.lid();
        
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setProtocol(Message.PROTOCOL_BIN);
		msg.setReqId(req.getRequestId());
		msg.setLinkId(lid);
		msg.setVersion(Message.MSG_VERSION);
		msg.setLevel(Message.PRIORITY_NORMAL);
		
		boolean isDebug = false;
		
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
				
				msg.setStream(sm.isStream());
				msg.setDumpDownStream(sm.isDumpDownStream());
				msg.setDumpUpStream(sm.isDumpUpStream());
	    		msg.setNeedResponse(sm.isNeedResponse());
	    		msg.setLoggable(JMicroContext.get().isLoggable(false));
	    		
	    		int f = sm.getMonitorEnable() == 1 ? 1 : (si.getMonitorEnable()== 1?1:0);
	    		msg.setMonitorable(f == 1);
	    		
	    		f = sm.getDebugMode() == 1 ? 1 : (si.getDebugMode()== 1?1:0);
	    		isDebug = f == 1;
	    		msg.setDebugMode(isDebug);
	    		
	    		if(isDebug) {
	    			msg.setInstanceName(Config.getInstanceName());
	    			msg.setTime(System.currentTimeMillis());
	    			lid = JMicroContext.lid();
	    			msg.setLinkId(lid);
	    			msg.setMethod(sm.getKey().getMethod());
	    		}
	    		
        	}
    		
        	msg.setPayload(ICodecFactory.encode(this.codecFactory,req,msg.getProtocol()));
        	
        	//JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
    		//JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, si);
    		//JMicroContext.setSrvLoggable();
    		msg.setLoggable(JMicroContext.get().isLoggable(false));
    	    
    	    IClientSession session = this.sessionManager.getOrConnect(s.getHost(), s.getPort());
    		
    	    if(isDebug) {
    	    	msg.setId(this.idGenerator.getLongId(Message.class.getName()));
    	    }
    		
    		if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
    			SF.doRequestLog(MonitorConstant.LOG_DEBUG,lid,TAG,req,null," do request");
    		}
    		
    		session.write(msg);
    		
    		if(!sm.isNeedResponse() && !msg.isStream()) {
    			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			//this.sessionManager.write(msg, null,retryCnt);
    			if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
    				SF.doServiceLog(MonitorConstant.LOG_DEBUG,TAG,lid,sm,null, " no need response and return");
        		}
    			return null;
    		}
    		
    		SF.doSubmit(MonitorConstant.CLIENT_REQ_BEGIN, req,null);
    		session.increment(MonitorConstant.CLIENT_REQ_BEGIN);
    		
    		if(msg.isStream()){
    			String key = req.getRequestId()+"";
    			if(session.getParam(key) != null) {
    				String errMsg = "Failure Callback have been exists reqID："+key;
    				if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_ERROR)) {
    					SF.doServiceLog(MonitorConstant.LOG_ERROR,TAG,lid,sm,null, errMsg);
    				}
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
    				} else {
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
    				try {
    					resp = ICodecFactory.decode(this.codecFactory,respMsg.getPayload(),
        						RpcResponse.class,msg.getProtocol());
    				}catch(ClassCastException e) {
    					logger.error("",e);
    					waitForResponse.remove(req.getRequestId());
    					throw new CommonException(req.toString(),e); 
    				}
    				
    				resp.setMsg(respMsg);
    				//req.setMsg(msg);
    				if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
        				SF.doResponseLog(MonitorConstant.LOG_DEBUG,lid,TAG,resp,null,"reqID ["+resp.getReqId()+"] response");
            		}
    			} else {
        			SF.doMessageLog(MonitorConstant.LOG_ERROR,TAG,respMsg,null,"reqID ["+resp.getReqId()+"] response");
        		}
    		} else {
    			SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT, req, null);
    			session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT);
    		}
    		
    		if(resp != null && resp.isSuccess() && !(resp.getResult() instanceof ServerError)) {
    			if(!msg.isStream()) {
    				//同步请求成功，直接返回
        			SF.doSubmit(MonitorConstant.CLIENT_REQ_OK, req, resp,null);
        			session.increment(MonitorConstant.CLIENT_REQ_OK);
        			
        			req.setFinish(true);
        			waitForResponse.remove(req.getRequestId());
        			return resp;
    			} else {
    				//异步请求
    				//异步请求，收到一个确认包
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS, req, resp,null);
    				session.increment(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS);
    				
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
			.append("] service[").append(si.getKey().getServiceName())
			.append("] method[").append(sm.getKey().getMethod())
			.append("] param[").append(sm.getKey().getParamsStr());
    		
    		if(resp == null){
    			if(retryCnt > 0){
    				sb.append("] do retry: ").append(retryCnt);
    				//SF.doRequestLog(MonitorConstant.WARN,msg.getLinkId(),TAG,req,null,sb.toString());
    			} else {
    				
    				//断开新打开连接
    				session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
    				
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL, req, null);
    				sb.append("] timeout request and stop retry: ").append(retryCnt)
    				.append(",reqId:").append(req.getRequestId()).append(", LinkId:").append(lid);
    				SF.doRequestLog(MonitorConstant.LOG_ERROR,msg.getLinkId(),TAG,req,null,sb.toString());
    				
    				if(session.getFailPercent() > 50) {
        				logger.warn("session.getFailPercent() > 50,Close session: {},Percent:{}",
        						sb.toString(),session.getFailPercent());
        				session.close(true);
        				session = null;
    				}
    				
    				//肯定是超时了
    				throw new TimeoutException(req,sb.toString());
    			}
    		
    			if(interval > 0 && retryCnt > 0){
    				try {
    					//超时重试间隔
    					Thread.sleep(si.getRetryInterval());
    				} catch (InterruptedException e) {
    					logger.error("Sleep exceptoin ",e);
    				}
    				SF.doRequestLog(MonitorConstant.LOG_WARN,lid,TAG,req,null," do retry");
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_RETRY, req, resp,null);
    				session.increment(MonitorConstant.CLIENT_REQ_RETRY);
    				continue;//重试循环
    			}
    			
    		}else if(resp.getResult() instanceof ServerError){
				//服务器已经发生错误,是否需要重试
				 se = (ServerError)resp.getResult();
				 //logger.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR, req, null);
				 SF.doResponseLog(MonitorConstant.LOG_ERROR,lid,TAG,resp,null,se.toString());
				 session.increment(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				 
				 throw new RpcException(req,sb.toString());
			} else if(!resp.isSuccess()){
				 //服务器正常逻辑处理错误，不需要重试，直接失败
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR, req, resp,null);
				 SF.doResponseLog(MonitorConstant.LOG_ERROR,lid,TAG,resp,null);
				 session.increment(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR);
				 
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
		if(msg.isLoggable()) {
			SF.doMessageLog(MonitorConstant.LOG_DEBUG,TAG,msg,null," receive message");
		}
		if(handler!= null){
			handler.onResponse(msg);
		} else {
			SF.doMessageLog(MonitorConstant.LOG_ERROR,TAG,msg,null," handler not found");
			logger.error("msdId:"+msg.getId()+",reqId:"+msg.getReqId()+",linkId:"+msg.getLinkId()+" IGNORE");
			session.increment(ISession.CLIENT_HANDLER_NOT_FOUND);
			if(session.getTakePercent(ISession.CLIENT_HANDLER_NOT_FOUND) > 50) {
				//断开重连
				session.close(true);
			}
		}
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
}

