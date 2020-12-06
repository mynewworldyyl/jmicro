
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
package cn.jmicro.gateway;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 */
@Component(side = Constants.SIDE_PROVIDER)
public class ApiRawRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRawRequestMessageHandler.class);
	private static final Class<?> TAG = ApiRawRequestMessageHandler.class;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private PermissionManager pm;
	
	@Inject
	private AccountManager accountManager;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private IObjectFactory objFactory;
	
	@Inject
	private MessageServiceImpl ms;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private ProcessInfo pi;
	
	@Cfg("/ApiRawRequestMessageHandler/openDebug")
	private boolean openDebug = false;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_REQ_RAW;
	}
	
	@Override
	public void onMessage(ISession session, Message msg) {
		ApiRequest req = null;
		JDataInput ji = null;
		final ApiResponse resp =  new ApiResponse();
		
		try {

			if(msg.getUpProtocol() == Message.PROTOCOL_JSON) {
				req = ICodecFactory.decode(codecFactory, msg.getPayload(), ApiRequest.class, msg.getUpProtocol());
				req.setArgs(getArgs(req.getServiceName(),req.getMethod(),req.getArgs(),session));
			} else {
				
				req = new ApiRequest();
				ji = new JDataInput((ByteBuffer)msg.getPayload());
				req.setReqId(ji.readLong());
				req.setServiceName(ji.readUTF());
				req.setNamespace(ji.readUTF());
				req.setVersion(ji.readUTF());
				req.setMethod(ji.readUTF());

				int len = (int)ji.readUnsignedInt();
				if(len > 0) {
					for(int i=0; i< len; i++) {
						String k = ji.readUTF();
						String v = ji.readUTF();
						req.getParams().put(k, v);
					}
				}
				int argLen = (int)ji.readUnsignedInt();
				if(argLen > 0) {
					req.setArgs(getArgs(req.getServiceName(),req.getMethod(),session,ji));
				}
			}
		
			Object result = null;
			
			msg.setType(Constants.MSG_TYPE_RRESP_RAW);
			resp.setReqId(req.getReqId());
			resp.setMsg(msg);
			resp.setSuccess(true);
			resp.setId(idGenerator.getLongId(ApiResponse.class));
			
			ActInfo ai = null;
			
			boolean doLogick = true;
			if(req.getParams().containsKey(JMicroContext.LOGIN_KEY)) {
				String lk = (String)req.getParams().get(JMicroContext.LOGIN_KEY);
				if(StringUtils.isNotEmpty(lk)) {
					ai = this.accountManager.getAccount(lk);
					if(ai == null) {
						ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,"Invalid login key!");
						resp.setResult(se);
						resp.setSuccess(false);
						doLogick = false;
						result = se;
					} else {
						JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
						JMicroContext.get().setAccount(ai);	
					}
				}
			}
			
			if(doLogick && MessageServiceImpl.TAG.equals(req.getServiceName())) {
				if(ai != null) {
					if("subscribe".equals(req.getMethod())) {
						String topic = (String)req.getArgs()[1];
						Map<String, Object> ctx = (Map<String, Object>)req.getArgs()[2];
						result = ms.subscribe((ISession)req.getArgs()[0], topic, ctx);
					} else if("unsubscribe".equals(req.getMethod())){
						result = ms.unsubscribe((int)req.getArgs()[0]);
					} else {
						logger.error("Method:"+req.getMethod()+" not found!");
						result = new ServerError(MC.MT_SERVICE_METHOD_NOT_FOUND,"Method:"+req.getMethod()+" not found!");
						resp.setSuccess(false);
					}
				} else {
					resp.setSuccess(false);
					result = new ServerError(MC.MT_INVALID_LOGIN_INFO,"Have to login before use pubsub service!");
				}
			} else if(doLogick){
				
				Object srv = objFactory.getRemoteServie(req.getServiceName(), req.getNamespace(), req.getVersion(),null);
				
				if(srv != null){

					AbstractClientServiceProxyHolder proxy = (AbstractClientServiceProxyHolder)srv;
					ServiceItem si = proxy.getHolder().getItem();
					if(si == null) {
						//SF.doRequestLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, null," service not found");
						throw new CommonException("Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] not found");
					} else if(si.isExternal()) {
						Method m = this.getSrvMethod(srv.getClass(), req.getMethod(),true);
						JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, session.localHost());
						JMicroContext.get().setParam(JMicroContext.LOCAL_PORT, session.localPort()+"");
						JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, session.remoteHost());
						JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, session.remotePort()+"");
						JMicroContext.get().mergeParams(req.getParams());
						
						ServiceMethod sm = si.getMethod(req.getMethod(), m.getParameterTypes());
						if(sm == null) {
							String errMsg = "Service mehtod ["+req.getServiceName()+"] method ["+req.getMethod()+"] not found";
							LG.log(MC.LOG_ERROR, TAG, errMsg);
							MT.rpcEvent(MC.MT_SERVICE_METHOD_NOT_FOUND);
							throw new CommonException(errMsg);
						}
						
						ServerError se = pm.permissionCheck(ai,sm,si.getClientId());
						if(se != null){
							result = se;
							resp.setSuccess(false);
						} else  {
							if(LG.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
								LG.log(MC.LOG_DEBUG, TAG," got request");
							}
							if(!sm.isNeedResponse()) {
								m.invoke(srv, req.getArgs());
								if(LG.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
									LG.log(MC.LOG_DEBUG, TAG," no need response");
								}
								return;
							} else {
								//IPromise<?> p = (IPromise<?>)m.invoke(srv, req.getArgs());
								
								PromiseUtils.callService(srv, req.getMethod(), null, req.getArgs())
								.then((r,fail,ctx)->{
									if(fail == null) {
										resp.setResult(r);
										resp.setSuccess(true);
									} else {
										ServerError se1 = new ServerError(fail.getCode(),fail.getMsg());
										resp.setSuccess(false);
										resp.setResult(se1);
										logger.error("",fail.toString());
									}
									
									if(msg.getDownProtocol() == Message.PROTOCOL_JSON) {
										msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getDownProtocol()));
									} else {
										msg.setPayload(resp.encode());
									}
									
									if(resp.isSuccess() && (msg.isUpSsl() || msg.isDownSsl())) {
										//由客户端决定返回数据加解密方式
										//msg.setDownSsl(false/*sm.isDownSsl()*/);
										//msg.setUpSsl(false/*sm.isUpSsl()*/);
										//msg.setEncType(sm.isRsa());
										secretMng.signAndEncrypt(msg, msg.getInsId());
									} else {
										//错误不需要做加密或签名
										msg.setDownSsl(false);
										msg.setUpSsl(false);
									}
									msg.setInsId(pi.getId());
									session.write(msg);
								});
							}
						}
					} else {
						String msgStr = "Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] is not external!";
						throw new CommonException(msgStr);
					}
				
				} else {
					resp.setSuccess(false);
					String msgStr = "Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] instance not found!";
					result = new ServerError(0,msgStr);
					LG.log(MC.LOG_ERROR, TAG,msgStr);
				}
			}
			
			if(LG.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
				LG.log(MC.LOG_DEBUG, TAG,"one response");
			}
			
			if(result != null ) {
				if(msg.isNeedResponse()) {
					resp.setResult(result);
					if(msg.getDownProtocol() == Message.PROTOCOL_JSON) {
						msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getDownProtocol()));
					} else {
						msg.setPayload(resp.encode());
					}
					
					if(resp.isSuccess() && (msg.isUpSsl() || msg.isDownSsl())) {
						//由客户端决定返回数据加解密方式
						//msg.setDownSsl(false/*sm.isDownSsl()*/);
						//msg.setUpSsl(false/*sm.isUpSsl()*/);
						//msg.setEncType(sm.isRsa());
						secretMng.signAndEncrypt(msg, msg.getInsId());
					} else {
						//错误不需要做加密或签名
						msg.setDownSsl(false);
						msg.setUpSsl(false);
					}
					msg.setInsId(pi.getId());
					session.write(msg);
				} else {
					logger.warn(result.toString());
				}
			}
			
		} catch (Throwable e) {
			CommonException ce = null;
			if(e instanceof CommonException) {
				ce = (CommonException)e;
				ce.setReq(req);
				ce.setResp(resp);
			} else {
				ce = new CommonException("",e,req);
				ce.setResp(resp);
			}
			
			if(req != null && req.getParams().containsKey(JMicroContext.LOGIN_KEY)) {
				String lk = (String)req.getParams().get(JMicroContext.LOGIN_KEY);
				if(StringUtils.isNotEmpty(lk)) {
					ActInfo ai = this.accountManager.getAccount(lk);
					if(ai != null) {
						JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
						JMicroContext.get().setAccount(ai);	
						ce.setAi(ai);
					}
				}
			}
			
			throw ce;
		}
		
	}

	
	private Object[] getArgs(String serviceName, String method, ISession session, JDataInput ji) {
		
		//ServiceItem item = registry.getServiceByImpl(r.getImpl());
		Class<?> srvClazz = objFactory.loadCls(serviceName);
		if(srvClazz == null) {
			throw new CommonException("Class ["+serviceName+"] not found");
		}
		
		Method m = getSrvMethod(srvClazz,method,false);
		
		if(m == null) {
			throw new CommonException("Class ["+serviceName+"] method ["+method+"] not found"); 
		}
		
		Class<?>[] clses = m.getParameterTypes();
		Object[] args = new Object[clses.length];
		
		int i = 0;
		try {
			//读掉数组前缀长度
			
			for(; i < clses.length; i++){
				Class<?> pt = clses[i];
				if(ISession.class.isAssignableFrom(pt)) {
					args[i] = session;
				} else {
					Object a = null;
					if(pt == Integer.class || Integer.TYPE == pt) {
						Long v = ji.readLong();
						a = v.intValue();
					} else if(pt == Long.class || Long.TYPE == pt) {
						a = ji.readLong();
					}  else if(pt == Short.class || Short.TYPE == pt) {
						Long v = ji.readLong();
						a = v.shortValue();
					} else if(pt == Byte.class || Byte.TYPE == pt) {
						Long v = ji.readLong();
						a = v.byteValue();
					} else if(pt == Float.class || Float.TYPE == pt) {
						Long v = ji.readLong();
						a = v.floatValue();
					} else if(pt == Double.class || Double.TYPE == pt) {
						Long v = ji.readLong();
						a = v.doubleValue();
					} else if(pt == Boolean.class || Boolean.TYPE == pt) {
						byte b = ji.readByte();
						a = b == 1;
					} else if(pt == Character.class || Character.TYPE == pt) {
						a = ji.readByte();
					}else if(pt == String.class) {
						a = ji.readUTF();
					}else if(Map.class.isAssignableFrom(pt)) {
						int len = (int)ji.readUnsignedInt();
						if(len > 0) {
							Map<String,String> data = new HashMap<>();
							for(int x = 0; x< len; x++) {
								String k = ji.readUTF();
								String v = ji.readUTF();
								data.put(k, v);
							}
							a = data;
						}
					}else if(pt == new byte[0].getClass()) {
						int len = (int)ji.readUnsignedInt();
						if(len > 0) {
							byte[] data = new byte[len];
							ji.readFully(data, 0, len);
							a = data;
						}
					}
					
					args[i] = a;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return args;
	}
	
	private Method getSrvMethod(Class<?> srvCls,String methodName,boolean async) {
		
		if(async && !methodName.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
			methodName = methodName + AsyncClientProxy.ASYNC_METHOD_SUBFIX;
		}
		/*Class<?> srvClazz = JMicro.getObjectFactory().loadCls(srvCls);
		if(srvClazz == null) {
			throw new CommonException("Class ["+srvCls+"] not found");
		}*/
		
		Method m = null;
		for(Method sm : srvCls.getMethods()){
			if(sm.getName().equals(methodName)) {
				m = sm;
				break;
			}
		}
		
		return m;
	}

	private Object[] getArgs(String srvCls,String methodName, Object[] jsonArgs, ISession sess){

		if(jsonArgs== null || jsonArgs.length ==0){
			return new Object[0];
		} else {
			Class<?> srvClazz = objFactory.loadCls(srvCls);
			if(srvClazz == null) {
				throw new CommonException("Service class not found: "+srvCls);
			}
			Method m = this.getSrvMethod(srvClazz, methodName,false);
			
			Class<?>[] clses = m.getParameterTypes();
			Object[] args = new Object[clses.length];
			
			int i = 0;
			int j = 0;
			Object arg = null;
			try {
				for(; i < clses.length; i++){
					Class<?> pt = clses[i];
					if(ISession.class.isAssignableFrom(pt)) {
						args[i] = sess;
					} else {
						arg = jsonArgs[j++];
						Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(arg), pt);
						args[i] = a;
					}
				}
			} catch (Exception e) {
				logger.error(srvCls+"." + methodName + ": " +(arg == null ? "":arg),e);
			}
			return args;
		}
	
	
	}
}
