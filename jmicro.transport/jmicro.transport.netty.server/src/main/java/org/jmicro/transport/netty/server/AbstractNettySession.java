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
package org.jmicro.transport.netty.server;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.jmicro.api.client.IClientSession;
import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public abstract class AbstractNettySession extends AbstractSession implements IClientSession {

	private ChannelHandlerContext ctx;
	
	private int type = Constants.NETTY_SOCKET;
	
	public AbstractNettySession(ChannelHandlerContext ctx,int readBufferSize,int heardbeatInterval,int type) {
		super(readBufferSize,heardbeatInterval);
		this.type = type;
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
		byte[] data = null;
		if(msg.getProtocol() == Message.PROTOCOL_JSON) {
			String json = JsonUtils.getIns().toJson(msg);
			try {
				data = json.getBytes(Constants.CHARSET);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		} else /*if(msg.getProtocol() == Message.PROTOCOL_BIN) */{
			ByteBuffer bb = msg.encode();
			data = bb.array();
		}
		
		if(this.type == Constants.NETTY_HTTP) {
			FullHttpResponse response;
			response = new DefaultFullHttpResponse(HTTP_1_1, OK,
					Unpooled.wrappedBuffer(data));
			response.headers().set(CONTENT_TYPE, "text/json");
			response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
			/* 
			if(HttpHeaders.isKeepAlive(request)) {
				response.headers().set(CONNECTION, Values.KEEP_ALIVE);
			}
			*/
			ctx.writeAndFlush(response);
		}else if(this.type == Constants.NETTY_WEBSOCKET) {
			ByteBuf bbf = Unpooled.buffer(data.length);
			bbf.writeBytes(data);
			ctx.channel().writeAndFlush(new BinaryWebSocketFrame(bbf));
		}else/* if(this.type == Constants.NETTY_SOCKET) */{
			ByteBuf bbf = Unpooled.buffer(data.length);
			bbf.writeBytes(data);
			ctx.channel().writeAndFlush(bbf);
		}
		
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
		ctx.close();
	}

}
