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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.IWaitingAction;
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
 * 依赖于ServiceManager做服务的增加，删除，修改，查询操作，及对应的监听操作
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:09
 */
@Component(value=Constants.DEFAULT_REGISTRY,lazy=false)
public class ZKRegistry implements IRegistry {

	private final static Logger logger = LoggerFactory.getLogger(ZKRegistry.class);
	
	/**
	 * 根据服务servicename, namespace, version三个值组成的KEY作为监听标准，增加删除都接收通知
	 * item.serviceName() as key
	 */
	private Map<String,Set<IServiceListener>> snvListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename一个值组成的KEY作为监听标准，增加删除都接收通知
	 * item.getKey().getServiceName() as key
	 */
	private Map<String,Set<IServiceListener>> serviceNameListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename, namespace, version三个值组成的KEY作为监听标准，服务存在性监听
	 * 存在性表示第一个服务实例进来，或最后一个服务实例删除时接收到通知
	 * item.serviceName() as key
	 */
	private Map<String,Set<IServiceListener>> snvExistsListeners = new ConcurrentHashMap<>();
	
	/**
	 * 根据服务servicename, 一个值组成的KEY作为监听标准，服务存在性监听
	 * 存在性表示第一个服务实例进来，或最后一个服务实例删除时接收到通知
	 * item.getKey().getServiceName() as key
	 */
	private Map<String,Set<IServiceListener>> serviceNameExistsListeners = new ConcurrentHashMap<>();
	
	private Map<String,ServiceItem> localRegistedItems = new ConcurrentHashMap<>();
	
	//当前在线服务，servicename, namespace, version
	private Map<String,AtomicInteger> services = new ConcurrentHashMap<>();
	
	@Cfg("/ZKRegistry/openDebug")
	private boolean openDebug = false;
	
	@Cfg("/ZKRegistry/registInterval")
	private int registInterval = 1000*5;
	
	private ServiceManager srvManager;
	
	private IDataOperator dataOperator;
	
	//private long startTime = System.currentTimeMillis();
	
	private long waitingActInterval = 1000*1*10;
	
	private boolean needWaiting = true;
	
	private boolean setNeedWaiting() {
		if(needWaiting) {
			this.needWaiting = System.currentTimeMillis() - Config.getSystemStartTime() < waitingActInterval;
		}
		return this.needWaiting;
	}
	
