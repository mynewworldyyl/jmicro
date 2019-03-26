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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:51:20
 */
@Component(lazy=false,active=true,value="serverReceiver",side=Constants.SIDE_PROVIDER)
public class ServerMessageReceiver implements IMessageReceiver{

	static final Logger logger = LoggerFactory.getLogger(ServerMessageReceiver.class);
	static final Class<?> TAG = ServerMessageReceiver.class;
	
	@Cfg("/ServerMessageReceiver/openDebug")
	private boolean openDebug;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private JRPCReqRespHandler jrpcHandler;
	
	@Inject("idRequestMessageHandler")
	private IMessageHandler idHandler;
	
	/*@Cfg(value="/ServerReceiver/receiveBufferSize")
	private int receiveBufferSize=1000;*/
	
	private ExecutorService executor = null;
	
	private volatile Map<Byte,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private Boolean ready = new Boolean(false);
	
	public void init(){
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("ServerMessageReceiver");
		executor = ExecutorFactory.createExecutor(config);
		//系统级RPC处理器，如ID请求处理器，和普通RPC处理理器同一个实例，但是TYPE标识不同，需要特殊处理
		handlers.put(Constants.MSG_TYPE_SYSTEM_REQ_JRPC, jrpcHandler);
		handlers.put(Constants.MSG_TYPE_ID_REQ, idHandler);
	}
	
	public void registHandler(IMessageHandler handler){
		Map<Byte,IMessageHandler> handlers = this.handlers;
		if(handlers.containsKey(handler.type())){
			return;
		}
		handlers.put(handler.type(), handler);
		ready = true;
		synchronized(ready){
			ready.notifyAll();
		}
	}
	
	@Override
	//@Suspendable
	public void receive(ISession s, Message msg) {
		 JMicroContext.configProvider(msg);
		if(openDebug) {
			//SF.getIns().doMessageLog(MonitorConstant.DEBUG, TAG, msg,"receive");
		}
		if(!ready) {
			synchronized(ready){
				try {
					ready.wait();
				} catch (InterruptedException e) {
					logger.error("receive(IServerSession s, ByteBuffer data) do wait",e);
				}
			}
		}
		JMicroContext jc = JMicroContext.get();
		//直接协程处理，IO LOOP线程返回
		
		/*new Fiber<Void>(() -> {
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		}).start();*/
		
		executor.submit(()->{
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
			
		});
		
		/*new Thread(()->{
			JMicroContext.get().mergeParams(jc);
			doReceive((IServerSession)s,msg);
		}).start();*/
	}
	
	//@Suspendable
	private void doReceive(IServerSession s, Message msg){
		
		if(msg.isLoggable()) {
			SF.doMessageLog(MonitorConstant.LOG_DEBUG, TAG, msg,null,"doReceive");
		}
		
		try {
			IMessageHandler h = handlers.get(msg.getType());
			if(h == null) {
				String errMsg = "Message type ["+Integer.toHexString(msg.getType())+"] handler not found!";
				SF.doMessageLog(MonitorConstant.LOG_ERROR, TAG, msg,null,errMsg);
				throw new CommonException(errMsg);
			}
			h.onMessage(s, msg);
		} catch (Throwable e) {
			SF.doMessageLog(MonitorConstant.LOG_ERROR, TAG, msg,e);
			SF.doSubmit(MonitorConstant.SERVER_REQ_ERROR);
			logger.error("reqHandler error msg:{} ",msg);
			logger.error("",e);
			msg.setType((byte)(msg.getType()+1));
			ServerError se = new ServerError();
			se.setMsg(e.getMessage());
			msg.setPayload(codeFactory.getEncoder(msg.getProtocol()).encode(se));
			s.write(msg);
		}
	}
}
