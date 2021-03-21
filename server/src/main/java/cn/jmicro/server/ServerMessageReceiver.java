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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.LogMonitorClient;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.DumpManager;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:51:20
 */
@Component(lazy=false, active=true, value="serverReceiver", side=Constants.SIDE_PROVIDER, level=1000)
public class ServerMessageReceiver implements IMessageReceiver{

	private static final Logger logger = LoggerFactory.getLogger(ServerMessageReceiver.class);
	private static final Class<?> TAG = ServerMessageReceiver.class;
	
	@Inject
	private ExecutorFactory ef;
	
	@Cfg("/ServerMessageReceiver/openDebug")
	private boolean openDebug;
	
	@Inject(required=false)
	private LogMonitorClient logMonitor;
	
	@Inject(required=false)
	private StatisMonitorClient monitor;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private ServiceManager srvMng;
	
	private ExecutorService defaultExecutor = null;
	
	private ExecutorService gatewayExecutor = null;
	
	private boolean useExecutorPool = true;
	
	private Boolean finishInit = false;
	
	private volatile Map<Byte,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private int maxCacheTaskSize = 10000;
	
	private Queue<JMicroTask> cacheTasks  = new ConcurrentLinkedQueue<>();
	
	@Inject
	private ServiceMethodTaskQueueManager taskWorker;
	
