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
package cn.jmicro.gateway.client;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.codec.Decoder;
import cn.jmicro.api.codec.PrefixTypeEncoderDecoder;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.Linker;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

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
	
	//private volatile Map<Long,Boolean> streamComfirmFlag = new ConcurrentHashMap<>();
	
	private ApiGatewayConfig config = null;
	
	private IdClient idClient = null;
	
	public ApiGatewayClient(ApiGatewayConfig cfg) {
		if(StringUtils.isEmpty(cfg.getHost())) {
			cfg.setHost(Utils.getIns().getLocalIPList().get(0));
		}
		this.config = cfg;
		this.idClient = new IdClient();
		init();
	}
	
	private void init() {
		sessionManager.setClientType(getClientType());
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_RRESP_RAW;
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
		
		//Decoder.setTransformClazzLoader(this::getEntityClazz);
	}
	
	public <T> T getService(Class<?> serviceClass, String namespace, String version) {		
		if(!serviceClass.isInterface()) {
			throw new CommonException(serviceClass.getName() + " have to been insterface");
		}
		Object srv = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] {serviceClass}, 
				(proxy,method,args) -> {
					
					if(serviceClass.getName().endsWith(AsyncClientProxy.INT_SUBFIX) && 
							method.getName().endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
						//异步RPC方法调用
						final PromiseImpl<T> p = new PromiseImpl<T>();
						IAsyncCallback<T> cb = new IAsyncCallback<T>() {
							@Override
							public void onResult(T val, AsyncFailResult fail) {
								if(fail == null) {
									p.setResult(val);
								} else {
									p.setFail(fail);
								}
								p.done();
							}
						};
						//String serviceName, String namespace, String version, String methodName, 
						//Class<?> returnType, Object[] args, IAsyncCallback<T> cb
						Class<?> returnType = TypeUtils.finalParameterType(method.getGenericReturnType(), 0);
						callService(serviceClass.getName(), namespace, version, method.getName(), 
								returnType, args, cb);
						return p;
					} else {
						return callService(serviceClass,method,args,namespace,version,null);
					}
		});
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
		msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = decoder.encode(req);
		
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		
		String clazzName =(String) getResponse(msg,String.class,null);
		
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
	
	private <T> T callService(Class<?> serviceClass, Method method, Object[] args,
			String namespace, String version, IAsyncCallback<T> cb) {
		
		Service srvAnno = method.getAnnotation(Service.class);
		
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
		
		Message msg = this.createMessage(serviceClass.getName(), namespace, version, method.getName(), args);
		return getResponse(msg ,method.getReturnType(),cb);
	}
	
	public <T> T callService(String serviceName, String namespace, String version, String methodName, 
			Class<?> returnType, Object[] args, IAsyncCallback<T> cb) {
		Message msg = this.createMessage(serviceName, namespace, version, methodName, args);
		return getResponse(msg ,returnType,cb);
	}
	
	/*public <T> T callService(String serviceName, String namespace, String version, String methodName, 
			Class<T> returnType, Object[] args, IAsyncCallback<T> cb) {
		Message msg = this.createMessage(serviceName, namespace, version, methodName, args);
		return getResponse(msg,returnType,cb);
	}*/
	
	//@SuppressWarnings("unchecked")
	private <T> T getResponse(Message msg, Class<?> returnType, final IAsyncCallback<T> cb) {
		//streamComfirmFlag.put(msg.getReqId(), true);
		waitForResponses.put(msg.getReqId(), respMsg1 -> {
			//streamComfirmFlag.remove(msg.getReqId());
			if(cb == null) {
				resqMsgCache.put(msg.getReqId(), respMsg1);
				synchronized (msg) {
					msg.notify();
				}
			} else {
				if(respMsg1 != null) {
					Object val = null;
					 if(respMsg1.getType() == Constants.MSG_TYPE_ID_RESP) {
						 val = this.decoder.decode((ByteBuffer)respMsg1.getPayload());
						 cb.onResult((T)val, null);
					} else {
						 try {
							 val = parseResult(respMsg1,returnType);
							 cb.onResult((T)val, null);
						} catch (Exception e) {
							AsyncFailResult f = new AsyncFailResult();
							f.setMsg(e.getMessage());
							f.setCode(1);
							cb.onResult(null, f);
						}
					}
				}
			}
			return;
		});
		
		IClientSession sessin = sessionManager.getOrConnect(null,this.config.getHost(),
				this.config.getPort());
		sessin.write(msg);
	
		if(cb != null) {
			//异步RPC
			return null;
		}
		
		synchronized (msg) {
			try {
				msg.wait(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		Message resqMsg = resqMsgCache.remove(msg.getReqId());
		
		if(resqMsg == null) {
			throw new CommonException("Timeout");
		}
		
		if(resqMsg.getType() == Constants.MSG_TYPE_ID_RESP) {
			return this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		} else {
			return (T)parseResult(resqMsg,returnType);
		}
	}
	
	private <R> R parseResult(Message resqMsg, Class<R> returnType) {
		 
		 if(resqMsg == null) {
			 return null;
		 }
		
		//Object v = this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		ByteBuffer bb =  (ByteBuffer)resqMsg.getPayload();
		String json = null;
		try {
			json = new String(bb.array(),Constants.CHARSET);
			ApiResponse apiResp = JsonUtils.getIns().fromJson(json, ApiResponse.class);
			if(apiResp.isSuccess()) {
				 if(apiResp.getResult() == null || returnType == Void.class || Void.TYPE == returnType) {
					 return null;
				 } else {
					 String js = JsonUtils.getIns().toJson(apiResp.getResult());
					 R rst = JsonUtils.getIns().fromJson(js, returnType);
					 return rst;
				 }
			} else {
				throw new CommonException(apiResp.toString());
			}
		} catch (UnsupportedEncodingException e) {
			throw new CommonException(json,e);
		}
		
	}
    
    private Message createMessage(String serviceName, String namespace, String version, String method, Object[] args) {
    	
    	ApiRequest req = new ApiRequest();
    	req.setReqId(idClient.getLongId(IRequest.class.getName()));
		req.setArgs(args);
		req.setMethod(generatorSrvMethodName(method));
		req.setServiceName(generatorSrvName(serviceName));
		req.setNamespace(namespace);
		req.setVersion(version);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_RAW);
		msg.setUpProtocol(Message.PROTOCOL_JSON);
		msg.setDownProtocol(Message.PROTOCOL_JSON);
		msg.setId(req.getReqId()/*idClient.getLongId(Message.class.getName())*/);
		msg.setReqId(req.getReqId());
		msg.setLinkId(req.getReqId()/*idClient.getLongId(Linker.class.getName())*/);
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		//全部异步返回，服务器可以异步返回，也可以同步返回
		msg.setAsyncReturnResult(true);
		
		//ByteBuffer bb = decoder.encode(req);
		
		String json = JsonUtils.getIns().toJson(req);
		ByteBuffer bb = null;
		try {
			bb = ByteBuffer.wrap(json.getBytes(Constants.CHARSET));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		
		return msg;
    }
    
    private String generatorSrvName(String className) {
		if(className.endsWith(AsyncClientProxy.INT_SUBFIX)) {
			 String cn = className.substring(0, className.indexOf(AsyncClientProxy.INT_SUBFIX));
			 String pkgName = cn.substring(0,cn.lastIndexOf("."));
			 pkgName = pkgName.substring(0, pkgName.indexOf(AsyncClientProxy.PKG_SUBFIX));
			 String simpleClassName = cn.substring(cn.lastIndexOf(".")+1,cn.length());
			 String iname = pkgName + simpleClassName;
			return iname;
		 }
		return className;
	}
    
    private String generatorSrvMethodName(String method) {
    	if(method.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
    		return method.substring(0,method.indexOf(AsyncClientProxy.ASYNC_METHOD_SUBFIX));
    	}
		return method;
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
		msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = decoder.encode(req);
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		Object v = getResponse(msg,Long.class,null);
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
