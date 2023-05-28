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
package cn.jmicro.transport.netty.server.udp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Server;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.server.IServerListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月4日 下午8:04:15
 */
@Component(value=Constants.TRANSPORT_NETTY_UDP,lazy=false,level=1,side=Constants.SIDE_PROVIDER)
@Server(transport=Constants.TRANSPORT_NETTY_UDP)
public class NettyUDPSocketServer implements IServer {

	static final Logger logger = LoggerFactory.getLogger(NettyUDPSocketServer.class);
	
	private static final String TAG = NettyUDPSocketServer.class.getName();
	
	private  Bootstrap server;
	
	@Cfg(value = "/startUdpSocket",required=false)
	private boolean enable = false;
	
	@Inject
	private IObjectFactory of;
	
	/*@Inject
	private NettySocketChannelInitializer initializer;*/
	
	@Inject
	private NettyUDPSocketHandler socketHandler;
	
	@Cfg(value="/NettySocketServer/nettyPort",required=false,defGlobal=false)
	private String port=null;
	
	@Inject
	private ProcessInfoJRso pi;
	
	private ExecutorService workerGroupExecutor = null;
	
	private ExecutorService bossGroupExecutor = null;
	
	@Inject(required=false)
	private Set<IServerListener> serverListener = new HashSet<>();
	
	//@Override
	public void jready() {
		if(Config.isClientOnly() || !this.enable) {
			logger.info("NettyUDPSocketServer is disable");
			return;
		}
		
		init0();
	}
	
	private void init0() {
		/*ExecutorConfigJRso config = new ExecutorConfigJRso();
		config.setMsCoreSize(2);
		config.setMsMaxSize(20);
		config.setTaskQueueSize(2);
		config.setThreadNamePrefix("NIO-Udp-WorkerGroup");
		workerGroupExecutor = of.get(ExecutorFactory.class).createExecutor(config);
		
		ExecutorConfigJRso config1 = new ExecutorConfigJRso();
		config1.setMsCoreSize(1);
		config1.setMsMaxSize(10);
		config1.setTaskQueueSize(2);
		config1.setThreadNamePrefix("NIO-Udp-BossGroup");
		bossGroupExecutor = of.get(ExecutorFactory.class).createExecutor(config1);*/
		
		start();
	}
	
	@Override
	public void start() {
		if(Config.isClientOnly()) {
			return;
		}
        if(StringUtils.isEmpty(Config.getExportSocketHost())){
        	throw new CommonException("IP not found");
        }
        
        int p = Utils.isEmpty(this.port) ? 0 : Integer.parseInt(this.port);
        
        //InetAddress.getByAddress(Array(127, 0, 0, 1))
        InetSocketAddress address = new InetSocketAddress(Config.getExportSocketHost(),p);
        EventLoopGroup bossGroup = new NioEventLoopGroup(/*0,bossGroupExecutor*/);
       // EventLoopGroup workerGroup = new NioEventLoopGroup(/*0,workerGroupExecutor*/);
        
        try {
        	
            //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
            //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
        	server = new Bootstrap();
            
            server.group(bossGroup)
            .option(ChannelOption.SO_BACKLOG, 128) 
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_BROADCAST, false)
            .handler(socketHandler);
            
            ChannelFuture channelFuture = server.bind(address).sync();
            
            address = (InetSocketAddress)channelFuture.channel().localAddress();
            
            port = address.getPort()+"";
            pi.setUdpPort(port);
            
            String m = "Running the netty socket server host["+Config.getExportSocketHost()+"],port ["+this.port+"]";
            logger.info(m);
            
            synchronized(server) {
           	 //让监听端口准备就绪时间，确保服务注册前监听端口处于可用状态
           	 server.wait(2000);
            }
            
            for(IServerListener l : serverListener) {
            	l.serverStared(host(), port, Constants.TRANSPORT_NETTY);
            }
        	
        	/*server = new ServerBootstrap();
        	server.option(ChannelOption.SO_KEEPALIVE, true)
        	.option(ChannelOption.SO_BACKLOG, 128) 
            .childOption(ChannelOption.SO_KEEPALIVE, true)
        	.group(bossGroup, workerGroup)
        	.channel(NioDatagramChannel.class)
            //.handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(initializer);
             
             ChannelFuture channelFuture = server.bind(address).sync();
             //channelFuture.channel().closeFuture().sync();
             address = (InetSocketAddress)channelFuture.channel().localAddress();
             
             port = address.getPort()+"";
             pi.setPort(port);
             
             String m = "Running the netty socket server host["+Config.getExportSocketHost()+"],port ["+this.port+"]";
             logger.info(m);
             
             synchronized(server) {
            	 //让监听端口准备就绪时间，确保服务注册前监听端口处于可用状态
            	 server.wait(2000);
             }
             
             for(IServerListener l : serverListener) {
             	l.serverStared(host(), port, Constants.TRANSPORT_NETTY);
             }*/
             
		} catch (InterruptedException e) {
			logger.error("启动Socket监听错误",e);
			System.exit(1);
		}finally{
            //bossGroup.shutdownGracefully();
            //workerGroup.shutdownGracefully();
        }
        
        //SF.serverStart(TAG, "Server start: " + Constants.TRANSPORT_NETTY+" : "+Config.getHost()+" : "+this.port );
        
	}

	@Override
	public void stop() {
		//SF.serverStop(TAG,Config.getHost(),this.port);
		 if(server != null){
			 server = null;
        }
	}

	@Override
	public String host() {
		return Config.getExportSocketHost();
	}

	@Override
	public String port() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isEnable() {
		return enable;
	}
	
	
}
