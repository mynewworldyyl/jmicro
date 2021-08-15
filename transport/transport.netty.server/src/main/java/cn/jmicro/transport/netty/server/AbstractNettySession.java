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
package cn.jmicro.transport.netty.server;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.net.AbstractSession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.server.IServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public abstract class AbstractNettySession extends AbstractSession implements IServerSession {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNettySession.class);
	
	private ChannelHandlerContext ctx;
	
	private int type = Constants.TYPE_SOCKET;
	
	private InetSocketAddress localAddre;
	private InetSocketAddress remoteAddre;
	
	public AbstractNettySession(ChannelHandlerContext ctx,int readBufferSize,int heardbeatInterval,int type) {
		super(readBufferSize,heardbeatInterval,type);
		this.type = type;
		this.ctx = ctx;
		localAddre = (InetSocketAddress)ctx.channel().localAddress();
		remoteAddre = (InetSocketAddress)ctx.channel().remoteAddress();
	}
	
	public InetSocketAddress getLocalAddress(){
		return localAddre;
	}
	
	public InetSocketAddress getRemoteAddress(){
		return remoteAddre;
	}
	
	@Override
	public void write(Message msg) {
		
		//客户端Debug模式下上行时记录的时间
		//long oldTime = msg.getTime();
		//记录下行时间
		msg.setTime(TimeUtils.getCurTime());
		
		ByteBuffer bb = msg.encode();
		bb.mark();
		ByteBuf bbf = Unpooled.copiedBuffer(bb);
		this.writeSum.addAndGet(bb.limit());
		
		if(this.type == Constants.TYPE_HTTP) {
			FullHttpResponse response =  new DefaultFullHttpResponse(HTTP_1_1, OK,bbf);
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.BINARY);
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
			cors(response.headers());
			/* 
			if(HttpHeaders.isKeepAlive(request)) {
				response.headers().set(CONNECTION, Values.KEEP_ALIVE);
			}
			*/
			ctx.writeAndFlush(response);
		}else if(this.type == Constants.TYPE_WEBSOCKET) {
			ctx.channel().writeAndFlush(new BinaryWebSocketFrame(bbf));
			//ctx.channel().writeAndFlush(new BinaryWebSocketFrame(JsonUtils.getIns().toJson(msg)));
			
		}else/* if(this.type == Constants.NETTY_SOCKET) */{
			ctx.channel().writeAndFlush(bbf);
			if(msg.isDebugMode()) {
				long cost = TimeUtils.getCurTime() - msg.getStartTime();
				ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
				
				if(sm != null) {
					if(sm.getTimeout() <= cost) {
						logger.warn("Client ins[{}],reqId[{}],cost[{}],Method[{}],TO[{}]",
								msg.getInsId(),msg.getMsgId(),cost,msg.getMethod(),sm.getTimeout());
					}/*else {
						logger.debug("Client ins[{}],reqId[{}],cost[{}],method[{}]",msg.getInstanceName(),msg.getReqId(),
								cost,sm.getKey().getMethod());
					}*/
				} else {
					logger.warn("Null ServiceMethod ins[{}],reqId[{}],cost[{}],method[{}]",
							msg.getInsId(),msg.getMsgId(),cost,msg.getMethod());
				}
			}
		}
		this.active();
		//服务方写信息，是下行
		bb.reset();
		this.dump(bb,false,msg);
		if(JMicroContext.get().getDebugLog() != null) {
			JMicroContext.get().appendCurUseTime(",Encode time:", true);
		}
	}
	
	public static void cors(HttpHeaders h) {
		 h.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		 h.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST,GET");
		 h.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with,content-type");
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
		ctx.close();
	}

	@Override
	public boolean isServer() {
		return true;
	}

}
