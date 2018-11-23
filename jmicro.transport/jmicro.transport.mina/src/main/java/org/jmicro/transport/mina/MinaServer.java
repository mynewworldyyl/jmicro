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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.server.IServer;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.jmicro.server.IServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:53
 */
@Component(value=Constants.TRANSPORT_MINA,lazy=false,level=1,side=Constants.SIDE_PROVIDER,active=true)
@Server(transport=Constants.TRANSPORT_MINA)
public class MinaServer implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(MinaServer.class);
	
	@Cfg(value="/MinaServer/openDebug",required=false)
	private boolean openDebug=false;
	
	AttributeKey<MinaServerSession> sessinKey = AttributeKey.createKey(MinaServerSession.class, Constants.SESSION_KEY);
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.createKey(Boolean.class, Constants.MONITOR_ENABLE_KEY);
	
	AttributeKey<Object> monitorCfgEnableKey = AttributeKey.createKey(Object.class, "monitorCfgEnableKey");
	
	private Set<MinaServerSession> sessions = new HashSet<>();
	
	//@Inject(required=false)
	private  NioTcpServer acceptor;
	
	//@Inject(required=false)
	private AbstractIoHandler iohandler;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Cfg(value = "/bindIp",required=false)
	private String host;
	
	@Cfg(value="/port",required=false)
	private int port;
	
	@Cfg(value = "/MinaServer/readBufferSize",required=false)
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	private Timer ticker = new Timer("ClientSessionHeardbeatWorker",true);
	
	@Override
	public void init() {
		if(Config.isClientOnly()) {
			return;
		}
		start();
		this.receiver.registHandler(new IMessageHandler(){
			@Override
			public Short type() {
				return Constants.MSG_TYPE_HEARBEAT_REQ;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				/*try {
					System.out.println(new String(msg.getPayload().array(),Constants.CHARSET));
				} catch (UnsupportedEncodingException e) {
				}*/
				session.active();
				msg.setType(Constants.MSG_TYPE_HEARBEAT_RESP);
				session.write(msg);
			}
		});
		
		ticker.schedule(new TimerTask(){
			@Override
			public void run() {
				Iterator<MinaServerSession> ite = sessions.iterator();
				for(;ite.hasNext();){
					IServerSession s = ite.next();
					if(!s.isActive()) {
						s.close(true);
						ite.remove();
					}
				}
			}	
		}, 0, heardbeatInterval*1000);
		
	}
	
	 private Boolean monitorEnable(IoSession ioSession) {
    	 Boolean v = ioSession.getAttribute(this.monitorEnableKey);
		 return v == null ? JMicroContext.get().isMonitor():v;
    }

	@Override
	public void start() {

        //LOG.info("starting echo server");

        if(acceptor == null){
        	 acceptor = new NioTcpServer();
        }
        
        if(this.iohandler == null){
        	this.iohandler = new AbstractIoHandler() {
                @Override
                public void sessionOpened(final IoSession session) {
                    LOG.info("session opened {}", session);           
                    MinaServerSession s = new MinaServerSession(session,readBufferSize,heardbeatInterval);
                    s.putParam(Constants.SESSION_KEY, session);
                    //s.setSessionId(idGenerator.getLongId(ISession.class));
                    session.setAttribute(sessinKey, s);
                    if(monitor != null && monitorEnable(session)){
                    	 SF.doSubmit(MonitorConstant.SERVER_IOSESSION_OPEN,session.getId()+"");
                    }
                }

                @Override
                public void messageReceived(IoSession session, Object message) {
                	 if(monitor != null && monitorEnable(session)){
                		 SF.doSubmit(MonitorConstant.SERVER_IOSESSION_READ,
                     			session.getId()+"",((ByteBuffer)message).remaining()+"");
                	 }
                	
                    MinaServerSession s = session.getAttribute(sessinKey);
                    
                  //先把网络数据存起来，放到缓存中
                    ByteBuffer buffer = s.getReadBuffer();
                    buffer.put((ByteBuffer)message);
            		
                    ByteBuffer body = Decoder.readMessage(buffer);
                    if(body == null){
                    	return;
                    }
                    Message msg = new Message();
                    msg.decode(body);
                    if(openDebug){
                    	LOG.debug("Rec Message reqId:"+msg.getReqId());
                    }
        			receiver.receive(s,msg);
            		//jmicroManager.addRequest(req);
                }

				@Override
				public void sessionClosed(IoSession session) {
					super.sessionClosed(session);
					 MinaServerSession s = session.getAttribute(sessinKey);
					 if(s != null){
						 s.close(true);
					 }
					if(monitor != null && monitorEnable(session)){
						SF.doSubmit(MonitorConstant.SERVER_IOSESSION_CLOSE,session.getId()+"");
					}
				}

				@Override
				public void sessionIdle(IoSession session, IdleStatus status) {
					// TODO Auto-generated method stub
					super.sessionIdle(session, status);
					if(monitor != null && monitorEnable(session)){
						SF.doSubmit(MonitorConstant.SERVER_IOSESSION_CLOSE, 
								session.getId()+"",status.toString());
					}
				}

				@Override
				public void messageSent(IoSession session, Object message) {
					super.messageSent(session, message);
					if(monitor != null && monitorEnable(session)){
						monitor.submit(MonitorConstant.SERVER_IOSESSION_WRITE,session.getId()+"",
							((ByteBuffer)message).remaining()+"");
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
					if(monitor != null && monitorEnable(session)){
						SF.doSubmit(MonitorConstant.SERVER_IOSESSION_EXCEPTION
								,cause,session.getId()+"");
					}
				}

				/*@Override
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
				}*/
                
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
        SF.doSubmit(MonitorConstant.SERVER_START,m);
	}

	@Override
	public void stop() {
		SF.doSubmit(MonitorConstant.SERVER_STOP, this.host,this.port+"");
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
