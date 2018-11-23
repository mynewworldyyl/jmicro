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

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Suspendable;

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
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	/*@Cfg(value="/ServerReceiver/receiveBufferSize")
	private int receiveBufferSize=1000;*/
	
	private volatile Map<Short,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private Boolean ready = new Boolean(false);
	
	public void init(){
		
	}
	
	public void registHandler(IMessageHandler handler){
		Map<Short,IMessageHandler> handlers = this.handlers;
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
	@Suspendable
	public void receive(ISession s, Message msg) {
		/*if(openDebug) {
			SF.getIns().doMessageLog(MonitorConstant.DEBUG, TAG, msg,"receive");
		}*/
		if(!ready) {
			synchronized(ready){
				try {
					ready.wait();
				} catch (InterruptedException e) {
					logger.error("receive(IServerSession s, ByteBuffer data) do wait",e);
				}
			}
		}
		//直接协程处理，IO LOOP线程返回
		//new Fiber<Void>(() ->doReceive((IServerSession)s,data)).start();
		new Thread(()->{doReceive((IServerSession)s,msg);}).start();
	}
	
	@Suspendable
	private void doReceive(IServerSession s, Message msg){
		
		JMicroContext.get().configMonitor(msg.isMonitorable());
		JMicroContext.setMonitor(monitor);
		
		JMicroContext.get().setParam(JMicroContext.SESSION_KEY, s);
			
		JMicroContext.get().setParam(JMicroContext.CLIENT_IP, s.remoteHost());
		JMicroContext.get().setParam(JMicroContext.CLIENT_PORT, s.remotePort());
		
		if(openDebug) {
			SF.doMessageLog(MonitorConstant.DEBUG, TAG, msg,null,"doReceive");
		}
		
		try {
			IMessageHandler h = handlers.get(msg.getType());
			if(h == null) {
				String errMsg = "Message type ["+Integer.toHexString(msg.getType())+"] handler not found!";
				SF.doMessageLog(MonitorConstant.ERROR, TAG, msg,null,errMsg);
				throw new CommonException(errMsg);
			}
			h.onMessage(s, msg);
		} catch (Throwable e) {
			SF.doMessageLog(MonitorConstant.ERROR, TAG, msg,e);
			SF.doSubmit(MonitorConstant.SERVER_REQ_ERROR);
			logger.error("reqHandler error: ",e);
			msg.setType((short)(msg.getType()+1));
			s.write(msg);
		}
	}
}
