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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.Constants;
import org.jmicro.common.HashUtils;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1 服务实例级列表管理器,实现服务实例的增加,删除,修改,查询
 * 只负责服务实例级的管理，更高服务抽象由IRegistry管理，IRegistry依赖ServiceManager做服务的基本操作
 * 
 * @author Yulei Ye
 * @date 2018年12月3日 下午1:52:47
 */
@Component
public class ServiceManager {

	private final static Logger logger = LoggerFactory.getLogger(ServiceManager.class);
	
	//服务实例级列表
	private Map<String,ServiceItem> path2SrvItems = new HashMap<>();
	
	//服务hash值
	private Map<String,Integer> path2Hash = new HashMap<>();
	
	//服务监听器，监听特定路径的服务
	private HashMap<String,Set<IServiceListener>> serviceListeners = new HashMap<>();
	
	//监听全部服务
	private Set<IServiceListener> listeners = new HashSet<>();
	
	private IDataOperator dataOperator;

	/*private INodeListener nodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == IListener.SERVICE_ADD){
				logger.error("NodeListener service add "+type+",path: "+path);
				//serviceAdd(path,data);
			} else if(type == IListener.SERVICE_REMOVE) {
				serviceRemove(path);
				logger.error("service remove:"+type+",path: "+path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};*/
	
