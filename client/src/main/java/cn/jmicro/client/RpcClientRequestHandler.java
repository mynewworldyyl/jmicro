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
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exception.AsyncRpcException;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.loadbalance.ISelector;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.LogMonitorClient;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.AbstractHandler;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.objectfactory.ClientServiceProxyHolder;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.Server;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/** 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_CLIENT_HANDLER,side=Constants.SIDE_COMSUMER)
public class RpcClientRequestHandler extends AbstractHandler implements IRequestHandler, IMessageHandler {
	
	public static final String RETRY_CNT = "_retryCnt";
	
	public static final String MSG = "_retryMsg";
	
    private final static Logger logger = LoggerFactory.getLogger(RpcClientRequestHandler.class);

    private static final Class<?> TAG = RpcClientRequestHandler.class;
	
	private final Map<Long,PromiseImpl<Object>> waitForResponse = new ConcurrentHashMap<>();
	
	//private final static Map<Long,IResponseHandler> asyncResponse = new ConcurrentHashMap<>();
	
	private final static Map<Long,Long> timeouts = new ConcurrentHashMap<>();
	
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
	
	@Inject
	private StatisMonitorClient statisMonitor;
	
	@Inject
	private LogMonitorClient logMonitor;
	
	@Inject(required=true)
	private ProcessInfo pi;
	
	@Inject
	private SecretManager secManager;
	
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
		
		TimerTicker.doInBaseTicker(5, Config.getInstanceName()+"_RpcClientRequestHandler-checker", null,
			(key,att)->{
				doChecker();
		});
	}

	@Override
	public IPromise<Object> onRequest(IRequest request) {
		ClientServiceProxyHolder proxy =  (ClientServiceProxyHolder)JMicroContext.get().getObject(Constants.PROXY, null);
		try {
			 /*if(openDebug) {
				logger.info("onRequest Method:"+request.getMethod()+",Service:" + request.getServiceName());
			   }*/
			 //请求开始
			 //SF.reqStart(TAG.getName(),request);
			 //LG.reqEvent(MC.MT_REQ_START, MC.LOG_NO, request,TAG.getName(),"");
			ServiceItem si = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM,null);
			if(si == null) {
        		//此方法可能抛出FusingException
				//此方法返回熔断异常，所以要放在MC.MT_REQ_START事件之前
				si = selector.getService(request.getServiceName(),request.getMethod(),request.getNamespace(),
						request.getVersion(), Constants.TRANSPORT_NETTY);
				
				if(si == null) {
	        		//SF.doSubmit(MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req,null);
	        		//服务未找到，或服务不存在
	        		String errMsg = "Service [" + request.getServiceName() + "] not found!";
	        		//SF.serviceNotFound(TAG.getSimpleName(), );
	        		LG.log(MC.LOG_ERROR, TAG, errMsg);
	        		MT.rpcEvent(MC.MT_SERVICE_ITEM_NOT_FOUND);
	    			throw new RpcException(request,errMsg,MC.MT_SERVICE_ITEM_NOT_FOUND);
	    		}
        	} else {
        		JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
        	}
			
			 MT.rpcEvent(MC.MT_REQ_START);
			 LG.log(MC.LOG_DEBUG, TAG.getName(), MC.MT_REQ_START);
			 //SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG,null, "request start");
			
			 ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
			 String mkey = sm.getKey().getMethod();
	         AsyncConfig ac = proxy.getAcs(mkey);
	         if(ac == null && JMicroContext.get().exists(Constants.ASYNC_CONFIG)) {
	        	 ac = (AsyncConfig)JMicroContext.get().getParam(Constants.ASYNC_CONFIG, null);
	        	 if(!ac.getForMethod().equals(request.getMethod())) {
	        		 //不属于当前方法的异步配置
	        		 ac = null;
	        	 } else {
	        		 JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
	        	 }
	         }
	         
	         if(ac != null && ac.isEnable()) {
	        	 if(ac.getCondition().equals(AsyncConfig.ASYNC_DIRECT)) {
	        		 //客户端直接做异步
	        		 return doAsyncInvoke(proxy,request,sm,ac);
	        	 } else {
	        		 //可以异步调用并异步返回结果
		        	 request.putObject(mkey, ac);
		        	 return doRequest(request,proxy,si);
	        	 }
	         } else {
	        	 return doRequest(request,proxy,si);
	         }
		} catch (SecurityException | IllegalArgumentException  e) {
			LG.log(MC.LOG_ERROR, TAG, e.getMessage(), e);
			throw new RpcException(request,e,MC.MT_REQ_ERROR);
		}
	}

	private <T> IPromise<T> doAsyncInvoke(ClientServiceProxyHolder proxy, IRequest req,ServiceMethod sm,AsyncConfig ac) {
		
		PromiseImpl<T> p = new PromiseImpl<T>();
		
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
				LG.log(MC.LOG_ERROR, TAG,msg);
				MT.rpcEvent(MC.MT_SERVICE_ITEM_NOT_FOUND);
				throw new RpcException(req,msg,MC.MT_SERVICE_ITEM_NOT_FOUND);
			}
			
			ServiceMethod callback = si.getMethod(ac.getMethod(), ac.getParamStr());
			if(callback == null) {
				String msg = "Async method not found for:"+sm.getKey().toKey(false, false, false)+",async :"+ ac.toString();
				logger.error(msg);
				//SF.doRequestLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, null, msg);
				LG.log(MC.LOG_ERROR, TAG, msg);
				MT.rpcEvent(MC.MT_SERVICE_ITEM_NOT_FOUND);
				throw new RpcException(req,msg,MC.MT_SERVICE_ITEM_NOT_FOUND);
			}
			
			data.setCallback(callback.getKey().toKey(false, false, false));
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
		
		//RpcResponse resp = new RpcResponse();
		if(id == PubSubManager.PUB_OK) {
			//resp.setReqId(data.getId());
			//异步调用RPC返回空值
			p.setFail(IPromise.ASYNC_CALL_RPC, "");
			p.done();
			return p;
		} else {
			String msg = "ErrorCode:"+id+",异步调用失败"+sm.getKey().toKey(false, false, false);
			LG.log(MC.LOG_ERROR,TAG,msg);
			MT.rpcEvent(MC.MT_ASYNC_RPC_FAIL);
			throw new AsyncRpcException(req,msg);
		}
	}

	private IPromise<Object> doRequest(IRequest req, ClientServiceProxyHolder proxy,ServiceItem si) {
        
		PromiseImpl<Object> p = new PromiseImpl<>();
		
		JMicroContext cxt = JMicroContext.get();
		
        ServiceMethod sm = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
        
        int retryCnt = -1;
        long interval = -1;
        long timeout = -1;
        //第一次进来在同一个线程中,同一个调用的超时重试使用
        boolean isFistLoop = true;
        
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setReqId(req.getRequestId());
		
		msg.setVersion(Message.MSG_VERSION);
		msg.setPriority(Message.PRIORITY_NORMAL);
		
		long curTime = TimeUtils.getCurTime();
    	
    	if(si == null) {
    		//SF.doSubmit(MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req,null);
    		//服务未找到，或服务不存在
    		String errMsg = "Service [" + req.getServiceName() + "] not found!";
    		//SF.serviceNotFound(TAG.getSimpleName(), );
    		LG.log(MC.LOG_ERROR, TAG, errMsg);
    		MT.rpcEvent(MC.MT_SERVICE_ITEM_NOT_FOUND);
			throw new RpcException(req,errMsg,MC.MT_SERVICE_ITEM_NOT_FOUND);
		}
    	
    	req.setImpl(si.getCode()+"");
    	
    	Server s = si.getServer(Constants.TRANSPORT_NETTY);
    	
    	cxt.setParam(JMicroContext.REMOTE_HOST, s.getHost());
    	cxt.setParam(JMicroContext.REMOTE_PORT, s.getPort());
		
		//保存返回结果
		//final Map<String,Object> result = new HashMap<>();
		//final MessageHolder mh = new MessageHolder();
		
    	if(isFistLoop){
    		ByteBuffer pl = ICodecFactory.encode(this.codecFactory, req, msg.getUpProtocol());
    		if(sm.getMaxPacketSize() > 0 && pl.limit() >= sm.getMaxPacketSize()) {
    			String m = "Packet too max " + pl.limit() + " limit size: " + sm.getMaxPacketSize();
    			//m = LG.reqMessage(m, req);
    			LG.log(MC.LOG_ERROR, TAG, m);
    			throw new RpcException(req,m,MC.MT_PACKET_TOO_MAX);
    		}
    		
    		msg.setEncType(sm.isRsa());
    		msg.setDownSsl(sm.isDownSsl());
    		msg.setUpSsl(sm.isUpSsl());
    		msg.setPayload(pl);
    		
    		msg.setRpcMk(true/*sm.getMaxSpeed() > 0*/);
    		msg.setSmKeyCode(sm.getKey().getSnvHash());
    		//logger.info("RpcclientRequest: " + sm.getKey().getSnvHash()+" => " + sm.getKey().toKey(true, true, true));
    		
    		if(sm.isUpSsl() || sm.isDownSsl()) {
    			this.secManager.signAndEncrypt(msg,si.getInsId());
    		}
    		
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
    		//msg.setLoggable(SF.isLoggable(sm.getLogLevel()));
    		//往监控服务器上传日志包,当前RPC的日志级别
    		msg.setLogLevel(sm.getLogLevel());
    		//往监控服务器上传监控包
    		msg.setMonitorable(cxt.isMonitorable());
    		//控制在各JVM实例内部转出日志
    		msg.setDebugMode(cxt.isDebug());
    		
			msg.setLinkId(JMicroContext.lid());
			msg.setInsId(pi.getId());
			
    		if(cxt.isDebug()) {
    			//开启Debug模式，设置更多信息在消息包中，网络流及编码会有损耗，但更晚于问题追踪
    			msg.setTime(curTime);
    			msg.setMethod(sm.getKey().toSnvm());
    		}
    		
    		if(cxt.isDebug()) {
    			cxt.appendCurUseTime("Encode Cost ",true);
    		}
    		
        	//超时重试不需要重复注册监听器
    		if(sm.isNeedResponse()) {
    			//5倍超时时间作为最后清理时间
				if(sm.getTimeout() > 0) {
					timeouts.put(req.getRequestId(), curTime + sm.getTimeout()*3);
				} else {
					//timeouts.put(req.getRequestId(), curTime + 60000L);
				}
				
				cxt.setParam(MSG, msg);
				cxt.setParam(JMicroContext.REQ_INS,req);
				cxt.setParam(Constants.SERVICE_ITEM_KEY, si);
				
				Map<String,Object> cxtParams = new HashMap<>();
				cxt.getAllParams(cxtParams);
				p.setContext(cxtParams);
				
				waitForResponse.put(req.getRequestId(),p);
    		}
    	}
    	
	    IClientSession session = this.sessionManager.getOrConnect(si.getKey().getInstanceName(),s.getHost(), s.getPort());
		
	    if(cxt.isDebug()) {
	    	//在调试模式下，给消息一个ID
	    	//每次超时重试，都起一个新的消息，但是同一个请求Req
	    	msg.setId(this.idGenerator.getLongId(Message.class));
	    }
		
	    if(cxt.isDebug()) {
			cxt.appendCurUseTime("Start Write",true);
		}
	    
		session.write(msg);
		
		if(msg.isMonitorable()) {
        	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE,1);
        	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE_BYTES,msg.getLen());
        }
		
		if(cxt.isDebug()) {
			cxt.appendCurUseTime("End Write",true);
		}
		
		/*if(openDebug) {
			//logger.info("Write req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
		}*/
		
		if(!sm.isNeedResponse()) {
			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
			if(openDebug) {
    			//logger.info("Not need response req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
    		}
			if(cxt.isDebug()) {
    			cxt.appendCurUseTime("No need response",true);
    		}
			//只是确认请求发送完成
			p.done();
			return p;
		}
		
		//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
		/*if(openDebug) {
			//logger.info("Not need response req:"+req.getMethod()+",Service:" + req.getServiceName()+", Namespace:"+req.getNamespace());
		}*/
		if(cxt.isDebug()) {
			cxt.appendCurUseTime("Async RPC",true);
		}
		return p;
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
	public void onMessage(ISession session,Message respMsg) {
		//receive response
		if(LG.isLoggable(MC.LOG_DEBUG,respMsg.getLogLevel())) {
			LG.log(MC.LOG_DEBUG,TAG,"receive message");
		}
		
		PromiseImpl<Object> p = waitForResponse.remove(respMsg.getReqId());
		
		if(p== null){
			String errMsg = LG.messageLog("waitForResponse keySet:" + waitForResponse.keySet(),respMsg);
			LG.log(MC.LOG_ERROR,TAG,errMsg);
			logger.error(errMsg);
			return;
		}

		ServiceMethod sm = null;
		JMicroContext cxt = JMicroContext.get();
		try {
			if(p.getContext() != null) {
				cxt.putAllParams(p.getContext());
				p.setContext(null);
			}
			
			sm = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
			ServiceItem si = cxt.getParam(Constants.SERVICE_ITEM_KEY,null);
			if(cxt.isDebug()) {
				cxt.appendCurUseTime("Got async resp ",true);
    		}
		
			MT.rpcEvent(MC.MT_REQ_SUCCESS);
			
			//下面处理响应消息
			if(respMsg.isMonitorable()) {
	      	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_READ,1);
	      	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_READ_BYTES,respMsg.getLen());
	        }
			
			RpcResponse resp = ICodecFactory.decode(this.codecFactory,respMsg.getPayload(),
					RpcResponse.class,respMsg.getUpProtocol());
			resp.setMsg(respMsg);
			
			if(resp.isSuccess()) {
				JMLogItem mi = cxt.getMRpcLogItem();
				if(mi != null) {
					mi.setResp(resp);
				}
				
				if(cxt.isDebug()) {
					cxt.appendCurUseTime("Got Resp ",true);
				}
				p.setResult(resp.getResult());
			} else {
				if(resp.getResult() instanceof ServerError) {
					ServerError se = (ServerError)resp.getResult();
					if(se.getErrorCode() == MC.MT_AES_DECRYPT_ERROR) {
						this.secManager.resetLocalSecret(respMsg.getType(),si.getInsId());
					}
					p.setFail(se.getErrorCode(), se.getMsg());
				} else {
					p.setFail(MC.LOG_ERROR, "Server return error!");
				}
			}
			
		}catch(Throwable e) {
			String errMsg = "Client callback error reqID:"+respMsg.getReqId()+",linkId:"+respMsg.getLinkId()+",Service: "+sm.getKey().toKey(true, true, true);
			logger.error(errMsg,e);
    		LG.log(MC.LOG_ERROR, TAG,errMsg);
    		MT.rpcEvent(MC.MT_REQ_ERROR);
    		/*RpcResponse resp = new RpcResponse();
    		resp.setSuccess(false);
    		resp.setResult();*/
    		Object errO = new ServerError(10,e.getMessage());
    		p.setResult(errO);
		} finally {
			if(cxt.getObject(Constants.NEW_LINKID,null) != null &&
					cxt.getBoolean(Constants.NEW_LINKID,false)) {
				//RPC链路结束
				LG.log(MC.LOG_DEBUG, TAG.getName(), MC.MT_LINK_END);
			}
			
			MT.rpcEvent(MC.MT_REQ_END);
			LG.log(MC.LOG_DEBUG, TAG.getName(), MC.MT_REQ_END);
			
			cxt.debugLog(0);
			if(sm.getKey().getMethod().equals("invokeRpcA")) {
				logger.debug("");
			}
			cxt.submitMRpcItem(logMonitor,statisMonitor);
			
			//从此开始脱离此RPC上下文
			p.done();
			
			JMicroContext.clear();
		}
	
	}
	
	private void doChecker() {
		if(timeouts.isEmpty()) {
			return;
		}
		
		Map<Long,Long> ts = new HashMap<>();
		synchronized(timeouts) {
			ts.putAll(timeouts);
		}
		
		long cutTime = TimeUtils.getCurTime();
		for(Long k : ts.keySet()) {
			long t = timeouts.get(k);
			if(cutTime > t) {
				timeouts.remove(k);
				if(waitForResponse.containsKey(k)) {
					logger.error("waitForResponse callback timeout reqID: " + k);
					PromiseImpl<Object> p = waitForResponse.get(k);
					if(timeoutCheck(p)) {
						waitForResponse.remove(k);
					}
				}
			}
		}
		
	}

	private boolean timeoutCheck(PromiseImpl<Object> p) {

		JMicroContext cxt = JMicroContext.get();
		JMicroContext.clear();
		
		if (p.getContext() != null) {
			cxt.putAllParams(p.getContext());
		}

		ServiceMethod sm = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);

		int retryCnt = 0;

		IRequest req = cxt.getParam(JMicroContext.REQ_INS, null);
		Message msg = cxt.getParam(MSG, null);

		if (cxt.exists(RETRY_CNT)) {
			retryCnt = cxt.getParam(RETRY_CNT, 0);
		} else {
			retryCnt = sm.getRetryCnt();
		}

		if (retryCnt <= 0) {
			String errMsg = "Request failure req [" + req.toString() + "],msg [" + msg.toString() + "] timeout"
					+ ",Method [" + sm.getKey().toKey(true, true, true)+"]";

			logger.warn(errMsg);
			// 肯定是超时失败了
			// SF.reqTimeoutFail(TAG.getName(),"");
			MT.rpcEvent(MC.MT_REQ_TIMEOUT_FAIL);
			MT.rpcEvent(MC.MT_REQ_END);
			LG.log(MC.LOG_ERROR, TAG.getName(),errMsg, MC.MT_REQ_END);
			
			p.setFail(MC.MT_REQ_TIMEOUT_FAIL, errMsg);
			p.done();
			
			cxt.debugLog(0);
			cxt.submitMRpcItem(logMonitor,statisMonitor);
			JMicroContext.clear();

			return true;
		}

		retryCnt--;
		cxt.setInt(RETRY_CNT, retryCnt);

		String errMsg = "Do timeout retry reqID:" + req.getRequestId() + ",linkId:" + msg.getLinkId() + ",retryCnt:"
				+ retryCnt + ",Service: " + sm.getKey().toKey(false, true, true);
		LG.log(MC.LOG_WARN, TAG, errMsg);
		MT.rpcEvent(MC.MT_REQ_TIMEOUT_RETRY);
		logger.warn(errMsg);
		/*if (sm.getRetryInterval() > 0) {
			try {
				// 超时重试间隔
				Thread.sleep(sm.getRetryInterval());
			} catch (InterruptedException e) {
				logger.error("Sleep exceptoin ", e);
			}
		}*/

		String host = cxt.getString(JMicroContext.REMOTE_HOST, null);

		String port = cxt.getString(JMicroContext.REMOTE_PORT, null);

		IClientSession session = this.sessionManager.getOrConnect(sm.getKey().getInstanceName(), host, port);

		if (cxt.isDebug()) {
			// 在调试模式下，给消息一个ID
			// 每次超时重试，都起一个新的消息，但是同一个请求Req
			msg.setId(this.idGenerator.getLongId(Message.class));
		}

		if (cxt.isDebug()) {
			cxt.appendCurUseTime("Start Write", true);
		}
		
		if(msg.isMonitorable()) {
	  	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE,1);
	  	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE_BYTES,msg.getLen());
	     }
		
		//保存当前上下文
		cxt.getAllParams(p.getContext());

		session.write(msg);
		
		if(sm.getTimeout() > 0) {
			timeouts.put(req.getRequestId(), TimeUtils.getCurTime() + sm.getTimeout()*3);
		} else {
			timeouts.put(req.getRequestId(), TimeUtils.getCurTime() + 60000L);
		}

		return false;
	}
}

