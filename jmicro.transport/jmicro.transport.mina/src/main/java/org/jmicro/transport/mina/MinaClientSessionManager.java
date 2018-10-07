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
import java.util.concurrent.ExecutionException;

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
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.client.IResponseHandler;
import org.jmicro.api.client.ReqResp;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.ISession;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.api.server.ServerError;
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

	static final Logger LOG = LoggerFactory.getLogger(MinaClientSessionManager.class);

	AttributeKey<IClientSession> sessionKey = AttributeKey.createKey(IClientSession.class, Constants.SESSION_KEY);
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.createKey(Boolean.class, Constants.MONITOR_ENABLE_KEY);
	
	private final Map<Long,ReqResp> waitForResponse = new ConcurrentHashMap<>();
	
	//private final Map<Long,IClientSession> sessions = new ConcurrentHashMap<>();
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	private AbstractIoHandler ioHandler = new AbstractIoHandler() {
        @Override
        public void sessionOpened(final IoSession session) {
            LOG.info("session opened {}", session);
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
            	monitor.submit(MonitorConstant.CLIENT_IOSESSION_READ, null,null,s.getSessionId(),session.getReadBytes(),((ByteBuffer)message).remaining());
            }
            doReceive(session,(ByteBuffer)message);
            
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            //LOG.info("message sent {}", message);
            //avoid ((ByteBuffer)message).remaining()  when disable monitor
            if(monitorEnable(session) && monitor != null){
            	IClientSession s  = session.getAttribute(sessionKey);
            	monitor.submit(MonitorConstant.CLIENT_IOSESSION_WRITE, null,null,s.getSessionId(),((ByteBuffer)message).remaining());
            }
        }

        @Override
        public void sessionClosed(IoSession session) {
            LOG.info("session closed {}", session);
            if(monitorEnable(session) && monitor != null){
            	IClientSession s  = session.getAttribute(sessionKey);
            	MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_CLOSE, null,null,s.getSessionId());
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
			 LOG.error("exceptionCaught",cause);
			 if(session !=null && monitorEnable(session) && monitor != null ){
				 IClientSession s  = session.getAttribute(sessionKey);
	             MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_IOSESSION_EXCEPTION, null,null,s.getSessionId(),cause.getMessage());
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
	public void write(IRequest req, IResponseHandler handler,int retryCnt) {
		
		Message msg = new Message();
		msg.setType(Message.PROTOCOL_TYPE_BEGIN);
		msg.setId(this.idGenerator.getLongId(Message.class));
		msg.setReqId(req.getRequestId());
		msg.setSessionId(req.getSession().getSessionId());
		msg.setPayload(req.encode());
		msg.setExt((byte)0);
		msg.setReq(true);
		msg.setVersion(Message.VERSION_STR);
		
		waitForResponse.put(req.getRequestId(), new ReqResp(msg,req,handler,retryCnt));
		
		byte[] data = msg.encode();
		
		IClientSession cs = (IClientSession)req.getSession();

		cs.write(ByteBuffer.wrap(data));
		
	}

	@Override
	public IClientSession connect(String host, int port) {

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
           MinaClientSession s = new MinaClientSession(session);
           s.setSessionId(this.idGenerator.getLongId(ISession.class));
           s.putParam(Constants.SESSION_KEY, session);
           
           s.putParam(Constants.MONITOR_ENABLE_KEY, JMicroContext.get().isMonitor());
           
           session.setAttribute(sessionKey, s);
           session.getAttribute(monitorEnableKey,JMicroContext.get().isMonitor());
           //this.sessions.put(s.getSessionId(), s);
           
           //LOG.info("session connected : {}", session);
           LOG.debug("connection finish");
           return s;
       } catch (Throwable e) {
           LOG.error("cannot connect : ", e);
           MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_CONN_FAIL, null, null,host,port,e.getMessage());
           throw new CommonException("",e);
       }
    
	}

	protected void doReceive(IoSession session, ByteBuffer message) {
		IClientSession cs = session.getAttribute(this.sessionKey);
		ByteBuffer b = cs.getReadBuffer();
		b.put(message);
		
		int totalLen = b.remaining();
		if(totalLen < Message.HEADER_LEN) {
			return;
		}
		
		b.flip();
		
		b.mark();
		
		int len = b.getInt();
		b.reset();
		if(totalLen-10 < len){
			return ;
		}
		
		
		byte[] data = new byte[Message.HEADER_LEN+len];
		b.get(data);
		b.compact();
		
		Message msg = new Message();
		msg.decode(data);
		
		//receive response
		 ReqResp rr = waitForResponse.get(msg.getReqId());
				
		if(rr != null && msg.getSessionId() != cs.getSessionId()) {
			rr.req.setSuccess(false);
			LOG.warn("Ignore MSG" + msg.getId() + "Rec session ID: "+msg.getSessionId()+",but this session ID: "+cs.getSessionId());
			return;
		}
		
		JMicroContext.get().configMonitor(rr.req.isMonitorEnable()?1:0,0);
		
		RpcResponse resp = new RpcResponse(msg.getId());
		resp.decode(msg.getPayload());
		resp.setMsg(msg);
		
		if(rr != null) {
			if(resp.getResult() instanceof ServerError){
				//should do retry or time logic
				ServerError se = (ServerError)resp.getResult();
				LOG.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				rr.req.setSuccess(false);
				rr.handler.onResponse(resp,rr.req,se);
			}else {
				rr.req.setSuccess(true);
				rr.handler.onResponse(resp,rr.req,null);
			}
		}
		
		if(msg.getType() == Message.PROTOCOL_TYPE_END) {
			if(rr != null) {
				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_CONN_CLOSE, rr.req,resp);
			}
			 
			waitForResponse.remove(msg.getReqId());
			cs.close(false);
		}
		
	}
	
}
