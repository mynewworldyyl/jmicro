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
package org.jmicro.gateway;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.client.ClientMessageReceiver;
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

public class ApiGatewaySessionManager implements IClientSessionManager {

	static final Logger logger = LoggerFactory.getLogger(ApiGatewaySessionManager.class);

	private static final AttributeKey<IClientSession> sessionKey = 
			AttributeKey.newInstance(Constants.SESSION_KEY+System.currentTimeMillis());
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.newInstance(Constants.MONITOR_ENABLE_KEY);
	
	private final Map<String,IClientSession> sessions = new ConcurrentHashMap<>();
	
	private int readBufferSize=1024*4;
	
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	private ClientMessageReceiver receiver = new ClientMessageReceiver();
	
	private Timer ticker = new Timer("ApiClientSessionHeardbeatWorker",true);
	
	private int clientType = ApiGatewayClient.TYPE_SOCKET;
	
	public void init()  {
		this.registerMessageHandler(new IMessageHandler(){
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
			//hearbeat.setId(idGenerator.getLongId(Message.class));
			hearbeat.setReqId(0L);
			hearbeat.setVersion(Constants.DEFAULT_VERSION);
			final ByteBuffer bb = ByteBuffer.wrap("Hello".getBytes(Constants.CHARSET));
			hearbeat.setPayload(bb);
		} catch (UnsupportedEncodingException e) {
			logger.error("",e);
		}
		
	}
	
	public void registerMessageHandler(IMessageHandler handler) {
		receiver.registHandler(handler);
	}

	@Override
	public IClientSession getOrConnect(String host, int port) {

		final String ssKey = host + port;
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
						ApiGatewaySession s = new ApiGatewaySession(ctx,readBufferSize,heardbeatInterval,clientType);
	                   s.putParam(Constants.SESSION_KEY, ctx);
	      	           sessions.put(sKey, s);
					}

					@Override
	                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
	                	 if(!(msg instanceof ByteBuf)) {
		                 		ctx.fireChannelRead(msg);
		                 		return;
		                 	}
	                	 
	                	 ApiGatewaySession cs = (ApiGatewaySession)ctx.channel().attr(sessionKey).get();
	                 	
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
	                     logger.debug("Got message reqId: "+ message.getReqId());
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
	        			 ApiGatewaySession session = (ApiGatewaySession)ctx.channel().attr(sessionKey);
	                 }
	             });

	            // Start the client.
	            b.connect(host, port).sync();

	            ApiGatewaySession s = (ApiGatewaySession)sessions.get(sKey);
	            ChannelHandlerContext ctx = (ChannelHandlerContext)s.getParam(Constants.SESSION_KEY);
	            
   	            s.putParam(Constants.SESSION_KEY, ctx);
   	            ctx.channel().attr(sessionKey).set(s);
	            
	           //LOG.info("session connected : {}", session);
	           logger.debug("connection finish,host:"+host+",port:"+port);
	           return s;
	       } catch (Throwable e) {
	    	   logger.error("cannot connect host:" + host + ", port:" + port, e);
	           throw new CommonException("host:" + host + ", port:" + port,e);
	       }
		}
	}

	public int getClientType() {
		return clientType;
	}

	public void setClientType(int clientType) {
		this.clientType = clientType;
	}

}
