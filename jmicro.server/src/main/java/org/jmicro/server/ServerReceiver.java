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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:51:20
 */
@Component(lazy=false,active=true,value="serverReceiver",side=Constants.SIDE_PROVIDER)
public class ServerReceiver implements IMessageReceiver{

	static final Logger logger = LoggerFactory.getLogger(ServerReceiver.class);
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject
	private IIdGenerator idGenerator;
	
	/*@Cfg(value="/ServerReceiver/receiveBufferSize")
	private int receiveBufferSize=1000;*/
	
	private volatile Map<Short,IMessageHandler> handlers = new ConcurrentHashMap<>();
	
	private Boolean ready = new Boolean(false);
	
	public void init(){
		
	}
	
	public void registHandler(IMessageHandler handler){
		if(this.handlers.containsKey(handler.type())){
			return;
		}
		this.handlers.put(handler.type(), handler);
		ready = true;
		synchronized(ready){
			ready.notifyAll();
		}
	}
	
	@Override
	@Suspendable
	public void receive(ISession s, ByteBuffer data) {
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
		new Fiber<Void>(() ->doReceive((IServerSession)s,data)).start();
	}
	
	@Suspendable
	private void doReceive(IServerSession s, ByteBuffer data){

		Message msg = new Message();
		msg.decode(data);
		
		if(s.getId() != -1 && msg.getSessionId() != s.getId()) {
			String msg1 = "Ignore MSG" + msg.getId() + "Rec session ID: "+msg.getSessionId()+",but this session ID: "+s.getId();
			logger.warn(msg1);
			if(monitorEnable(s)){
				MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_PACKAGE_SESSION_ID_ERR,
						null,null,msg.getId(),msg.getReqId(),s.getId(),msg1);
			}
			msg.setType((short)(msg.getType()+1));
			s.write(msg.encode());
			return;
		}
		
		try {
			IMessageHandler h = handlers.get(msg.getType());
			h.onMessage(s, msg);
		} catch (Throwable e) {
			MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_REQ_ERROR, null,null);
			logger.error("reqHandler error: ",e);
			msg.setType((short)(msg.getType()+1));
			s.write(msg.encode());
		}
	}
	
	public static Boolean monitorEnable(IServerSession session) {
   	 	 Boolean v = (Boolean)session.getParam(Constants.MONITOR_ENABLE_KEY);
		 return v == null ? JMicroContext.get().isMonitor():v;
    }
}
