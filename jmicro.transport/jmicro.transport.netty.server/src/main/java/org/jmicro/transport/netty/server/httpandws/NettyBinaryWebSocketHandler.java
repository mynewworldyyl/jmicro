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
package org.jmicro.transport.netty.server.httpandws;

import java.nio.ByteBuffer;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.common.Constants;
import org.jmicro.transport.netty.server.NettyServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.AttributeKey;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:16:10
 */
@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettyBinaryWebSocketHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame>{
	
	static final Logger logger = LoggerFactory.getLogger(NettyServerSession.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_BIN_SESSION_KEY);
	
	@Cfg(value="/NettyBinaryWebSocketHandler/openDebug",required=false,defGlobal=false)
	private boolean openDebug=false;
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize = 1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Inject
	private StaticResourceHttpHandler staticResourceHandler;
	
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame binData) throws Exception {
    	
    	if(openDebug) {
    		logger.debug("channelRead Data: {}",binData.refCnt());
    	}
    	
    	ByteBuf bb = binData.content();
    	if(bb.readableBytes() <= 0) {
    		return;
    	}
    	
    	ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
    	bb.readBytes(b);
    	b.flip();
    	//bb.release();
    	
    	NettyServerSession session = ctx.channel().attr(sessionKey).get();
    	
    	session.receive(b);
    	
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval
    			,Constants.TYPE_WEBSOCKET);
    	
    	session.setReceiver(receiver);
    	session.setDumpDownStream(false);
    	session.setDumpUpStream(false);
    	session.init();
    	ctx.channel().attr(sessionKey).set(session);
    	
    	session.init();
    	ctx.channel().attr(sessionKey).set(session);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	ISession s = ctx.channel().attr(sessionKey).get();
    	ctx.channel().attr(sessionKey).set(null);
    	if( s != null) {
    		s.close(true);
    	}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("exceptionCaught",cause);
    	ISession s = ctx.channel().attr(sessionKey).get();
    	ctx.channel().attr(sessionKey).set(null);
    	if( s != null) {
    		s.close(true);
    	}
    }

}
