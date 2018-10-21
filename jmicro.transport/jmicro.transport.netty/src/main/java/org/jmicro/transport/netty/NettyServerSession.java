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
package org.jmicro.transport.netty;

import java.net.InetSocketAddress;

import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.Message;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.server.IServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:15:12
 */
public class NettyServerSession extends AbstractSession implements IServerSession{

	static final Logger LOG = LoggerFactory.getLogger(NettyServerSession.class);
	
	private ChannelHandlerContext ctx;
	
	public NettyServerSession(ChannelHandlerContext ctx,int readBufferSize,int hearbeatInterval) {
		super(readBufferSize,hearbeatInterval);
		this.ctx = ctx;
	}

	public InetSocketAddress getLocalAddress(){
		return (InetSocketAddress)ctx.channel().localAddress();
	}
	
	public InetSocketAddress getRemoteAddress(){
		return (InetSocketAddress)ctx.channel().remoteAddress();
	}
	
	@Override
	public void write(Message msg) {
		String json = JsonUtils.getIns().toJson(msg);
		ctx.channel().writeAndFlush(new TextWebSocketFrame(json));
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
		ctx.close();
	}
}
