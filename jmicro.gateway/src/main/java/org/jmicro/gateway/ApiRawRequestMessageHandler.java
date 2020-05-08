
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
package org.jmicro.gateway;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.JDataInput;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.security.ActInfo;
import org.jmicro.api.security.IAccountService;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private IAccountService accountManager;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private IObjectFactory objFactory;
	
	@Inject
	private MessageServiceImpl ms;
	
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
		if(msg.getUpProtocol() == Message.PROTOCOL_JSON) {
			req = ICodecFactory.decode(codecFactory, msg.getPayload(), ApiRequest.class, msg.getUpProtocol());
			req.setArgs(getArgs(req.getServiceName(),req.getMethod(),req.getArgs(),session));
		} else {
			try {
				req = new ApiRequest();
				ji = new JDataInput((ByteBuffer)msg.getPayload());
				req.setReqId(ji.readLong());
				req.setServiceName(ji.readUTF());
				req.setNamespace(ji.readUTF());
				req.setVersion(ji.readUTF());
				req.setMethod(ji.readUTF());

				int len = ji.readUnsignedInt();
				if(len > 0) {
					for(int i=0; i< len; i++) {
						String k = ji.readUTF();
						String v = ji.readUTF();
						req.getParams().put(k, v);
					}
				}
				int argLen = ji.readUnsignedInt();
				if(argLen > 0) {
					req.setArgs(getArgs(req.getServiceName(),req.getMethod(),session,ji));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		 
		ApiResponse resp = new ApiResponse();
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
					ServerError se = new ServerError(ServerError.SE_INVLID_LOGIN_KEY,"Invalid login key!");
					resp.setResult(se);
					resp.setSuccess(false);
					doLogick = false;
				} else {
					JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
					JMicroContext.get().setObject(JMicroContext.LOGIN_ACT, ai);	
				}
			}
		}
		
		if(doLogick && MessageServiceImpl.TAG.equals(req.getServiceName())) {
			if("subscribe".equals(req.getMethod())) {
				String topic = (String)req.getArgs()[1];
				Map<String, Object> ctx = (Map<String, Object>)req.getArgs()[2];
				result = ms.subscribe((ISession)req.getArgs()[0], topic, ctx);
			} else if("unsubscribe".equals(req.getMethod())){
				result = ms.unsubscribe((int)req.getArgs()[0]);
			} else {
				logger.error("Method:"+req.getMethod()+" not found!");
				result = false;
			}
			
			resp.setResult(result);
		} else if(doLogick){
			Object srv = JMicro.getObjectFactory().getRemoteServie(req.getServiceName(), 
					req.getNamespace(), req.getVersion(),null);
			JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, session.localHost());
			JMicroContext.get().setParam(JMicroContext.LOCAL_PORT, session.localPort()+"");
			JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, session.remoteHost());
			JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, session.remotePort()+"");
			
			JMicroContext.get().mergeParams(req.getParams());
			
			if(srv != null){
				Method m = this.getSrvMethod(req.getServiceName(), req.getMethod());
				
				try {
					AbstractClientServiceProxy proxy = (AbstractClientServiceProxy)srv;
					ServiceItem si = proxy.getItem();
					if(si == null) {
						SF.doRequestLog(MonitorConstant.LOG_ERROR, TAG, null," service not found");
						throw new CommonException("Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] not found");
					}
					ServiceMethod sm = si.getMethod(req.getMethod(), m.getParameterTypes());
					if(sm == null) {
						SF.doRequestLog(MonitorConstant.LOG_ERROR, TAG, null," service method not found");
						throw new CommonException("Service mehtod ["+req.getServiceName()+"] method ["+req.getMethod()+"] not found");
					}
					
					if(SF.isLoggable(MonitorConstant.LOG_DEBUG, msg.getLogLevel())) {
						SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG, null," got request");
					}
					
					if(!sm.isNeedResponse()) {
						m.invoke(srv, req.getArgs());
						if(SF.isLoggable(MonitorConstant.LOG_DEBUG, msg.getLogLevel())) {
							SF.doRequestLog(MonitorConstant.LOG_DEBUG, TAG, null," no need response");
						}
						return;
					}
					
					result = m.invoke(srv, req.getArgs());
					resp.setResult(result);
				} catch ( SecurityException | IllegalAccessException 
						| IllegalArgumentException | InvocationTargetException | CommonException e) {
					logger.error("",e);
					result = new ServerError(0,e.getMessage());
					resp.setSuccess(false);
					resp.setResult(result);
					SF.doResponseLog(MonitorConstant.LOG_ERROR, TAG, e," service error");
				}
			} else {
				resp.setSuccess(false);
				resp.setResult(result);
			}
		}
		
		if(msg.getDownProtocol() == Message.PROTOCOL_JSON) {
			msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
		} else {
			msg.setPayload(resp.encode());
		}
		
		if(SF.isLoggable(MonitorConstant.LOG_DEBUG, msg.getLogLevel())) {
			SF.doResponseLog(MonitorConstant.LOG_DEBUG, TAG, null," one response");
		}
		session.write(msg);
		
	}

	
	private Object[] getArgs(String serviceName, String method, ISession session, JDataInput ji) {
		
		//ServiceItem item = registry.getServiceByImpl(r.getImpl());
		Class<?> srvClazz = JMicro.getObjectFactory().loadCls(serviceName);
		if(srvClazz == null) {
			throw new CommonException("Class ["+serviceName+"] not found");
		}
		
		Method m = getSrvMethod(serviceName,method);
		
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
						int len = ji.readUnsignedInt();
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
						int len = ji.readUnsignedInt();
						if(len > 0) {
							byte[] data = new byte[len];
							for(int x = 0; x< len; x++) {
								data[x] = ji.readByte();
							}
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
	
	private Method getSrvMethod(String srvCls,String methodName) {
		Class<?> srvClazz = JMicro.getObjectFactory().loadCls(srvCls);
		if(srvClazz == null) {
			throw new CommonException("Class ["+srvCls+"] not found");
		}
		
		Method m = null;
		for(Method sm : srvClazz.getMethods()){
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
			
			Object[] args = new Object[jsonArgs.length];
			Method m = this.getSrvMethod(srvCls, methodName);
			
			Class<?>[] clses = m.getParameterTypes();
			args = new Object[clses.length];
			
			int i = 0;
			int j = 0;
			try {
				for(; i < clses.length; i++){
					Class<?> pt = clses[i];
					if(ISession.class.isAssignableFrom(pt)) {
						args[i] = sess;
					} else {
						Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(jsonArgs[j++]), pt);
						args[i] = a;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return args;
		}
	
	
	}
}
