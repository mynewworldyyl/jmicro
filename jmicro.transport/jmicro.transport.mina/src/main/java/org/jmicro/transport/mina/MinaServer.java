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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.transport.nio.NioTcpServer;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.net.IServerReceiver;
import org.jmicro.api.server.IServer;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.url.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:53
 */
@Component(value=Constants.DEFAULT_SERVER,lazy=false,level=10)
@Server
public class MinaServer implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(MinaServer.class);
	
	AttributeKey<MinaServerSession> sessinKey = AttributeKey.createKey(MinaServerSession.class, Constants.SESSION_KEY);
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.createKey(Boolean.class, Constants.MONITOR_ENABLE_KEY);
	
	AttributeKey<Object> monitorCfgEnableKey = AttributeKey.createKey(Object.class, "monitorCfgEnableKey");
	
	//@Inject(required=false)
	private  NioTcpServer acceptor;
	
	//@Inject(required=false)
	private AbstractIoHandler iohandler;
	
	//@Inject
	//private IRequestHandler reqHandler;
	
/*	@Inject
	private JmicroManager jmicroManager;*/
	
	@Inject
	private IServerReceiver receiver;
			 
/*	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(value=Constants.DEFAULT_CODEC_FACTORY,required=true)
	private ICodecFactory codecFactory;*/
	
	@Cfg(value = "/bindIp",required=false)
	private String host;
	
	@Cfg(value="/port",required=false)
	private int port;
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	@Override
	public void init() {
		start();
	}
	
	 private Boolean monitorEnable(IoSession ioSession) {
    	 Boolean v = ioSession.getAttribute(this.monitorEnableKey);
		 return v == null ? JMicroContext.get().isMonitor():v;
    }

	@Override
	public void start() {

        LOG.info("starting echo server");

        if(acceptor == null){
        	 acceptor = new NioTcpServer();
        }
        
        if(this.iohandler == null){
        	this.iohandler = new AbstractIoHandler() {
                @Override
                public void sessionOpened(final IoSession session) {
                    LOG.info("session opened {}", session);           
                    MinaServerSession s = new MinaServerSession(session);
                    s.putParam(Constants.SESSION_KEY, session);
                    //s.setSessionId(idGenerator.getLongId(ISession.class));
                    session.setAttribute(sessinKey, s);
                    if(monitorEnable(session)){
                    	 MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_IOSESSION_OPEN, null,null,session.getId());
                    }
                }

                @Override
                public void messageReceived(IoSession session, Object message) {
                	 if(monitorEnable(session)){
                		 MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_IOSESSION_READ,
                     			null,null,session,((ByteBuffer)message).remaining());
                	 }
                	
                    MinaServerSession s = session.getAttribute(sessinKey);
                    
                    ByteBuffer body = Decoder.readMessage((ByteBuffer)message, s.getReadBuffer());
                    if(body == null){
                    	return;
                    }
        			
        			receiver.receive(s,body);
            		//jmicroManager.addRequest(req);
                }

				@Override
				public void sessionClosed(IoSession session) {
					super.sessionClosed(session);
					 MinaServerSession s = session.getAttribute(sessinKey);
					 if(s != null){
						 s.close(true);
					 }
					if(monitorEnable(session)){
						MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_IOSESSION_CLOSE, null,null,session.getId());
					}
				}

				@Override
				public void sessionIdle(IoSession session, IdleStatus status) {
					// TODO Auto-generated method stub
					super.sessionIdle(session, status);
					if(monitorEnable(session)){
						MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_IOSESSION_CLOSE, 
								null,null,session,status);
					}
				}

				@Override
				public void messageSent(IoSession session, Object message) {
					super.messageSent(session, message);
					if(monitorEnable(session)){
					monitor.submit(MonitorConstant.SERVER_IOSESSION_WRITE, null,null,session.getId(),
							((ByteBuffer)message).remaining());
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
					if(monitorEnable(session)){
						MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_IOSESSION_EXCEPTION,
								null,null,session.getId(),cause);
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
        }

        // create the filter chain for this service
        //acceptor.setFilters(new LoggingFilter("LoggingFilter1"));
        acceptor.setIoHandler(this.iohandler);
        if(StringUtils.isEmpty(this.host)){
        	List<String> ips = Utils.getIns().getLocalIPList();
            if(ips.isEmpty()){
            	throw new CommonException("IP not found");
            }
            this.host = ips.get(0);
        }
        
        //InetAddress.getByAddress(Array(127, 0, 0, 1))
        InetSocketAddress address = new InetSocketAddress(this.host,this.port);
        acceptor.bind(address);
        try {
			address = (InetSocketAddress)acceptor.getServerSocketChannel().getLocalAddress();
		} catch (IOException e) {
			LOG.error("",e);
		}
        this.port = address.getPort();
        
        String m = "Running the server host["+this.host+"],port ["+this.port+"]";
        LOG.debug(m);    
        MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_START, null,null,m);
	}

	@Override
	public void stop() {
		MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_STOP, null,null,this.host,this.port);
		 if(acceptor != null){
			 acceptor.unbind();
			 acceptor = null;
        }
	}

	@Override
	public String host() {
		return this.host;
	}

	@Override
	public int port() {
		return this.port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
