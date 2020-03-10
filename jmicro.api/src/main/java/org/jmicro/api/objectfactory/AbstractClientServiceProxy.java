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
package org.jmicro.api.objectfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:01
 */
public abstract class AbstractClientServiceProxy implements InvocationHandler,IServiceListener{

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ServiceItem item = null;
	
	private IMonitorDataSubmiter monitor;
	
	private IObjectFactory of;
	
	private Map<String,AsyncConfig> acs = null;
	
	private volatile InvocationHandler targetHandler = null;
	
	public IObjectFactory getOf() {
		return of;
	}

	public void setOf(IObjectFactory of) {
		this.of = of;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void serviceChanged(int type, ServiceItem item) {

		if(!this.serviceKey().equals(item.serviceKey())){
			throw new CommonException("Service listener give error service oriItem:"+ 
					this.getItem()==null ? serviceKey():this.getItem().getKey().toKey(true, true, true)+" newItem:"+item.getKey().toKey(true, true, true));
		}
		if(IServiceListener.ADD == type){
			logger.info("Service Item Add: cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(item);
		}else if(IServiceListener.REMOVE == type) {
			logger.info("Service Item Remove cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(null);
		}else if(IServiceListener.DATA_CHANGE == type) {
			logger.info("Service Item Change: cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(item);
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		ServiceItem si = this.item;
		if(si == null) {
			if(isUsable()) {
				String msg = "Service Item is NULL when call method ["
						+method.getName()+"] with params ["+ UniqueServiceMethodKey.paramsStr(args) +"] proxy ["
						+this.getClass().getName()+"]";
				logger.error(msg);
				throw new CommonException(msg);
			}
		}
		
		InvocationHandler h = targetHandler;
		if(h == null) {
			synchronized(this) {
				h = targetHandler;
				if(h == null) {
					String handler = si.getHandler();
					if(StringUtils.isEmpty(handler)) {
			    		handler = Constants.DEFAULT_INVOCATION_HANDLER;
			    	}
			    	h = of.getByName(handler);
			    	if(h == null) {
			    		String msg = "Handler not found when call method ["
								+method.getName()+"] with params ["+ UniqueServiceMethodKey.paramsStr(args) +"] proxy ["
								+this.getClass().getName()+"]";
						logger.error(msg);
						throw new CommonException(msg);
			    	}
			    	targetHandler = h;
				}
			}
		}
		ServiceMethod sm = si.getMethod(method.getName(), args);
		JMicroContext.get().configMonitor(sm.getMonitorEnable(), si.getMonitorEnable()); 
		JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
		JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, si);
		
		return h.invoke(proxy, method, args);
	}
	
	public boolean isUsable() {
		if(this.item != null) {
			return true;
		}

		synchronized(this) {
			if(this.item == null) {
				this.item = getItemFromRegistry();
			}
		}
		return this.item != null;
	
	}
	
	protected ServiceItem getItemFromRegistry() {
		IRegistry r = of.get(IRegistry.class);
		Set<ServiceItem> sis = r.getServices(this.getServiceName(), this.getNamespace(), this.getVersion());
		if(sis != null && !sis.isEmpty()) {
			return sis.iterator().next();
		}
		return null;
	}

	public abstract String getNamespace();
	
	public abstract String getVersion();
	
	public abstract String getServiceName();
	
	public void backupAndSetContext(){
		//System.out.println("backupAndSetContext");
		boolean breakFlag = JMicroContext.get().getBoolean(Constants.BREAKER_TEST_CONTEXT, false);
		
		Reference ref = JMicroContext.get().getParam(Constants.REF_ANNO, null);
		
		ServiceItem dsi = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
		
		AsyncConfig async = JMicroContext.get().getParam(Constants.ASYNC_CONFIG, null);
		
		JMicroContext.get().backup();
		
		if(this.monitor == null) {
			try {
	    		this.setMonitor(of.get(IMonitorDataSubmiter.class));
	    	}catch(CommonException e) {
	    		logger.error(e.getMessage());
	    	}
		}
		JMicroContext.setMonitor();
		
		//false表示不是provider端
		JMicroContext.callSideProdiver(false);
		if(breakFlag) {
			JMicroContext.get().setBoolean(Constants.BREAKER_TEST_CONTEXT, true);
		}
		if(ref != null) {
			JMicroContext.get().setParam(Constants.REF_ANNO, ref);
		}
		
		if(dsi != null) {
			JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, dsi);
		}
		
		if(async != null) {
			JMicroContext.get().setParam(Constants.ASYNC_CONFIG, async);
		}
		
	}
	
	public void restoreContext(){
		//System.out.println("restoreContext");
		JMicroContext.get().restore();
	}

	//public abstract  boolean enable();
	//public abstract void enable(boolean enable);
	
	public  void setItem(ServiceItem item){
		this.item = item;
	}
	
	public  ServiceItem getItem(){
		return this.item;
	}
	
	public String serviceKey(){
		return UniqueServiceKey.serviceName(this.getServiceName(), this.getNamespace(), this.getVersion()).toString();
	}
	
	@Override
	public int hashCode() {
		return this.serviceKey().hashCode();
	}

	public IMonitorDataSubmiter getMonitor() {
		return monitor;
	}

	public void setMonitor(IMonitorDataSubmiter monitor) {
		this.monitor = monitor;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof AbstractClientServiceProxy)){
			return false;
		}
		AbstractClientServiceProxy o = (AbstractClientServiceProxy)obj;
		return this.serviceKey().equals(o.serviceKey());
	}
	
	
	
	public void setAsyncConfig(AsyncConfig[] acs) {
		if(acs != null && acs.length > 0) {
			this.acs = new HashMap<>();
			for(AsyncConfig a : acs) {
				this.acs.put(a.getForMethod(), a);
			}
		}
	}
	
	public AsyncConfig getAcs(String mkey) {
		if(acs == null) {
			return null;
		}
		return this.acs.get(mkey);
	}
	
}