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

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.transport.netty.server.NettyServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:16:10
 */
@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettyTextWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>{
	
	static final Logger logger = LoggerFactory.getLogger(NettyServerSession.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY);
	
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
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame text) throws Exception {
    	NettyServerSession session = ctx.channel().attr(sessionKey).get();
    	Message msg = JsonUtils.getIns().fromJson(text.text(), Message.class);
    	JMicroContext.configProvider(session,msg);
		receiver.receive(session,msg);
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval
    			,Constants.TYPE_WEBSOCKET);
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