	public void ready() {
	
		/*ExecutorConfig gateWayCfg = new ExecutorConfig();
		gateWayCfg.setMsCoreSize(5);
		gateWayCfg.setMsMaxSize(20);
		gateWayCfg.setTaskQueueSize(500);
		gateWayCfg.setThreadNamePrefix("ServerMessageReceiver-gateway");
		gateWayCfg.setRejectedExecutionHandler(new JicroAbortPolicy());
		gatewayExecutor = ef.createExecutor(gateWayCfg);*/
		
		//系统级RPC处理器，如ID请求处理器，和普通RPC处理理器同一个实例，但是TYPE标识不同，需要特殊处理
		//registHandler(jrpcHandler);
		//registHandler(idHandler);
		
		/*
		String name = Config.getInstanceName()+"_limitDataSubscribe";
		ServiceItem si = sl.createSrvItem(IStatisDataSubscribe.class, name,"0.0.1", 
				LimitStatisDataSubscribe.class.getName());
		LimitStatisDataSubscribe ds = new LimitStatisDataSubscribe();
		
		of.regist(name, ds);
		sl.registService(si,ds);
		*/
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(10);
		config.setMsMaxSize(20);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("ServerMessageReceiver-default");
		config.setRejectedExecutionHandler(new JicroAbortPolicy());
		defaultExecutor = ef.createExecutor(config);
		
		taskWorker.setDefaultExecutor(defaultExecutor);
		
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
	public void receive(ISession s, Message msg) {
		
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
		
		ServiceMethod sm = null;
		if(msg.getType() == Constants.MSG_TYPE_REQ_RAW 
				|| msg.getType() == Constants.MSG_TYPE_REQ_JRPC) {
			sm = srvMng.getServiceMethodByHash(msg.getSmKeyCode());
			if(sm == null) {
				sm = srvMng.getServiceMethodWithHashBySearch(msg.getSmKeyCode());
				String errMsg = "Invalid message method code: [" + msg.toString() + "]";
				errMsg += ",client host: " + s.remoteHost()+",remotePort: " + s.remotePort();
				
				if(sm != null) {
					errMsg += ", sm key: " + sm.getKey().toKey(true, true, true);
				}
				
				LG.log(MC.LOG_ERROR, TAG, errMsg);
				responseException(msg,(IServerSession)s, new CommonException(Resp.CODE_FAIL,errMsg));
				return;
			}
		}
		//确保服务器对称密钥一定是最新的
		try {
			secretMng.updateSecret(msg);
		} catch (Exception e1) {
			responseException(msg,(IServerSession)s,e1);
		}
		
		JMicroTask t = this.popTask();
		t.setMsg(msg);
		t.setS((IServerSession)s);
		t.setSm(sm);
		
		if (useExecutorPool) {
			/*if(msg.isDebugMode()) {
				 long curTIme = System.currentTimeMillis();
				 LogUtil.B.debug("Server receive msg ins[{}] reqId[{}] method[{}] Total Cost:[{}]",
	              			msg.getInstanceName(),msg.getReqId(),msg.getMethod(),(curTIme-msg.getTime()));
			}*/
			
			if(sm == null || sm.getMaxSpeed() <= 0) {
				//不限速
				defaultExecutor.execute(t);
			} else {
				if(sm.getKey().getSnvHash() != msg.getSmKeyCode()) {
					String errMsg = "Invalid service method code: [" + msg.getSmKeyCode() + "] but target [" + sm.getKey().getSnvHash()+"] ";
					errMsg += ", client host: " + s.remoteHost();
					errMsg += ", smKey: " + sm.getKey().toKey(true, true, true);
					LG.log(MC.LOG_ERROR, TAG, errMsg);
					logger.error(errMsg);
					return;
		    	}
				try {
					taskWorker.sumbit(t,sm);
				} catch (CommonException e) {
					responseException(msg,t.s,e);
				}
			}
			
			/*
			if(Constants.MSG_TYPE_REQ_RAW == msg.getType()) {
				this.gatewayExecutor.execute(t);
			}else {
				defaultExecutor.execute(t);
			}
			*/
			
		} else {
			doReceive(t);
		}
		
		/*
		new Thread(()->{
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		}).start();
		*/
	}
	
	//@Suspendable
	public void doReceive(JMicroTask task){
		Message msg = task.msg;
		IServerSession s = task.s;
		JMicroContext.configProvider(s,msg);
		if(task.sm != null) {
			JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, task.sm);
		}
		try {
			/*
			 if(msg.isDebugMode()) {
				StringBuilder sb = JMicroContext.get().getDebugLog();
				 sb.append(msg.getMethod())
				.append(",MsgId:").append(msg.getId()).append(",reqID:").append(msg.getReqId())
				.append(",linkId:").append(JMicroContext.lid());
			}
			*/
			
			if(LG.isLoggable(MC.LOG_DEBUG,msg.getLogLevel())) {
				LG.log(MC.LOG_DEBUG, TAG,LG.messageLog("doReceive",msg));
			}
			
			if(msg.isUpSsl() || msg.isDownSsl()) {
				this.secretMng.checkAndDecrypt(msg);
			}
			
			IMessageHandler h = handlers.get(msg.getType());
			if(h == null) {
				String errMsg = "Message type ["+Integer.toHexString(msg.getType())+"] handler not found!";
				MT.rpcEvent(MC.MT_HANDLER_NOT_FOUND);
				LG.log(MC.LOG_ERROR, TAG,errMsg,null);
				responseException(msg,s,null);
			} else {
				h.onMessage(s, msg);
			}
		} catch (Throwable e) {
			MT.rpcEvent(MC.MT_SERVER_ERROR);
			LG.log(MC.LOG_ERROR, TAG,e.getMessage(),e);
			responseException(msg,s,e);
		} finally {
			offerTask(task);
		}
	}
	
	private void responseException(Message msg,IServerSession s,Throwable e) {
		
		StackTraceElement se = Thread.currentThread().getStackTrace()[2];
		logger.error("From line [" + se.getLineNumber() + "] reqHandler error msg:{}, exp:{} ",msg,e);
		
		if(msg.isNeedResponse()) {
			RpcResponse resp = null;
			String errMsg = e == null?"":e.getMessage();
			if(e instanceof CommonException) {
				CommonException ce = (CommonException)e;
				resp = new RpcResponse(msg.getReqId(),new ServerError(ce.getKey(),errMsg));
			} else {
				resp = new RpcResponse(msg.getReqId(),new ServerError(Resp.CODE_FAIL,errMsg));
			}
			resp.setSuccess(false);
			msg.setPayload(ICodecFactory.encode(codecFactory,resp,msg.getUpProtocol()));
			msg.setType((byte)(msg.getType()+1));
			msg.setUpSsl(false);
			msg.setDownSsl(false);
			msg.setSign(false);
			msg.setSec(false);
			msg.setSalt(null);
			s.write(msg);
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
			t.msg = null;
			t.s = null;
			t.sm = null;
			//t.typeStatis.clear();
			this.cacheTasks.offer(t);
		}
	}
	
