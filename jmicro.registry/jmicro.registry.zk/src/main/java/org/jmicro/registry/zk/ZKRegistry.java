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
package org.jmicro.registry.zk;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.BreakerException;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:09
 */
@Component(value=Constants.DEFAULT_REGISTRY,lazy=false)
public class ZKRegistry implements IRegistry {

	private final static Logger logger = LoggerFactory.getLogger(ZKRegistry.class);
	
	private Map<String,Set<IServiceListener>> snvListeners = new ConcurrentHashMap<>();
	
	private Map<String,Set<IServiceListener>> serviceNameListeners = new ConcurrentHashMap<>();
	
	private Map<String,ServiceItem> localRegistedItems = new ConcurrentHashMap<>();
	
	@Cfg("/ZKRegistry/openDebug")
	private boolean openDebug = false;
	
	@Cfg("/ZKRegistry/registInterval")
	private int registInterval = 1000*5;
	
	private ServiceManager srvManager;
	
	private IDataOperator dataOperator;
	
	public void init() {
		if(!Config.isClientOnly()) {
			//只做服务提供者,不需要监听服务变化
			new Thread(this::startRegisterWorker).start();
		}
		srvManager.addListener(new IServiceListener() {
			@Override
			public void serviceChanged(int type, ServiceItem item) {
				srvChange(type,item);
			}
		});
		
	}	
	
	/** +++++++++++++++++++++++Service listen START ++++++++++++++++++**/
	
