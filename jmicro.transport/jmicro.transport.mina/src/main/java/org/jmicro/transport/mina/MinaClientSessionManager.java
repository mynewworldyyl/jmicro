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
package org.jmicro.transport.mina;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.transport.nio.NioTcpClient;
import org.jmicro.api.IIdGenerator;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.IClientReceiver;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.server.ISession;
import org.jmicro.api.server.Message;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:45
 */
@Component(lazy=false)
public class MinaClientSessionManager implements IClientSessionManager{

	static final Logger logger = LoggerFactory.getLogger(MinaClientSessionManager.class);

	AttributeKey<IClientSession> sessionKey = AttributeKey.createKey(IClientSession.class, Constants.SESSION_KEY);
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.createKey(Boolean.class, Constants.MONITOR_ENABLE_KEY);
	
	private final Map<String,IClientSession> sessions = new ConcurrentHashMap<>();
	
	@Cfg("/MinaClientSessionManager/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	@Inject(required=false)
	private IClientReceiver receiver;
	
	private AbstractIoHandler ioHandler = new AbstractIoHandler() {
        @Override
        public void sessionOpened(final IoSession session) {
            //LOG.info("session opened {}", session);
            if(monitorEnable(session)){
            	MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_OPEN, null,null,session.getId());
            }
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            //LOG.info("message received {}", message);
            //avoid ((ByteBuffer)message).remaining() when disable monitor
            if(monitorEnable(session) && monitor != null){
            	IClientSession s  = session.getAttribute(sessionKey);
            	monitor.submit(MonitorConstant.CLIENT_IOSESSION_READ, null,null,s.getId(),session.getReadBytes(),((ByteBuffer)message).remaining());
            }
            IClientSession cs = session.getAttribute(sessionKey);
            
            ByteBuffer body = Decoder.readMessage((ByteBuffer)message, cs.getReadBuffer());
            if(body == null){
            	return;
            }
        	Message msg = new Message();
    		msg.decode(body);
    		
            receiver.onMessage(cs,msg);
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            //LOG.info("message sent {}", message);
            //avoid ((ByteBuffer)message).remaining()  when disable monitor
            if(monitorEnable(session) && monitor != null){
            	IClientSession s  = session.getAttribute(sessionKey);
            	monitor.submit(MonitorConstant.CLIENT_IOSESSION_WRITE, null,null,s.getId(),((ByteBuffer)message).remaining());
            }
        }

        @Override
        public void sessionClosed(IoSession session) {
            //LOG.info("session closed {}", session);
            if(monitorEnable(session) && monitor != null){
            	IClientSession s  = session.getAttribute(sessionKey);
            	MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_CLOSE, null,null,s.getId());
            }
        }

		@Override
		public void sessionIdle(IoSession session, IdleStatus status) {
			super.sessionIdle(session, status);
			if(monitorEnable(session)){
				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_IDLE, null,null,session,status);
			}
		}

		@Override
		public void serviceActivated(IoService service) {
			super.serviceActivated(service);
		}

		@Override
		public void serviceInactivated(IoService service) {
			super.serviceInactivated(service);
		}

		@Override
		public void exceptionCaught(IoSession session, Exception cause) {
			 super.exceptionCaught(session, cause);
			 logger.error("exceptionCaught",cause);
			 if(session !=null && monitorEnable(session) && monitor != null ){
				 IClientSession s  = session.getAttribute(sessionKey);
	             MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_EXCEPTION, null,null,s.getId(),cause.getMessage());
	         }
		}

		@Override
		public void handshakeStarted(IoSession abstractIoSession) {
			super.handshakeStarted(abstractIoSession);
		}

		@Override
		public void handshakeCompleted(IoSession session) {
			super.handshakeCompleted(session);
		}

		@Override
		public void secureClosed(IoSession session) {
			super.secureClosed(session);
		}
    };
    
    private Boolean monitorEnable(IoSession ioSession) {
    	 Boolean v = ioSession.getAttribute(this.monitorEnableKey);
		 return v == null ? JMicroContext.get().isMonitor():v;
    }

	@Override
	public IClientSession connect(String host, int port) {

		String sKey = host+port;
		if(sessions.containsKey(sKey)){
			return sessions.get(sKey);
		}
		
		sKey = sKey.intern();
		synchronized(sKey) {
			
			if(sessions.containsKey(sKey)){
				return sessions.get(sKey);
			}
			
	        final NioTcpClient client = new NioTcpClient();
	        client.setFilters();
	        client.setIoHandler(ioHandler);

	        try {
	       	   IoFuture<IoSession> future = client.connect(new InetSocketAddress(host,port));
	           IoSession session = future.get();
	          // LOG.info("session opened {}", session);
	           if(session == null){
	               throw new CommonException("Fail to create session");
	           }
	           MinaClientSession s = new MinaClientSession(session,readBufferSize);
	           s.setId(this.idGenerator.getLongId(ISession.class));
	           s.putParam(Constants.SESSION_KEY, session);
	           
	           s.putParam(Constants.MONITOR_ENABLE_KEY, JMicroContext.get().isMonitor());
	           
	           session.setAttribute(sessionKey, s);
	           session.getAttribute(monitorEnableKey,JMicroContext.get().isMonitor());
	           //this.sessions.put(s.getSessionId(), s);
	           sessions.put(sKey, s);
	           
	           //LOG.info("session connected : {}", session);
	           logger.debug("connection finish,host:"+host+",port:"+port);
	           return s;
	       } catch (Throwable e) {
	    	   logger.error("cannot connect : ", e);
	           MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_CONN_FAIL, null, null,host,port,e.getMessage());
	           throw new CommonException("",e);
	       }
		}
	}
	
}