	//动态服务数据监听器
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data,false);
		}
	};
	
	//全局服务配置数据监听器
	private IDataListener cfgDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data,true);
		}
	};
	
	public void init() {
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				refleshChildren();
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				refleshChildren();
			}
		});
		
		logger.info("add listener");
		dataOperator.addChildrenListener(Config.ServiceRegistDir, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent, String child,String data) {
				if(IListener.SERVICE_ADD == type) {
					childrenAdd(parent+"/"+child,data);
				}else if(IListener.SERVICE_REMOVE == type) {
					childrenRemove(parent+"/"+child);
				}else if(IListener.SERVICE_DATA_CHANGE == type){
					//refleshChildrenUpdate(parent,child,data);
				}
			}
		});
	}
	
	private void refleshChildren() {
		Set<String> children = this.dataOperator.getChildren(Config.ServiceRegistDir);
		for(String child : children) {
			String path = Config.ServiceRegistDir+"/"+child;
			String data = this.dataOperator.getData(path);
			childrenAdd(path,data);
		}
		
	}

	protected void childrenAdd(String path, String data) {
		ServiceItem i = this.fromJson(data);
		if(i == null){
			logger.warn("Item NULL,path:{},data:{}",path,data);
			return;
		}
		
		if(this.path2Hash.containsKey(path)) {
			logger.warn("Item exists,path:{},data:{}",path,data);
			return;
		}
		
		//从配置服务合并
		//this.persisFromConfig(i);
		
		boolean flag = this.path2Hash.containsKey(path);
		
		if(!this.isChange(i, path)) {
			logger.warn("Service Item no change {}",path);
			return;
		}
				
		if(!flag) {
			logger.debug("Service Add: {}",path);
			this.notifyServiceChange(IServiceListener.SERVICE_ADD, i,path);
			//dataOperator.addNodeListener(path, nodeListener);
			dataOperator.addDataListener(path, this.dataListener);
			//dataOperator.addDataListener(i.path(Config.ServiceItemCofigDir), this.cfgDataListener);
		}
		
	}
	
	protected void childrenRemove(String path) {
		serviceRemove(path);
	}
	
	public void addServiceListener(String srvPath,IServiceListener lis) {
		HashMap<String,Set<IServiceListener>> serviceListeners = this.serviceListeners;
		if(serviceListeners.containsKey(srvPath)){
			Set<IServiceListener> l = serviceListeners.get(srvPath);
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
			serviceListeners.put(srvPath, l);
			l.add(lis);
		}

		ServiceItem si = this.path2SrvItems.get(srvPath);
		if(si != null) {
			lis.serviceChanged(IServiceListener.SERVICE_ADD, si);
		}
	}
	
	public void removeServiceListener(String key, IServiceListener lis) {
		HashMap<String,Set<IServiceListener>> serviceListeners = this.serviceListeners;
		if(!serviceListeners.containsKey(key)){
			return;
		}
		
		Set<IServiceListener> l = serviceListeners.get(key);
		if(l == null){
			return;
		}
		for(IServiceListener al : l){
			if(al == lis){
				l.remove(lis);
			}
		}
	}
	
	public synchronized void addListener(IServiceListener lis) {
		if(!this.listeners.contains(lis)) {
			if(!path2SrvItems.isEmpty()) {
				for(ServiceItem si : path2SrvItems.values()) {
					lis.serviceChanged(IServiceListener.SERVICE_ADD, si);
				}
			}
			this.listeners.add(lis);
		}
	}
	
	public void removeListener(IServiceListener lis) {
		if(this.listeners.contains(lis)) {
			this.listeners.remove(lis);
		}
	}
	
	public void setDataOperator(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
	}
	
	public void updateOrCreate(ServiceItem item,String path,boolean isel) {
		String data = JsonUtils.getIns().toJson(item);
		if(dataOperator.exist(path)){
			dataOperator.setData(path, data);
		} else {
			logger.debug("Create node: {}", path);
			dataOperator.createNode(path,data, isel);
		}
	}
	
	public void removeService(String path) {
		dataOperator.deleteNode(path);
	}
	
	public ServiceItem getItem(String path) {
		if(this.path2SrvItems.containsKey(path)) {
			return this.path2SrvItems.get(path);
		}
		String data = this.dataOperator.getData(path);
		refleshOneService(path,data);
		return this.path2SrvItems.get(path);
	}
	
	public Set<ServiceItem> getServiceItems(String srvPrefix) {
		Set<ServiceItem> sets = new HashSet<>();
		this.path2SrvItems.forEach((key,val) -> {
			//logger.debug("prefix {}, key: {}" ,srvPrefix,key);
			if(key.indexOf(srvPrefix) > 0 || key.startsWith(srvPrefix)) {
				sets.add(val);
			}
		});
		return sets;
	}
	
	public Set<ServiceItem> getAllItems() {
		Set<ServiceItem> sets = new HashSet<>();
		sets.addAll(this.path2SrvItems.values());
		return sets;
	}
	
	public boolean exist(String path) {
		return this.dataOperator.exist(path);
	}
	
	public void breakService(String key) {
		UniqueServiceMethodKey usm = UniqueServiceMethodKey.fromKey(key);
		ServiceItem item = this.path2SrvItems.get(ServiceItem.pathForKey(key));
		if(item == null) {
			logger.error("Service [{}] not found",key);
			return;
		}
		ServiceMethod sm = item.getMethod(usm.getMethod(), usm.getParamsStr());
		sm.setBreaking(true);
		this.updateOrCreate(item, item.getKey().toKey(true, true, true), true);
	}
	
	public void breakService(ServiceMethod sm) {
		String path = ServiceItem.pathForKey(sm.getKey().getUsk().toKey(true, true, true));
		ServiceItem item = this.path2SrvItems.get(path);
		if(item == null) {
			logger.error("Service [{}] not found",path);
			return;
		}
		ServiceMethod sm1 = item.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
		sm1.setBreaking(sm.isBreaking());
		if(dataOperator.exist(path)){
			dataOperator.setData(path,JsonUtils.getIns().toJson(item));
		} 
	}
	
	public ServiceItem getServiceByKey(String key) {
		String path = ServiceItem.pathForKey(key);
		ServiceItem item = this.path2SrvItems.get(path);
		return item;
	}
	
	public ServiceItem getServiceByServiceMethod(ServiceMethod sm) {
		String path = ServiceItem.pathForKey(sm.getKey().toKey(true, true, false));
		ServiceItem item = this.path2SrvItems.get(path);
		return item;
	}
	
	private synchronized void serviceRemove(String path) {
		ServiceItem si = null;
		synchronized(path2Hash) {
			this.path2Hash.remove(path);
			si = this.path2SrvItems.remove(path);
		}
		
		if(si == null) {
			return;
		}
		//path = si.path(Config.ServiceRegistDir);
		this.notifyServiceChange(IServiceListener.SERVICE_REMOVE, si, path);
		this.dataOperator.removeDataListener(path, dataListener);
		//this.dataOperator.removeDataListener(si.path(Config.ServiceItemCofigDir), cfgDataListener);
		//this.dataOperator.removeNodeListener(path, this.nodeListener);
	}
	
	/**
	 *     配置改变，服务配置信息改变时，从配置信息合并到服务信息，反之则存储到配置信息
	 * @param path
	 * @param data
	 * @param isConfig true全局服务配置信息改变， false 服务实例信息改变
	 */
	private void updateItemData(String path, String data, boolean isConfig) {
		ServiceItem si = this.fromJson(data);
		
		if(isConfig) {
			String srvPath = si.path(Config.ServiceRegistDir);
			ServiceItem srvItem = null;
			synchronized(path2Hash) {
				srvItem = this.path2SrvItems.get(srvPath);
			}
			srvItem.formPersisItem(si);
			if(!this.isChange(srvItem, srvPath)) {
				//没有改变,接返回
				return;
			}
			//this.updateOrCreate(srvItem, srvPath, true);
			si = srvItem;
			path = srvPath;
		} else {
			if(!this.isChange(si, path)) {
				//没有改变,接返回
				return;
			}
			
			//更新服务配置
			//this.updateOrCreate(si, si.path(Config.ServiceItemCofigDir), true);
		}
		
		this.notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE, si,path);
	}
	
	private ServiceItem fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItem.class);
	}
	
	private void persisFromConfig(ServiceItem item){
        if(item== null){
        	logger.error("Item is NULL");
        	return;
        }
		String key = item.path(Config.ServiceItemCofigDir);
		if(dataOperator.exist(key)){
			String data = dataOperator.getData(key);
			ServiceItem perItem = this.fromJson(data);
			item.formPersisItem(perItem);
		}
	}
	
	private boolean isChange(ServiceItem si,String path) {
		Integer hash = HashUtils.FNVHash1(JsonUtils.getIns().toJson(si));
		
		if(this.path2Hash.containsKey(path) && hash.equals(this.path2Hash.get(path))) {
			return false;
		}
		
		this.path2Hash.put(path, hash);
		this.path2SrvItems.put(path, si);
		return true;
	}
	
	private synchronized void refleshOneService(String path,String data) {
		
		ServiceItem i = this.fromJson(data);
		if(i == null){
			logger.warn("path:"+path+", data: "+data);
			return;
		}
		
		//从配置服务合并
		//this.persisFromConfig(i);
		
		boolean flag = this.path2Hash.containsKey(path);
		
		if(!this.isChange(i, path)) {
			logger.warn("Service Item no change {}",path);
			return;
		}
				
		this.path2SrvItems.put(path, i);
		
		if(flag) {
			this.notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE, i,path);
		} else {
			logger.debug("Service Add: {}",path);
			this.notifyServiceChange(IServiceListener.SERVICE_ADD, i,path);
			//dataOperator.addNodeListener(path, nodeListener);
			dataOperator.addDataListener(path, this.dataListener);
			//dataOperator.addDataListener(i.path(Config.ServiceItemCofigDir), this.cfgDataListener);
			
		}
	}
	
	/**
	 * 
	 * @param type
	 * @param item
	 */
	private void notifyServiceChange(int type,ServiceItem item,String path){
		Set<IServiceListener> ls = this.serviceListeners.get(path);
		if(ls != null && !ls.isEmpty()){
			//服务名称，名称空间，版本 三维一体服务监听
			for(IServiceListener l : ls){
				l.serviceChanged(type, item);
			}
		}
		ls = this.listeners;
		if(ls != null && !ls.isEmpty()){
			for(IServiceListener l : ls){
				//服务运行实例监听器
				l.serviceChanged(type, item);
			}
		}
	}
	
}