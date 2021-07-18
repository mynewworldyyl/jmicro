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
package cn.jmicro.api.registry.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IWaitingAction;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exception.BreakerException;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 依赖于ServiceManager做服务的增加，删除，修改，查询操作，及对应的监听操作
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:09
 */
@Component(value=Constants.DEFAULT_REGISTRY,lazy=false)
public class RegistryImpl implements IRegistry {

	private final static Logger logger = LoggerFactory.getLogger(RegistryImpl.class);
	
	/**
	 * 根据服务servicename, namespace, version三个值组成的KEY作为监听标准，增加删除都接收通知
	 * item.serviceName() as key
	 */
	private Map<String,Set<IServiceListener>> snvKeyListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename(服务接口名称)一个值组成的KEY作为监听标准，增加删除都接收通知
	 * item.getKey().getServiceName() as key
	 */
	private Map<String,Set<IServiceListener>> serviceNameListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename, namespace, version三个值组成的KEY作为监听标准，服务存在性监听
	 * 存在性表示第一个服务实例进来，或最后一个服务实例删除时接收到通知
	 * item.serviceName() as key
	 */
	private Map<String,Set<IServiceListener>> snvKeyExistsListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename(服务接口名称), 一个值组成的KEY作为监听标准，服务存在性监听
	 * 存在性表示第一个服务实例进来，或最后一个服务实例删除时接收到通知
	 * item.getKey().getServiceName() as key
	 */
	private Map<String,Set<IServiceListener>> serviceNameExistsListeners = new ConcurrentHashMap<>();
	
	private Map<String,ServiceItemJRso> localRegistedItems = new ConcurrentHashMap<>();
	
	//当前在线服务，servicename, namespace, version
	private Map<String,AtomicInteger> servicesCounters = new ConcurrentHashMap<>();
	
	@Cfg("/ZKRegistry/openDebug")
	private boolean openDebug = false;
	
	@Cfg("/ZKRegistry/registInterval")
	private int registInterval = 1000*5;
	
	private ServiceManager srvManager;
	
	private IDataOperator dataOperator;
	
	//private long startTime = System.currentTimeMillis();
	
	private long waitingActInterval = 1000*10*3;
	
	@Cfg("/ZKRegistry/needWaiting")
	private boolean needWaiting = false;
	
	private boolean setNeedWaiting() {
		if(needWaiting) {
			this.needWaiting = TimeUtils.getCurTime() - Config.getSystemStartTime() < waitingActInterval;
		}
		return this.needWaiting;
	}
	
	@JMethod("init")
	public void init() {
		if(!Config.isClientOnly()) {
			//只做服务提供者,不需要监听服务变化
			/*Thread t = new Thread(this::startRegisterWorker);
			t.setName("JMicro-ZKRegistry_regWorker");
			t.start();*/
			
			TimerTicker.doInBaseTicker(30, "JMicro-ZKRegistry_regWorker", null, (key,att)->{
				startRegisterWorker();
			});
			
		}
		srvManager.addListener(new IServiceListener() {
			@Override
			public void serviceChanged(int type, UniqueServiceKeyJRso siKey,ServiceItemJRso si) {
				srvChange(type,siKey,si);
			}
		});
		
	}	
	
	/** +++++++++++++++++++++++Service listen START ++++++++++++++++++**/
	
	private void startRegisterWorker() {
		
		/*try {
			Thread.sleep(registInterval);
		} catch (InterruptedException e) {
			logger.error("",e);
		}*/
		
		if(!localRegistedItems.isEmpty()) {
			//如果只是服消费者，则没有注册服务
			long curTime = TimeUtils.getCurTime();
			this.localRegistedItems.forEach((path,si) -> {
				if(curTime - si.getCreatedTime() > 120000 && !this.srvManager.exist(path)) {
					this.regist(si);
				}
			});
		}
	
	}
	
