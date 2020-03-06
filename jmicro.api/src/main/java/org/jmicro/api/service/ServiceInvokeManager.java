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
package org.jmicro.api.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

/**
 * 
 * 根据 ServiceItem及ServiceMethod，参数，直接调用RPC方法
 * @author Yulei Ye
 * @date 2020年3月6日
 */
@Component
public class ServiceInvokeManager {

	private static final Class TAG = ServiceInvokeManager.class;
	
	private Map<String,AbstractClientServiceProxy> proxes = new HashMap<>();
	
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
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
			throw new CommonException(msg);
		}
		
		
		ServiceMethod sm = si.getMethod(mkey.getMethod(), mkey.getParamsStr());
		if(sm == null) {
			String msg = "Service method not found for: "+mkey.toKey(false, false, false);
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
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
	
	public <T> T call(ServiceItem si, ServiceMethod sm, Object[] args,AsyncConfig ac) {

		if(si == null) {
			String msg = "Cannot call service for NULL ServiceItem";
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
			throw new CommonException(msg);
		}
		
		if(sm == null) {
			String msg = "Cannot call service for NULL ServiceMethod:"+si.getKey().toKey(false, false, false);
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
			throw new CommonException(msg);
		}
		
		String key = si.getKey().toKey(false, false, false);
		AbstractClientServiceProxy p = null;
		if(!proxes.containsKey(key)) {
			p = of.getRemoteServie(si, null, null);
			if(p == null) {
				String msg = "Fail to create remote service proxy: "+key;
				SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
				throw new CommonException(msg);
			}
			proxes.put(key, p);
		}
		
		Method m = null;
		try {
			Class[] argTypes = UniqueServiceMethodKey.paramsClazzes(args);
			m = p.getClass().getMethod(sm.getKey().getMethod(), argTypes);
			
		} catch (NoSuchMethodException | SecurityException e) {
			String msg = "Service method not found: "+key;
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
			throw new CommonException(msg,e);
		}
		
		try {
			if(ac != null) {
				JMicroContext.get().setParam(Constants.ASYNC_CONFIG,ac);
			}
			return (T) m.invoke(p, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			String msg = "Invoke service error: "+key+",msg:"+e.getMessage();
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, null, msg);
			throw new CommonException(msg,e);
		} finally {
			if(ac != null) {
				JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
			}
		}
	}

}
