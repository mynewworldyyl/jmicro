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
package org.jmicro.transport.netty.client;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.IIdClient;
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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:45
 */
@Component(value="nettyClientSessionManager",lazy=false,side=Constants.SIDE_COMSUMER)
public class NettyClientSessionManager implements IClientSessionManager{

	static final Logger logger = LoggerFactory.getLogger(NettyClientSessionManager.class);

	private static final AttributeKey<IClientSession> sessionKey = 
			AttributeKey.newInstance(Constants.SESSION_KEY+System.currentTimeMillis());
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.newInstance(Constants.MONITOR_ENABLE_KEY);
	
	private final Map<String,IClientSession> sessions = new ConcurrentHashMap<>();
	
	@Cfg("/MinaClientSessionManager/openDebug")
	private boolean openDebug;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Cfg("/MinaClientSessionManager/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject("idClient")
	private IIdClient idGenerator;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject(required=true)
	private IMessageReceiver receiver;
	
	@Cfg(value="/NettyClientSessionManager/dumpDownStream",defGlobal=false)
	private boolean dumpDownStream  = false;
	
	@Cfg(value="/NettyClientSessionManager/dumpUpStream",defGlobal=false)
	private boolean dumpUpStream  = false;
	
	private Timer ticker = new Timer("ClientSessionHeardbeatWorker",true);
	
	public void init()  {
		this.receiver.registHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_HEARBEAT_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
			}
		});
		
		/*try {
			final Message hearbeat = new Message();
			hearbeat.setType(Constants.MSG_TYPE_HEARBEAT_REQ);
			hearbeat.setId(idGenerator.getLongId(Message.class));
			hearbeat.setReqId(0L);
			hearbeat.setVersion(Constants.MSG_VERSION);
			final ByteBuffer bb = ByteBuffer.wrap("Hello".getBytes(Constants.CHARSET));
			hearbeat.setPayload(bb);
		} catch (UnsupportedEncodingException e) {
			logger.error("",e);
		}*/
	}
    
    private Boolean monitorEnable(ChannelHandlerContext ctx) {
    	 Boolean v = ctx.channel().attr(this.monitorEnableKey).get();
		 return v == null ? JMicroContext.get().isMonitor():v;
    }

	@Override
	public IClientSession getOrConnect(String host, int port) {

		final String ssKey = host+port;
		if(sessions.containsKey(ssKey)){
			return sessions.get(ssKey);
		}
		
		final String sKey = ssKey.intern();
		synchronized(sKey) {
			
			if(sessions.containsKey(sKey)){
				return sessions.get(sKey);
			}			

	        // Configure the client.
	        EventLoopGroup group = new NioEventLoopGroup();
	        try {
	        	Bootstrap b = new Bootstrap();
	            b.group(group)
	             .channel(NioSocketChannel.class)
	             .option(ChannelOption.TCP_NODELAY, true)
	             .handler(new ChannelInboundHandlerAdapter() {
	                 @Override
	                 public void channelActive(ChannelHandlerContext ctx) {
	                 }

	                 @Override
					public boolean isSharable() {
						return true;
					}

					@Override
					public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
						super.handlerAdded(ctx);
	                   NettyClientSession s = new NettyClientSession(ctx,readBufferSize,heardbeatInterval,false);
	                   s.setReceiver(receiver);
	                   s.putParam(Constants.SESSION_KEY, ctx);
	      	           sessions.put(sKey, s);
					}

					@Override
	                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
						JMicroContext.setMonitor(monitor);
			        	JMicroContext.callSideProdiver(false);
	                	if(!(msg instanceof ByteBuf)) {
	                 		ctx.fireChannelRead(msg);
	                 		return;
		                }
	                	 
	                	ByteBuf bb = (ByteBuf)msg;
	                 	if(bb.readableBytes() <= 0) {
	                 		return;
	                 	}
	                 	
	                	NettyClientSession cs = (NettyClientSession)ctx.channel().attr(sessionKey).get();
	                	if(cs == null) {
	                		logger.error("Got NULL Session when read data {},data:{}",sKey,msg);
	                		return;
	                	}
	                	if(monitorEnable(ctx) && monitor != null){
	                		monitor.submit(MonitorConstant.CLIENT_IOSESSION_READ, cs.getId()+"",
	                				((ByteBuf)msg).readableBytes()+"");
	                    }
	                	
	             		ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
	                	bb.readBytes(b);
	                	b.flip();
	                	bb.release();
	                	
	                	//服务端接收信息是上行，客户端接收信息是下行
	             		cs.dump(b.array(),false);
	                	
	                 	cs.receive(b);
	                 }

	                 @Override
	                 public void channelReadComplete(ChannelHandlerContext ctx) {
	                    ctx.flush();
	                 }

	                 @Override
	                 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	                	 try {
							super.exceptionCaught(ctx, cause);
						} catch (Exception e) {
							logger.error("",e);
						}
	        			 logger.error("exceptionCaught",cause);
	        			 NettyClientSession session = (NettyClientSession)ctx.channel().attr(sessionKey).get();
	        			 if(session !=null && monitorEnable(ctx) && monitor != null ){
	        	             SF.doSubmit(MonitorConstant.CLIENT_IOSESSION_EXCEPTION
	        	            		 ,cause,session.getId()+"");
	        	         }
	        			 closeCtx(ctx);
	                 }

					@Override
					public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
						super.channelUnregistered(ctx);
						 closeCtx(ctx);
					}
	                 
					private void closeCtx(ChannelHandlerContext ctx) {
						logger.warn("Session Close for : {}",sKey);
						 NettyClientSession session = (NettyClientSession)ctx.channel().attr(sessionKey).get();
						 if(session != null) {
							 ctx.channel().attr(sessionKey).set(null);;
		        			 ctx.close();
		        			 session.close(true);
		        			 sessions.remove(sKey);
						 }
					}              
	             });

	            // Start the client.
	            b.connect(host, port).sync();

	            // Wait until the connection is closed.
	            //f.channel().closeFuture().sync();
	            
	            NettyClientSession s = (NettyClientSession)sessions.get(sKey);
	            ChannelHandlerContext ctx = (ChannelHandlerContext)s.getParam(Constants.SESSION_KEY);
	            
	            s.setId(idGenerator.getLongId(ISession.class.getName()));
   	            s.putParam(Constants.SESSION_KEY, ctx);
   	            s.setOpenDebug(openDebug);
   	           
   	            s.putParam(Constants.MONITOR_ENABLE_KEY, JMicroContext.get().isMonitor());
   	           
   	            ctx.channel().attr(sessionKey).set(s);
   	            ctx.channel().attr(monitorEnableKey).set(JMicroContext.get().isMonitor());;
	            
   	            s.setDumpDownStream(this.dumpDownStream);
     		    s.setDumpUpStream(this.dumpUpStream);
     		
	           //LOG.info("session connected : {}", session);
	           logger.debug("connection finish,host:"+host+",port:"+port);
	           return s;
	       } catch (Throwable e) {
	    	   String msg = "cannot connect host:" + host + ", port:" + port;
	    	   logger.error(msg);
	           SF.doSubmit(MonitorConstant.CLIENT_REQ_CONN_FAIL, e,msg);
	           throw new CommonException(msg);
	       }
		}
	}
	
}
