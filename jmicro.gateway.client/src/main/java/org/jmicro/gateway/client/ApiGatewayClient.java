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
package org.jmicro.gateway.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.api.annotation.Service;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.OnePrefixDecoder;
import org.jmicro.api.codec.OnePrefixTypeEncoder;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:23
 */
public class ApiGatewayClient {
	
	private final static Logger logger = LoggerFactory.getLogger(ApiGatewayClient.class);
	
	//private static int clientType = ApiGatewayClient.TYPE_SOCKET;
	//private  static int clientType = ApiGatewayClient.TYPE_WEBSOCKET;
	private static int clientType = ApiGatewayClient.TYPE_HTTP;
		
    public static final int TYPE_HTTP = 1;
	
	public static final int TYPE_SOCKET = 2;
	
	public static final int TYPE_WEBSOCKET = 3;
	
	private OnePrefixDecoder decoder = new OnePrefixDecoder();
	
	private OnePrefixTypeEncoder encoder = new OnePrefixTypeEncoder();
	
	private ApiGatewaySessionManager sessionManager = new ApiGatewaySessionManager();
	
	private volatile Map<Long,IResponseHandler> waitForResponses = new ConcurrentHashMap<>();
	
	private volatile Map<Long,Message> resqMsgCache = new ConcurrentHashMap<>();
	
	private static final ApiGatewayClient ins = new ApiGatewayClient();
	
	private static final AtomicLong reqId = new AtomicLong(0);
	
	//private String host = "172.16.22.200";
	//private String host = "192.168.1.102";
	private String host = "192.168.1.100";
	
	private int port= 9090;
	//private int port= 51875;
	//private int port= 51287;
	
    static {
    	Decoder.setTransformClazzLoader(getIns()::getEntityClazz);
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
	
	private static int getClientType() {
		//clientType = TYPE_SOCKET;
		return clientType;
	}
	
	private ApiGatewayClient() {
		sessionManager.setClientType(getClientType());
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Short type() {
				return Constants.MSG_TYPE_API_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				waitForResponses.get(msg.getReqId()).onResponse(msg);
			}
		});
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Short type() {
				return Constants.MSG_TYPE_API_CLASS_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				waitForResponses.get(msg.getReqId()).onResponse(msg);
			}
		});
	}
	
	
	public static ApiGatewayClient getIns() {
		return ins;
	}
	
	public <T> T getService(Class<T> serviceClass, String namespace, String version) {		
		if(!serviceClass.isInterface()) {
			throw new CommonException(serviceClass.getName() + " have to been insterface");
		}
		Object srv = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] {serviceClass}, (proxy,method,args) -> {
					return callService(serviceClass,method,args,namespace,version);
				});
		return (T)srv;
	}
	
	private Object callService(Class<?> serviceClass,Method mehtod,Object[] args,
			String namespace, String version ) {
		Service srvAnno = mehtod.getAnnotation(Service.class);
		if(srvAnno == null && StringUtils.isEmpty(namespace)) {
			throw new CommonException("Service ["+serviceClass.getName() +"] not specify namespage");
		}
		if(srvAnno != null && StringUtils.isEmpty(namespace)) {
			namespace = srvAnno.namespace();
		}
		
		if(srvAnno == null && StringUtils.isEmpty(version)) {
			throw new CommonException("Service ["+serviceClass.getName() + "] not specify version");
		}
		if(srvAnno != null && StringUtils.isEmpty(version)) {
			version = srvAnno.version();
		}
		return callService(serviceClass.getName(),namespace,version,mehtod.getName(),args);
	}
	
    public Class<?> getEntityClazz(Short type) {
    	
    	ApiRequest req = new ApiRequest();
		req.setArgs(new Object[] {type});
		req.setReqId(reqId.decrementAndGet());
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_API_CLASS_REQ);
		msg.setProtocol(Message.PROTOCOL_BIN);
		msg.setId(reqId.decrementAndGet());
		msg.setReqId(reqId.decrementAndGet());
		msg.setLinkId(reqId.decrementAndGet());
		
		ByteBuffer bb = encoder.encode(req);
		bb.flip();
		
		msg.setPayload(bb);
		msg.setVersion(Constants.VERSION_STR);
		
		String clazzName =(String) getResponse(msg);
		
		if(StringUtils.isEmpty(clazzName)) {
			try {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
				Decoder.registType(clazz,type);
				return clazz;
			} catch (ClassNotFoundException e) {
				logger.error("",e);
			}
		}
		return null;
	}
    
	
	public Object callService(String serviceName, String namespace, String version, String method, Object[] args) {
		Message msg = this.createMessage(serviceName, namespace, version, method, args);
		return getResponse(msg);
		
	}
	
	private Object getResponse(Message msg) {
		
		waitForResponses.put(msg.getReqId(), respMsg1 -> {
			resqMsgCache.put(msg.getReqId(), respMsg1);
			synchronized (msg) {
				msg.notify();
			}
		});
		
		IClientSession sessin = sessionManager.getOrConnect(host, port);
		sessin.write(msg);
		
		synchronized (msg) {
			try {
				msg.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		 Message resqMsg = resqMsgCache.get(msg.getReqId());
		 
		 if(resqMsg == null) {
			 return null;
		 }
		
		 ApiResponse resp = this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		 
		 if(resp.isSuccess()) {
			 return resp.getResult();
		 }else {
			 throw new CommonException(resp.getResult().toString());
		 }
		 
	}
    
    private Message createMessage(String serviceName, String namespace, String version, String method, Object[] args) {
    	
    	ApiRequest req = new ApiRequest();
		req.setArgs(args);
		req.setMethod(method);
		req.setNamespace(namespace);
		req.setReqId(reqId.decrementAndGet());
		req.setServiceName(serviceName);
		req.setVersion(version);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_API_REQ);
		msg.setProtocol(Message.PROTOCOL_BIN);
		msg.setId(reqId.decrementAndGet());
		msg.setReqId(reqId.decrementAndGet());
		msg.setLinkId(reqId.decrementAndGet());
		ByteBuffer bb = encoder.encode(req);
		bb.flip();
		msg.setPayload(bb);
		msg.setVersion(Constants.VERSION_STR);
		
		return msg;
    }
    

}