	public class JMicroTask implements Runnable{
		
		private Message msg;
		private IServerSession s;
		
		private ServiceMethod sm = null;
		//private TaskRunnable r;
		//private Map<Short,StatisItem> typeStatis = new HashMap<>();

        public JMicroTask() {}

		@Override
		public void run() {
			try {
				/*if(msg.isDebugMode())
					logger.debug(msg.getMethod() + " reqId: "+msg.getReqId()+" Got " + cnt.decrementAndGet());*/
				doReceive(this);
				//r.doReceive(this);
			} finally{
			/*	
			 if(msg.isDebugMode())
					logger.debug(msg.getMethod() + " reqId: "+msg.getReqId()+" Release " + cnt.decrementAndGet());
					*/
				
			}
		}
		
		/*public StatisItem addType(Short type, long val) {
			StatisItem si = typeStatis.get(type);
			if(si == null) {
				si = new StatisItem();
				si.setType(type);
				typeStatis.put(type, si);
			}
			si.add(val);
			return si;
		}*/

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

		public ServiceMethod getSm() {
			return sm;
		}

		public void setSm(ServiceMethod sm) {
			this.sm = sm;
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
        		sb.append(" localhost[").append(Config.getExportSocketHost()).append("]");
        		sb.append(" localport[").append(s.localPort()).append("]");
        		sb.append(" reqid[").append(msg.getReqId()).append("]");
        		sb.append(" linkId[").append(msg.getLinkId()).append("]");
        		sb.append(" msgId[").append(msg.getId()).append("]");
        		sb.append(" queueSize[").append(e.getQueue().size()).append("]");
        		sb.append(" activeCount[").append(e.getActiveCount()).append("]");
        		sb.append(" largestPoolSize[").append(e.getLargestPoolSize()).append("]");
        		sb.append(" corePoolSize[").append(e.getCorePoolSize()).append("]");
        		sb.append(" maximumPoolSize[").append(e.getMaximumPoolSize()).append("]");
        		
        		MRpcLogItem mi = LG.logWithNonRpcContext(MC.LOG_ERROR, JicroAbortPolicy.class,sb.toString(),MC.MT_EXECUTOR_REJECT,false);
        		
        		if(mi != null) {
        			mi.setLinkId(msg.getLinkId());
        			mi.setProvider(true);
        			
        			if(t.sm != null) {
        				mi.setSmKey(t.sm.getKey());
        			}
        			
        			if(Constants.MSG_TYPE_REQ_RAW == msg.getType()) {
            			ApiRequest re = ICodecFactory.decode(codecFactory, msg.getPayload(), ApiRequest.class,
            					msg.getUpProtocol());
            			 mi.setReq(re);
            			 mi.setReqId(re.getReqId());
            			
            		} else if(Constants.MSG_TYPE_ID_REQ == msg.getType()) {
            			IdRequest re = ICodecFactory.decode(codecFactory, msg.getPayload(), IdRequest.class,
            					msg.getUpProtocol());
            		} else {
            			RpcRequest re = ICodecFactory.decode(codecFactory, msg.getPayload(), RpcRequest.class,
            					msg.getUpProtocol());
            			mi.setReq(re);
            			mi.setReqId(re.getRequestId());
            			mi.setReqParentId(re.getReqParentId());
            		}
        		}
        		
        		logger.error(sb.toString());
        		
        		MT.rpcEvent(MC.MT_EXECUTOR_REJECT);
        		LG.submit2Cache(mi);
        		
        	} else {
        		throw new RejectedExecutionException("Task " + r.toString() +
                        " rejected from " +
                        e.toString());
        	}
            
        }
    }
	
}