	public void init() {
		if(!Config.isClientOnly()) {
			//只做服务提供者,不需要监听服务变化
			Thread t = new Thread(this::startRegisterWorker);
			t.setName("JMicro-ZKRegistry_regWorker");
			t.start();
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
	
	private void notifyListener(int type,ServiceItem item,Set<IServiceListener> listeners) {
		if(listeners != null && !listeners.isEmpty()) {
			listeners.forEach((l)->l.serviceChanged(type, item));
		}
	}
	
	
	private void srvChange(int type, ServiceItem item) {
		String key = item.serviceName();
		if(type == IServiceListener.SERVICE_ADD) {
			if(!services.containsKey(key)) {
				services.put(key, new AtomicInteger(0));
			}
			
			int val = services.get(key).incrementAndGet();
			if(val == 1) {
				//服务进来，服务存在性监听器
				notifyListener(type,item,snvExistsListeners.get(key));
				notifyListener(type,item,serviceNameExistsListeners.get(item.getKey().getServiceName()));
			}
			
			//全量监听
			notifyListener(type,item,snvListeners.get(key));
			notifyListener(type,item,serviceNameListeners.get(item.getKey().getServiceName()));
			
		} else if(type == IServiceListener.SERVICE_REMOVE) {
			
			if(services.get(key) == null || services.get(key).decrementAndGet() == 0) {
				//最后一个服务删除，服务存在性监听器
				notifyListener(type,item,snvExistsListeners.get(key));
				notifyListener(type,item,serviceNameExistsListeners.get(item.getKey().getServiceName()));
			}
			
			//全量监听
			notifyListener(type,item,snvListeners.get(key));
			notifyListener(type,item,serviceNameListeners.get(item.getKey().getServiceName()));
		}
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addExistsServiceNameListener(String key,IServiceListener lis) {
		addServiceListener(this.serviceNameExistsListeners,key,lis);	
	}

	@Override
	public void removeExistsServiceNameListener(String key,IServiceListener lis) {
		removeServiceListener(this.serviceNameExistsListeners,key,lis);
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addExistsServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.snvExistsListeners,key,lis);	
	}

	@Override
	public void removeExistsServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.snvExistsListeners,key,lis);
	}
	
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.snvListeners,key,lis);	
	}

	@Override
	public void removeServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.snvListeners,key,lis);
	}
	
	/**
	 * 服务名称,接口名称
	 */
	@Override
	public void addServiceNameListener(String serviceName, IServiceListener lis) {
		addServiceListener(this.serviceNameListeners,serviceName,lis);
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
		} else {
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
	public boolean isExists(String serviceName, String namespace, String version) {
		if(this.needWaiting) {
			logger.warn("Do isExists waiting get Key: {}",UniqueServiceKey.serviceName(serviceName, namespace, version));
			setNeedWaiting();
			return IWaitingAction.doAct(()->isExists0(serviceName,namespace,version),false);
		} else {
			return isExists0(serviceName,namespace,version);
		}
	}
	
	private boolean isExists0(String serviceName,final String namespace,final String version) {
		String ns = UniqueServiceKey.namespace(namespace);
		String v =  UniqueServiceKey.version(version);
		Set<ServiceItem> sis = matchVersion(serviceName, ns, v);
		return sis != null && !sis.isEmpty();
	}
	
	public boolean isExists(String serviceName) {
		Set<ServiceItem> sis = this.getServices(serviceName);
		return sis != null && !sis.isEmpty();
	}
	

	@Override
	public Set<ServiceItem> getServices(String serviceName, String namespace, String version) {
		if(this.needWaiting) {
			logger.warn("Do getServices(String serviceName, String namespace, String version) waiting get Key: {}",UniqueServiceKey.serviceName(serviceName, namespace, version));
			setNeedWaiting();
			return IWaitingAction.doAct(()->getServices0(serviceName,namespace,version),null);
		} else {
			return getServices0(serviceName,namespace,version);
		}	
	}
	
	private Set<ServiceItem> getServices0(String serviceName, String namespace, String version) {
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
		if(this.needWaiting) {
			logger.warn("Do getServices(String serviceName) waiting get serviceName:{}",serviceName);
			setNeedWaiting();
			return IWaitingAction.doAct(()->srvManager.getServiceItems(serviceName),null);
		} else {
			return this.srvManager.getServiceItems(serviceName);
		}	
	}

	@Override
	public ServiceItem getServiceByImpl(String impl) {
		if(this.needWaiting) {
			logger.warn("Do getServiceByImpl waiting get impl:{}",impl);
			setNeedWaiting();
			return IWaitingAction.doAct(()->getServiceByImpl0(impl),null);
		} else {
			return getServiceByImpl0(impl);
		}	
	}
	
	private ServiceItem getServiceByImpl0(String impl) {
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
		if(this.needWaiting) {
			logger.warn("Do getServices waiting get key:{},method:{},transport:{}",UniqueServiceKey.serviceName(serviceName, namespace, version),
					method,transport);
			setNeedWaiting();
			return IWaitingAction.doAct(
					()->getServices0(serviceName,method,args,namespace,version,transport),null);
		} else {
			return getServices0(serviceName,method,args,namespace,version,transport);
		}	
	}
	
	private Set<ServiceItem> getServices0(String serviceName,String method,Class<?>[] args
			,String namespace,String version,String transport) {
		
		namespace = UniqueServiceKey.namespace(namespace);
		version = UniqueServiceKey.version(version);
		
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		
		//this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		
		if(sis == null || sis.isEmpty()) {
			return null;
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

