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
package org.jmicro.gateway.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.jmicro.api.client.IClientSession;
import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.Message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:37
 *
 */
public class ApiGatewayClientSocketSession extends AbstractSession implements IClientSession {

	private ChannelHandlerContext ctx;
	
	public ApiGatewayClientSocketSession(ChannelHandlerContext ctx,int readBufferSize,int heardbeatInterval) {
		super(readBufferSize,heardbeatInterval);
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
		//String json = JsonUtils.getIns().toJson(msg);
		ByteBuffer bb = msg.encode();
		ByteBuf bbf = Unpooled.buffer(bb.remaining());
		bbf.writeBytes(bb);
		ctx.channel().writeAndFlush(bbf);
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
		ctx.close();
	}



}
