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

import java.io.UnsupportedEncodingException;
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
import org.jmicro.api.codec.Decoder;
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
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject(required=true)
	private IMessageReceiver receiver;
	
	private Timer ticker = new Timer("ClientSessionHeardbeatWorker",true);
	
	public void init()  {
		this.receiver.registHandler(new IMessageHandler(){
			@Override
			public Short type() {
				return Constants.MSG_TYPE_HEARBEAT_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
			}
		});
		
		try {
			final Message hearbeat = new Message();
			hearbeat.setType(Constants.MSG_TYPE_HEARBEAT_REQ);
			hearbeat.setId(idGenerator.getLongId(Message.class));
			hearbeat.setReqId(0L);
			hearbeat.setVersion(Constants.DEFAULT_VERSION);
			final ByteBuffer bb = ByteBuffer.wrap("Hello".getBytes(Constants.CHARSET));
			hearbeat.setPayload(bb);
		} catch (UnsupportedEncodingException e) {
			logger.error("",e);
		}
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
	                   s.putParam(Constants.SESSION_KEY, ctx);
	      	           sessions.put(sKey, s);
					}

					@Override
	                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
						JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
	                	 if(!(msg instanceof ByteBuf)) {
		                 		ctx.fireChannelRead(msg);
		                 		return;
		                 	}
	                	 
	                	 NettyClientSession cs = (NettyClientSession)ctx.channel().attr(sessionKey).get();
	                	 if(monitorEnable(ctx) && monitor != null){
	                     	monitor.submit(MonitorConstant.CLIENT_IOSESSION_READ, cs.getId()+"",cs.getReadBuffer().remaining()+""
	                     			,((ByteBuf)msg).readableBytes()+"");
	                     }
	                 	
	                 	ByteBuf bb = (ByteBuf)msg;
	                 	if(bb.readableBytes() <= 0) {
	                 		return;
	                 	}
	                 	
	                 	ByteBuffer buffer = cs.getReadBuffer();
	                 	
	                 	ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
	                	bb.readBytes(b);
	                	b.flip();
	                	
	                	buffer.put(b);
	                	
	                 	ByteBuffer body = Decoder.readMessage(buffer);
	                     if(body == null){
	                     	return;
	                     }
	                     
	                     Message message = new Message();
	                     message.decode(body);
	                     if(openDebug) {
	                     	logger.debug("Got message reqId: "+ message.getReqId());
	                     }
	                     receiver.receive(cs,message);
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
	        			 NettyClientSession session = (NettyClientSession)ctx.channel().attr(sessionKey);
	        			 if(session !=null && monitorEnable(ctx) && monitor != null ){
	        	             SF.doSubmit(MonitorConstant.CLIENT_IOSESSION_EXCEPTION
	        	            		 ,cause,session.getId()+"");
	        	         }
	                 }
	             });

	            // Start the client.
	            b.connect(host, port).sync();

	            // Wait until the connection is closed.
	            //f.channel().closeFuture().sync();
	            
	            NettyClientSession s = (NettyClientSession)sessions.get(sKey);
	            ChannelHandlerContext ctx = (ChannelHandlerContext)s.getParam(Constants.SESSION_KEY);
	            
	            s.setId(idGenerator.getLongId(ISession.class));
   	            s.putParam(Constants.SESSION_KEY, ctx);
   	            s.setOpenDebug(openDebug);
   	           
   	            s.putParam(Constants.MONITOR_ENABLE_KEY, JMicroContext.get().isMonitor());
   	           
   	            ctx.channel().attr(sessionKey).set(s);
   	            ctx.channel().attr(monitorEnableKey).set(JMicroContext.get().isMonitor());;
	            
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