	private void startRegisterWorker() {
		for(;;) {
			
			if(!Config.isClientOnly() && !localRegistedItems.isEmpty()) {
				//如果只是服消费者，则没有注册服务
				this.localRegistedItems.forEach((path,si) -> {
					if(!this.srvManager.exist(path)) {
						this.regist(si);
					}
				});
			}
			
			try {
				Thread.sleep(registInterval);
			} catch (InterruptedException e) {
				logger.error("",e);
			}
		}
	}
	
	
	private void srvChange(int type, ServiceItem item) {		
		
		Set<IServiceListener> listeners = this.serviceNameListeners.get(item.getKey().getServiceName());
		if(listeners != null && !listeners.isEmpty()) {
			listeners.forEach((l)->l.serviceChanged(type, item));
		}
		
		String key = item.getKey().toKey(false, false, false);
		listeners = snvListeners.get(key);
		if(listeners != null && !listeners.isEmpty()) {
			listeners.forEach((l)->l.serviceChanged(type, item));
		}
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.snvListeners,key,lis);	
	}

	/**
	 * 服务名称,接口名称
	 */
	@Override
	public void addServiceNameListener(String serviceName, IServiceListener lis) {
		addServiceListener(this.serviceNameListeners,serviceName,lis);
	}
	
	@Override
	public void removeServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.snvListeners,key,lis);
	}
	
	@Override
	public void removeServiceNameListener(String key, IServiceListener lis) {
		removeServiceListener(this.serviceNameListeners,key,lis);
	}
	
	private void removeServiceListener(Map<String,Set<IServiceListener>> listeners, String key,IServiceListener lis){
		if(!listeners.containsKey(key)){
			return;
		}
		
		Set<IServiceListener> l = listeners.get(key);
		if(l == null){
			return;
		}
		for(IServiceListener al : l){
			if(al == lis){
				l.remove(lis);
			}
		}
	}
	
	private void addServiceListener(Map<String,Set<IServiceListener>> listeners, String key,IServiceListener lis){

		if(listeners.containsKey(key)){
			Set<IServiceListener> l = listeners.get(key);
			boolean flag = false;
			for(IServiceListener al : l){
				if(al == lis){
					flag = true;
					break;
				}
			}
			if(!flag){
				l.add(lis);
			}
		} else {
			Set<IServiceListener> l = new HashSet<>();
			listeners.put(key, l);
			l.add(lis);
		}

		Set<ServiceItem> s = this.getServices(key);
		if(s!= null && !s.isEmpty()){
			lis.serviceChanged(IServiceListener.SERVICE_ADD, s.iterator().next());
		}
	}
	
	/** +++++++++++++++++++++++Service listen END ++++++++++++++++++**/
	

	/** +++++++++++++++++++++++Service CRUD for service exporter START++++++++++++++++++**/
	
	@Override
	public void regist(ServiceItem item) {
		
		String srvKey = item.path(Config.ServiceRegistDir);
		localRegistedItems.put(srvKey, item);
		
		this.persisFromConfig(item);
		String configKey = item.path(Config.ServiceItemCofigDir);
		if(!this.srvManager.exist(configKey)){
			this.srvManager.updateOrCreate(item,configKey, false);
		}
		
		if(srvManager.exist(srvKey)){
			srvManager.removeService(srvKey);
		}else {
			this.srvManager.updateOrCreate(item,srvKey, true);
		}
	}

	@Override
	public void unregist(ServiceItem item) {
		String key = item.path(Config.ServiceRegistDir);
		logger.debug("unregist service: "+key);
		if(srvManager.exist(key)){
			srvManager.removeService(key);
		}
		localRegistedItems.remove(key);
	}

	@Override
	public void update(ServiceItem item) {
		String key = item.path(Config.ServiceRegistDir);
		logger.debug("regist service: "+key);
		if(srvManager.exist(key)){
			srvManager.updateOrCreate(item, key, true);
			localRegistedItems.put(key, item);
		}else {
			logger.debug("update not found: "+key);
		}
	}

	/** +++++++++++++++++++++++Service crud for service provider END ++++++++++++++++++**/
	
	/** +++++++++++++++++++++++Service QUERY for consumer START ++++++++++++++++++**/
	@Override
	public Set<ServiceItem> getServices(String serviceName, String method, Object[] args
			,String namespace,String version,String transport) {
		Class<?>[] clazzes = null;
		if(args != null && args.length > 0){
			int i = 0;
			clazzes = new Class<?>[args.length];
			for(Object a : args){
				clazzes[i++] = a.getClass();
			}
		}else {
			clazzes = new Class<?>[0];
		}
		return this.getServices(serviceName, method, clazzes,namespace,version,transport);
	}
	
	@Override
	public boolean isExists(String serviceName,String namespace,String version) {
		namespace = UniqueServiceKey.namespace(namespace);
		version =  UniqueServiceKey.version(version);
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		//Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		return sis != null && !sis.isEmpty();
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName, String namespace, String version) {
		namespace = UniqueServiceKey.namespace(namespace);
		version = UniqueServiceKey.version(version);
		//Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		if(sis == null){
			return Collections.EMPTY_SET;
		}		
		return sis;
	}

	/**
	 * use for set inject
	 */
	@Override
	public Set<ServiceItem> getServices(String serviceName) {
		return this.srvManager.getServiceItems(serviceName);
	}

	@Override
	public ServiceItem getServiceByImpl(String impl) {
		for(ServiceItem si : this.srvManager.getAllItems()){
			if(this.openDebug) {
				logger.debug("Impl:"+si.getImpl());
			}
			if(si.getImpl().equals(impl)){
				return si;
			}
		}
		logger.error("Impl not found:"+impl);
		return null;
	}
	
	@Override
	public Set<ServiceItem> getServices(String serviceName,String method,Class<?>[] args
			,String namespace,String version,String transport) {
		
		namespace = UniqueServiceKey.namespace(namespace);
		version = UniqueServiceKey.version(version);
		
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		
		//this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		
		if(sis == null || sis.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		
		Set<ServiceItem> breakings = new HashSet<ServiceItem>();
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		
		String mStr = UniqueServiceMethodKey.paramsStr(args);
		for(ServiceItem si : sis) {
			if(!checkTransport(si,transport)){
				continue;
			}
			
			for(ServiceMethod sm : si.getMethods()){
				if(sm.getKey().getMethod().equals(method) 
						&& mStr.equals(sm.getKey().getParamsStr())){
					if(sm.isBreaking()){
						breakings.add(si);
					} else {
						set.add(si);
						break;
					}
				}
			}
		}
		if(set.isEmpty() && !breakings.isEmpty()){
			throw new  BreakerException("Request services is breaking",breakings);
		} else {
			return set;
		}
	}

	private boolean checkTransport(ServiceItem si,String transport) {
		if(StringUtils.isEmpty(transport)){
			return true;
		}
		return si.getServer(transport) != null;
	}

	private Set<ServiceItem> matchVersion(String serviceName,String namespace,String version){
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		String prefix = UniqueServiceKey.snnsPrefix(serviceName,namespace).toString();
		for(ServiceItem si : this.srvManager.getServiceItems(prefix)) {
			if(UniqueServiceKey.matchVersion(version,si.getKey().getVersion())) {
				set.add(si);
			}
		}
		return set;
	}

	/** +++++++++++++++++++++++Service QUERY for consumer END ++++++++++++++++++**/
	
	private void persisFromConfig(ServiceItem item){
        if(item== null){
        	logger.error("Item is NULL");
        	return;
        }
		String key = item.path(Config.ServiceItemCofigDir);
		if(this.srvManager.exist(key)){
			String data = dataOperator.getData(key);
			ServiceItem perItem = this.fromJson(data);
			item.formPersisItem(perItem);
		}
	}
	
	public void setSrvManager(ServiceManager srvManager) {
		this.srvManager = srvManager;
	}

	public void setDataOperator(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
	}

	private ServiceItem fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItem.class);
	}
	
}

