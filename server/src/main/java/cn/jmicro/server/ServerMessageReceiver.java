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
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfigJRso;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.DumpManager;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequestJRso;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

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
	private AccountManager accountManager;
	
	@Inject
	private PermissionManager pm;
	
	@Inject
	private ExecutorFactory ef;
	
	@Cfg("/ServerMessageReceiver/openDebug")
	private boolean openDebug;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private ProcessInfoJRso pi;
	
	@Inject
	private ServiceManager srvMng;
	
	private IMessageHandler gatewayHandler = null;
	
	@Cfg(Constants.EXECUTOR_GATEWAY_KEY)
	private boolean gatewayModel = false;
	
	private ExecutorService defaultExecutor = null;
	
	private ExecutorService gatewayExecutor = null;
	
	private boolean useExecutorPool = true;
	
	private volatile Map<Byte,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private int maxCacheTaskSize = 10000;
	
	private Queue<JMicroTask> cacheTasks  = new ConcurrentLinkedQueue<>();
	
	@Inject
	private ServiceMethodTaskQueueManager taskWorker;
	
	public void jready() {
	
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
		
		if(gatewayModel) {
			this.gatewayHandler = of.getByName("apiGatewayHandler");
			if(null == this.gatewayHandler) {
				throw new CommonException("apiGatewayHandler handle not found maybe apigateway jar not in classpath!");
			}
		}
		
		ExecutorConfigJRso config = new ExecutorConfigJRso();
		config.setMsCoreSize(1);
		config.setMsMaxSize(10);
		config.setTaskQueueSize(5);
		config.setThreadNamePrefix("ServerMessageReceiver-default");
		config.setRejectedExecutionHandler(new JicroAbortPolicy());
		defaultExecutor = ef.createExecutor(config);
		
		taskWorker.setDefaultExecutor(defaultExecutor);
		
		/*finishInit = true;
		synchronized(finishInit) {
			finishInit.notifyAll();
		}*/
		logger.info("Server ready:{}",Config.getInstanceName());
	}
	
	public void registHandler(IMessageHandler handler){
		if(handler.type() == -1) return;//网关处理器不用注册
		
		Map<Byte,IMessageHandler> handlers = this.handlers;
		if(handlers.containsKey(handler.type())){
			return;
		}
		handlers.put(handler.type(), handler);
		pi.getTypes().add(handler.type());
	}
	
	@Override
	public void receive(ISession s, Message msg) {
		
		/*
		 new Fiber<Void>(() -> {
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		 }).start();
		*/
		
		/*if(!finishInit) {
			synchronized(finishInit) {
				try {
					finishInit.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}*/
		
		long gotTime = 0;
		
		if(msg.isDebugMode()) {
			gotTime = TimeUtils.getCurTime();
		}
		
		ServiceMethodJRso sm = null;
		if(/*!msg.isOuterMessage() && */msg.getType() == Constants.MSG_TYPE_REQ_JRPC) {
			sm = srvMng.getServiceMethodByHash(msg.getSmKeyCode());
			if(sm == null) {
				/*UniqueServiceMethodKeyJRso smkey = srvMng.getServiceMethodWithHashBySearch(msg.getSmKeyCode());
				if(smkey != null) { 
					sm = srvMng.getServiceMethodByKey(smkey);
				}*/
				String errMsg = "Invalid message method code: [" + msg.toString() + "] mcode: " + msg.getSmKeyCode();
				errMsg += ",client host: " + s.remoteHost() + ",remotePort: " + s.remotePort()+",from insId: " + msg.getInsId();
				
				/*if(sm != null) {
					errMsg += ", sm key: " + sm.getKey().methodID();
				}*/
				
				LG.log(MC.LOG_ERROR, TAG, errMsg);
				responseException(msg,(IServerSession)s, new CommonException(RespJRso.CODE_FAIL,errMsg),sm);
				return;
			}
		}
		
		//确保服务器对称密钥一定是最新的
		try {
			secretMng.updateSecret(msg);
		} catch (Exception e1) {
			responseException(msg,(IServerSession)s,e1,sm);
		}
		
		JMicroTask t = this.popTask();
		t.setMsg(msg);
		t.setS((IServerSession)s);
		t.setSm(sm);
		t.gotTime = gotTime;
		
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
					errMsg += ", smKey: " + sm.getKey().fullStringKey()+",from insId: " + msg.getInsId();
					LG.log(MC.LOG_ERROR, TAG, errMsg);
					logger.error(errMsg);
					return;
		    	}
				try {
					taskWorker.sumbit(t);
				} catch (CommonException e) {
					responseException(msg,t.s,e,sm);
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
	private void doReceive(JMicroTask task){
		Message msg = task.msg;
		IServerSession s = task.s;
		JMicroContext.configProvider(s, msg, task.sm);
		
		if(task.sm != null) {
			JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, task.sm);
		}
		
		try {
			if(!checkLoginAndPermission(s,msg,task.sm)) {
				return;
			}
			
			if(msg.isDebugMode()) {
				JMicroContext.get().setParam(JMicroContext.DEBUG_LOG_BASE_TIME, task.gotTime);
				StringBuilder sb = JMicroContext.get().getDebugLog();
				long curTime = TimeUtils.getCurTime();
				
				sb.append("Client to server cost: ").append(task.gotTime - msg.getTime()).append(", ");
				sb.append("Queue cost: ").append(curTime-task.gotTime).append(", ");
				sb.append(msg.getMethod())
				.append(",MsgId:").append(msg.getMsgId())
				.append(",linkId:").append(JMicroContext.lid());
			}
			
			if(LG.isLoggable(MC.LOG_DEBUG,msg.getLogLevel())) {
				LG.log(MC.LOG_DEBUG, TAG,LG.messageLog("doReceive",msg));
			}
			
			if(msg.isUpSsl() || msg.isDownSsl()) {
				this.secretMng.checkAndDecrypt(msg);
			}
			
			 if(msg.isOuterMessage() && msg.getType() != Constants.MSG_TYPE_PUBSUB) {
				 //非订阅消息都经由网关转发到后台系统
				 gatewayHandler.onMessage(s, msg);
			 } else {
				 //全部后台系统或API网关内部服务走此代码
				IMessageHandler h = handlers.get(msg.getType());
				if(h == null) {
					String errMsg = "Message type ["+Integer.toHexString(msg.getType())+"] handler not found!"+",from insId: " + msg.getInsId();
					MT.rpcEvent(MC.MT_HANDLER_NOT_FOUND);
					LG.log(MC.LOG_ERROR, TAG,errMsg,null);
					responseException(msg,s,null,task.sm);
					JMicroContext.get().submitMRpcItem();
				} else {
					h.onMessage(s, msg);
				}
			}
			
		} catch (Throwable e) {
			MT.rpcEvent(MC.MT_SERVER_ERROR);
			LG.log(MC.LOG_ERROR, TAG,e.getMessage()+",from insId: " + msg.getInsId(),e);
			responseException(msg,s,e,task.sm);
			JMicroContext.get().submitMRpcItem();
		} finally {
			offerTask(task);
		}
	}

	private boolean checkLoginAndPermission(IServerSession s, Message msg,ServiceMethodJRso sm) {
		
		if(sm != null) {
			if(sm.getMaxPacketSize() > 0 && msg.getLen() > sm.getMaxPacketSize()) {
				String errMsg = "Packet too max " + msg.getLen() + 
	    				" limit size: " + sm.getMaxPacketSize()+",insId: " + msg.getInsId()+","+sm.getKey().getMethod();
	    		LG.log(MC.LOG_ERROR, TAG,errMsg);
				MT.rpcEvent(MC.MT_PACKET_TOO_MAX,1);
				RespJRso<Object> se = new RespJRso<>(MC.MT_PACKET_TOO_MAX,errMsg);
				resp2Client(se,s,msg,sm);
				return false;
			}
		}
		
		String lk = msg.getExtra(Message.EXTRA_KEY_LOGIN_KEY);
		ActInfoJRso ai = null;
		
		String slk = msg.getExtra(Message.EXTRA_KEY_LOGIN_SYS);
		ActInfoJRso sai = null;
		
		if(StringUtils.isNotEmpty(lk)) {
			ai = this.accountManager.getAccount(lk);
		}
		
		if(StringUtils.isNotEmpty(slk)) {
			sai = this.accountManager.getAccount(slk);
		}
		
		if(sai != null) {
			JMicroContext.get().setString(JMicroContext.LOGIN_KEY_SYS, slk);
			JMicroContext.get().setSysAccount(sai);
		}
		
		if(ai != null) {
			JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
			JMicroContext.get().setAccount(ai);
		}
		
		if(sm != null) {
			if(ai == null && sm.isNeedLogin()) {
				RespJRso<Object> se = new RespJRso<>(MC.MT_INVALID_LOGIN_INFO,"JRPC check invalid login key!"+",insId: " + msg.getInsId());
				String errMsg = "JRPC check invalid login key!"+",insId: " + msg.getInsId();
				LG.log(MC.LOG_ERROR, TAG, errMsg);
				MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
				resp2Client(se,s,msg,sm);
				return false;
			} 
		
			if(sai == null && sm.getForType() == Constants.FOR_TYPE_SYS) {
				String errMsg = "Invalid system login key: " + slk+",insId: " + msg.getInsId();
				RespJRso<Object> se = new RespJRso<>(MC.MT_INVALID_LOGIN_INFO,errMsg);
				LG.log(MC.LOG_ERROR, TAG,errMsg);
				MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
				resp2Client(se,s,msg,sm);
				return false;
			}
			
			RespJRso<Object> se = pm.permissionCheck(sm, sm.getKey().getUsk().getClientId());
			
			if(se != null) {
				resp2Client(se,s,msg,sm);
				return false;
			}
			
		}
		
		return true;
	}
	
	private void resp2Client(RespJRso<Object> rr, ISession s,Message msg,ServiceMethodJRso sm) {
		if(!msg.isNeedResponse()){
			submitItem();
			return;
		}
		
		if(msg.isDebugMode()) {
    		JMicroContext.get().appendCurUseTime("Service Return",true);
		}
		
		if(msg.isError()) {
			msg.setPayload(ICodecFactory.encode(codecFactory,rr,Message.PROTOCOL_JSON));
		} else {
			msg.setPayload(ICodecFactory.encode(codecFactory,rr,msg.getDownProtocol()));
		}
		
		//请求类型码比响应类型码大1，
		msg.setType((byte)(msg.getType()+1));
		LG.log(MC.LOG_ERROR, TAG, "Request failure end: " + msg.getMsgId()+",insId: " + msg.getInsId());
		MT.rpcEvent(MC.MT_SERVER_ERROR,1);
		//错误不需要做加密或签名
		msg.setUpSsl(false);
		msg.setDownSsl(false);
		msg.setSign(false);
		msg.setSec(false);
		msg.setSaltData(null);
		msg.setError(true);//响应错误响应消息

		msg.setInsId(pi.getId());
		//msg.setFromWeb(false);
		msg.setOuterMessage(false);
		
		try {
			msg.setTime(TimeUtils.getCurTime());
			s.write(msg);
			MT.rpcEvent(MC.MT_SERVER_JRPC_RESPONSE_WRITE, msg.getLen());
			if(msg.isDebugMode()) {
				JMicroContext.get().appendCurUseTime("Server finish write",true);
			}
			
			submitItem();
			
		} catch (Throwable e) {
			//到这里不能再抛出异常，否则可能会造成重复响应
			logger.error("",e);
		}
	}
	
	private void submitItem() {
		if(JMicroContext.get().isDebug()) {
			JMicroContext.get().appendCurUseTime("Async respTime",false);
			//JMicroContext.get().debugLog(0);
		}
		JMicroContext.get().submitMRpcItem();
	}

	private void responseException(Message msg,IServerSession s,Throwable e,ServiceMethodJRso sm) {
		
		StackTraceElement se = Thread.currentThread().getStackTrace()[2];
		logger.error("From line [" + se.getLineNumber() + "] reqHandler error msg:{}, sm:{}",msg,sm== null?"":sm.getKey().fullStringKey());
		if(e!= null) {
			logger.error("",e);
		}
		
		if(msg.isNeedResponse()) {
			RespJRso<Object> resp = null;
			String errMsg = e == null?"from insId: " + msg.getInsId():e.getMessage()+",from insId: " + msg.getInsId();
			if(e instanceof CommonException) {
				CommonException ce = (CommonException)e;
				resp = new RespJRso<Object>(ce.getKey(),errMsg);
			} else {
				resp = new RespJRso<Object>(RespJRso.CODE_FAIL,errMsg);
			}
			
			msg.setPayload(ICodecFactory.encode(codecFactory,resp,msg.getUpProtocol()));
			msg.setType((byte)(msg.getType()+1));
			msg.setUpSsl(false);
			msg.setDownSsl(false);
			msg.setSign(false);
			msg.setSec(false);
			msg.setSaltData(null);
			msg.setError(true);
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
		
		private ServiceMethodJRso sm = null;
		//private TaskRunnable r;
		//private Map<Short,StatisItem> typeStatis = new HashMap<>();

		private long gotTime;
		
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

		public ServiceMethodJRso getSm() {
			return sm;
		}

		public void setSm(ServiceMethodJRso sm) {
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
        		sb.append(" reqid[").append(msg.getMsgId()).append("]");
        		sb.append(" linkId[").append(msg.getLinkId()).append("]");
        		sb.append(" msgId[").append(msg.getMsgId()).append("]");
        		sb.append(" queueSize[").append(e.getQueue().size()).append("]");
        		sb.append(" activeCount[").append(e.getActiveCount()).append("]");
        		sb.append(" largestPoolSize[").append(e.getLargestPoolSize()).append("]");
        		sb.append(" corePoolSize[").append(e.getCorePoolSize()).append("]");
        		sb.append(" maximumPoolSize[").append(e.getMaximumPoolSize()).append("]");
        		
        		JMLogItemJRso mi = LG.logWithNonRpcContext(MC.LOG_ERROR, JicroAbortPolicy.class,sb.toString(),MC.MT_EXECUTOR_REJECT,false);
        		
        		if(mi != null) {
        			mi.setLinkId(msg.getLinkId());
        			mi.setProvider(true);
        			
        			if(t.sm != null) {
        				mi.setSmKey(t.sm.getKey());
        			}
        			
        			/*if(Constants.MSG_TYPE_REQ_RAW == msg.getType()) {
            			ApiRequest re = ICodecFactory.decode(codecFactory, msg.getPayload(), ApiRequest.class,
            					msg.getUpProtocol());
            			 mi.setReq(re);
            			 mi.setReqId(re.getReqId());
            			
            		} else */if(Constants.MSG_TYPE_ID_REQ == msg.getType()) {
            			IdRequest re = ICodecFactory.decode(codecFactory, msg.getPayload(), IdRequest.class,
            					msg.getUpProtocol());
            		} else {
            			RpcRequestJRso re = ICodecFactory.decode(codecFactory, msg.getPayload(), RpcRequestJRso.class,
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
