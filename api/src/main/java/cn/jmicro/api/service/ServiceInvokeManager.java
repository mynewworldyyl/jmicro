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
package cn.jmicro.api.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * 根据 ServiceItem及ServiceMethod，参数，直接调用RPC方法
 * @author Yulei Ye
 * @date 2020年3月6日
 */
@Component
public class ServiceInvokeManager {

	private final static Logger logger = LoggerFactory.getLogger(ServiceInvokeManager.class);
	private static final Class<?> TAG = ServiceInvokeManager.class;
	
	private Map<String,ServiceProxyHolder> proxes = new HashMap<>();
	
	@Inject
	private IObjectFactory of = null;
	
	@Inject
	private ServiceManager srvManager = null;
	
	@SuppressWarnings("unchecked")
	public <T> IPromise<T> call(String srvName,String ns,String ver,String method, 
			Class<?> returnParamClazz, Class<?>[] paramsCls, Object[] args) {
		IPromise<T> promise = null;
		
		String key = UniqueServiceKeyJRso.serviceName(srvName,ns,ver);
		
		ServiceProxyHolder proxy = getProxy(srvName,ns,ver);
		if(proxy == null) {
			PromiseImpl<T> p = new PromiseImpl<T>();
			p.setFail(MC.MT_SERVICE_RROXY_NOT_FOUND,"Service not found: " +key);
			p.done();
			return p;
		}
		
		Method m = proxy.getSMethod(method,returnParamClazz,paramsCls,args);
		
		if(m == null) {
			PromiseImpl<T> p = new PromiseImpl<T>();
			p.setFail(MC.MT_SERVICE_METHOD_NOT_FOUND,"Service method not found: " + key + UniqueServiceKeyJRso.SEP + method);
			p.done();
			return p;
		}
		

		final AsyncConfigJRso oldAc = JMicroContext.get().getParam(Constants.ASYNC_CONFIG,null);
		
		/*if(ac != null) {
			JMicroContext.get().setParam(Constants.ASYNC_CONFIG,ac);
		}*/
		
		try {
			
			boolean f = m.isAccessible();
			if(!f) {
				//通过Lambda动态注册的服务方法,，会报方法调用异常，应该是内部生成的类是非public导致，在此暂时做此处理
				if(proxy.getClass().getName().contains("$$Lambda$")) {
					m.setAccessible(true);
				}
			}
			
			/*if("bestHost".equals(req.getMethod())) {
				logger.info("");
			}*/
			
			Object result = m.invoke(proxy.proxy, args);
			if(!f) {
				//正常的非public方法调用不到跑到这里，所以可以直接设置即可
				m.setAccessible(f);
			}
			
			if(result != null && result instanceof IPromise) {
				promise = (IPromise<T>)result;
			}else {
				PromiseImpl<T> p = new PromiseImpl<T>();
				promise = p ;
				p.setResult((T)result);
				p.done();
			}
			
			/*if(ac != null) {
				promise.then((rst,fail,cxt)->{
					if(oldAc != null) {
						JMicroContext.get().setParam(Constants.ASYNC_CONFIG,oldAc);
					} else {
						JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
					}
				});
			}*/
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("onRequest:" +srvName + "." + m.getName()+ ",arg: "+ JsonUtils.getIns().toJson(args),e);
			PromiseImpl<T> p = new PromiseImpl<T>();
			promise = p ;
			p.setFail(MC.MT_SERVER_ERROR, e.getMessage());
			p.done();
			
			if(oldAc != null) {
				JMicroContext.get().setParam(Constants.ASYNC_CONFIG,oldAc);
			} else {
				JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
			}
		}
		return promise;
	}
	
/*	public <T> IPromise<T> call(String srvName,String ns,String ver,String method,
			Class<?> returnParamClazz,Class<?>[] paramsCls, Object[] args, Map<String,Object> opts) {
		return call( srvName, ns, ver, method, returnParamClazz,paramsCls, args, Map<String,Object> opts);
	}*/
	
