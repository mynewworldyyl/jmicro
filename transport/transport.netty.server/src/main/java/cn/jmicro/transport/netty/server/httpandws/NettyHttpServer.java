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

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Server;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.server.IServerListener;
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
	private static final String TAG = NettyHttpServer.class.getName();
	
	private  ServerBootstrap server;
	
	@Inject
	private ProcessInfo pi;
	
	@Inject
	private NettyHttpChannelInitializer initializer;
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private String port="9090";
	
	//@Cfg(value = "/"+Constants.ExportHttpIP,required=false,defGlobal=false)
	private String listenHttpIP = null;
	
	@Inject(required=false)
	private Set<IServerListener> serverListener = new HashSet<>();
	
	@Cfg(value="/startHttp",required=false,defGlobal=false)
	private boolean startHttp = false;
	
	@Inject
	private IObjectFactory of;
	
	public void ready() {
		if(Config.isClientOnly() || !this.startHttp) {
			LOG.info("NettyHttpServer is disable");
			return;
		}

		/*this.of.masterSlaveListen((type,isMaster)->{
			if(isMaster && (IMasterChangeListener.MASTER_ONLINE == type || IMasterChangeListener.MASTER_NOTSUPPORT == type)) {
				//主从模式
				start();
			}
		});*/
	
		start();
	}
	
	@Override
	public void start() {
		if(Config.isClientOnly()) {
			return;
		}
		
		listenHttpIP = Config.getListenHttpHost();
		
        if(StringUtils.isEmpty(listenHttpIP)){
        	throw new CommonException("IP not found");
        }
        
        //InetAddress.getByAddress(Array(127, 0, 0, 1))
        
        int p = Utils.isEmpty(this.port) ? 0 : Integer.parseInt(this.port);
        
        InetSocketAddress address = new InetSocketAddress(listenHttpIP,p);
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
        this.port = address.getPort()+"";
        pi.setHttpPort(port);
        
        for(IServerListener l : serverListener) {
        	l.serverStared(host(), port, Constants.TRANSPORT_NETTY_HTTP);
        }
        
        String m = "Running netty http server host["+listenHttpIP+"],port ["+this.port+"]";
        LOG.debug(m);    
        //SF.doSubmit(MonitorConstant.SERVER_START,m);
        //SF.serverStart(TAG,Constants.TRANSPORT_NETTY_HTTP+" : "+Config.getHost()+" : "+this.port);
	}

	@Override
	public void stop() {
		 //SF.serverStop(TAG,Config.getHost(),this.port);
		//SF.doSubmit(MonitorConstant.SERVER_STOP,Config.getHost(),this.port+"");
		 if(server != null){
			 //server.;
			 server = null;
        }
	}

	@Override
	public String host() {
		return listenHttpIP;
	}

	@Override
	public String port() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

}
