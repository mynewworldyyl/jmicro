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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.INodeListener;
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
 * 
 * @author Yulei Ye
 * @date 2018年12月3日 下午1:52:47
 */
@Component
public class ServiceManager {

	private final static Logger logger = LoggerFactory.getLogger(ServiceManager.class);
	
	//服务实例级列表
	private Map<String,ServiceItem> path2SrvItems = new HashMap<>();
	
	private Map<String,Integer> path2Hash = new HashMap<>();
	
	private HashMap<String,Set<IServiceListener>> serviceListeners = new HashMap<>();
	
	private Set<IServiceListener> listeners = new HashSet<>();
	
	private IDataOperator dataOperator;

	private INodeListener nodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				logger.error("service add "+type+",path: "+path);
			} else if(type == INodeListener.NODE_REMOVE) {
				serviceRemove(path,data);
				logger.error("service remove:"+type+",path: "+path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data,false);
		}
	};
	
	private IDataListener cfgDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data,true);
		}
	};
	
	public void init() {
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.debug("CONNECTED, add listeners");
				refleshChildren(null);
			}else if(Constants.CONN_LOST == state) {
				logger.debug("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.debug("Reconnected");
				refleshChildren(null);
			}
		});
		dataOperator.addChildrenListener(Config.ServiceRegistDir, new IChildrenListener() {
			@Override
			public void childrenChanged(String path, List<String> children) {
				refleshChildren(children);
			}
		});
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
	
	public void addListener(IServiceListener lis) {
		if(!this.listeners.contains(lis)) {
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
			dataOperator.createNode(path,data, isel);
		}
	}
	
	public void removeService(String path) {
		dataOperator.deleteNode(path);
	}
	
	public ServiceItem getItem(String path) {
		return this.path2SrvItems.get(path);
	}
	
	public Set<ServiceItem> getServiceItems(String srvPrefix) {
		Set<ServiceItem> sets = new HashSet<>();
		this.path2SrvItems.forEach((key,val) -> {
			if(key.indexOf(srvPrefix) > 0) {
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
		ServiceItem item = this.path2SrvItems.get(usm.getUsk().toKey(true, true, false));
		ServiceMethod sm = item.getMethod(usm.getMethod(), usm.getParamsStr());
		sm.setBreaking(true);
		this.updateOrCreate(item, item.getKey().toKey(true, true, false), true);
	}
	
	public void breakService(ServiceMethod sm) {
		String path = sm.getKey().getUsk().toKey(true, true, false);
		ServiceItem item = this.path2SrvItems.get(path);
		ServiceMethod sm1 = item.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
		sm1.setBreaking(true);
		this.updateOrCreate(item, path, true);
	}
	
	private void serviceRemove(String path, String data) {
		this.path2Hash.remove(path);
		ServiceItem si = this.path2SrvItems.remove(path);
		if(si == null) {
			return;
		}
		path = si.key(Config.ServiceRegistDir);
		this.notifyServiceChange(IServiceListener.SERVICE_REMOVE,si,path);
		this.dataOperator.removeDataListener(path, dataListener);
		this.dataOperator.removeDataListener(si.key(Config.ServiceItemCofigDir), cfgDataListener);
		this.dataOperator.removeNodeListener(path, this.nodeListener);
	}
	
	/**
	 *     配置改变，服务配置信息改变时，从配置信息合并到服务信息，反之则存储到配置信息
	 * @param path
	 * @param data
	 * @param isConfig true服务配置信息改变， false 服务信息改变
	 */
	private void updateItemData(String path, String data, boolean isConfig) {
		ServiceItem si = this.fromJson(data);
		
		if(isConfig) {
			ServiceItem srvItem = this.path2SrvItems.get(path);
			srvItem.formPersisItem(si);
			String srvPath = srvItem.key(Config.ServiceRegistDir);
			if(!this.isChange(srvItem, srvPath)) {
				//没有改变,接返回
				return;
			}
			this.updateOrCreate(srvItem, srvPath, true);
			si = srvItem;
			path = srvPath;
		} else {
			if(!this.isChange(si, path)) {
				//没有改变,接返回
				return;
			}
			//更新服务配置
			this.updateOrCreate(si, si.key(Config.ServiceItemCofigDir), true);
		}
		
		this.notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE, si,path);
	}
	
	private void refleshChildren(List<String> children) {
		if(children == null) {
			children = dataOperator.getChildren(Config.ServiceRegistDir);
		}
		for(String child : children) {
			refleshOneService(Config.ServiceRegistDir+"/"+ child);
		}
	}

	private ServiceItem fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItem.class);
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
	
	private boolean isChange(ServiceItem si,String path) {
		Integer hash = HashUtils.FNVHash1(JsonUtils.getIns().toJson(si));
		if( hash == this.path2Hash.get(path)) {
			//信息没变
			return false;
		}
		this.path2Hash.put(path, hash);
		return true;
	}
	
	private void refleshOneService(String path) {
		
		String data =  dataOperator.getData(path);
		ServiceItem i = this.fromJson(data);
		if(i == null){
			logger.warn("path:"+path+", data: "+data);
			return;
		}
		
		//从配置服务合并
		this.persisFromConfig(i);
		
		boolean flag = this.path2Hash.containsKey(path);
		
		if(!this.isChange(i, path)) {
			return;
		}
				
		this.path2SrvItems.put(path, i);
		
		if(flag) {
			this.notifyServiceChange(IServiceListener.SERVICE_DATA_CHANGE, i,path);
		} else {
			this.notifyServiceChange(IServiceListener.SERVICE_ADD, i,path);
			dataOperator.addNodeListener(path, nodeListener);
			dataOperator.addDataListener(path, this.dataListener);
			dataOperator.addDataListener(i.key(Config.ServiceItemCofigDir), this.cfgDataListener);
			
		}
	}
	
	/**
	 * 服务名称，名称空间，版本 三维一体服务监听
	 * @param type
	 * @param item
	 */
	private void notifyServiceChange(int type,ServiceItem item,String path){
		Set<IServiceListener> ls = this.serviceListeners.get(path);
		if(ls != null && !ls.isEmpty()){
			for(IServiceListener l : ls){
				l.serviceChanged(type, item);
			}
		}
		ls = this.listeners;
		if(ls != null && !ls.isEmpty()){
			for(IServiceListener l : ls){
				l.serviceChanged(type, item);
			}
		}
	}
	
}
