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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exception.AsyncRpcException;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.exception.TimeoutException;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.loadbalance.ISelector;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.AbstractHandler;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.Server;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.TimeUtils;

/** 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_CLIENT_HANDLER,side=Constants.SIDE_COMSUMER)
public class RpcClientRequestHandler extends AbstractHandler implements IRequestHandler, IMessageHandler {
	
    private final static Logger logger = LoggerFactory.getLogger(RpcClientRequestHandler.class);

    private static final Class<?> TAG = RpcClientRequestHandler.class;
	
	private final static Map<Long,IResponseHandler> waitForResponse = new ConcurrentHashMap<>();
	
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
	private PubSubManager pubsubManager;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ServiceManager srvManager;
	
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
			 /*if(openDebug) {
				logger.info("onRequest Method:"+request.getMethod()+",Service:" + request.getServiceName());
			   }*/
			 //请求开始
			 SF.reqStart(TAG.getName(),request);
			 //SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG,null, "request start");
			
			 ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
			 String mkey = sm.getKey().getMethod();
	         AsyncConfig ac = proxy.getAcs(mkey);
	         if(ac == null && JMicroContext.get().exists(Constants.ASYNC_CONFIG)) {
	        	 ac = (AsyncConfig)JMicroContext.get().getParam(Constants.ASYNC_CONFIG, null);
	        	 if(!ac.getForMethod().equals(request.getMethod())) {
	        		 //不属于当前方法的异步配置
	        		 ac = null;
	        	 }
	         }
	         
	         if(ac != null && ac.isEnable()) {
	        	 if(ac.getCondition().equals(AsyncConfig.ASYNC_DIRECT)) {
	        		 //客户端直接做异步
	        		 return doAsyncInvoke(proxy,request,sm,ac);
	        	 } else {
	        		 //可以异步调用并异步返回结果
		        	 request.putObject(mkey, ac);
		        	 resp = doRequest(request,proxy);
	        	 }
	         } else {
	        	 resp = doRequest(request,proxy);
	         }
		} catch (SecurityException | IllegalArgumentException  e) {
			throw new RpcException(request,e);
		} finally {
			//无论成功或失败都有一个结束事件
			SF.reqEnd(TAG.getName(),resp);
		}
		return resp;
	}

	private IResponse doAsyncInvoke(AbstractClientServiceProxy proxy, IRequest req,ServiceMethod sm,AsyncConfig ac) {
		
		String topic = sm.getKey().toKey(false, false, false);
		
		Map<String,Object> cxt = new HashMap<>();
		//结果回调RPC方法
		//cxt.put(topic, ac);
		
		//链路相关ID
		cxt.put(JMicroContext.LINKER_ID, JMicroContext.lid());
		cxt.put(JMicroContext.REQ_ID, req.getRequestId());
		cxt.put(JMicroContext.MSG_ID, req.getMsgId());
		
		PSData data = new PSData();
		data.setContext(cxt);
		data.setData(req.getArgs());
		data.setTopic(topic);
		
		data.setFlag(PSData.flag(PSData.FLAG_PUBSUB,PSData.FLAG_ASYNC_METHOD));
		
		if(sm.isNeedResponse()) {
			
			ServiceItem si = this.getServiceItem(ac);
			
			if(si == null) {
				String msg = "Async service not found for:"+sm.getKey().toKey(false, false, false)+",async :"+ ac.toString();
				logger.error(msg);
				SF.doRequestLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, null, msg);
				throw new RpcException(req,msg);
			}
			
			ServiceMethod callback = si.getMethod(ac.getMethod(), ac.getParamStr());
			if(callback == null) {
				String msg = "Async method not found for:"+sm.getKey().toKey(false, false, false)+",async :"+ ac.toString();
				logger.error(msg);
				SF.doRequestLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, null, msg);
				throw new RpcException(req,msg);
			}
			
			data.setCallback(callback.getKey());
			data.mergeContext(ac.getContext());
		}
		
		//异步后,就不一定是本实例接收到此RPC调用了
		Integer msgId = idGenerator.getIntId(PSData.class);
		if(msgId == null) {
			throw new CommonException("Fail to get msg ID");
		}
		data.setId(msgId);
		long id = pubsubManager.publish(data);
		if(openDebug) {
			logger.info("Do async req:"+id+",Method:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
		}
		RpcResponse resp = new RpcResponse();
		if(id == PubSubManager.PUB_OK) {
			//resp.setReqId(data.getId());
			//异步调用RPC返回空值
			return resp;
		} else {
			String msg = "ErrorCode:"+id+",异步调用失败"+sm.getKey().toKey(false, false, false);
			SF.doRequestLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR,TAG,null,msg);
			/*ServerError se = new ServerError();
			se.setErrorCode(ServerError.SE_ASYNC_PUBSUB_FAIL);
			se.setMsg(msg);
			resp.setResult(se);
			resp.setSuccess(false);
			*/
			throw new AsyncRpcException(req,msg);
		}
	}

	private RpcResponse doRequest(IRequest req, AbstractClientServiceProxy proxy) {
        
        ServerError se = null;
        		
        JMicroContext cxt = JMicroContext.get();
        
        ServiceItem si = cxt.getParam(Constants.DIRECT_SERVICE_ITEM,null);
        ServiceMethod sm = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
        
        int retryCnt = -1;
        long interval = -1;
        long timeout = -1;
        //第一次进来在同一个线程中,同一个调用的超时重试使用
        boolean isFistLoop = true;
        
        //long lid = 0;
        
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setReqId(req.getRequestId());
		
		msg.setVersion(Message.MSG_VERSION);
		msg.setPriority(Message.PRIORITY_NORMAL);
		
        do {
        	
        	if(si == null) {
        		//此方法可能抛出FusingException
        		si = selector.getService(req.getServiceName(),req.getMethod(),req.getArgs(),req.getNamespace(),
            			req.getVersion(), Constants.TRANSPORT_NETTY);
        	}
        	
        	if(si == null) {
        		//SF.doSubmit(MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req,null);
        		//服务未找到，或服务不存在
        		SF.serviceNotFound(TAG.getSimpleName(), "Service [" + req.getServiceName() + "] not found!");
    			throw new RpcException(req,"Service [" + req.getServiceName() + "] not found!");
    		}
        	
        	req.setImpl(si.getCode()+"");
        	
        	Server s = si.getServer(Constants.TRANSPORT_NETTY);
        	
        	cxt.setParam(JMicroContext.REMOTE_HOST, s.getHost());
        	cxt.setParam(JMicroContext.REMOTE_PORT, s.getPort()+"");
    		
    		//保存返回结果
    		//final Map<String,Object> result = new HashMap<>();
    		final MessageHolder mh = new MessageHolder();
    		
        	if(isFistLoop){
        		//超时重试时,只需要执行一次此代码块
        		isFistLoop = false;
        		retryCnt = sm.getRetryCnt();
        		if(retryCnt < 0){
        			retryCnt = si.getRetryCnt();
        		}
        		
        		interval = sm.getRetryInterval();
    			if(interval < 0){
    				interval = si.getRetryInterval();
    			}
    			interval = TimeUtils.getMilliseconds(interval, sm.getBaseTimeUnit());
    			
    			timeout = sm.getTimeout();
				if(timeout < 0){
					timeout = si.getTimeout();
				}
				timeout = TimeUtils.getMilliseconds(timeout, sm.getBaseTimeUnit());
				
				//msg.setStream(sm.isStream());
				//是否记录二进制流数据到日志文件
				msg.setDumpDownStream(sm.isDumpDownStream());
				msg.setDumpUpStream(sm.isDumpUpStream());
	    		msg.setNeedResponse(sm.isNeedResponse());
	    		
	    		//废弃此字段
	    		msg.setLoggable(SF.isLoggable(sm.getLogLevel()));
	    		//往监控服务器上传日志包,当前RPC的日志级别
	    		msg.setLogLevel(sm.getLogLevel());
	    		//往监控服务器上传监控包
	    		msg.setMonitorable(cxt.isMonitorable());
	    		//控制在各JVM实例内部转出日志
	    		msg.setDebugMode(cxt.isDebug());
	    		
    			msg.setLinkId(JMicroContext.lid());
    			
	    		if(cxt.isDebug()) {
	    			//开启Debug模式，设置更多信息在消息包中，网络流及编码会有损耗，但更晚于问题追踪
	    			msg.setInstanceName(Config.getInstanceName());
	    			msg.setTime(System.currentTimeMillis());
	    			msg.setMethod(sm.getKey().getMethod());
	    		}
	    		
	    		ByteBuffer pl = ICodecFactory.encode(this.codecFactory, req, msg.getUpProtocol());
	    		if(cxt.isDebug()) {
	    			cxt.appendCurUseTime("End encode req",true);
	    		}
	    		
	    		/*if(pl == null || pl.position() <= 0) {
					System.out.println(pl);
				}*/
	        	msg.setPayload(pl);
	    		
	    		//超时重试不需要重复注册监听器
	    		if(sm.isNeedResponse()) {
	    			//只有需要响应的请求才需要等待结果
	    			waitForResponse.put(req.getRequestId(), (message)->{
	    				mh.msg = message;
	    				//在请求响应之间做同步
	    				synchronized(req) {
	        				req.notify();
	        			}
	    			});
	    		}
        	}
        	
    	    IClientSession session = this.sessionManager.getOrConnect(si.getKey().getInstanceName(),s.getHost(), s.getPort());
    		
    	    if(cxt.isDebug()) {
    	    	//在调试模式下，给消息一个ID
    	    	//每次超时重试，都起一个新的消息，但是同一个请求Req
    	    	msg.setId(this.idGenerator.getLongId(Message.class));
    	    }
    		
    	    //session.increment(MonitorConstant.CLIENT_REQ_BEGIN);
    		//long st = System.currentTimeMillis();
    		//logger.info(""+st);
    	    
    	    if(cxt.isDebug()) {
    			cxt.appendCurUseTime("Start write:",true);
    		}
    	    
    		session.write(msg);
    		
    		if(cxt.isDebug()) {
    			cxt.appendCurUseTime("End write:",true);
    		}
    		
    		if(openDebug) {
    			//logger.info("Write req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
    		}
    		
    		if(!sm.isNeedResponse()) {
    			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			if(openDebug) {
        			//logger.info("Not need response req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
        		}
    			if(cxt.isDebug()) {
        			cxt.appendCurUseTime("No need response",true);
        		}
    			return null;
    		}
    		
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
    		
    		//下面处理响应消息
    		Message respMsg = mh.msg;
    		
    		RpcResponse resp = null;
    		if(respMsg != null){
    		
    			if(openDebug) {
        			//logger.info("Got response req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
        		}
    			
				resp = ICodecFactory.decode(this.codecFactory,respMsg.getPayload(),
						RpcResponse.class,msg.getUpProtocol());
				resp.setMsg(respMsg);
				
				if(cxt.isDebug()) {
	    			cxt.appendCurUseTime("Go resp",false);
	    		}
				
    		} else {
    			if(openDebug) {
        			logger.info("Timeout req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
        		}
    			if(cxt.isDebug()) {
	    			cxt.appendCurUseTime("Timeout",true);
	    		}
    			//到这里肯定是超时了
    			//SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT, req, null);
    			logger.warn("Timeout reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+",timeout"+sm.getTimeout()+",Service: "+sm.getKey().toKey(true, true, true));
    			SF.reqTimeout(TAG.getName(),"");
    			//session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT);
    		}
    		
    		if(resp != null && resp.isSuccess() && !(resp.getResult() instanceof ServerError)) {
				//session.increment(MonitorConstant.CLIENT_REQ_OK);
				//同步请求成功，直接返回
    			req.setFinish(true);
    			waitForResponse.remove(req.getRequestId());
    			SF.reqSuccess(TAG.getName());
    			if(cxt.isDebug()) {
	    			cxt.appendCurUseTime("Success",false);
	    		}
    			return resp;
    		}
    		
    		//总失败数=MonitorConstant.CLIENT_GET_RESPONSE_ERROR+
    		
    		req.setFinish(false);
    		
    		//下面是此次请求失败,进入重试处理过程
    		if(resp == null){
    			retryCnt--;
    			if(retryCnt <= 0){
    				//不能再重试了
    				//断开新打开连接
    				//session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
    				//SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL, req, null);
    				//SF.doRequestLog(MonitorConstant.LOG_ERROR,TAG,req,null,sb.toString());
    				//请求失败
    				if(session.getFailPercent() > 50) {
        				this.sessionManager.closeSession(session);
    				}
    				logger.warn("Fail reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+",timeout"+",Service: "+sm.getKey().toKey(true, true, true));
    				//肯定是超时失败了
    				SF.reqTimeoutFail(TAG.getName(),"");
    				throw new TimeoutException(req,si.getRetryCnt()+"");
    			} else {
    				if(interval > 0 ) {
    					try {
        					//超时重试间隔
        					Thread.sleep(si.getRetryInterval());
        				} catch (InterruptedException e) {
        					logger.error("Sleep exceptoin ",e);
        				}
    				}
    				logger.warn("Do timeout retry reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+",retryCnt:"+retryCnt+",Service: "+sm.getKey().toKey(false, true, true));
    				//session.increment(MonitorConstant.CLIENT_REQ_RETRY);
    				SF.reqTimeoutRetry(TAG.getName(),"");
    				continue;//重试循环
    			}
    			
    		}else if(resp.getResult() instanceof ServerError){
				 se = (ServerError)resp.getResult();
				 logger.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg()+",Service: "+sm.getKey().toKey(true, true, true));
				 req.setSuccess(false);
				 session.increment(MC.MT_CLIENT_RESPONSE_SERVER_ERROR);
				 waitForResponse.remove(req.getRequestId());
				 SF.reqServiceRespError(TAG.getName(), se);
				 throw new RpcException(req,se);
			} else if(!resp.isSuccess()){
				 //服务器正常逻辑处理错误，不需要重试，直接失败
				 logger.error("服务器响应错误reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+ resp.getResult()+",Service: "+sm.getKey().toKey(true, true, true));
				 req.setSuccess(false);
				 session.increment(MC.MT_CLIENT_SERVICE_ERROR);
				 SF.reqServerError(TAG.getName(), resp.getResult() == null ? "":resp.getResult().toString());
				 waitForResponse.remove(req.getRequestId());
			     throw new RpcException(req,resp);
			}
    		//waitForResponse.remove(req.getRequestId());
    		//代码不应该走到这里，如果走到这里，说明系统还有问题
    		SF.reqError(TAG.getName(),"未知错误1");
    		
    		logger.error("未知错误reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+",Service: "+sm.getKey().toKey(true, true, true));
    		throw new CommonException("未知错误1");
    		
        }while((retryCnt--) > 0);
        logger.error("未知错误2,reqID:"+req.getRequestId()+",linkId:"+msg.getLinkId()+",Service: "+sm.getKey().toKey(true, true, true));
        SF.reqError(TAG.getName(),"未知错误2");
        
        throw new CommonException("Service:"+req.getServiceName()+", Method: "+req.getMethod()+", Params: "+req.getArgs());
	}

	private String argStr(Object[] args) {
		if(args == null || args.length == 0) {
			return "null";
		}
		
		StringBuffer sb = new StringBuffer();
		for(Object a : args) {
			if(a == null) {
				sb.append("null").append(",");
			}else {
				sb.append(a.toString()).append(",");
			}
		}
		sb = sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	private ServiceItem getServiceItem(AsyncConfig ac) {
		
		Set<ServiceItem> items = this.srvManager.getServiceItems(ac.getServiceName(), ac.getNamespace(), ac.getVersion());
		if(items == null || items.isEmpty()) {
			return null;
		}
		
		for(ServiceItem si : items) {
			ServiceMethod sm = si.getMethod(ac.getMethod(), ac.getParamStr());
			if(sm != null) {
				return si;
			}
		}
		
		return null;
	}


	@Override
	public Byte type() {
		return Constants.MSG_TYPE_RRESP_JRPC;
	}

	@Override
	public void onMessage(ISession session,Message msg) {
		//receive response
		if(SF.isLoggable(MC.LOG_DEBUG,msg.getLogLevel())) {
			SF.doMessageLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG,TAG,msg,null," receive message");
		}
		
		IResponseHandler handler = waitForResponse.get(msg.getReqId());
		if(handler!= null){
			//logger.info("get result reqID: {}",msg.getReqId());
			handler.onResponse(msg);
		} else {
			if(SF.isLoggable(MC.LOG_ERROR,msg.getLogLevel())) {
				SF.doMessageLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR,TAG,msg,null," handler not found");
			}
			logger.error("msdId:"+msg.getId()+",reqId:"+msg.getReqId()+",linkId:"+msg.getLinkId()+
					",waitForResponse keySet"+waitForResponse.keySet());
			session.increment(MC.MT_CLIENT_HANDLER_NOT_FOUND);
			if(session.getTakePercent(MC.MT_CLIENT_HANDLER_NOT_FOUND) > 50) {
				//断开重连
				this.sessionManager.closeSession(session);
			}
		}
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
	
	private class MessageHolder {
		public Message msg;
	}
}

