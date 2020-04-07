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
package org.jmicro.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.IServer;
import org.jmicro.api.net.Message;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**

{"protocol":2,"msgId":11977,"reqId":7832,"sessionId":162,"len":0,"version":"0.0.1","type":1,"flag":0,
 "payload":"{\"serviceName\":\"org.jmicro.example.api.ITestRpcService\",\"method\":\"getPerson\",\"args\":[{\"username\":\"Client person Name\",\"id\":1234}],\"namespace\":\"defaultNamespace\",
 \"version\":\"0.0.0\",\"impl\":\"org.jmicro.example.provider.TestRpcServiceImpl\",\"reqId\":7832,\"isMonitorEnable\":true,\"params\":{}}"}


 * @author Yulei Ye
 * @date 2018年10月19日-下午12:34:17
 */
@SuppressWarnings("restriction")
@Component(value=Constants.TRANSPORT_JDKHTTP,lazy=false,level=1,side=Constants.SIDE_PROVIDER)
@Server(transport=Constants.TRANSPORT_JDKHTTP)
public class JMicroHttpServer implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(JMicroHttpServer.class);
	
	private static final String TAG = JMicroHttpServer.class.getName();
	
	private  HttpServer server;
	
	@Cfg(value = "/startHttp",required=false)
	private boolean enable = false;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Cfg(value = "/bindIp",required=false)
	private String host;
	
	@Cfg(value="/port",required=false)
	private int port=9990;
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private StaticResourceHttpHandler staticResourceHandler;
	
	//@Inject(required=false)
	private HttpHandler httpHandler = new HttpHandler(){
        @Override
        public void handle(HttpExchange exchange) {
        	JMicroContext.setMonitor();
        	JMicroContext.callSideProdiver(true);
        	HttpServerSession session = new HttpServerSession(exchange,readBufferSize,heardbeatInterval);
			session.init();
        	try {
				if(exchange.getRequestMethod().equals("POST")){
					InputStream in = exchange.getRequestBody();
					byte[]data = new byte[in.available()];
					in.read(data, 0, data.length);
		        	String json = new String(data,0,data.length, Constants.CHARSET);
		        	Message msg = JsonUtils.getIns().fromJson(json, Message.class);
		        	JMicroContext.configProvider(msg);
		 			receiver.receive(session,msg);
				}else {
					Message msg = new Message();
		    		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		    		msg.setProtocol(Message.PROTOCOL_JSON);
		    		msg.setId(idGenerator.getLongId(Message.class));
		    		msg.setReqId(-1L);
		    		msg.setPayload("");
		    		msg.setVersion(Message.MSG_VERSION);
					exchange.sendResponseHeaders(200, 0);
					exchange.getResponseBody().write(JsonUtils.getIns().toJson(msg).getBytes(Constants.CHARSET));
					session.close(true);
				}
			} catch (IOException e) {
				LOG.error("handle",e);
			}
        }
    };
	    
	@Override
	public void init() {
		if(!enable) {
			return;
		}
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
        try {
        	 server = HttpServer.create(address, 0);
        	 server.createContext("/jmicro", this.httpHandler);
        	 server.createContext("/", this.staticResourceHandler);
        	 server.start();
             address = server.getAddress();
		} catch (IOException e) {
			LOG.error("",e);
		}
        this.port = address.getPort();
        
        String m = "Running the server host["+this.host+"],port ["+this.port+"]";
        LOG.debug(m);    
        SF.serverStart(TAG,this.host,this.port,Constants.TRANSPORT_NETTY_HTTP);
	}

	@Override
	public void stop() {
		SF.serverStop(TAG,this.host,this.port);
		 if(server != null){
			 server.stop(0);
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
