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
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.SecretKey;

import com.google.gson.reflect.TypeToken;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.rsa.EncryptUtils;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.gateway.pubsub.ApiGatewayPubsubClient;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:23
 */
public class ApiGatewayClient {
	
	//private final static Logger logger = LoggerFactory.getLogger(ApiGatewayClient.class);
	
	private static final String API_GATEWAY_PUB_KEY_FILE = "/META-INF/keys/jmicro_apigateway_pub.key";
	
	private static ApiGatewayClient client;
	
	private static final AtomicLong reqId = new AtomicLong(1);
	
	//private PrefixTypeEncoderDecoder decoder = new PrefixTypeEncoderDecoder();
	
	private ApiGatewayClientSessionManager sessionManager = new ApiGatewayClientSessionManager();
	
	private volatile Map<Long,IResponseHandler> waitForResponses = new ConcurrentHashMap<>();
	
	private volatile Map<Long,Message> resqMsgCache = new ConcurrentHashMap<>();
	
	//private volatile Map<Long,Boolean> streamComfirmFlag = new ConcurrentHashMap<>();
	
	private ApiGatewayConfig config = null;
	
	private IdClient idClient = null;
	
	private ApiGatewayPubsubClient pubsubClient;
	
	private ActInfo actInfo;
	
	private static final String SEC_SRV = "cn.jmicro.api.security.IAccountService";
	private static final String SEC_NS = "sec";
	private static final String SEC_VER = "0.0.1";
	
	public static final ApiGatewayClient getClient() {
		if(!isInit()) {
			throw new NullPointerException("ApiGatewayClient not init");
		}
		return client;
	}
	
	public static final boolean isInit() {
		return client != null;
	}
	
	public static final synchronized boolean initClient(ApiGatewayConfig cfg) {
		if(client == null) {
			client = new ApiGatewayClient(cfg);
		}	
		return true;
	}
	
	private ApiGatewayClient(ApiGatewayConfig cfg) {
		if(Utils.isEmpty(cfg.getHost())) {
			cfg.setHost(Utils.getIns().getLocalIPList().get(0));
		}
		this.config = cfg;
		this.idClient = new IdClient();
		this.pubsubClient = new ApiGatewayPubsubClient(this);
		init();
	}
	
