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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import com.alibaba.fastjson.TypeReference;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.gateway.ApiRequestJRso;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.rsa.EncryptUtils;
import cn.jmicro.api.security.ActInfoJRso;
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
	
	public static final String NS_API_GATEWAY = "apigateway";
	public static final String NS_MNG = "mng";
	public static final String NS_SECURITY = "security";
	public static final String NS_REPONSITORY = "repository";
	public static final String NS_PUBSUB = "pubSubServer";
	
	private static final String API_GATEWAY_PUB_KEY_FILE = "/META-INF/keys/jmicro_apigateway_pub.key";
	
	private static ApiGatewayClient client;
	
	private ApiGatewayClientSessionManager sessionManager = new ApiGatewayClientSessionManager();
	
	private final Map<Long,Promise<?>> waitForResponse = new ConcurrentHashMap<>();
	
	private ApiGatewayConfig config = null;
	
	private IdClient idClient = null;
	
	private ApiGatewayPubsubClient pubsubClient;
	
	private ActInfoJRso actInfo;
	
	private static final String SEC_SRV = "cn.jmicro.api.security.IAccountService";
	private static final String SEC_SRV_ = "cn.jmicro.security.api.IAccountService";
	private static final String SEC_NS = ApiGatewayClient.NS_SECURITY;
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
		init0();
	}
	
	private void init0() {
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
				return Constants.MSG_TYPE_RRESP_JRPC;
			}
			@Override
			public boolean onMessage(ISession session, Message msg) {
				//session.active();
				if(msg.isDownSsl()) {
					checkSignAndDecrypt(msg);
				}
				notifyOnMessage(msg);
				return true;
			}
		});
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_ASYNC_RESP;
			}
			
			@Override
			public boolean onMessage(ISession session, Message msg) {
				//session.active();
				PSDataJRso pd = parseResult(msg,PSDataJRso.class,null);
				pubsubClient.onMsg(pd);
				return true;
			}
		});
		
		sessionManager.registerMessageHandler(new IMessageHandler(){
			@Override
			public Byte type() {
				return Constants.MSG_TYPE_PUBSUB_RESP;
			}
			
			@Override
			public boolean onMessage(ISession session, Message msg) {
				if(msg.isDownSsl()) {
					checkSignAndDecrypt(msg);
				}
				notifyOnMessage(msg);
				return true;
			}
		});
		
		//Decoder.setTransformClazzLoader(this::getEntityClazz);
	}
	
	protected void notifyOnMessage(Message msg) {
		Promise p = this.waitForResponse.remove(msg.getMsgId());
		parseResult(msg,p.resultType(),p);
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
						/*final PromiseImpl<T> p = new PromiseImpl<T>();
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
						};*/
						
						//String serviceName, String namespace, String version, String methodName, 
						//Class<?> returnType, Object[] args, IAsyncCallback<T> cb
						//Class<?> returnType = TypeUtils.finalParameterType(method.getGenericReturnType(), 0);
						//TypeToken<?>  returnType = TypeToken.get(method.getGenericReturnType());
						
						Class<?> returnType = TypeUtils.finalParameterType(method.getGenericReturnType(), 0);
						
						Object[] reqArgs = null;
						if(method.isAnnotationPresent(WithContext.class)) {
							reqArgs = new Object[args.length-1];
							 System.arraycopy(args, 0, reqArgs, 0, args.length-1);
						} else {
							reqArgs = args;
						}
						IPromise<T> p = callService(serviceClass.getName(), namespace, version, method.getName(), 
								returnType, reqArgs);
						return p;
					} else {
						Type returnType = getType(method.getReturnType(),method.getGenericReturnType());
						IPromise<T> p = callService(serviceClass.getName(), namespace, version, method.getName(), 
								returnType, args);
						return (T) p.getResult();
					}
		});
		return (T)srv;
	}
	
	public IPromise<RespJRso<ActInfoJRso>> loginJMAsync(String actName,String pwd) {
		IPromise<RespJRso<ActInfoJRso>> p = null;
		
		if(actInfo != null) {
			Promise<RespJRso<ActInfoJRso>> p0 = new Promise<>();
			p0.setFail(1, "Have login and have to logout before relogin");
			p0.setResult(null);
			p0.done();
			p = p0;
		} else {
			//Type returnType = TypeToken.getParameterized(RespJRso.class, ActInfoJRso.class).getType();
			Type returnType = new TypeReference<RespJRso<ActInfoJRso>>(){}.getType();
			p = this.callService(SEC_SRV, SEC_NS, SEC_VER, "login", 
					returnType, new Object[] {actName, pwd});
			p.then((RespJRso<ActInfoJRso> resp, AsyncFailResult fail, Object ctx)->{
				if(fail == null && resp.getCode() == RespJRso.CODE_SUCCESS) {
					setActInfo(resp.getData());
				}
			});
		}
		return p;
	} 
	
	public IPromise<RespJRso> logoutJMAsync() {
		IPromise<RespJRso> p = null;
		if(actInfo == null) {
			Promise<RespJRso> p0 = new Promise<>();
			p0.setResult(null);
			p0.setFail(1, "Not login");
			p0.done();
			p = p0;
		} else {
			//Type returnType = TypeToken.getParameterized(RespJRso.class, Boolean.class).getType();
			Type returnType = new TypeReference<RespJRso<Boolean>>(){}.getType();
			p = callService(SEC_SRV_, SEC_NS, SEC_VER, "logout", returnType, new Object[] {});
			p.success((rst,cxt0)->{
				setActInfo(null);
			});
		}
		
		return p;
	} 
	
	private void setActInfo(ActInfoJRso ai) {
		this.actInfo = ai;
	}
	
    public Class<?> getEntityClazz(Short type) {
    	
    	ApiRequestJRso req = new ApiRequestJRso();
		req.setArgs(new Object[] {type});
		req.setReqId(idClient.getLongId(IRequest.class.getName()));
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_API_CLASS_REQ);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setMsgId(idClient.getLongId(Message.class.getName()));
		msg.setOuterMessage(true);
		//msg.setReqId(req.getReqId());
		//msg.setLinkId(idClient.getLongId(Linker.class.getName()));
		
		//msg.setStream(false);
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setRespType(Message.MSG_TYPE_PINGPONG);;
		//msg.setLogLevel(MC.LOG_NO);
		msg.setMonitorable(false);
		msg.setDebugMode(false);
		
		ByteBuffer bb = null;//decoder.encode(req);
		
		msg.setPayload(bb);
		//msg.setVersion(Message.MSG_VERSION);
		
		String clazzName = null;//getResponse(msg,String.class,null);
		
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
    	//return TypeToken.get(gt).getType();
    	return new TypeReference(gt){}.getType();
    }
	
	public <T> IPromise<T> callService(String serviceName, String namespace, String version, String methodName, 
			Type  returnType, Object[] args) {
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
		final Promise<T> p = new Promise<T>();
		p.setResultType(returnType);
		waitForResponse.put(msg.getMsgId(), (Promise<?>)p);
		
		IClientSession sessin = sessionManager.getOrConnect(null,this.config.getHost(),
				this.config.getPort());
		sessin.write(msg);
		
		return p;
	}
	
	public <T> IPromise<T> sendMessage(byte msgType, Object arg, Class<T> resultType) {
		Message msg = this.createMessage(msgType,arg);
		final Promise<T> p = new Promise<T>();
		p.setResultType(resultType);
		waitForResponse.put(msg.getMsgId(), (Promise<?>)p);
		IClientSession sessin = sessionManager.getOrConnect(null,this.config.getHost(),this.config.getPort());
		sessin.write(msg);
		return p;
	}
	
	@SuppressWarnings("unchecked")
	private <R> R parseResult(Message respMsg, Type resultType, Promise p) {
		 
		 if(respMsg == null) {
			 return null;
		 }
		
		//Object v = this.decoder.decode((ByteBuffer)resqMsg.getPayload());
		ByteBuffer bb =  (ByteBuffer)respMsg.getPayload();
		String json = null;
		try {
			json = new String(bb.array(),Constants.CHARSET);
			
			if(respMsg.isError()) {
				//错误下行消息全用JSON，返回数据为ServerError实例
				RespJRso se = JsonUtils.getIns().fromJson(json, RespJRso.class);
				p.setFail(se.getCode(),se.getMsg());
				p.done();
				return null;
			}
			
			if(respMsg.getType() == Constants.MSG_TYPE_ASYNC_RESP) {
				R psData = JsonUtils.getIns().fromJson(json, resultType);
				return psData;
			}else if(respMsg.getType() == Constants.MSG_TYPE_PUBSUB_RESP) {
				R rst = JsonUtils.getIns().fromJson(json, resultType);
				if(p != null) {
					p.setResult(rst);
					p.done();
				}
				return rst;
			} else {
				RespJRso apiResp = JsonUtils.getIns().fromJson(json, RespJRso.class);
				if(apiResp.getCode() == RespJRso.CODE_SUCCESS) {
					 if(apiResp.result() == null || resultType == Void.class || Void.TYPE == resultType) {
						 p.done();
					 } else {
						 String js = JsonUtils.getIns().toJson(apiResp.result());
						 R rst = JsonUtils.getIns().fromJson(js, resultType);
						 p.setResult(rst);
						 p.done();
						 return rst;
					 }
				} else {
					 String js = JsonUtils.getIns().toJson(apiResp.result());
					 RespJRso se = JsonUtils.getIns().fromJson(js, RespJRso.class);
					 if(se != null) {
						 p.setFail(se.getCode(), se.getMsg());
					 }else {
						 p.setFail(RespJRso.CODE_FAIL, js);
					 }
					 p.done();
					 System.out.println(p.getResult());
					//throw new CommonException(apiResp.toString());
					 return null;
				}
			}
			
		} catch (UnsupportedEncodingException e) {
			p.setFail(RespJRso.CODE_FAIL, e.getMessage());
			p.done();
			//throw new CommonException(json,e);
		}
		 return null;
	}
	
	   private Message createMessage(byte msgType,Object payload) {
	    	
	    	Message msg = new Message();
			if(this.actInfo != null) {
				msg.putExtra(Message.EXTRA_KEY_LOGIN_KEY,this.actInfo.getLoginKey());
			}
			msg.setType(msgType);
			msg.setUpProtocol(Message.PROTOCOL_JSON);
			msg.setDownProtocol(Message.PROTOCOL_JSON);
			
			//msg.setId(req.getReqId()/*idClient.getLongId(Message.class.getName())*/);
			msg.setMsgId(idClient.getLongId(IRequest.class.getName()));
			//msg.setLinkId(req.getReqId()/*idClient.getLongId(Linker.class.getName())*/);
			msg.setRpcMk(false);

			//msg.setStream(false);
			msg.setDumpDownStream(false);
			msg.setDumpUpStream(false);
			msg.setRespType(Message.MSG_TYPE_PINGPONG);
			msg.setOuterMessage(true);
			//msg.setLogLevel(MC.LOG_NO);
			msg.setMonitorable(false);
			msg.setDebugMode(false);
			//全部异步返回，服务器可以异步返回，也可以同步返回
			msg.setUpSsl(false);
			msg.setInsId(0);
			msg.setForce2Json(true);
			
			//msg.setVersion(Message.MSG_VERSION);
			
			byte[] data = null;
			String json = JsonUtils.getIns().toJson(payload);
			try {
				data = json.getBytes(Constants.CHARSET);
			} catch (UnsupportedEncodingException e) {
				throw new CommonException(json,e);
			}
			
			signAndEncrypt(msg,data);
			
			return msg;
	}
    
    private Message createMessage(String serviceName, String namespace, String version, String method, Object[] args) {
    	
    	String mn = generatorSrvMethodName(method);
		String sn = AsyncClientUtils.genSyncServiceName(serviceName);
		String key = HashUtils.mkey(sn, namespace, version, method, config.getClientId());
		
		Integer hash = this.methodCodes.get(key);
		if(hash == null) {
			hash = HashUtils.FNVHash1(key);
			this.methodCodes.put(key, hash);
		}
		
		Long reqId = idClient.getLongId(IRequest.class.getName());
		String loginKey = this.actInfo != null ? this.actInfo.getLoginKey() : null;
		Message msg = Message.createRpcMessage(hash, args, reqId, mn, loginKey, Message.PROTOCOL_BIN, Message.PROTOCOL_JSON);
		
		signAndEncrypt(msg,((ByteBuffer)msg.getPayload()).array());
		
		return msg;
    }
    
    private byte getUpProtocol(Object[] args) {
		if(args == null || args.length == 0) {
			return Message.PROTOCOL_JSON;
		}else {
			for(Object ar : args) {
				if(ar.getClass() == new byte[0].getClass() || ar.getClass() == ByteBuffer.class) {
					return Message.PROTOCOL_BIN;
				}
			}
		}
		return Message.PROTOCOL_JSON;
	}

	private void signAndEncrypt(Message msg, byte[] data) {
		
		try {
			
			if(config.isUpSsl()) {
				
				byte[] salt = getSalt();
				msg.setSaltData(salt);
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
					
					msg.setSecData(pwdData);
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
    		edata = EncryptUtils.decryptAes(bb.array(), 0, bb.limit(), msg.getSaltData(), sec);
    		msg.setPayload(ByteBuffer.wrap(edata));
    	}
    	
    	if(msg.isSign()) {
    		if(edata != null) {
    			if (!EncryptUtils.doCheck(edata, 0, edata.length, msg.getSignData(), pubKey4ApiGateway)) {
    				throw new CommonException("invalid sign");
    			}
    		} else {
    			if (!EncryptUtils.doCheck(bb.array(), 0, bb.limit(), msg.getSignData(), pubKey4ApiGateway)) {
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
	
	private Map<String,Integer> methodCodes = new HashMap<>();
	
	private SecretKey sec = null;
	
	private RSAPublicKey pubKey4ApiGateway = null;
	
	private RSAPublicKey myPubKey = null;
	
	private RSAPrivateKey myPriKey;
	
	private byte[] pwdData = null;
	
	private long lastUpdatePwdTime = System.currentTimeMillis();
	
	
}