	private void notifyListener(int type,UniqueServiceKeyJRso siKey,ServiceItemJRso si,Set<IServiceListener> listeners) {
		if(listeners != null && !listeners.isEmpty()) {
			listeners.forEach((l)->l.serviceChanged(type, siKey,si));
		}
	}
	
	
	private void srvChange(int type, UniqueServiceKeyJRso siKey,ServiceItemJRso si) {
		String key = siKey.serviceID();
		if(type == IServiceListener.ADD) {
			if(!servicesCounters.containsKey(key)) {
				servicesCounters.put(key, new AtomicInteger(0));
			}
			
			int val = servicesCounters.get(key).incrementAndGet();
			if(val == 1) {
				//服务进来，服务存在性监听器
				notifyListener(IServiceListener.ADD,siKey,si,snvKeyExistsListeners.get(key));
				notifyListener(IServiceListener.ADD,siKey,si,serviceNameExistsListeners.get(siKey.getServiceName()));
			}
			
			//全量监听
			notifyListener(type,siKey,si,snvKeyListeners.get(key));
			notifyListener(type,siKey,si,serviceNameListeners.get(siKey.getServiceName()));
			
		} else if(type == IServiceListener.REMOVE) {
			
			if(servicesCounters.get(key) == null || servicesCounters.get(key).decrementAndGet() == 0) {
				//最后一个服务删除，服务存在性监听器
				notifyListener(IServiceListener.REMOVE,siKey,si,snvKeyExistsListeners.get(key));
				notifyListener(IServiceListener.REMOVE,siKey,si,serviceNameExistsListeners.get(siKey.getServiceName()));
			}
			
			//全量监听
			notifyListener(type,siKey,si,snvKeyListeners.get(key));
			notifyListener(type,siKey,si,serviceNameListeners.get(siKey.getServiceName()));
		}
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addExistsServiceNameListener(String key,IServiceListener lis) {
		addServiceListener(this.serviceNameExistsListeners,AsyncClientUtils.genSyncServiceName(key),lis);	
	}

	@Override
	public void removeExistsServiceNameListener(String key,IServiceListener lis) {
		removeServiceListener(this.serviceNameExistsListeners,AsyncClientUtils.genSyncServiceName(key),lis);
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addExistsServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.snvKeyExistsListeners,key,lis);	
	}

