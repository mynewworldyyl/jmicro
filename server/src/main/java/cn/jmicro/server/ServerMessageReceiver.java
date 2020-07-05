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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.debug.LogUtil;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorClient;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.DumpManager;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:51:20
 */
@Component(lazy=false,active=true,value="serverReceiver",side=Constants.SIDE_PROVIDER,level=100000)
public class ServerMessageReceiver implements IMessageReceiver{

	static final Logger logger = LoggerFactory.getLogger(ServerMessageReceiver.class);
	static final Class<?> TAG = ServerMessageReceiver.class;
	
	@Inject
	private ExecutorFactory ef;
	
	@Cfg("/ServerMessageReceiver/openDebug")
	private boolean openDebug;
	
	@Inject(required=false)
	private MonitorClient monitor;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private JRPCReqRespHandler jrpcHandler;
	
	@Inject
	private ICodecFactory codecFactory;
	
	/*@Inject("idRequestMessageHandler")
	private IMessageHandler idHandler;*/
	
	/*@Cfg(value="/ServerReceiver/receiveBufferSize")
	private int receiveBufferSize=1000;*/
	
	private ExecutorService defaultExecutor = null;
	
	private ExecutorService gatewayExecutor = null;
	
	private boolean useExecutorPool = true;
	
	private Boolean finishInit = false;
	
	private volatile Map<Byte,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private AtomicInteger cnt = new AtomicInteger(0);
	
	private int maxCacheTaskSize = 10000;
	
	private Queue<JMicroTask> cacheTasks  = new ConcurrentLinkedQueue<>();
	
	public void init(){
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(5);
		config.setMsMaxSize(20);
		config.setTaskQueueSize(100);
		config.setThreadNamePrefix("ServerMessageReceiver-default");
		config.setRejectedExecutionHandler(new JicroAbortPolicy());
		defaultExecutor = ef.createExecutor(config);
		
		ExecutorConfig gateWayCfg = new ExecutorConfig();
		gateWayCfg.setMsCoreSize(5);
		gateWayCfg.setMsMaxSize(20);
		gateWayCfg.setTaskQueueSize(100);
		gateWayCfg.setThreadNamePrefix("ServerMessageReceiver-gateway");
		gateWayCfg.setRejectedExecutionHandler(new JicroAbortPolicy());
		gatewayExecutor = ef.createExecutor(gateWayCfg);
		
		//系统级RPC处理器，如ID请求处理器，和普通RPC处理理器同一个实例，但是TYPE标识不同，需要特殊处理
		//registHandler(jrpcHandler);
		//registHandler(idHandler);
	}
	
	public void ready() {
		finishInit = true;
		synchronized(finishInit) {
			finishInit.notifyAll();
		}
		logger.info("Server ready:{}",Config.getInstanceName());
	}
	
	public void registHandler(IMessageHandler handler){
		Map<Byte,IMessageHandler> handlers = this.handlers;
		if(handlers.containsKey(handler.type())){
			return;
		}
		handlers.put(handler.type(), handler);
	}
	
	@Override
	//@Suspendable
	public void receive(ISession s, Message msg) {
		
		if(openDebug) {
			//SF.getIns().doMessageLog(MonitorConstant.DEBUG, TAG, msg,"receive");
		}
		//JMicroContext jc = JMicroContext.get();
		//直接协程处理，IO LOOP线程返回
		
		/*
		 new Fiber<Void>(() -> {
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		 }).start();
		*/
		
		if(!finishInit) {
			synchronized(finishInit) {
				try {
					finishInit.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (useExecutorPool) {
			if(msg.isDebugMode()) {
				 long curTIme = System.currentTimeMillis();
				 LogUtil.B.debug("Server receive msg ins[{}] reqId[{}] method[{}] Total Cost:[{}]",
	              			msg.getInstanceName(),msg.getReqId(),msg.getMethod(),(curTIme-msg.getTime()));
			}
			
			JMicroTask t = this.popTask();
			t.setMsg(msg);
			t.setS((IServerSession)s);
			
			if(Constants.MSG_TYPE_REQ_RAW == msg.getType()) {
				this.gatewayExecutor.execute(t);
			}else {
				defaultExecutor.execute(t);
			}
			
		} else {
			doReceive((IServerSession) s, msg);
		}
		
		/*
		new Thread(()->{
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		}).start();
		*/
	}
	
	//@Suspendable
	private void doReceive(IServerSession s, Message msg){
		try {
			
			JMicroContext.configProvider(s,msg);
			/*if(msg.isDebugMode()) {
				StringBuilder sb = JMicroContext.get().getDebugLog();
				 sb.append(msg.getMethod())
				.append(",MsgId:").append(msg.getId()).append(",reqID:").append(msg.getReqId())
				.append(",linkId:").append(JMicroContext.lid());
			}*/
			
			if(SF.isLoggable(MC.LOG_DEBUG,msg.getLogLevel())) {
				SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG, TAG,"doReceive");
			}
			
			IMessageHandler h = handlers.get(msg.getType());
			if(h == null) {
				String errMsg = "Message type ["+Integer.toHexString(msg.getType())+"] handler not found!";
				SF.eventLog(MC.MT_HANDLER_NOT_FOUND,MC.LOG_ERROR, TAG,errMsg);
				throw new CommonException(errMsg);
			} else {
				h.onMessage(s, msg);
			}
		} catch (Throwable e) {
			//SF.doMessageLog(MonitorConstant.LOG_ERROR, TAG, msg,e);
			//SF.doSubmit(MonitorConstant.SERVER_REQ_ERROR);
			logger.error("reqHandler error msg:{} ",msg);
			logger.error("doReceive",e);
			
			SF.eventLog(MC.MT_SERVER_ERROR,MC.LOG_ERROR, TAG,"error",e);
			
			RpcResponse resp = new RpcResponse(msg.getReqId(),new ServerError(0,e.getMessage()));
			resp.setSuccess(false);
			msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getUpProtocol()));
			msg.setType((byte)(msg.getType()+1));
			s.write(msg);
		} finally {
			if(!msg.isAsyncReturnResult()) {
				if(JMicroContext.get().isDebug()) {
					JMicroContext.get().appendCurUseTime("respTime",false);
					JMicroContext.get().debugLog(0);
				}
				JMicroContext.get().submitMRpcItem(monitor);
			}else {
				JMicroContext.get().appendCurUseTime("Async req service return",false);
			}
			JMicroContext.clear();
		}
	}
	