	public <T> IPromise<T> call(UniqueServiceMethodKeyJRso mkey, Object[] args) {
		return call(mkey.getServiceName(),mkey.getNamespace(),mkey.getVersion(),mkey.getMethod(),
				mkey.getReturnParamClass(),mkey.getParameterClasses(),args);
	}
	
	public <T> IPromise<T> call(int mcode, Object[] args) {
		ServiceMethodJRso m = srvManager.getServiceMethodByHash(mcode);
		return call(m,args);
	}
	
	public <T> IPromise<T> call(ServiceMethodJRso sm, Object[] args) {
		return call(sm.getKey(),args);
	}
	
	public <T> IPromise<T> call(String strSmKey, Object[] args) {
		UniqueServiceMethodKeyJRso mkey = UniqueServiceMethodKeyJRso.fromKey(strSmKey);
		return call(mkey.getServiceName(),mkey.getNamespace(),mkey.getVersion(),mkey.getMethod(),
				mkey.getReturnParamClass(),mkey.getParameterClasses(),args);
	}
	
	public <T> IPromise<T> callDirect(ServiceItemJRso si, ServiceMethodJRso sm, Object[] args) {
		ServiceItemJRso oldDirectItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
		JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, si);
		IPromise<T> p = null;
		try {
			p = call(sm,args);
			p.then((rst,fail,cxt)->{
				if(oldDirectItem != null) {
					JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, oldDirectItem);
				}else {
					JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
				}
			});
			return p;
		}catch(Throwable e){
			if(p ==null) {
				PromiseImpl<T> promise = new PromiseImpl<T>();
				p = promise ;
				promise.setFail(MC.MT_SERVER_ERROR, e.getMessage());
				promise.done();
			}
			if(oldDirectItem != null) {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, oldDirectItem);
			}else {
				JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
			}
		}	
		return p;
	}
	
	
	public ServiceProxyHolder getProxy(ServiceItemJRso si) {
		if(si == null) {
			String msg = "Cannot call service for NULL ServiceItem";
			LG.log(MC.LOG_ERROR, TAG, msg);
			//MT.nonRpcEvent(si.getKey().toKey(true, true, true),MC.MT_SERVICE_ITEM_NOT_FOUND);
			throw new CommonException(msg);
		}
		String key = si.getKey().toSnv();
		ServiceProxyHolder h = proxes.get(key);
		if(h != null) return h;
		
		
		AbstractClientServiceProxyHolder p = null;

		p = of.getRemoteServie(si, null);
		if(p == null) {
			String msg = "Fail to create remote service proxy: "+key;
			LG.log(MC.LOG_ERROR, TAG, msg);
			MT.nonRpcEvent(key,MC.MT_SERVICE_ITEM_NOT_FOUND);
			throw new CommonException(msg);
		}
		
		h = new ServiceProxyHolder(p);
		proxes.put(key, h);
		return h;
	}
	
	public ServiceProxyHolder getProxy(String srvName,String ns,String ver) {
		String key = UniqueServiceKeyJRso.serviceName(srvName,ns,ver);
		ServiceProxyHolder h = proxes.get(key);
		if(h == null) {
			synchronized(key.intern()) {
				h = proxes.get(key);
				if(h == null) {
					AbstractClientServiceProxyHolder p = of.getRemoteServie(srvName,ns,ver,null);
					if(p == null) {
						String msg = "Fail to create remote service proxy: "+key;
						LG.log(MC.LOG_ERROR, TAG, msg);
						MT.nonRpcEvent(key,MC.MT_SERVICE_ITEM_NOT_FOUND);
						throw new CommonException(msg);
					}
					h = new ServiceProxyHolder(p);
					proxes.put(key, h);
				}
			}
		}
		return h;
	}
	
	public void jready() {
		this.srvManager.addListener((type,siKey,item)->{
			if(type == IListener.REMOVE || type == IListener.DATA_CHANGE) {
				String snv = item.toSnv();
				if(this.proxes.containsKey(snv)) {
					this.proxes.remove(snv);
				}
			}
		});
	}
	
	private class ServiceProxyHolder{
		private String srvKey;
		
		private String insName;
		private int clientId;
		private String insNamePrefix;
		
		private AbstractClientServiceProxyHolder proxy;
		
		private Map<String,Method> methods = new HashMap<>();
		
		public ServiceProxyHolder(AbstractClientServiceProxyHolder p) {
			this.proxy = p;
			this.init(p.getItem());
		}
		
		public void init(ServiceItemJRso si) {
			if(si == null) return;

			this.srvKey = si.serviceKey();
			this.insName = si.getInsName();
			this.clientId = si.getClientId();
			
			StringBuffer sb = new StringBuffer();
			int len = this.insName.length();
			for(int i = 0; i < len; i++ ) {
				char c = this.insName.charAt(i);
				if(Character.isDigit(c)) {
					break;
				}
				sb.append(c);
			}
			this.insNamePrefix = sb.toString();
		}
		
		public Method getSMethod(String methodName, Class<?> returnParamClazz,Class<?>[] paramsCls, Object args[]) {
			Method m = this.methods.get(methodName);
			if(m == null) {
				m = getSrvMethod0(this.proxy.getClass(),methodName,returnParamClazz,paramsCls,args);
				if(m != null) {
					methods.put(methodName, m);
				}
			}
			return m;
		}
		
		private Method getSrvMethod0(Class<?> srvCls, String methodName, Class<?> returnParamClazz,
				Class<?>[] paramsCls, Object args[]) {
			
			Method m = null;
			
			if(((paramsCls == null || paramsCls.length ==0) && args.length > 0) || (paramsCls.length != args.length)) {
				paramsCls = new Class<?>[args.length];
				for(int i = 0; i < args.length; i++) {
					Object ar = args[i];
					if(ar == null) {
						paramsCls[i] = Object.class;
					} else {
						paramsCls[i] = ar.getClass();
					}
				}
			}
			
			if(returnParamClazz == null) {
				String asyncMethod = methodName;
				if(!methodName.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
					asyncMethod = methodName + AsyncClientProxy.ASYNC_METHOD_SUBFIX;
				}
				
				try {
					m = srvCls.getMethod(asyncMethod, paramsCls);
					if(m != null) {
						return m;
					}
					
					for(Method m0 : srvCls.getMethods()){
						if(m0.getName().equals(asyncMethod)) {
							return m0;
						}
					}
					
				} catch (NoSuchMethodException | SecurityException e) {
					logger.warn(e.getMessage());
				}
				
				try {
					m = srvCls.getMethod(asyncMethod, paramsCls);
					if(m != null) {
						return m;
					}
					
					for(Method m0 : srvCls.getMethods()){
						if(m0.getName().equals(asyncMethod)) {
							return m0;
						}
					}
				} catch (NoSuchMethodException | SecurityException e) {
					logger.warn(e.getMessage());
				}
				
			} else if(returnParamClazz == IPromise.class) {
				//服务原生异步方法
				try {
					m = srvCls.getMethod(methodName, paramsCls);
					if(m != null) return m;
				} catch (NoSuchMethodException | SecurityException e) {
					logger.warn(e.getMessage());
				}
				
				for(Method m0 : srvCls.getMethods()){
					if(m0.getName().equals(methodName) && IPromise.class.isAssignableFrom(m0.getReturnType()) ) {
						return m0;
					}
				}
				
			} else {
				if(!methodName.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
					methodName = methodName + AsyncClientProxy.ASYNC_METHOD_SUBFIX;
				}
				try {
					m = srvCls.getMethod(methodName, paramsCls);
				} catch (NoSuchMethodException | SecurityException e) {
					logger.warn(e.getMessage());
				}
			}
			
			if(m != null) {
				return m;
			}
			
			for(Method m0 : srvCls.getMethods()){
				if(m0.getName().equals(methodName)) {
					return m0;
				}
			}
			
			return null;
		}
	}
	
}
