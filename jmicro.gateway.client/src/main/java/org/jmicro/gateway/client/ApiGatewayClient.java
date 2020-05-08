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
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.PrefixTypeEncoderDecoder;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.IdRequest;
import org.jmicro.api.monitor.v1.Linker;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
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
	
	private static final AtomicLong reqId = new AtomicLong(1);
	
	private PrefixTypeEncoderDecoder decoder = new PrefixTypeEncoderDecoder();
	
	private ApiGatewayClientSessionManager sessionManager = new ApiGatewayClientSessionManager();
	
	private volatile Map<Long,IResponseHandler> waitForResponses = new ConcurrentHashMap<>();
	
	private volatile Map<Long,Message> resqMsgCache = new ConcurrentHashMap<>();
	
	private volatile Map<Long,Boolean> streamComfirmFlag = new ConcurrentHashMap<>();
	
	private ApiGatewayConfig config = null;
	
	private IdClient idClient = null;
	
	public ApiGatewayClient(ApiGatewayConfig cfg) {
		if(StringUtils.isEmpty(cfg.getHost())) {
			cfg.setHost(Utils.getIns().getLocalIPList().get(0));
		}
		this.config = cfg;
		this.idClient = new IdClient(this);
		init();
	}
	
	private void init() {
		sessionManager.setClientType(getClientType());
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
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
			public Byte type() {
				return Constants.MSG_TYPE_API_CLASS_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				waitForResponses.get(msg.getReqId()).onResponse(msg);
			}
		});
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_ID_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				waitForResponses.get(msg.getReqId()).onResponse(msg);
			}
		});
		
		Decoder.setTransformClazzLoader(this::getEntityClazz);
	}
	
	public <T> T getService(Class<T> serviceClass, String namespace, String version) {		
		if(!serviceClass.isInterface()) {
			throw new CommonException(serviceClass.getName() + " have to been insterface");
		}
		Object srv = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] {serviceClass}, 
				(proxy,method,args) -> callService(serviceClass,method,args,namespace,version));
		return (T)srv;
	}
	
    public Class<?> getEntityClazz(Short type) {
    	
    	ApiRequest req = new ApiRequest();
		req.setArgs(new Object[] {type});
		req.setReqId(idClient.getLongId(IRequest.class.getName()));
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_API_CLASS_REQ);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setId(idClient.getLongId(Message.class.getName()));
		msg.setReqId(req.getReqId());
		msg.setLinkId(idClient.getLongId(Linker.class.getName()));
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		msg.setLoggable(false);
		msg.setLogLevel(MonitorConstant.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = decoder.encode(req);
		
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		
		String clazzName =(String) getResponse(msg,null);
		
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
		return getResponse(msg,null);
	}
	
	public <R> Object callService(String serviceName, String namespace, String version
			, String method, Object[] args,IMessageCallback<R> callback) {
		Message msg = this.createMessage(serviceName, namespace, version, method, args);
		//msg.setStream(true);
		return getResponse(msg,callback);
	}
	
	private Object callService(Class<?> serviceClass,Method mehtod,Object[] args,
			String namespace, String version) {
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
	
	@SuppressWarnings("unchecked")
	private <R> Object getResponse(Message msg, final IMessageCallback<R> callback) {
		streamComfirmFlag.put(msg.getReqId(), true);
		waitForResponses.put(msg.getReqId(), respMsg1 -> {
			streamComfirmFlag.remove(msg.getReqId());
			resqMsgCache.put(msg.getReqId(), respMsg1);
			synchronized (msg) {
				msg.notify();
			}
		});
		
		IClientSession sessin = sessionManager.getOrConnect("apiGatewayClient",this.config.getHost(), this.config.getPort());
		sessin.write(msg);
	
		synchronized (msg) {
			try {
				msg.wait(30*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		Message resqMsg = resqMsgCache.remove(msg.getReqId());
		
		if(resqMsg.getType() == Constants.MSG_TYPE_ID_RESP) {
			return this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		} else {
			return parseResult(resqMsg);
		}
	}
	
	private Object parseResult(Message resqMsg) {
		 
		 if(resqMsg == null) {
			 return null;
		 }
		
		 Object v = this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		 
		if(v != null && v instanceof ServerError) {
			throw new CommonException(v.toString());
		} else if(v != null) {
			ApiResponse resp = (ApiResponse)v;
			 if(resp.isSuccess()) {
				 return resp.getResult();
			 } else {
				 throw new CommonException(resp.getResult().toString());
			 }
		}else {
			return null;
		}
			
		 
	}
    
    private Message createMessage(String serviceName, String namespace, String version, String method, Object[] args) {
    	
    	ApiRequest req = new ApiRequest();
		req.setArgs(args);
		req.setMethod(method);
		req.setNamespace(namespace);
		req.setReqId(idClient.getLongId(IRequest.class.getName()));
		req.setServiceName(serviceName);
		req.setVersion(version);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_API_REQ);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setId(idClient.getLongId(Message.class.getName()));
		msg.setReqId(req.getReqId());
		msg.setLinkId(idClient.getLongId(Linker.class.getName()));
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		msg.setLoggable(false);
		msg.setLogLevel(MonitorConstant.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = decoder.encode(req);
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		
		return msg;
    }
    
    @SuppressWarnings("unchecked")
	public Object[] getIds(String clazz, int num, byte type) {
    	
    	IdRequest req = new IdRequest();
		req.setClazz(clazz);
		req.setNum(num);
		req.setType(type);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_ID_REQ);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setId(reqId.getAndIncrement());
		msg.setReqId(msg.getId());
		msg.setLinkId(reqId.getAndIncrement());
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		msg.setLoggable(false);
		msg.setLogLevel(MonitorConstant.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = decoder.encode(req);
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		Object v = getResponse(msg,null);
		if(v != null && v instanceof ServerError) {
			throw new CommonException(v.toString());
		} else {
			return (Object[])v ;
		}
		
	}

    private static interface IResponseHandler{
		void onResponse(Message msg);
	}
	
	private int getClientType() {
		//clientType = TYPE_SOCKET;
		return this.getConfig().getClientType();
	}
	
	public ApiGatewayConfig getConfig() {
		return config;
	}
}
