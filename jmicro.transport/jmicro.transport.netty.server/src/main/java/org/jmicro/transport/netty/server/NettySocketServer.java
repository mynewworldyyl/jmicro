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

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IServer;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.PostFactoryAdapter;
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
 * 
 * @author Yulei Ye
 * @date 2018年11月4日 下午8:04:15
 */
@Component(value=Constants.TRANSPORT_NETTY,lazy=false,level=1,side=Constants.SIDE_PROVIDER)
@Server(transport=Constants.TRANSPORT_NETTY)
public class NettySocketServer extends PostFactoryAdapter implements IServer {

	static final Logger LOG = LoggerFactory.getLogger(NettySocketServer.class);
	
	private static final String TAG = NettySocketServer.class.getName();
	
	private  ServerBootstrap server;
	
	@Cfg(value = "/startSocket",required=false)
	private boolean enable = true;
	
	@Inject
	private NettySocketChannelInitializer initializer;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Cfg(value="/NettySocketServer/nettyPort",required=false,defGlobal=false)
	private int port=0;
	
	private ExecutorService workerGroupExecutor = null;
	
	private ExecutorService bossGroupExecutor = null;
	
	@Inject(required=false)
	private Set<IServerListener> serverListener = new HashSet<>();
	
	@Override
	public void init() {
		if(!this.enable) {
			LOG.info("NettySocketServer is disable");
			return;
		}
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(1000);
		config.setThreadNamePrefix("NIO-WorkerGroup");
		workerGroupExecutor = ExecutorFactory.createExecutor(config);
		
		ExecutorConfig config1 = new ExecutorConfig();
		config1.setMsCoreSize(2);
		config1.setMsMaxSize(60);
		config1.setTaskQueueSize(500);
		config1.setThreadNamePrefix("NIO-BossGroup");
		bossGroupExecutor = ExecutorFactory.createExecutor(config1);
		
		start();
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
        
        String m = "Running the netty socket server host["+Config.getHost()+"],port ["+this.port+"]";
        LOG.debug(m);    
        
        SF.serverStart(TAG,Config.getHost(),this.port, "Server start: " + Constants.TRANSPORT_NETTY );
        
	}

	@Override
	public void stop() {
		SF.serverStop(TAG,Config.getHost(),this.port);
		 if(server != null){
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

	@Override
	public void preInit(IObjectFactory of) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInit(IObjectFactory of) {
	}

	@Override
	public int runLevel() {
		// TODO Auto-generated method stub
		return 0;
	}


}
