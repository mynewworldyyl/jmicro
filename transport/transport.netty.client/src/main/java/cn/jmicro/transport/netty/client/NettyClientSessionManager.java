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
package cn.jmicro.transport.netty.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
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

	private static final String TAG = NettyClientSessionManager.class.getName();
	
	private static final AttributeKey<IClientSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY+TimeUtils.getCurTime());
	
	AttributeKey<Boolean> monitorEnableKey = AttributeKey.newInstance(JMicroContext.IS_MONITORENABLE);
	
	private final Map<String,IClientSession> sessions = new ConcurrentHashMap<>();
	
	private final Map<String,IClientSession> tempSessions = new ConcurrentHashMap<>();
	
	@Cfg(value="/NettyClientSessionManager/openDebug",defGlobal=false,changeListener="openDebugChange")
	private boolean openDebug=false;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Cfg("/MinaClientSessionManager/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=true)
	private IMessageReceiver receiver;
	
	@Cfg(value="/NettyClientSessionManager/dumpDownStream",defGlobal=false)
	private boolean dumpDownStream  = false;
	
	@Cfg(value="/NettyClientSessionManager/dumpUpStream",defGlobal=false)
	private boolean dumpUpStream  = false;
	
	@Cfg(value="/NettyClientSessionManager/waitBeforeCloseSession",defGlobal=false)
	private Long waitBeforeCloseSession  = 1000*3L;
	
	//private Timer ticker = new Timer("ClientSessionHeardbeatWorker",true);
	
	public void openDebugChange() {
		for(IClientSession s : sessions.values()) {
			((NettyClientSession)s).setOpenDebug(this.openDebug);
		}
	}
	
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
		 return v == null ? JMicroContext.get().isMonitorable():v;
    }
    
    @Override
	public void closeSession(ISession session) {
		String skey = (String)session.getParam(SKEY);
		if(skey == null) {
			logger.error("Session KEY is NULL when close session");
			return;
		}
		//阻止新的请求使用此会话
		sessions.remove(skey);
		if(!session.isClose() && session.waitingClose()) {
			if(waitBeforeCloseSession > 0) {
				//在真正关闭会话前待指时间，使已经发送的请求得到正常响应
				TimerTicker.getDefault(this.waitBeforeCloseSession).addListener(CLOSE_SESSION_TIMER + session.getId(),
						null,(key,att)->{
							session.close(true);
							TimerTicker.getDefault(this.waitBeforeCloseSession).removeListener(key, true);
				});
			} else {
				session.close(true);
			}
		}
		
	}

	@Override
	public IClientSession getOrConnect(String targetInstanceName,String host, int port) {

		final String ssKey = host+":"+port;
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
	                    s.putParam(Constants.IO_SESSION_KEY, ctx);
	                    s.putParam(SKEY, sKey);
	                   
	   	                s.setId(idGenerator.getLongId(ISession.class));
	      	            s.putParam(Constants.IO_SESSION_KEY, ctx);
	      	            s.setOpenDebug(openDebug);
	      	           
	      	            s.putParam(JMicroContext.IS_MONITORENABLE, JMicroContext.get().isMonitorable());
	      	           
	      	            ctx.channel().attr(sessionKey).set(s);
	      	            ctx.channel().attr(monitorEnableKey).set(JMicroContext.get().isMonitorable());;
	   	            
	      	            s.setDumpDownStream(dumpDownStream);
	        		    s.setDumpUpStream(dumpUpStream);
	                   
	                    tempSessions.put(sKey, s);
					}

					@Override
	                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
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

                		//SF.netIoRead(TAG,MonitorConstant.CLIENT_IOSESSION_READ,((ByteBuf)msg).readableBytes());
	                	
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
	                	 if(openDebug) {
	                		 logger.debug("channelReadComplete: {}",ctx);
	                	 }
	                    ctx.flush();
	                 }

	                 @Override
	                 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	                	 try {
							super.exceptionCaught(ctx, cause);
						} catch (Exception e) {
							if(e instanceof IOException) {
								logger.error("",e.getMessage());
							}else {
								logger.error("exceptionCaught",cause);
							}
						}
	        			 
	                	 /*  NettyClientSession session = (NettyClientSession)ctx.channel().attr(sessionKey).get();
	        			if(session !=null && monitorEnable(ctx) ){
	        	             LG.netIo(MC.MT_CLIENT_IOSESSION_EXCEPTION,MC.LOG_ERROR,
	        	            		 NettyClientSessionManager.class
	        	            		 ,"exceptionCaught sessionId:"+session.getId()+"",cause);
	        	         }*/
	        			 closeCtx(ctx);
	                 }

					@Override
					public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
						super.channelUnregistered(ctx);
						if(openDebug) {
	                		 logger.debug("channelUnregistered: {}",ctx);
	                	 }
						 closeCtx(ctx);
					}
	                 
					@Override
					public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
						super.channelWritabilityChanged(ctx);
					}

					private void closeCtx(ChannelHandlerContext ctx) {
						logger.warn("Session Close for : {} ",sKey);
						 NettyClientSession session = (NettyClientSession)ctx.channel().attr(sessionKey).get();
						 if(session != null && !session.isClose()) {
							 ctx.channel().attr(sessionKey).set(null);;
		        			 ctx.close();
		        			 closeSession(session);
						 }
					}              
	             });

	            // Start the client.
	            ChannelFuture cf = b.connect(host, port);
	            		
	            cf.sync();

	            // Wait until the connection is closed.
	            //f.channel().closeFuture().sync();
	            
	            NettyClientSession s = (NettyClientSession)tempSessions.get(sKey);
	            tempSessions.remove(sKey);
	            
	            sessions.put(sKey, s);
	            s.init();
	            
	           //LOG.info("session connected : {}", session);
	           logger.debug("Connection finish,host:" + host + ", port:" + s.getRemoteAddress().getPort()+", instanceName: "+ targetInstanceName);
	           return s;
	       } catch (Throwable e) {
	    	   String msg = "Cannot connect " + host + ":" + port;
	    	   logger.error(msg,e);
	    	   /*LG.netIo(MC.MT_CLIENT_CONNECT_FAIL,MC.LOG_ERROR,
	            		 NettyClientSessionManager.class,msg,e);*/
	           throw new CommonException(msg);
	       }
		}
	}
	
}
