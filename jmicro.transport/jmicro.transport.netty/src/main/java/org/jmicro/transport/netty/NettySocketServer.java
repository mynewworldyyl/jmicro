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
import java.util.List;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.server.IServer;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
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
 * 
 * @author Yulei Ye
 * @date 2018年11月4日 下午8:04:15
 */
@Component(value=Constants.TRANSPORT_NETTY,lazy=false,level=1,side=Constants.SIDE_PROVIDER)
@Server(transport=Constants.TRANSPORT_NETTY)
public class NettySocketServer  implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(NettySocketServer.class);
	
	private  ServerBootstrap server;
	
	@Inject
	private NettySocketChannelInitializer initializer;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Cfg(value = "/bindIp",required=false)
	private String host;
	
	@Cfg(value="/nettyPort",required=false)
	private int port=0;
	
	@Override
	public void init() {
		start();
	}
	
	@Override
	public void start() {
		if(Config.isClientOnly()) {
			return;
		}
        if(StringUtils.isEmpty(this.host)){
        	List<String> ips = Utils.getIns().getLocalIPList();
            if(ips.isEmpty()){
            	throw new CommonException("IP not found");
            }
            this.host = ips.get(0);
        }
        
        //InetAddress.getByAddress(Array(127, 0, 0, 1))
        InetSocketAddress address = new InetSocketAddress(this.host,this.port);
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
        
        String m = "Running the server host["+this.host+"],port ["+this.port+"]";
        LOG.debug(m);    
        MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_START, null,null,m);
	}

	@Override
	public void stop() {
		MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_STOP, null,null,this.host,this.port);
		 if(server != null){
			 //server.;
			 server = null;
        }
	}

	@Override
	public String host() {
		return this.host;
	}

	@Override
	public int port() {
		return this.port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}


}
