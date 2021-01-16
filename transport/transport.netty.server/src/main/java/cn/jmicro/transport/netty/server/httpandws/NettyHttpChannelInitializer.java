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
package cn.jmicro.transport.netty.server.httpandws;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:15:02
 */
@Component(lazy=false)
public class NettyHttpChannelInitializer extends ChannelInitializer<SocketChannel>{

	//@Inject
	//private NettyTextWebSocketHandler txtWsHandler;
	
	@Inject
	private NettyBinaryWebSocketHandler binWsHandler;
	
	@Inject
	private NettyHttpServerHandler httpHandler;
	
	//@Cfg(value="/textWebsocketContextPath",defGlobal=true)
	//private String textWebsocketContextPath = "/_txt_";
	
	@Cfg(value="/binaryWebsocketContextPath",defGlobal=true)
	private String binaryWebsocketContextPath = "/_bin_";
	
	@Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //HttpServerCodec: 针对http协议进行编解码
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        //ChunkedWriteHandler分块写处理，文件过大会将内存撑爆
        pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
         //将一个Http的消息组装成一个完成的HttpRequest或者HttpResponse
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(8192));
        
        pipeline.addLast("binWebSocketServerProtocolHandler", 
        		new WebSocketServerProtocolHandler(binaryWebsocketContextPath,null,false,65536));
        pipeline.addLast("binWebSocketWsHandler", binWsHandler);
        
      /*  pipeline.addLast("webSocketServerProtocolHandler", new WebSocketServerProtocolHandler(textWebsocketContextPath));
        pipeline.addLast("textWebSocketHandler", txtWsHandler);*/
       
        pipeline.addLast("jmicroHttpHandler", httpHandler);
        
        
    }
	
}
