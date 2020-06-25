
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
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
public class ApiRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRequestMessageHandler.class);
	private static final Class<?> TAG = ApiRequestMessageHandler.class;
	
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
	
	@Cfg("/ApiRequestMessageHandler/openDebug")
	private boolean openDebug = false;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_API_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		ApiRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequest.class, msg.getUpProtocol());
		
		
		ApiResponse resp = new ApiResponse();
		Object result = null;
		
		msg.setType(Constants.MSG_TYPE_API_RESP);
		resp.setReqId(req.getReqId());
		resp.setMsg(msg);
		resp.setSuccess(true);
		resp.setId(idGenerator.getLongId(ApiResponse.class));
		
		ActInfo ai = null;
		
		if(req.getParams().containsKey(JMicroContext.LOGIN_KEY)) {
			String lk = (String)req.getParams().get(JMicroContext.LOGIN_KEY);
			if(StringUtils.isNotEmpty(lk)) {
				ai = this.accountManager.getAccount(lk);
				if(ai == null) {
					ServerError se = new ServerError(ServerError.SE_INVLID_LOGIN_KEY,"Invalid login key!");
					resp.setResult(se);
					resp.setSuccess(false);
					msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
					SF.eventLog(MC.MT_INVALID_LOGIN_INFO,MC.LOG_ERROR, TAG,lk);
					session.write(msg);
					return;
				} else {
					JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
					JMicroContext.get().setAccount(ai);	
				}
			}
		}
		
		if(MessageServiceImpl.TAG.equals(req.getServiceName())) {
			if(msg.getUpProtocol() == Message.PROTOCOL_JSON) {
				req.setArgs(getArgs(req.getServiceName(),req.getMethod(),req.getArgs()));
			}
			if("subscribe".equals(req.getMethod())) {
				String topic = (String)req.getArgs()[0];
				Map<String, Object> ctx = (Map<String, Object>)req.getArgs()[1];
				result = ms.subscribe(session, topic, ctx);
			} else if("unsubscribe".equals(req.getMethod())){
				result = ms.unsubscribe((int)req.getArgs()[0]);
			} else {
				logger.error("Method:"+req.getMethod()+" not found!");
				result = false;
			}
			
			resp.setResult(result);
			msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
			if(SF.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
				SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG, TAG," one response");
			}
			session.write(msg);
			
		} else {
			Object srv = JMicro.getObjectFactory().getRemoteServie(req.getServiceName(), 
					req.getNamespace(), req.getVersion(),null);
			
			//long lid = JMicroContext.lid();
			if(msg.getUpProtocol() == Message.PROTOCOL_JSON) {
				req.setArgs(getArgs(req.getServiceName(),req.getMethod(),req.getArgs()));
			}
			
			JMicroContext.get().setParam(JMicroContext.LOCAL_HOST, session.localHost());
			JMicroContext.get().setParam(JMicroContext.LOCAL_PORT, session.localPort()+"");
			JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, session.remoteHost());
			JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, session.remotePort()+"");
			
			JMicroContext.get().mergeParams(req.getParams());
			
			if(srv != null){
				Class<?>[] clazzes = null;
				if(req.getArgs() != null && req.getArgs().length > 0){
					clazzes = new Class<?>[req.getArgs().length];
					for(int index = 0; index < req.getArgs().length; index++){
						if(req.getArgs()[index] != null) {
							clazzes[index] = req.getArgs()[index].getClass();
						}else {
							clazzes[index] = Void.class;
						}
					}
				} else {
					clazzes = new Class<?>[0];
				}
				
				try {
					AbstractClientServiceProxy proxy = (AbstractClientServiceProxy)srv;
					ServiceItem si = proxy.getItem();
					if(si == null) {
						String errMsg = "Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] not found";
						SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG," service not found");
						throw new CommonException(errMsg);
					}
					ServiceMethod sm = si.getMethod(req.getMethod(), clazzes);
					if(sm == null) {
						String errMsg = "Service mehtod ["+req.getServiceName()+"] method ["+req.getMethod()+"] not found";
						SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG,errMsg);
						throw new CommonException(errMsg);
					}
					
					Method m = srv.getClass().getMethod(req.getMethod(), clazzes);
					
					//JMicroContext.get().configMonitor(sm.getMonitorEnable(), si.getMonitorEnable());
					
					if(SF.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
						SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG, TAG," got request");
					}
					
					if(!sm.isNeedResponse()) {
						m.invoke(srv, req.getArgs());
						if(SF.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
							SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG, TAG," no need response");
						}
						return;
					}
					
					result = m.invoke(srv, req.getArgs());
					
					resp.setResult(result);
					msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
					if(SF.isLoggable(MC.LOG_DEBUG, msg.getLogLevel())) {
						SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG, TAG," one response");
					}
					session.write(msg);
				
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
						| IllegalArgumentException | InvocationTargetException | CommonException e) {
					logger.error("",e);
					result = new ServerError(0,e.getMessage());
					resp.setSuccess(false);
					resp.setResult(result);
					SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG," service error", e);
				}
			} else {
				resp.setSuccess(false);
				resp.setResult(result);
				msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
				SF.eventLog(MC.MT_SERVICE_RROXY_NOT_FOUND,MC.LOG_ERROR, TAG,req.getServiceName());
				session.write(msg);
			}
		}
		
		
	}

	
	private Object[] getArgs(String srvCls,String methodName,Object[] jsonArgs){

		if(jsonArgs== null || jsonArgs.length ==0){
			return new Object[0];
		} else {
			int argLen = jsonArgs.length;
			//ServiceItem item = registry.getServiceByImpl(r.getImpl());
			Class<?> srvClazz = JMicro.getObjectFactory().loadCls(srvCls);
			if(srvClazz == null) {
				throw new CommonException("Class ["+srvCls+"] not found");
			}
			
			Object[] args = new Object[jsonArgs.length];
			
			for(Method sm : srvClazz.getMethods()){
				if(sm.getName().equals(methodName)/* &&
						argLen == sm.getParameterCount()*/){
					Class<?>[] clses = sm.getParameterTypes();
					int i = 0;
					int j = 0;
					try {
						for(; i < argLen; i++){
							Class<?> pt = clses[i];
							if(ISession.class.isAssignableFrom(pt)) {
								continue;
							}
							Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(jsonArgs[j]), pt);
							args[j] = a;
							j++;
						}
					} catch (Exception e) {
						continue;
					}
					if( i == argLen) {
						break;
					}
				}
			}
			return args;
		}
	
	
	}
}
