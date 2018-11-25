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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.BreakerException;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.INodeListener;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
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
	
	//stand on user side ,service unique name as key(key = servicename+namespace+version)
	//when the value set is NULL, should notify IServiceListener service REMOVE event
	//when the value set is NULL, and service add  to set first, should notify IServiceListener 
	//service ADD event
	private Map<String,Set<ServiceItem>> serviceItems = new ConcurrentHashMap<String,Set<ServiceItem>>();
	
	private Map<String,ServiceItem> path2Items = new HashMap<>();
	
	private Map<String,Boolean> serviceNameItems = new ConcurrentHashMap<String,Boolean>();
	
	//stand on user side ,service unique name as key(key = servicename+namespace+version)
	private HashMap<String,Set<IServiceListener>> keyListeners = new HashMap<>();
	
	private HashMap<String,Set<IServiceListener>> serviceNameListeners = new HashMap<>();
	
	//private static final long startTime = System.currentTimeMillis();
	//private static final long inactiveTimeLong = 30*1000;
	
	private Map<String,ServiceItem> localRegistedItems = new HashMap<>();
	
	@Cfg("/ZKRegistry/openDebug")
	private boolean openDebug=false;
	
	@Cfg("/ZKRegistry/registInterval")
	private int registInterval = 1000*5;
	
	private IDataOperator dataOperator;
	
	private boolean isServerOnly = Config.isServerOnly();
	
	private boolean isClientOnly = Config.isClientOnly();
	
	//service instance path as key(key=ServiceItem.key())
	//private  Map<String,INodeListener> nodeListeners = new HashMap<>();
	//@JMethod("init")
	public void init() {
		if(isServerOnly) {
			//只做服务提供者，不需要监听服务变化
			new Thread(this::startRegisterWorker).start();
			return;
		}
		
		//肯定是服务消费者
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.debug("ZKRegistry CONNECTED, add listeners");
			}else if(Constants.CONN_LOST == state) {
				logger.debug("ZKRegistry DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.debug("ZKRegistry Reconnected and reregist Services");
				/*for(Set<ServiceItem> sis : serviceItems.values()) {
					for(ServiceItem si: sis) {
						regist(si);
					}	
				}*/
			}
		});
		
		dataOperator.addChildrenListener(Config.ServiceRegistDir, (path,children)->{
			serviceChanged(path,children);
		});
		
		refreshList();
		
		new Thread(this::startRegisterWorker).start();
	}	
	
	/** +++++++++++++++++++++++Service listen START ++++++++++++++++++**/
	
	private void refreshList() {
		List<String> children = dataOperator.getChildren(Config.ServiceRegistDir);
		boolean f = true;
		for(String c : children) {
			if(!this.path2Items.containsKey(Config.ServiceRegistDir+"/"+c)) {
				f = false;
				break;
			}
		}
		if(!f) {
			serviceChanged(Config.ServiceRegistDir,children);
		}
	}
	
	private void regist2Local(String path,ServiceItem si) {
		if(this.isServerOnly) {
			//只做服务提供者，则将本地服务注册给自己，不做远程监听
			this.path2Items.put(path, si);
			if(!this.serviceItems.containsKey(si.serviceName())) {
				this.serviceItems.put(si.serviceName(), new HashSet<ServiceItem>());
			}
			Set<ServiceItem> se = this.serviceItems.get(si.serviceName());
			se.add(si);
		}
	}
	
	private void reRegistLocalServices() {
		//如果只是服消费者，则没有注册服务
		if(localRegistedItems.isEmpty()) {
			return;
		}

		this.localRegistedItems.forEach((path,si)->{
			if(!this.dataOperator.exist(path)) {
				this.regist(si);
				regist2Local(path,si);
			}
		});
	
	}
	
	private void startRegisterWorker() {
		for(;;) {
			
			if(!isClientOnly) {
				reRegistLocalServices();
			}
			
			/*if(!this.isServerOnly) {
				//如果只是服务提供者，则不需要去刷新服务列表
				refreshList();
			}*/
			
			try {
				Thread.sleep(registInterval);
			} catch (InterruptedException e) {
				logger.error("",e);
			}
		}
	}

	private INodeListener nodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				//serviceChanged(path);
			} else if(type == INodeListener.NODE_REMOVE) {
				if(!localRegistedItems.containsKey(path)) {
					if(openDebug) {
						logger.debug("nodeListener.NODE_REMOVE doremove servcie {} ",path);
					}
					serviceRemove(path);
				} else {
					if(openDebug) {
						logger.debug("nodeListener.NODE_REMOVE reregisted local service {} ",path);
						reRegistLocalServices();
					}
				}
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data);
		}
	};
	
	private void updateItemData(String configPath, String data) {
		ServiceItem si = this.fromJson(data);
		//String p = si.key(ServiceItem.ROOT);
		//this.path2Items.put(p, si);
		
		Set<ServiceItem> items = this.serviceItems.get(si.serviceName());
		if(items != null && !items.isEmpty()){
			for(ServiceItem ei: items) {
				ei.formPersisItem(si);
			}
		}
		notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE,si);
		notifyServiceNameChange(IServiceListener.SERVICE_DATA_CHANGE,si);
	}

	private void serviceChanged(String path, List<String> children) {		
		this.path2Items.clear();
		this.serviceItems.clear();
		this.serviceNameItems.clear();
		for(String child : children) {
			serviceChanged(path+"/"+child);
		}
	}
	
	private void serviceChanged(String path) {		
		
		String data =  dataOperator.getData(path);
		ServiceItem i = this.fromJson(data);
		if(i == null){
			logger.warn("path:"+path+", data: "+data);
			return;
		}
		this.persisFromConfig(i);
		
		this.path2Items.put(path, i);
		
		String serviceName = i.serviceName();
		logger.debug("service add: " + path);
		if(!serviceItems.containsKey(serviceName)){
			serviceItems.put(serviceName, new HashSet<ServiceItem>());
		}
		Set<ServiceItem> items = serviceItems.get(serviceName);
		boolean e = items.isEmpty();
		items.add(i);
		if(e){
			this.notifyServiceChange(IServiceListener.SERVICE_ADD, i);
		}
		
		if(!this.serviceNameItems.containsKey(i.getServiceName())){
			serviceNameItems.put(i.getServiceName(), true);
			this.notifyServiceNameChange(IServiceListener.SERVICE_ADD, i);
		}
		
		dataOperator.addNodeListener(path, nodeListener);
		dataOperator.addDataListener(i.key(Config.ServiceRegistDir), this.dataListener);
	}
	
	private void serviceRemove(String path) {
		logger.debug("remove service: "+path);
    	ServiceItem i = this.path2Items.remove(path);
    	if(null==i){
    		logger.debug("node have been removed: "+path);
    		return;
    	}
    	Set<ServiceItem> items = serviceItems.get(i.serviceName());
    	items.remove(i);
    	
    	String name = i.getServiceName();
    	boolean f = false;
    	for(ServiceItem si : this.path2Items.values()){
    		if(name.equals(si.getServiceName())){
    			f = true;
    			break;
    		}
    	}
    	
    	if(!f) {
    		serviceNameItems.remove(name);
    		this.notifyServiceNameChange(IServiceListener.SERVICE_REMOVE, i);
    	}
    	
    	dataOperator.removeNodeListener(path, nodeListener);
    	dataOperator.removeDataListener(path, dataListener);
    	
    	if(items.isEmpty()){
    		this.notifyServiceChange(IServiceListener.SERVICE_REMOVE, i);
    	}
	}
	
	/**
	 * 通知接级的服级的服务监听
	 * @param type
	 * @param item
	 */
	private void notifyServiceNameChange(int type,ServiceItem item){
		//接口名为KEY监听器
		Set<IServiceListener> lss = this.serviceNameListeners.get(item.getServiceName());
		if(lss != null && !lss.isEmpty()){
			for(IServiceListener l : lss){
				l.serviceChanged(type, item);
			}
		}
	}
	
	/**
	 * 服务名称，名称空间，版本 三维一体服务监听
	 * @param type
	 * @param item
	 */
	private void notifyServiceChange(int type,ServiceItem item){
		//key = servicename + namespace + version
		Set<IServiceListener> ls = this.keyListeners.get(item.serviceName());
		if(ls != null && !ls.isEmpty()){
			for(IServiceListener l : ls){
				l.serviceChanged(type, item);
			}
		}
	}
	
	/**
	 * 务名称，名称空间，版本
	 */
	@Override
	public void addServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.keyListeners,key,lis);	
	}

	/**
	 * 服务名称
	 */
	@Override
	public void addServiceNameListener(String serviceName, IServiceListener lis) {
		addServiceListener(this.serviceNameListeners,serviceName,lis);
	}
	
	@Override
	public void removeServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.keyListeners,key,lis);
	}
	
	@Override
	public void removeServiceNameListener(String key, IServiceListener lis) {
		removeServiceListener(this.serviceNameListeners,key,lis);
	}
	
	private void removeServiceListener(HashMap<String,Set<IServiceListener>> listeners, String key,IServiceListener lis){
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
	
	private void addServiceListener(HashMap<String,Set<IServiceListener>> listeners, String key,IServiceListener lis){

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
		
		String srvKey = item.key(Config.ServiceRegistDir);
		localRegistedItems.put(srvKey, item);
		
		this.persisFromConfig(item);
		String configKey = item.key(Config.ServiceItemCofigDir);
		String data = JsonUtils.getIns().toJson(item);
		if(!dataOperator.exist(configKey)){
			dataOperator.createNode(configKey,data, false);
		}
		
		if(dataOperator.exist(srvKey)){
			//删除后，会在删除事件中立即注册本地还未注册的服务，避免注册后被上次停止的服务消失事件删除
			dataOperator.deleteNode(srvKey);
			if(this.isServerOnly) {
				dataOperator.createNode(srvKey,data, true);
			}
		} else {
			dataOperator.createNode(srvKey,data, true);
		}
		regist2Local(srvKey,item);
	}

	@Override
	public void unregist(ServiceItem item) {
		String key = item.key(Config.ServiceRegistDir);
		logger.debug("unregist service: "+key);
		if(dataOperator.exist(key)){
			dataOperator.deleteNode(key);
		}
		localRegistedItems.remove(key);
	}

	@Override
	public void update(ServiceItem item) {
		String key = item.key(Config.ServiceRegistDir);
		logger.debug("regist service: "+key);
		if(dataOperator.exist(key)){
			String data = JsonUtils.getIns().toJson(item);
			dataOperator.setData(key,data);
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
		namespace = ServiceItem.namespace(namespace);
		version =  ServiceItem.version(version);
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		//Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		return sis != null && !sis.isEmpty();
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName, String namespace, String version) {
		namespace = ServiceItem.namespace(namespace);
		version = ServiceItem.version(version);
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
		Set<ServiceItem> is = new HashSet<>();
		for(String key : this.serviceItems.keySet()){
			if(key.startsWith(serviceName)){
				is.addAll(this.serviceItems.get(key));
			}
		}
		return is;
	}

	@Override
	public ServiceItem getServiceByImpl(String impl) {
		for(ServiceItem si : path2Items.values()){
			if(this.openDebug) {
				logger.debug("Impl:"+si.getImpl());
			}
			if(si.getImpl().equals(impl)){
				return si;
			}
		}
		return null;
	}
	
	@Override
	public Set<ServiceItem> getServices(String serviceName,String method,Class<?>[] args
			,String namespace,String version,String transport) {
		
		namespace = ServiceItem.namespace(namespace);
		version = ServiceItem.version(version);
		
		Set<ServiceItem> sis = matchVersion(serviceName, namespace, version);
		
		//this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		
		if(sis == null || sis.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		
		Set<ServiceItem> breakings = new HashSet<ServiceItem>();
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		
		for(ServiceItem si : sis) {
			if(si.isBreaking()){
				breakings.add(si);
				continue;
			}
			if(!checkTransport(si,transport)){
				continue;
			}
			for(ServiceMethod sm : si.getMethods()){
				if(sm.getMethodName().equals(method) 
						&& ServiceMethod.methodParamsKey(args).equals(sm.getMethodParamTypes())){
					if(sm.isBreaking()){
						breakings.add(si);
					}else {
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
		String prefix = ServiceItem.snnsPrefix(serviceName,namespace);
		for(Map.Entry<String, Set<ServiceItem>> e: this.serviceItems.entrySet()) {
			String k = e.getKey();
			if(k.startsWith(prefix) &&
					ServiceItem.matchVersion(version, k.substring(k.lastIndexOf(
							ServiceItem.KEY_SEPERATOR)+ServiceItem.KEY_SEPERATOR.length(), k.length()))) {
				set.addAll(e.getValue());
			}
		}
		return set;
	}

	/** +++++++++++++++++++++++Service QUERY for consumer END ++++++++++++++++++**/
	
	public void setDataOperator(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
	}
	
	private void persisFromConfig(ServiceItem item){
        if(item== null){
        	logger.error("Item is NULL");
        	return;
        }
		String key = item.key(Config.ServiceItemCofigDir);
		if(dataOperator.exist(key)){
			String data = dataOperator.getData(key);
			ServiceItem perItem = this.fromJson(data);
			item.formPersisItem(perItem);
		}
	}
	
	private ServiceItem fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItem.class);
	}
}