	private void init() {
		if(this.config.isUpSsl()) {
			String keyStr = EncryptUtils.loadKeyContent(config.getApiGwPriKeyFile(),API_GATEWAY_PUB_KEY_FILE);
			this.pubKey4ApiGateway = EncryptUtils.loadPublicKeyByStr(keyStr);
			if(this.pubKey4ApiGateway == null) {
				throw new CommonException("Fail to load api gateway public key: "+ API_GATEWAY_PUB_KEY_FILE );
			}
		}
		
		if(this.config.isDownSsl()) {
			String keyStr = EncryptUtils.loadKeyContent(config.getMyPubKeyFile(),null);
			this.myPubKey = EncryptUtils.loadPublicKeyByStr(keyStr);
			
			try {
				String priKeyStr = EncryptUtils.loadKeyContent(config.getMyPriKeyFile(),null);
				if(Utils.isEmpty(priKeyStr)) {
					throw new CommonException("Private key [" + config.getMyPriKeyFile() + "] not found!");
				}
				if (!Utils.isEmpty(config.getMyPriKeyPwd())) {
					SecretKey key = EncryptUtils.generatorSecretKey(config.getMyPriKeyPwd(), EncryptUtils.KEY_AES);
					byte[] data = Base64.getDecoder().decode(priKeyStr);
					data = EncryptUtils.decryptAes(data, 0, data.length, EncryptUtils.SALT_DEFAULT, key);
					myPriKey = EncryptUtils.loadPrivateKey(data);
				} else {
					myPriKey = EncryptUtils.loadPrivateKeyByStr(priKeyStr);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		sessionManager.setClientType(getClientType());
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_RRESP_RAW;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				if(msg.isDownSsl()) {
					checkSignAndDecrypt(msg);
				}
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
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_ASYNC_RESP;
			}
			
			@Override
			public void onMessage(ISession session, Message msg) {
				session.active();
				PSData pd = parseResult(msg,PSData.class);
				pubsubClient.onMsg(pd);
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
							public void onResult(T val, AsyncFailResult fail,Object ctx) {
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
						//Class<?> returnType = TypeUtils.finalParameterType(method.getGenericReturnType(), 0);
						//TypeToken<?>  returnType = TypeToken.get(method.getGenericReturnType());
						
						Class<?> returnType = TypeUtils.finalParameterType(method.getGenericReturnType(), 0);
						
						Object[] as = null;
						if(method.isAnnotationPresent(WithContext.class)) {
							 as = new Object[args.length-1];
							 System.arraycopy(args, 0, as, 0, args.length-1);
						}else {
							as = args;
						}
						callService(serviceClass.getName(), namespace, version, method.getName(), 
								returnType, as, cb);
						return p;
					} else {
						Type returnType = getType(method.getReturnType(),method.getGenericReturnType());
						return callService(serviceClass.getName(), namespace, version, method.getName(), 
								returnType, args, null);
					}
		});
		return (T)srv;
	}
	
	public IPromise<Resp<ActInfo>> loginJMAsync(String actName,String pwd) {
		final PromiseImpl<Resp<ActInfo>> p = new PromiseImpl<>();
		if(actInfo != null) {
			p.setFail(1, "Have login and have to logout before relogin");
			p.setResult(null);
			p.done();
		} else {
			IAsyncCallback<Resp<ActInfo>> cb = new IAsyncCallback<Resp<ActInfo>>() {
				@Override
				public void onResult(Resp<ActInfo> resp, AsyncFailResult fail, Object ctx) {
					if(fail == null && resp.getCode() == Resp.CODE_SUCCESS) {
						setActInfo(resp.getData());
						p.setResult(resp);
					} else {
						if(fail == null) {
							fail = new AsyncFailResult();
							fail.setCode(resp.getCode());
							fail.setMsg(resp.getMsg());
						}
						p.setFail(fail);
					}
					p.done();
				}
			};
			//TypeToken<?>  returnType
			Type returnType = TypeToken.getParameterized(Resp.class, ActInfo.class).getType();
			this.callService(SEC_SRV, SEC_NS, SEC_VER, "login", 
					returnType, new Object[] {actName, pwd}, cb);
		}
		return p;
	} 
	
	public IPromise<Boolean> logoutJMAsync() {
		final PromiseImpl<Boolean> p = new PromiseImpl<>();
		if(actInfo == null) {
			p.setResult(null);
			p.setFail(1, "Not login");
			p.done();
		} else {
			IAsyncCallback<Resp<Boolean>> cb = new IAsyncCallback<Resp<Boolean>>() {
				@Override
				public void onResult(Resp<Boolean> resp, AsyncFailResult fail,Object ctx) {
					if(fail == null && resp.getData()) {
						p.setResult(true);
						setActInfo(null);
					} else {
						if(fail == null) {
							fail = new AsyncFailResult();
							fail.setCode(resp.getCode());
							fail.setMsg(resp.getMsg());
						}
						p.setFail(fail);
					}
					p.done();
				}
			};
			Type returnType = TypeToken.getParameterized(Resp.class, Boolean.class).getType();
			this.callService(SEC_SRV, SEC_NS, SEC_VER, "logout", 
					returnType, new Object[] {}, cb);
		}
		
		return p;
	} 
	
	private void setActInfo(ActInfo ai) {
		this.actInfo = ai;
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
		//msg.setLinkId(idClient.getLongId(Linker.class.getName()));
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		//msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = null;//decoder.encode(req);
		
		msg.setPayload(bb);
		msg.setVersion(Message.MSG_VERSION);
		
		String clazzName =(String) getResponse(msg,String.class,null);
		
		if(Utils.isEmpty(clazzName)) {
			try {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
				//Decoder.registType(clazz,type);
				return clazz;
			} catch (ClassNotFoundException e) {
				//logger.error("",e);
				e.printStackTrace();
			}
		}
		return null;
	}
    
    private Type getType(Class<?> rawType,Type gt) {
    	if(gt == null) {
    		return rawType;
    	}
    	return TypeToken.get(gt).getType();
    	
    }
	
	public <T> T callService(String serviceName, String namespace, String version, String methodName, 
			Type  returnType, Object[] args, IAsyncCallback<T> cb) {
		if(Utils.isEmpty(serviceName)) {
			throw new CommonException("Service cabnot be null");
		}
		
		if(Utils.isEmpty(namespace)) {
			throw new CommonException("Namespace cabnot be null");
		}
		
		if(Utils.isEmpty(version)) {
			throw new CommonException("Version cabnot be null");
		}
		
		if(Utils.isEmpty(methodName)) {
			throw new CommonException("Method name cabnot be null");
		}
		
		Message msg = this.createMessage(serviceName, namespace, version, methodName, args);
		return getResponse(msg ,returnType,cb);
	}
	
	
	//@SuppressWarnings("unchecked")
	private <T> T getResponse(Message msg, Type  returnType, final IAsyncCallback<T> cb) {
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
						 //val = this.decoder.decode((ByteBuffer)respMsg1.getPayload());
						 cb.onResult((T)val, null,null);
					} else {
						 try {
							 val = parseResult(respMsg1,returnType);
							 cb.onResult((T)val, null,null);
						} catch (Exception e) {
							e.printStackTrace();
							AsyncFailResult f = new AsyncFailResult();
							f.setMsg(e.getMessage());
							f.setCode(1);
							cb.onResult(null, f,null);
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
			return null;//this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		} else {
			return (T)parseResult(resqMsg,returnType);
		}
	}
	
	private <R> R parseResult(Message respMsg, Type returnType) {
		 
		 if(respMsg == null) {
			 return null;
		 }
		
		//Object v = this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		ByteBuffer bb =  (ByteBuffer)respMsg.getPayload();
		String json = null;
		try {
			json = new String(bb.array(),Constants.CHARSET);
			if(respMsg.getType() == Constants.MSG_TYPE_ASYNC_RESP) {
				R psData = JsonUtils.getIns().fromJson(json, returnType);
				return psData;
			} else {
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
		if(!ApiGatewayPubsubClient.messageServiceImplName.equals(serviceName)) {
			req.setServiceName(generatorSrvName(serviceName));
		}else {
			req.setServiceName(serviceName);
		}
		req.setNamespace(namespace);
		req.setVersion(version);
		if(this.actInfo != null) {
			req.getParams().put("loginKey", this.actInfo.getLoginKey());
		}
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_RAW);
		msg.setUpProtocol(Message.PROTOCOL_JSON);
		msg.setDownProtocol(Message.PROTOCOL_JSON);
		msg.setId(req.getReqId()/*idClient.getLongId(Message.class.getName())*/);
		msg.setReqId(req.getReqId());
		msg.setLinkId(req.getReqId()/*idClient.getLongId(Linker.class.getName())*/);
		msg.setRpcMk(true);
		int hash = HashUtils.FNVHash1(serviceName + "##"+namespace+"##"+version+"########"+method);
		
		msg.setSmKeyCode(hash);
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setNeedResponse(true);
		//msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		//全部异步返回，服务器可以异步返回，也可以同步返回
		msg.setAsyncReturnResult(true);
		msg.setUpSsl(false);
		msg.setInsId(0);
		
		msg.setVersion(Message.MSG_VERSION);
		
		String json = JsonUtils.getIns().toJson(req);
		
		signAndEncrypt(msg,json);
		
		return msg;
    }
    
    private void signAndEncrypt(Message msg,String json) {
		
		try {
			byte[] data = json.getBytes(Constants.CHARSET);
			if(config.isUpSsl()) {
				
				byte[] salt = getSalt();
				msg.setSalt(salt);
				msg.setUpSsl(config.isUpSsl());
				msg.setDownSsl(config.isDownSsl());
				msg.setEncType(config.getEncType()==1);
				
				String insName = config.getHost() + ":" + config.getPort();
				msg.setInsId(insName.hashCode()%65534);
				
				if(sec == null || ((System.currentTimeMillis() - this.lastUpdatePwdTime) > config.getPwdUpdateInterval())) {
					sec = EncryptUtils.generatorSecretKey(EncryptUtils.KEY_AES);
					this.lastUpdatePwdTime = System.currentTimeMillis();
					pwdData = sec.getEncoded();
					pwdData = EncryptUtils.encryptRsa(pubKey4ApiGateway, pwdData, 0, pwdData.length);
					
					msg.setSec(pwdData);
					msg.setSec(true);
				}
				
				data = EncryptUtils.encryptAes(data, 0, data.length, salt, sec);
			}
			
			msg.setPayload(ByteBuffer.wrap(data));
		} catch (Exception e) {
			throw new CommonException("Sign or encrypt error: ",e);
		}
    }
    
    private void checkSignAndDecrypt(Message msg) {
    	
    	ByteBuffer bb = (ByteBuffer) msg.getPayload();
    	byte[] edata = null;
    	if(msg.isDownSsl()) {
    		edata = EncryptUtils.decryptAes(bb.array(), 0, bb.limit(), msg.getSalt(), sec);
    		msg.setPayload(ByteBuffer.wrap(edata));
    	}
    	
    	if(msg.isSign()) {
    		if(edata != null) {
    			if (!EncryptUtils.doCheck(edata, 0, edata.length, msg.getSign(), pubKey4ApiGateway)) {
    				throw new CommonException("invalid sign");
    			}
    		} else {
    			if (!EncryptUtils.doCheck(bb.array(), 0, bb.limit(), msg.getSign(), pubKey4ApiGateway)) {
    				throw new CommonException("invalid sign");
    			}
    		}
    	}
		
	}
    
    private byte[] getSalt() {
		byte[] salt = new byte[EncryptUtils.SALT_LEN];
		Random random = new Random();
		random.nextBytes(salt);
		return salt;
	}
    
    private String generatorSrvName(String className) {
		return AsyncClientUtils.genSyncServiceName(className);
	}
    
    private String generatorSrvMethodName(String method) {
		return AsyncClientUtils.genSyncMethodName(method);
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

	public ApiGatewayPubsubClient getPubsubClient() {
		return pubsubClient;
	}
	
	
	
	private SecretKey sec = null;
	
	private RSAPublicKey pubKey4ApiGateway = null;
	
	private RSAPublicKey myPubKey = null;
	
	private RSAPrivateKey myPriKey;
	
	private byte[] pwdData = null;
	
	private long lastUpdatePwdTime = System.currentTimeMillis();
	
	
}
