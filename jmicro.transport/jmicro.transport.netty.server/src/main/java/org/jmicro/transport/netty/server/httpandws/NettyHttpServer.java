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

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IServer;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.jmicro.server.IServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
{"protocol":2,"msgId":11977,"reqId":7832,"sessionId":162,"len":0,"version":"0.0.1","type":1,"flag":0,
 "payload":"{\"serviceName\":\"org.jmicro.example.api.ITestRpcService\",\"method\":\"getPerson\",\"args\":[{\"username\":\"Client person Name\",\"id\":1234}],\"namespace\":\"defaultNamespace\",
 \"version\":\"0.0.0\",\"impl\":\"org.jmicro.example.provider.TestRpcServiceImpl\",\"reqId\":7832,\"isMonitorEnable\":true,\"params\":{}}"}

 * @author Yulei Ye
 * @date 2018年10月21日-下午9:15:25
 */
@SuppressWarnings("restriction")
@Component(value=Constants.TRANSPORT_NETTY_HTTP,lazy=false,level=1,side=Constants.SIDE_PROVIDER)
@Server(transport=Constants.TRANSPORT_NETTY_HTTP)
public class NettyHttpServer implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(NettyHttpServer.class);
	
	private  ServerBootstrap server;
	
	@Inject
	private NettyHttpChannelInitializer initializer;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Cfg(value="/NettyHttpServer/nettyPort",required=false,defGlobal=false)
	private int port=9090;
	
	@Inject(required=false)
	private Set<IServerListener> serverListener = new HashSet<>();
	
	@Override
	public void init() {
		boolean flag = Config.getCommandParam(Constants.START_HTTP, Boolean.class, false);
		if(flag) {
			start();
		}
	}
	
	@Override
	public void start() {
		if(Config.isClientOnly()) {
			return;
		}
        if(StringUtils.isEmpty(Config.getHost())){
        	throw new CommonException("IP not found");
        }
        
        //InetAddress.getByAddress(Array(127, 0, 0, 1))
        InetSocketAddress address = new InetSocketAddress(Config.getHost(),this.port);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
        	server = new ServerBootstrap();
        	server.option(ChannelOption.SO_KEEPALIVE, true)
        	.option(ChannelOption.SO_BACKLOG, 128) 
            .childOption(ChannelOption.SO_KEEPALIVE, true)
        	.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(initializer);
             
             ChannelFuture channelFuture = server.bind(address).sync();
             //channelFuture.channel().closeFuture().sync();
             address = (InetSocketAddress)channelFuture.channel().localAddress();
		} catch (InterruptedException e) {
			LOG.error("",e);
		}finally{
            //bossGroup.shutdownGracefully();
            //workerGroup.shutdownGracefully();
        }
        this.port = address.getPort();
        
        for(IServerListener l : serverListener) {
        	l.serverStared(host(), port, Constants.TRANSPORT_NETTY);
        }
        
        String m = "Running netty http server host["+Config.getHost()+"],port ["+this.port+"]";
        LOG.debug(m);    
        SF.doSubmit(MonitorConstant.SERVER_START,m);
	}

	@Override
	public void stop() {
		SF.doSubmit(MonitorConstant.SERVER_STOP,Config.getHost(),this.port+"");
		 if(server != null){
			 //server.;
			 server = null;
        }
	}

	@Override
	public String host() {
		return Config.getHost();
	}

	@Override
	public int port() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
