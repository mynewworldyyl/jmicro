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

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * @author Yulei Ye
 * @date 2018年10月10日-下午12:56:02
 */
@Component(active=true,side=Constants.SIDE_COMSUMER)
public class ClientMessageReceiver implements IMessageReceiver{

	static final Logger logger = LoggerFactory.getLogger(ClientMessageReceiver.class);
	
	private Map<Byte,IMessageHandler> handlers = new HashMap<>();
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	public void init(){
		
	}

	@Override
	public void receive(ISession session,Message msg) {
		/*Message msg = new Message();
		msg.decode(buffer);*/
		//CodecFactory.decode(this.codecFactory,buffer);
		try {
			IMessageHandler h = handlers.get(msg.getType());
			if(h != null){
				h.onMessage(session,msg);
			} else {
				SF.netIoRead(this.getClass().getName(),MonitorConstant.CLIENT_IOSESSION_READ, msg.getLen(),session);
				SF.doMessageLog(MonitorConstant.LOG_ERROR, ClientMessageReceiver.class, msg, null, 
						"Handler not found:" + Integer.toHexString(msg.getType()));
				logger.error("Handler not found:" + Integer.toHexString(msg.getType()));
			}
		} catch (Throwable e) {
			logger.error("reqHandler error: {}",msg,e);
			SF.doMessageLog(MonitorConstant.LOG_ERROR, ClientMessageReceiver.class, msg, e, 
					"Handler not found:" + Integer.toHexString(msg.getType()));
			msg.setType((byte)(msg.getType()+1));
		}
	}

	public void registHandler(IMessageHandler handler){
		if(this.handlers.containsKey(handler.type())){
			return;
		}
		this.handlers.put(handler.type(), handler);
	}
	
}
