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
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * 根据 ServiceItem及ServiceMethod，参数，直接调用RPC方法
 * @author Yulei Ye
 * @date 2020年3月6日
 */
@Component
public class ServiceInvokeManager {

	private static final Class TAG = ServiceInvokeManager.class;
	
	private Map<String,AbstractClientServiceProxyHolder> proxes = new HashMap<>();
	
	@Inject
	private IObjectFactory of = null;
	
	@Inject
	private ServiceManager srvManager = null;
	
	public <T> T call(UniqueServiceMethodKey mkey, Object[] args,AsyncConfig ac) {
		Set<ServiceItem> items = this.srvManager.getServiceItems(mkey.getServiceName(), mkey.getNamespace(), mkey.getVersion());
		if(items == null || items.isEmpty()) {
			return null;
		}
		
		ServiceItem si = items.iterator().next();
		if(si == null) {
			String msg = "Service item not found for: "+mkey.toKey(false, false, false);
			SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		
		ServiceMethod sm = si.getMethod(mkey.getMethod(), mkey.getParamsStr());
		if(sm == null) {
			String msg = "Service method not found for: "+mkey.toKey(false, false, false);
			SF.eventLog(MC.MT_SERVICE_METHOD_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		return call(si,sm,args,ac);
	}
	
	public <T> T call(UniqueServiceMethodKey mkey, Object[] args) {
		return call(mkey,args,null);
	}
	
	public <T> T call(ServiceItem si, ServiceMethod sm, Object[] args) {
		return call(si,sm,args,null);
	}
	
	public <T> T callDirect(ServiceItem si, ServiceMethod sm, Object[] args) {
		ServiceItem oldDirectItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
		JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, si);
		try {
			return call(si,sm,args,null);
		}finally{
			if(oldDirectItem != null) {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, oldDirectItem);
			}else {
				JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
			}
		}	
	}
	
	public <T> T call(ServiceItem si, ServiceMethod sm, Object[] args, AsyncConfig ac) {
		
		if(si == null) {
			String msg = "Cannot call service for NULL ServiceItem";
			SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		
		AbstractClientServiceProxyHolder p = getProxy(si);
		
		Method m = null;
		try {
			Class<?>[] argTypes = UniqueServiceMethodKey.paramsClazzes(args);
			m = p.getClass().getMethod(sm.getKey().getMethod(), argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			String msg = "Service method not found: "+si.getKey().toKey(true, true, true);
			SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg,e);
		}
		
		try {
			if(ac != null) {
				JMicroContext.get().setParam(Constants.ASYNC_CONFIG,ac);
			}
			return (T) m.invoke(p, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			String msg = "Invoke service error: "+si.getKey().toKey(true, true, true)+",msg:"+e.getMessage();
			throw new CommonException(msg,e);
		} finally {
			if(ac != null) {
				JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
			}
		}
	}
	
	public <T> IPromise<T> callAsync(UniqueServiceMethodKey mkey, Object[] args) {
		ServiceItem si = getServiceItem(mkey.getServiceName(),mkey.getNamespace(),mkey.getVersion());
		ServiceMethod sm = getServiceMethod(si,mkey.getMethod(), mkey.getParamsStr());
		return callAsync(si,sm,args);
	}
	
	@SuppressWarnings("unchecked")
	public <T> IPromise<T> callAsync(ServiceItem si, ServiceMethod sm, Object[] args) {
		
		if(sm == null) {
			String msg = "Cannot call service for NULL ServiceMethod:"+si.getKey().toKey(false, false, false);
			SF.eventLog(MC.MT_SERVICE_METHOD_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		
		AbstractClientServiceProxyHolder p = getProxy(si);
		
		Method m = null;
		try {
			Class<?>[] argTypes = UniqueServiceMethodKey.paramsClazzes(args);
			String asyncName = AsyncClientUtils.genAsyncMethodName(sm.getKey().getMethod());
			m = p.getClass().getMethod(asyncName, argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			String msg = "Service method not found: "+si.getKey().toKey(true, true, true);
			SF.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg,e);
		}
		
		try {
			return (IPromise<T>) m.invoke(p, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			String msg = "Invoke service error: "+si.getKey().toKey(false, false, false)+",msg:"+e.getMessage();
			throw new CommonException(msg,e);
		} finally {
		}
	}


	private AbstractClientServiceProxyHolder getProxy(ServiceItem si) {
		if(si == null) {
			String msg = "Cannot call service for NULL ServiceItem";
			SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		
		String key = si.getKey().toKey(false, false, false);
		
		AbstractClientServiceProxyHolder p = null;
		
		if(!proxes.containsKey(key)) {
			p = of.getRemoteServie(si, null);
			if(p == null) {
				String msg = "Fail to create remote service proxy: "+key;
				SF.eventLog(MC.MT_SERVICE_RROXY_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
				throw new CommonException(msg);
			}
			proxes.put(key, p);
		} else {
			p = this.proxes.get(key);
		}
		
		return p;
	}
	
	private ServiceItem getServiceItem(String srvName,String ns,String ver) {
		Set<ServiceItem> items = this.srvManager.getServiceItems(srvName, ns,ver);
		if(items == null || items.isEmpty()) {
			String msg = "Service item not found for: "+ UniqueServiceKey.serviceName(srvName, ns, ver);
			SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		
		ServiceItem si = items.iterator().next();
		if(si == null) {
			String msg = "Service item not found for: "+ UniqueServiceKey.serviceName(srvName, ns, ver);
			SF.eventLog(MC.MT_SERVICE_ITEM_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		return si;
	}
	
	private ServiceMethod getServiceMethod(ServiceItem si, String method, String paramStr) {
		ServiceMethod sm = si.getMethod(method, paramStr);
		if(sm == null) {
			String msg = "Service method not found for: " + si.getKey().toKey(false, false, false)
					+UniqueServiceMethodKey.SEP+method + UniqueServiceMethodKey.SEP + paramStr;
			SF.eventLog(MC.MT_SERVICE_METHOD_NOT_FOUND,MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		return sm;
	}

}
