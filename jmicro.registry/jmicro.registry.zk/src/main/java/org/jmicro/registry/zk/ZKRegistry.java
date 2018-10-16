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

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Registry;
import org.jmicro.api.exception.FusingException;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.INodeListener;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.zk.ZKDataOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:09
 */
@Component(value=Constants.DEFAULT_REGISTRY,lazy=false,level=0)
@Registry
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
	
	//service instance path as key(key=ServiceItem.key())
	//private  Map<String,INodeListener> nodeListeners = new HashMap<>();
	@Override
	@JMethod("init")
	public void init() {
		ZKDataOperator.getIns().addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.debug("ZKRegistry CONNECTED, add listeners");
			}else if(Constants.CONN_LOST == state) {
				logger.debug("ZKRegistry DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.debug("ZKRegistry Reconnected and reregist Services");
				for(Set<ServiceItem> sis : serviceItems.values()) {
					for(ServiceItem si: sis) {
						regist(si);
					}	
				}
			}
		});
		
		ZKDataOperator.getIns().addChildrenListener(ServiceItem.ROOT, (path,children)->{
			serviceChanged(path,children);
		});
		
		List<String> children = ZKDataOperator.getIns().getChildren(ServiceItem.ROOT);
		logger.debug("Service: "+children.toString());
		serviceChanged(ServiceItem.ROOT,children);
	}	
	
	private INodeListener nodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				serviceChanged(path);
			} else if(type == INodeListener.NODE_REMOVE) {
				serviceRemove(path);
			}else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItem(path,data);
		}
	};
	
	@Override
	public void addServiceListener(String key,IServiceListener lis) {
		addServiceListener(this.keyListeners,key,lis);
		
	}

	protected void updateItem(String path, String data) {
		ServiceItem si = this.fromJson(data);
		String p = si.key(ServiceItem.ROOT);
		this.path2Items.put(p, si);
		Set<ServiceItem> items = this.serviceItems.get(si.serviceName());
		if(items != null){
			items.add(si);
		}
		notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE,si);
		notifyServiceNameChange(IServiceListener.SERVICE_DATA_CHANGE,si);
	}

	@Override
	public void removeServiceListener(String key,IServiceListener lis) {
		removeServiceListener(this.keyListeners,key,lis);
	}

	private void notifyServiceNameChange(int type,ServiceItem item){
		//接口名为KEY监听器
		Set<IServiceListener> lss = this.serviceNameListeners.get(item.getServiceName());
		if(lss != null && !lss.isEmpty()){
			for(IServiceListener l : lss){
				l.serviceChanged(type, item);
			}
		}
		
	}
	

	private void notifyServiceChange(int type,ServiceItem item){
		//key = servicename + namespace + version
		Set<IServiceListener> ls = this.keyListeners.get(item.serviceName());
		if(ls != null && !ls.isEmpty()){
			for(IServiceListener l : ls){
				l.serviceChanged(type, item);
			}
		}
	}
	
	@Override
	public void addServiceNameListener(String key, IServiceListener lis) {
		addServiceListener(this.serviceNameListeners,key,lis);
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

	@Override
	public void removeServiceNameListener(String key, IServiceListener lis) {
		removeServiceListener(this.serviceNameListeners,key,lis);
	}

	private void serviceChanged(String path, List<String> children) {		
		for(String child : children){
			serviceChanged(path+"/"+child);
		}
	}
	
	private void serviceChanged(String path) {		

		String data =  ZKDataOperator.getIns().getData(path);
		ServiceItem i = this.fromJson(data);
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
		
		ZKDataOperator.getIns().addNodeListener(path, nodeListener);
		ZKDataOperator.getIns().addDataListener(i.key(ServiceItem.PERSIS_ROOT), this.dataListener);
		
	}
	
	private void persisFromConfig(ServiceItem item){
		String key = item.key(ServiceItem.PERSIS_ROOT);
		if(ZKDataOperator.getIns().exist(key)){
			String data = ZKDataOperator.getIns().getData(key);
			ServiceItem perItem = this.fromJson(data);
			item.formPersisItem(perItem);
		}
	}
	
	private ServiceItem fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItem.class);
	}
	
	private void serviceRemove(String path) {
    	ServiceItem i = this.path2Items.remove(path);
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
    		this.notifyServiceNameChange(IServiceListener.SERVICE_REMOVE, i);
    	}
    	
    	ZKDataOperator.getIns().removeNodeListener(path, nodeListener);
    	ZKDataOperator.getIns().removeDataListener(path, dataListener);
    	
    	if(items.isEmpty()){
    		this.notifyServiceChange(IServiceListener.SERVICE_REMOVE, i);
    	}
	}

	@Override
	public void regist(ServiceItem item) {
		String key = item.key(ServiceItem.PERSIS_ROOT);
		this.persisFromConfig(item);
		
		String data = JsonUtils.getIns().toJson(item);
		if(!ZKDataOperator.getIns().exist(key)){
			ZKDataOperator.getIns().createNode(key,data, false);
		}
		
		key = item.key(ServiceItem.ROOT);
		if(ZKDataOperator.getIns().exist(key)){
			ZKDataOperator.getIns().deleteNode(key);
		}
		ZKDataOperator.getIns().createNode(key,data, true);
	}

	@Override
	public void unregist(ServiceItem item) {
		String key = item.key(ServiceItem.ROOT);
		logger.debug("unregist service: "+key);
		if(ZKDataOperator.getIns().exist(key)){
			ZKDataOperator.getIns().deleteNode(key);
		}
	}

	@Override
	public void update(ServiceItem item) {
		String key = item.key(ServiceItem.ROOT);
		logger.debug("regist service: "+key);
		if(ZKDataOperator.getIns().exist(key)){
			String data = JsonUtils.getIns().toJson(item);
			ZKDataOperator.getIns().setData(key,data);
		}else {
			logger.debug("update not found: "+key);
		}
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName, String method, Object[] args
			,String namespace,String version) {
		if(args != null && args.length > 0){
			int i = 0;
			Class<?>[] clazzes = new Class<?>[args.length];
			for(Object a : args){
				clazzes[i++] = a.getClass();
			}
			return this.getServices(serviceName, method, clazzes,namespace,version);
		}
		return Collections.EMPTY_SET;
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName,String method,Class<?>[] args
			,String namespace,String version) {
		
		namespace = ServiceItem.namespace(namespace);
		version = ServiceItem.version(version);
		Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		if(sis == null || sis.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<ServiceItem> fusings = new HashSet<ServiceItem>();
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		
		for(ServiceItem si : sis) {
			if(!si.getNamespace().equals(namespace)||
					!si.getVersion().equals(version)) {
				continue;
			}
			if(si.isFusing()){
				fusings.add(si);
				continue;
			}
			for(ServiceMethod sm : si.getMethods()){
				if(sm.getMethodName().equals(method) 
						&& ServiceMethod.methodParamsKey(args).equals(sm.getMethodParamTypes())){
					if(sm.isFusing()){
						fusings.add(si);
					}else {
						set.add(si);
						break;
					}
				}
			}
		}
		if(set.isEmpty() && !fusings.isEmpty()){
			throw new  FusingException("Request services is fusing",fusings);
		}else {
			return set;
		}
	}

	@Override
	public boolean isExist(String serviceName,String namespace,String version) {
		namespace = ServiceItem.namespace(namespace);
		version =  ServiceItem.version(version);
		Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
		if(sis == null || sis.isEmpty()) {
			return false;
		}
		
		for(ServiceItem si : sis){
			if(si.getNamespace().equals(namespace)&&
					si.getVersion().equals(version)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName, String namespace, String version) {
		Set<ServiceItem> is = new HashSet<>();
		
		if(namespace == null || version == null){
			for(String key : this.serviceItems.keySet()){
				if(key.startsWith(serviceName)){
					is.addAll(this.serviceItems.get(key));
				}
			}
		}else{
			namespace = ServiceItem.namespace(namespace);
			version = ServiceItem.version(version);
			Set<ServiceItem> sis = this.serviceItems.get(ServiceItem.serviceName(serviceName, namespace, version));
			
			if(sis == null){
				return Collections.EMPTY_SET;
			}
			
			for(ServiceItem si : sis){
				if(si.getNamespace().equals(namespace) &&
						si.getVersion().equals(version)) {
					is.add(si);
				}
			}
		}
		return is;
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
			if(si.getImpl().equals(impl)){
				return si;
			}
		}
		return null;
	}
	
}