	private final JMicroTask popTask() {
		if(!this.cacheTasks.isEmpty()) {
			return this.cacheTasks.poll();
		}else {
			return new JMicroTask();
		}
	}
	
	private final void offerTask(JMicroTask t) {
		if(this.cacheTasks.size() < maxCacheTaskSize) {
			t.setMsg(null);
			t.setS(null);
			this.cacheTasks.offer(t);
		}
	}
	
	private class JMicroTask implements Runnable {
		
		private Message msg;
		private IServerSession s;
	       
        public JMicroTask() { }

		@Override
		public void run() {
			try {
				if(msg.isDebugMode())
					logger.debug(msg.getMethod() + " reqId: "+msg.getReqId()+" Got " + cnt.decrementAndGet());
				doReceive((IServerSession)s, msg);
			} finally{
				if(msg.isDebugMode())
					logger.debug(msg.getMethod() + " reqId: "+msg.getReqId()+" Release " + cnt.decrementAndGet());
				offerTask(this);
			}
		}

		public Message getMsg() {
			return msg;
		}

		public void setMsg(Message msg) {
			this.msg = msg;
		}

		public IServerSession getS() {
			return s;
		}

		public void setS(IServerSession s) {
			this.s = s;
		}
    }
	
	public class JicroAbortPolicy implements RejectedExecutionHandler {
       
        public JicroAbortPolicy() { }
     
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        	
        	if( r != null && r instanceof JMicroTask) {
        		JMicroTask t = (JMicroTask)r;

        		Message msg = t.getMsg();
        		IServerSession s = t.getS();
        		ByteBuffer bf = msg.encode();
        		DumpManager.getIns().doDump(bf);
        		
        		StringBuffer sb = new StringBuffer("Executor reject: ");
        		sb.append("ins[").append(Config.getInstanceName()).append("]");
        		sb.append(" localhost[").append(Config.getHost()).append("]");
        		sb.append(" localport[").append(s.localPort()).append("]");
        		sb.append(" reqid[").append(msg.getReqId()).append("]");
        		sb.append(" linkId[").append(msg.getLinkId()).append("]");
        		sb.append(" msgId[").append(msg.getId()).append("]");
        		sb.append(" queueSize[").append(e.getQueue().size()).append("]");
        		sb.append(" activeCount[").append(e.getActiveCount()).append("]");
        		sb.append(" largestPoolSize[").append(e.getLargestPoolSize()).append("]");
        		sb.append(" corePoolSize[").append(e.getCorePoolSize()).append("]");
        		sb.append(" maximumPoolSize[").append(e.getMaximumPoolSize()).append("]");
        		
        		IReq req = null;
        		if(Constants.MSG_TYPE_REQ_RAW == msg.getType()) {
        			 req = ICodecFactory.decode(codecFactory, msg.getPayload(), ApiRequest.class,
        					msg.getUpProtocol());
        		} else if(Constants.MSG_TYPE_ID_REQ == msg.getType()) {
        			req = ICodecFactory.decode(codecFactory, msg.getPayload(), RpcRequest.class,
        					msg.getUpProtocol());
        		}else {
        			req = ICodecFactory.decode(codecFactory, msg.getPayload(), IdRequest.class,
        					msg.getUpProtocol());
        		}
        		
        		logger.error(sb.toString());
        		
        		//invalid for monitor server
        		SF.reqEvent(MC.MT_EXECUTOR_REJECT, MC.LOG_ERROR, req, 
        				JicroAbortPolicy.class.getName(),sb.toString());
        		
        	} else {
        		throw new RejectedExecutionException("Task " + r.toString() +
                        " rejected from " +
                        e.toString());
        	}
            
        }
    }
	
}
