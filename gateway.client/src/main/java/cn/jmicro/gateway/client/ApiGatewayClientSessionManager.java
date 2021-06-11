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
package cn.jmicro.gateway.client;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.gateway.client.http.ApiGatewayClientHttpSession;
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
 * @date 2018年11月18日 下午7:45:35
 */
public class ApiGatewayClientSessionManager implements IClientSessionManager {

	//static final Logger logger = LoggerFactory.getLogger(ApiGatewayClientSessionManager.class);

	private static final AttributeKey<IClientSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY + System.currentTimeMillis());
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.newInstance("_is_monitorenable");
	
	private final Map<String,IClientSession> sessions = new ConcurrentHashMap<>();
	
	private int readBufferSize=1024*4;
	
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	private ClientMessageReceiver receiver = new ClientMessageReceiver(true);
	
	private Timer ticker = new Timer("ApiClientSessionHeardbeatWorker",true);
	
	//private int clientType = Constants.TYPE_SOCKET;
	//private int clientType = Constants.TYPE_WEBSOCKET;
	private int clientType = Constants.TYPE_HTTP;
	
	public void init()  {
		this.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
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
			hearbeat.setMsgId(0L);
			//hearbeat.setVersion(Message.MSG_VERSION);
			final ByteBuffer bb = ByteBuffer.wrap("Hello".getBytes(Constants.CHARSET));
			hearbeat.setPayload(bb);
		} catch (UnsupportedEncodingException e) {
			//logger.error("",e);
			e.printStackTrace();
		}		
	}
	
	public void registerMessageHandler(IMessageHandler handler) {
		receiver.registHandler(handler);
	}

	@Override
	public IClientSession getOrConnect(String instanceName,String host, String port) {

		final String ssKey = host +":"+ port;
		if(sessions.containsKey(ssKey)){
			return sessions.get(ssKey);
		}
		
		final String sKey = ssKey.intern();
		synchronized(sKey) {
			if(sessions.containsKey(sKey)){
				return sessions.get(sKey);
			}			
			if(this.getClientType() == Constants.TYPE_SOCKET) {
				createSocketSession(sKey,host,port);
			}else if(this.getClientType() == Constants.TYPE_HTTP) {
				createHttpSession(sKey,host,port);
			}else if(this.getClientType() == Constants.TYPE_WEBSOCKET) {
				createWebSocketSession(sKey,host,port);
			}else {
				throw new CommonException("Connection type ["+this.getClientType()+"] not support");
			}
			return sessions.get(sKey);
		}
	}
	
	@Override
	public void closeSession(ISession session) {
		String skey = (String)session.getParam(SKEY);
		sessions.remove(skey);
		if(!session.isClose()) {
			session.close(true);
		}
	}

	private void createWebSocketSession(String sKey, String host, String port) {		
		
	}

	private void createHttpSession(String sKey, String host, String port) {
		String url = "http://" + host + ":" + port;
		ApiGatewayClientHttpSession s = new ApiGatewayClientHttpSession(receiver,url,readBufferSize,heardbeatInterval);
        //s.putParam(Constants.SESSION_KEY, null);
        sessions.put(sKey, s);
	}

	private IClientSession createSocketSession(final String sKey,String host, String port) {
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
					ApiGatewayClientSocketSession s = new ApiGatewayClientSocketSession(ctx,readBufferSize,heardbeatInterval);
                    s.setReceiver(receiver);
					s.putParam(Constants.IO_SESSION_KEY, ctx);
					//s.init();
      	           sessions.put(sKey, s);
				}

				@Override
                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
                	if(!(msg instanceof ByteBuf)) {
                 		ctx.fireChannelRead(msg);
                 		return;
                 	}
                	 
                	ApiGatewayClientSocketSession cs = (ApiGatewayClientSocketSession)ctx.channel().attr(sessionKey).get();
                 	cs.putParam(SKEY, sKey);
                 	
                 	ByteBuf bb = (ByteBuf)msg;
                 	if(bb.readableBytes() <= 0) {
                 		return;
                 	}
                 	
                 	ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
                	bb.readBytes(b);
                	b.flip();
                	bb.release();
                	
                	cs.receive(b);
                 }

                 @Override
                 public void channelReadComplete(ChannelHandlerContext ctx) {
                    ctx.flush();
                    //System.out.println("Read complete!");
                 }

                 @Override
                 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                	 try {
						super.exceptionCaught(ctx, cause);
					} catch (Exception e) {
						//logger.error("",e);
						e.printStackTrace();
					}
        			 //logger.error("exceptionCaught",cause);
                	 cause.printStackTrace();
        			 //ApiGatewaySession session = (ApiGatewaySession)ctx.channel().attr(sessionKey);
                 }
             });

            // Start the client.
            b.connect(host, Integer.parseInt(port)).sync();

            ApiGatewayClientSocketSession s = (ApiGatewayClientSocketSession)sessions.get(sKey);
            ChannelHandlerContext ctx = (ChannelHandlerContext)s.getParam(Constants.IO_SESSION_KEY);
            
            s.putParam(Constants.IO_SESSION_KEY, ctx);
            ctx.channel().attr(sessionKey).set(s);
            
           //LOG.info("session connected : {}", session);
           //logger.debug("connection finish,host:"+host+",port:"+port);
           System.out.println("connection finish,host:"+host+",port:"+port);
           return s;
       } catch (Throwable e) {
    	   //logger.error("cannot connect host:" + host + ", port:" + port, e);
           throw new CommonException("host:" + host + ", port:" + port,e);
       }
	}
	
	public int getClientType() {
		return clientType;
	}

	public void setClientType(int clientType) {
		this.clientType = clientType;
	}

}
