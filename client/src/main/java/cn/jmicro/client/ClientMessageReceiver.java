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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.executor.NamedThreadFactory;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/** 
 * @author Yulei Ye
 * @date 2018年10月10日-下午12:56:02
 */
@Component(active=true,side=Constants.SIDE_COMSUMER,level=9)
public class ClientMessageReceiver implements IMessageReceiver{

	static final Logger logger = LoggerFactory.getLogger(ClientMessageReceiver.class);
	
	private Map<Byte,IMessageHandler> handlers = new HashMap<>();
	
	private ExecutorService defaultExecutor = null;
	
	@Inject
	private ExecutorFactory ef;
	
	public ClientMessageReceiver() {}
	
	public ClientMessageReceiver(boolean client) {
		
		ExecutorConfig cfg = new ExecutorConfig();
		cfg.setMsCoreSize(1);
		cfg.setMsMaxSize(10);
		cfg.setTaskQueueSize(100);
		cfg.setThreadNamePrefix("ClientMessageReceiver-client");
		cfg.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				logger.error("Reject task: " + r.toString());
			}
		});
		
		if(StringUtils.isEmpty(cfg.getThreadNamePrefix())) {
			cfg.setThreadNamePrefix("Default");
		}
		
		defaultExecutor = new ThreadPoolExecutor(cfg.getMsCoreSize(),cfg.getMsMaxSize(),
				cfg.getIdleTimeout(),TimeUtils.getTimeUnit(cfg.getTimeUnit()),
				new ArrayBlockingQueue<Runnable>(cfg.getTaskQueueSize()),
				new NamedThreadFactory("JMicro-"+"client"+"-"+cfg.getThreadNamePrefix())
				,cfg.getRejectedExecutionHandler());
	}
	
	public void ready(){
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(5);
		config.setMsMaxSize(20);
		config.setTaskQueueSize(10);
		config.setThreadNamePrefix("ClientMessageReceiver-default");
		config.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				logger.error("Reject task: " + r.toString());
			}
		});
		defaultExecutor = ef.createExecutor(config);
	}

	@Override
	public void receive(ISession session,Message msg) {
		/*Message msg = new Message();
		msg.decode(buffer);*/
		//CodecFactory.decode(this.codecFactory,buffer);
       /* if(msg.isMonitorable()) {
      	  SF.netIoRead(this.getClass().getName(),MC.MT_CLIENT_IOSESSION_READ, msg.getLen());
        }*/
        
		try {
			IMessageHandler h = handlers.get(msg.getType());
			if(h != null){
				defaultExecutor.execute(()->{
					h.onMessage(session,msg);
				});
			} else {
				String errMsg = "Handler not found:" + Integer.toHexString(msg.getType());
				
				logger.error("Handler not found:" + Integer.toHexString(msg.getType()));
				
				if(msg.isLoggable()) {
					LG.log(MC.LOG_ERROR, ClientMessageReceiver.class,errMsg);
				}
				
				if(msg.isMonitorable()) {
					MT.rpcEvent(MC.MT_HANDLER_NOT_FOUND, 1, 1);
				}
			}
		} catch (Throwable e) {
			logger.error("reqHandler error: {}",msg,e);
			if(msg.isLoggable()) {
				LG.log(MC.LOG_ERROR, ClientMessageReceiver.class,"receive error",e);
			}
			
			if(msg.isMonitorable()) {
				MT.rpcEvent(MC.MT_REQ_ERROR, 1, 1);
			}
		}
	}

	public void registHandler(IMessageHandler handler){
		if(this.handlers.containsKey(handler.type())){
			return;
		}
		this.handlers.put(handler.type(), handler);
	}
	
}