	@Override
	public void removeExistsServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.snvKeyExistsListeners,key,lis);
	}
	
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.snvKeyListeners,key,lis);	
	}

	@Override
	public void removeServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.snvKeyListeners,key,lis);
	}
	
	/**
	 * 服务名称,接口名称
	 */
	@Override
	public void addServiceNameListener(String serviceName, IServiceListener lis) {
		addServiceListener(this.serviceNameListeners,AsyncClientUtils.genSyncServiceName(serviceName),lis);
	}
	
	@Override
	public void removeServiceNameListener(String key, IServiceListener lis) {
		removeServiceListener(this.serviceNameListeners,AsyncClientUtils.genSyncServiceName(key),lis);
	}
	
	private void removeServiceListener(Map<String,Set<IServiceListener>> listeners, String key,IServiceListener lis){
		if(!listeners.containsKey(key)){
			return;
		}
		
		Set<IServiceListener> l = listeners.get(key);
		if(l == null || l.isEmpty()){
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

		Set<UniqueServiceKeyJRso> s = this.getServices(key);
		if(s!= null && !s.isEmpty()){
			lis.serviceChanged(IServiceListener.ADD, s.iterator().next(),null);
		}
	}
	
	/** +++++++++++++++++++++++Service listen END ++++++++++++++++++**/
	

	/** +++++++++++++++++++++++Service CRUD for service exporter START++++++++++++++++++**/
	
	@Override
	public void regist(ServiceItemJRso item) {
		
		String srvKey = item.fullStringKey();
		
		if(item.getKey().getInstanceName().equals(Config.getInstanceName())) {
			localRegistedItems.put(srvKey, item);
		}
		/*this.persisFromConfig(item);
		String configKey = item.path(Config.ServiceItemCofigDir);
		if(!this.srvManager.exist(configKey)){
			this.srvManager.updateOrCreate(item,configKey, false);
		}*/
		
		/*if(srvManager.exist(srvKey)){
			srvManager.removeService(srvKey);
		} else {
			this.srvManager.updateOrCreate(item,srvKey, true);
		}*/
		logger.debug("code:" + item.getKey().getSnvHash() + ", Service: " + item.getKey().toSnv());
		
		//item.setClientId(Config.getClientId());
		//item.setActName(Config.getAccountName());
		this.srvManager.updateOrCreate(item,srvKey, true);
	}

	@Override
	public void unregist(UniqueServiceKeyJRso item) {
		String key = item.fullStringKey();
		logger.debug("unregist service: "+key);
		if(srvManager.exist(key)){
			srvManager.removeService(key);
		}
		localRegistedItems.remove(key);
	}

	@Override
	public void update(ServiceItemJRso item) {
		String key = item.fullStringKey();
		logger.debug("regist service: "+key);
		if(srvManager.exist(key)){
			srvManager.updateOrCreate(item, key, true);
			if(item.getKey().getInstanceName().equals(Config.getInstanceName())) {
				localRegistedItems.put(key, item);
			}
		} else {
			logger.debug("update not found: "+key);
		}
	}

	/** +++++++++++++++++++++++Service crud for service provider END ++++++++++++++++++**/
	
	/** +++++++++++++++++++++++Service QUERY for consumer START ++++++++++++++++++**/
	
	/*@Override
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
	}*/
	
	@Override
	public boolean isExists(String serviceName, String namespace, String version) {
		if(this.needWaiting) {
			logger.warn("Do isExists waiting get Key: {}",UniqueServiceKeyJRso.serviceName(serviceName, namespace, version));
			setNeedWaiting();
			return IWaitingAction.doAct(()->isExists0(serviceName,namespace,version),false);
		} else {
			return isExists0(serviceName,namespace,version);
		}
	}
	
	private boolean isExists0(String serviceName,final String namespace,final String version) {
		//String ns = UniqueServiceKey.namespace(namespace);
		//String v =  UniqueServiceKey.version(version);
		Set<UniqueServiceKeyJRso> sis = matchServiceItems(serviceName, namespace, version);
		return sis != null && !sis.isEmpty();
	}
	
	public boolean isExists(String serviceName) {
		Set<UniqueServiceKeyJRso> sis = this.getServices(serviceName);
		return sis != null && !sis.isEmpty();
	}
	

	@Override
	public Set<UniqueServiceKeyJRso> getServices(String serviceName, String namespace, String version) {
		if(this.needWaiting) {
			logger.warn("Do getServices(String serviceName, String namespace, String version) waiting get Key: {}",UniqueServiceKeyJRso.serviceName(serviceName, namespace, version));
			setNeedWaiting();
			return IWaitingAction.doAct(()->getServices0(serviceName,namespace,version),null);
		} else {
			return getServices0(serviceName,namespace,version);
		}	
	}
	
	
	
	@Override
	public UniqueServiceKeyJRso getServiceSingleItem(String serviceName, String namespace, String version) {
		 Set<UniqueServiceKeyJRso> sis = this.getServices(serviceName, namespace, version);
		 if(sis != null && !sis.isEmpty()) {
			 return sis.iterator().next();
		 }
		return null;
	}

	private Set<UniqueServiceKeyJRso> getServices0(String serviceName, String namespace, String version) {
		//namespace = UniqueServiceKey.namespace(namespace);
		//version = UniqueServiceKey.version(version);
		//Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		Set<UniqueServiceKeyJRso> sis = matchServiceItems(serviceName, namespace, version);
		/*if(sis == null){
			return Collections.EMPTY_SET;
		}*/		
		return sis;
	}

	/**
	 * use for set inject
	 */
	@Override
	public Set<UniqueServiceKeyJRso> getServices(String serviceName) {
		String sn = AsyncClientUtils.genSyncServiceName(serviceName);
		if(this.needWaiting) {
			logger.warn("Do getServices(String serviceName) waiting get serviceName:{}",serviceName);
			setNeedWaiting();
			return IWaitingAction.doAct(()->srvManager.getServiceItems(sn,null,null),null);
		} else {
			return this.srvManager.getServiceItems(sn,null,null);
		}	
	}
	
	@Override
	public UniqueServiceKeyJRso getService(String serviceName, int insId) {
		Set<UniqueServiceKeyJRso> items = this.getServices(serviceName);
		if(items == null || items.isEmpty()) return null;
		for(UniqueServiceKeyJRso si : items) {
			if(si.getInsId() == insId) {
				return si;
			}
		}
		return null;
	}

	/*@Override
	public UniqueServiceKeyJRso getServiceByImpl(String impl) {
		if(this.needWaiting) {
			logger.warn("Do getServiceByImpl waiting get impl:{}",impl);
			setNeedWaiting();
			return IWaitingAction.doAct(()->getServiceByImpl0(impl),null);
		} else {
			return getServiceByImpl0(impl);
		}	
	}*/
	
	@Override
	public UniqueServiceKeyJRso getServiceByCode(int code) {
		if(this.needWaiting) {
			logger.warn("Do getServiceByCode waiting get code:{}",code);
			setNeedWaiting();
			return IWaitingAction.doAct(()->getServiceByHash0(code),null);
		} else {
			return getServiceByHash0(code);
		}	
	}
	
	@Override
	public ServiceItemJRso getOwnItem(int hash) {
		if(localRegistedItems.isEmpty()) {
			return null;
		}
		
		for(ServiceItemJRso si : localRegistedItems.values()) {
			if(si.getKey().getSnvHash() == hash) {
				return si;
			}
		}
		
		return null;
	}
	
	private UniqueServiceKeyJRso getServiceByHash0(int hash) {
		for(UniqueServiceKeyJRso si : this.srvManager.getAllItems()){
			if(si.getSnvHash() == hash){
				return si;
			}
		}
		logger.error("Service with hash: "+hash+" not found!");
		return null;
	}
	
	@Override
	public Set<ServiceItemJRso> getServices(String serviceName,String method/*,Class<?>[] args*/
			,String namespace,String version,String transport) {
		if(this.needWaiting) {
			logger.warn("Do getServices waiting get key:{},method:{},transport:{}",UniqueServiceKeyJRso.serviceName(serviceName, namespace, version),
					method,transport);
			setNeedWaiting();
			return IWaitingAction.doAct(
					()->getServices0(serviceName,method,/*args,*/namespace,version,transport),null);
		} else {
			return getServices0(serviceName,method,/*args,*/namespace,version,transport);
		}	
	}
	
	private Set<ServiceItemJRso> getServices0(String serviceName,String method/*,Class<?>[] args*/
			,String namespace,String version,String transport) {
		
		//namespace = UniqueServiceKey.namespace(namespace);
		//version = UniqueServiceKey.version(version);
		
		Set<UniqueServiceKeyJRso> sis = matchServiceItems(serviceName, namespace, version);
		
		//this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		
		if(sis == null || sis.isEmpty()) {
			return null;
		}
		
		Set<ServiceItemJRso> breakings = new HashSet<>();
		Set<ServiceItemJRso> set = new HashSet<>();
		
		for(UniqueServiceKeyJRso si : sis) {
			
			ServiceItemJRso item = this.srvManager.getItem(si.fullStringKey());
			if(item == null) {
				continue;
			}
			if(!checkTransport(item,transport)){
				continue;
			}
			
			ServiceMethodJRso sm = item.getMethod(method/*, args*/);
			if(sm.isBreaking()){
				breakings.add(item);
			} else {
				set.add(item);
			}
		}
		
		if(JMicroContext.get().getBoolean(Constants.BREAKER_TEST_CONTEXT, false)) {
			return breakings;
		} else {
			if(set.isEmpty() && !breakings.isEmpty()){
				throw new  BreakerException("Request services is breaking",breakings);
			} else {
				return set;
			}
		}
		
	}

	private boolean checkTransport(ServiceItemJRso si,String transport) {
		if(StringUtils.isEmpty(transport)){
			return true;
		}
		return si.getServer(transport) != null;
	}

	private Set<UniqueServiceKeyJRso> matchServiceItems(String serviceName,String namespace,String version){
		return this.srvManager.getServiceItems(AsyncClientUtils.genSyncServiceName(serviceName),namespace,version);
	}

	/** +++++++++++++++++++++++Service QUERY for consumer END ++++++++++++++++++**/
	/*private void persisFromConfig(ServiceItemJRso item){
        if(item== null){
        	logger.error("Item is NULL");
        	return;
        }
		String key = item.path(Config.getRaftBasePath(Config.GrobalServiceRegistDir));
		if(this.srvManager.exist(key)){
			String data = dataOperator.getData(key);
			ServiceItemJRso perItem = this.fromJson(data);
			item.formPersisItem(perItem);
		}
	}*/
	
	public void setSrvManager(ServiceManager srvManager) {
		this.srvManager = srvManager;
	}

	public void setDataOperator(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
	}

	private ServiceItemJRso fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItemJRso.class);
	}
	
}

