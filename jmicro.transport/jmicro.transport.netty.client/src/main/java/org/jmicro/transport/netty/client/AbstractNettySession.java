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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public abstract class AbstractNettySession extends AbstractSession implements IClientSession {

	private ChannelHandlerContext ctx;
	
	private boolean isWebSocket = false;
	
	
	public AbstractNettySession(ChannelHandlerContext ctx,int readBufferSize,int heardbeatInterval,boolean isWebSocket) {
		super(readBufferSize,heardbeatInterval);
		this.isWebSocket = isWebSocket;
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
		if(msg.getProtocol() == Message.PROTOCOL_JSON) {
			String json = JsonUtils.getIns().toJson(msg);
			if(this.isWebSocket) {
				ctx.channel().writeAndFlush(new TextWebSocketFrame(json));
			} else {		
				FullHttpResponse response;
				try {
					response = new DefaultFullHttpResponse(HTTP_1_1, OK,
							Unpooled.wrappedBuffer(json.getBytes(Constants.CHARSET)));
					response.headers().set(CONTENT_TYPE, "text/json");
					response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
					/*if (HttpHeaders.isKeepAlive(request)) {
						response.headers().set(CONNECTION, Values.KEEP_ALIVE);
					}*/
					ctx.writeAndFlush(response);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}else {
			//String json = JsonUtils.getIns().toJson(msg);
			ByteBuffer bb = msg.encode();
			ByteBuf bbf = Unpooled.buffer(bb.remaining());
			bbf.writeBytes(bb);
			ctx.channel().writeAndFlush(bbf);
		}
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
		ctx.close();
	}

}
