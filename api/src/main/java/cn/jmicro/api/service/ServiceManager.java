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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.IExecutorInfoJMSrv;
import cn.jmicro.api.monitor.IMonitorAdapterJMSrv;
import cn.jmicro.api.monitor.IMonitorDataSubscriberJMSrv;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 1 服务实例级列表管理器,实现服务实例的增加,删除,修改,查询
 * 只负责服务实例级的管理，更高服务抽象由IRegistry管理，IRegistry依赖ServiceManager做服务的基本操作
 * 
 * @author Yulei Ye
 * @date 2018年12月3日 下午1:52:47
 */
@Component(limit2Packages="cn.jmicro")
public class ServiceManager {

	private final static Logger logger = LoggerFactory.getLogger(ServiceManager.class);
	
	private static final Set<String> excludeServices = new HashSet<>();
	
	static {
		excludeServices.add(IExecutorInfoJMSrv.class.getName());
		excludeServices.add("cn.jmicro.api.choreography.IAgentProcessService");
		excludeServices.add(IMonitorDataSubscriberJMSrv.class.getName());
		excludeServices.add(IMonitorAdapterJMSrv.class.getName());
	}
	
	private String parent = null;
	
	private Map<String,UniqueServiceKeyJRso> allPaths = new HashMap<>();
	
	//服务实例级列表
	private Map<String,ServiceItemJRso> path2SrvItems = new HashMap<>();

	private Map<String,Set<UniqueServiceMethodKeyJRso>> service2Methods = new HashMap<>();
	
	private Map<Integer,ServiceMethodJRso> methodHash2Method = new HashMap<>();
	
	private Map<String,ServiceMethodJRso> httpPath2Method = new HashMap<>();
	
	//服务hash值
	//private Map<String,Integer> path2Hash = new HashMap<>();
	
	//服务监听器，监听特定路径的服务
	private Map<String,Set<IServiceListener>> serviceListeners = Collections.synchronizedMap(new HashMap<>());
	
	//监听全部服务
	private Set<IServiceListener> listeners = new HashSet<>();
	
	private IDataOperator op;
	
	@Cfg(value="/includeServices",toValType=Cfg.VT_SPLIT)
	private Set<String> includeServices = new HashSet<>();
	
	@Cfg("/openDebug")
	private boolean openDebug = false;
	
	@Cfg("/gatewayModel")
	private boolean gatewayModel = false;
	
	//是否加载全部服务元数据
	@Cfg("/eagerLoad")
	private boolean eagLoad = false;
	
	private ReentrantReadWriteLock path2SrvItemsLocker = new ReentrantReadWriteLock();
	
	private ReentrantReadWriteLock allPathsLocker = new ReentrantReadWriteLock();
	
	private ReentrantReadWriteLock service2MethodsLocker = new ReentrantReadWriteLock();
	
	private ReentrantReadWriteLock methodHash2MethodLocker = new ReentrantReadWriteLock();

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
	/*private IDataListener cfgDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data,true);
		}
	};*/
	
	@JMethod("init")
	public void init() {
		parent = Config.getRaftBasePath(Config.ServiceRegistDir);
		op.addListener((state)->{
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
		op.addChildrenListener(parent, (type,parent,child)-> {
			child = subPath(child);
			if(IListener.ADD == type) {
				if(openDebug) {
					logger.debug("Service add,path:{}",child);
				}
				childrenAdd(child);
			}else if(IListener.REMOVE == type) {
				logger.debug("Service remove, path:{}",child);
				allPaths.remove(child);
				if(path2SrvItems.containsKey(child)) {
					serviceRemove(child);
				}
			}else if(IListener.DATA_CHANGE == type){
				logger.debug("Invalid service data change event, path:{}",child);
			}
		});
		//refleshChildren();
	}
	
	private void refleshChildren() {
		if(!this.eagLoad && path2SrvItems.isEmpty()) {
			return;
		}
		
		Set<String> children = new HashSet<String>();
		if(eagLoad) {
			children = this.op.getChildren(Config.getRaftBasePath(parent),true);
		}else {
			children.addAll(this.path2SrvItems.keySet());
		}
		
		logger.warn("clean cache data!");
		
		path2SrvItems.clear();
		allPaths.clear();
		service2Methods.clear();
		methodHash2Method.clear();
		httpPath2Method.clear();
		
		for(String child : children) {
			childrenAdd(child);
		}
	}
	
	private void parseService2Method(ServiceItemJRso item) {
		
		ReentrantReadWriteLock.ReadLock lp = service2MethodsLocker.readLock();
		try {
			lp.lock();
			String srvKey = item.serviceID();
			Set<UniqueServiceMethodKeyJRso> methods = this.service2Methods.get(srvKey);
			if(methods == null) {
				methods = new HashSet<>();
				service2Methods.put(srvKey,methods);
			}
			methods.clear();
			for(ServiceMethodJRso m : item.getMethods()) {
				methods.add(m.getKey());
			}
		}finally {
			lp.unlock();
		}
	}

	protected void childrenAdd(String child) {
		//if(this.path2SrvItems.containsKey(child)) return;
		
		UniqueServiceKeyJRso key = UniqueServiceKeyJRso.fromKey(child);
		
		if(key == null){
			logger.warn("Key NULL,path:{}",child);
			return;
		}
		
		if(key.getServiceName().equals("cn.jmicro.api.area.IAreaDataServiceJMSrv")) {
			logger.info(key.fullStringKey());
		}
		
		if(!PermissionManager.checkClientPermission(Config.getClientId(), key.getClientId())) {
			//logger.info("No permisstion for: " + path);
			//this.allPaths.put(child, false);
			return;
		}
		
		//logger.info("{}",this.toString());
		ReentrantReadWriteLock.WriteLock l = allPathsLocker.writeLock();
		try {
			l.lock();
			this.allPaths.put(child, key);
		}finally {
			l.unlock();
		}
		
		//从配置服务合并
		//this.persisFromConfig(i);
		//加载时间
		//si.setLoadTime(TimeUtils.getCurTime());
		ServiceItemJRso osi = this.path2SrvItems.get(child);
		
		loadMethodHash(key,null);
		
		if(osi == null && !eagLoad) {
			if(logger.isInfoEnabled()) {
				logger.info("Lazy load service data: " + child);
			}
			return; //延时到使用时加载
		}
		
		/*ReentrantReadWriteLock.ReadLock lp = path2SrvItemsLocker.readLock();
		try {
			lp.lock();
		}finally {
			lp.unlock();
		}*/
		
		loadItem(child,false);
		
	}
	
	private void loadMethodHash(UniqueServiceKeyJRso key,String siData) {
		if(key.getServiceName().equals("cn.jmicro.api.area.IAreaDataServiceJMSrv")) {
			logger.info(key.fullStringKey());
		}
		
		//非自身服务，也非API网关模式，
		String path = path(key.fullStringKey());
		if(siData == null) {
			siData = op.getData(path);
			if(siData == null) {
				logger.error("Null service data: " + key.fullStringKey());
				return;
			}
		}
		
		ServiceItemJRso si = JsonUtils.getIns().fromJson(siData,ServiceItemJRso.class);

		Set<Integer> methodHash = key.getMethodHash();
		for(ServiceMethodJRso sm : si.getMethods()) {
			methodHash.add(sm.getKey().getSnvHash());
		}
		notifyServiceChange(IServiceListener.ADD, si.getKey(),si,key.fullStringKey());
		op.addDataListener(path, this.dataListener);
		
	}

	private boolean loadItem(String child,boolean forceLoad) {
		//已经加载，判断是否需要更新
		String path = path(child);
		String data = this.op.getData(path);
		
		if(!isChange(data, child,forceLoad)) {
			//同时更新methodHash2Method列表
			logger.warn("Service Item no change {}",child);//没数据变动
			return false;
		}
		
		ServiceItemJRso si = JsonUtils.getIns().fromJson(data,ServiceItemJRso.class);
		ReentrantReadWriteLock.ReadLock lp = path2SrvItemsLocker.readLock();
		try {
			lp.lock();
			this.path2SrvItems.put(child,si);
		}finally {
			lp.unlock();
		}
		
		this.parseService2Method(si);
		
		if(logger.isInfoEnabled()) {
			logger.info("Load service data: {}",child);
		}
		
		notifyServiceChange(IServiceListener.ADD, si.getKey(),si,child);
		
		op.addDataListener(path, this.dataListener);
		
		return true;
	}

	public void addServiceListener(String srvPath,IServiceListener lis) {
		Map<String,Set<IServiceListener>> serviceListeners = this.serviceListeners;
		if(serviceListeners.containsKey(srvPath)){
			Set<IServiceListener> l = serviceListeners.get(srvPath);
			boolean flag = false;
			
			synchronized(l) {
				for(IServiceListener al : l){
					if(al == lis){
						flag = true;
						break;
					}
				}
				if(!flag){
					l.add(lis);
				}
			}
		} else {
			Set<IServiceListener> l = new HashSet<>();
			l.add(lis);
			serviceListeners.put(srvPath, l);
		}

		UniqueServiceKeyJRso si = this.allPaths.get(srvPath);
		if(si != null) {
			lis.serviceChanged(IServiceListener.ADD, si,this.path2SrvItems.get(srvPath));
		}
	}
	
	public void removeServiceListener(String key, IServiceListener lis) {
		Map<String,Set<IServiceListener>> serviceListeners = this.serviceListeners;
		if(!serviceListeners.containsKey(key)){
			return;
		}
		
		Set<IServiceListener> l = serviceListeners.get(key);
		if(l == null){
			serviceListeners.remove(key);
			return;
		}
		synchronized(l) {
			for(IServiceListener al : l){
				if(al == lis){
					l.remove(lis);
				}
			}
		}
	}
	
	public void addListener(IServiceListener lis) {
		if(!this.listeners.contains(lis)) {
			if(!this.allPaths.isEmpty()) {
				ReentrantReadWriteLock.ReadLock l = this.allPathsLocker.readLock();
				try {
					l.lock();
					for(UniqueServiceKeyJRso siKey : allPaths.values()) {
						lis.serviceChanged(IServiceListener.ADD, siKey,this.path2SrvItems.get(siKey.fullStringKey()));
					}
				} finally {
					l.unlock();
				}
			}
			
			synchronized(this.listeners) {
				this.listeners.add(lis);
			}
			
		}
	}
	
	public void removeListener(IServiceListener lis) {
		synchronized(this.listeners) {
			if(this.listeners.contains(lis)) {
				this.listeners.remove(lis);
			}
		}
	}
	
	public void setDataOperator(IDataOperator dataOperator) {
		this.op = dataOperator;
	}
	
	public UniqueServiceMethodKeyJRso checkConflictServiceMethodByHash(int hash,String smKey) {
		if(!this.methodHash2Method.containsKey(hash)) {
			return null;
		}
		
		ReentrantReadWriteLock.ReadLock l = service2MethodsLocker.readLock();
		try {
			l.lock();
			for(Set<UniqueServiceMethodKeyJRso> methods : service2Methods.values()) {
				for(UniqueServiceMethodKeyJRso esi : methods) {
					if(hash == esi.getSnvHash() && !smKey.equals(esi.methodID())) {
						return esi;
					}
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return null;
	}
	
	/**
	 * 查找路径最佳匹配的服务方法
	 * @param path
	 * @param httpMethod
	 * @param reqContentType
	 * @return
	 */
	public ServiceMethodJRso getMethodByHttpPath(String path) {
		if(Utils.isEmpty(path)) {
			return null;
		}
		
		path = path.trim();
		if(httpPath2Method.containsKey(path)) {
			return httpPath2Method.get(path);
		}
		
		// /a/b/c/*   /a/b/* /a/*
		// /a/b/c/cdd  匹配  /a/b/c/*
		String curMathPath = null;
		for(String p : httpPath2Method.keySet()) {
			if(!p.endsWith("*")) {
				continue;
			}

			String prefix = p.substring(0,p.length()-1);
			if(!path.startsWith(prefix)) {
				continue;
			}

			if(curMathPath == null) {
				curMathPath = p;
				continue;
			}
			
			if(prefix.length() > curMathPath.length()) {
				curMathPath = p;
			}
		}
		
		if(curMathPath != null) {
			return httpPath2Method.get(curMathPath);
		}
		
		return null;
	}
	
	public ServiceMethodJRso getServiceMethodByHash(int hash) {
		
		ServiceMethodJRso sm = methodHash2Method.get(hash);
		if(sm != null) return sm;
		
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		UniqueServiceKeyJRso siKey = null;
		try {
			//找方法对应的服务键
			l.lock();
			for(UniqueServiceKeyJRso sik : allPaths.values()) {
				if(sik.getMethodHash().contains(hash)) {
					siKey = sik;
					break;
				}
			}
		} finally {
			l.unlock();
		}
		
		if(siKey == null) {
			
			logger.error("Service not found for method hash: " + hash);
			return null;
		}
		
		//加载全量服务信息
		this.loadItem(siKey.fullStringKey(),true);
		
		return this.methodHash2Method.get(hash);
		
	}
	
	public ServiceMethodJRso getServiceMethodByKey(UniqueServiceMethodKeyJRso key) {
		ServiceItemJRso item = this.getServiceByKey(key.getUsk().fullStringKey());
		if(item == null) {
			this.loadItem(key.getUsk().fullStringKey(),true);
			item = this.getServiceByKey(key.getUsk().fullStringKey());
			if(item == null) return null;
		};
		return item.getMethod(key.getMethod());
	}
	
	
	/*public UniqueServiceMethodKeyJRso getServiceMethodWithHashBySearch(int hash) {
		ReentrantReadWriteLock.ReadLock l = service2MethodsLocker.readLock();
		try {
			l.lock();
			for(Set<UniqueServiceMethodKeyJRso> i : service2Methods.values()) {
				for(UniqueServiceMethodKeyJRso s : i) {
					if(s.getSnvHash() == hash) {
						return s;
					}
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return null;
	}*/
	
	public void updateOrCreate(ServiceItemJRso item,String path,boolean isel) {
		String data = JsonUtils.getIns().toJson(item);
		String p = path(path);
		if(op.exist(p)){
			op.setData(p, data);
		} else {
			logger.debug("Create node: {}", p);
			op.createNodeOrSetData(p,data, isel);
		}
	}
	
	private String path(String path) {
		if(path.startsWith(parent)) return path;
		return parent+"/"+path;
	}

	public void removeService(String path) {
		op.deleteNode(path(path));
	}
	
	public ServiceItemJRso getItem(String child) {
		if(this.path2SrvItems.containsKey(child)) {
			return this.path2SrvItems.get(child);
		}
		
		if(!this.allPaths.containsKey(child)) {
			return null;//无此服务存在
		}
		
		this.loadItem(child,true);
		return this.path2SrvItems.get(child);
	}
	
	public Set<UniqueServiceKeyJRso> getServiceItems(String serviceName,String namespace,String version) {
		if(StringUtils.isEmpty(serviceName)) {
			throw new CommonException("Service Name cannot be null");
		}
		Set<UniqueServiceKeyJRso> sets = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			//logger.info(this.toString());
			l.lock();
			allPaths.forEach((key,uk) -> {
				//logger.debug("key: {}" ,key);
				if(uk.getServiceName().equals(serviceName)) {
					if(StringUtils.isEmpty(version) && StringUtils.isEmpty(namespace)) {
						sets.add(uk);
					}else if(UniqueServiceKeyJRso.matchVersion(version,uk.getVersion())
						&& UniqueServiceKeyJRso.matchNamespace(namespace,uk.getNamespace())) {
						sets.add(uk);
						}
				}
			});
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		
		return sets;
	}
	
	public Set<ServiceItemJRso> getServiceItems(String serviceName,String namespace,String version,String insName) {
		if(StringUtils.isEmpty(serviceName)) {
			throw new CommonException("Service Name cannot be null");
		}
		Set<ServiceItemJRso> sets = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = path2SrvItemsLocker.readLock();
		try {
			l.lock();
			this.path2SrvItems.forEach((key,si) -> {
				//logger.debug("key: {}" ,key);
				if(si.getKey().getServiceName().equals(serviceName) && si.getKey().getInstanceName().equals(insName)) {
					if(StringUtils.isEmpty(version) && StringUtils.isEmpty(namespace)) {
						sets.add(si);
					}else if(UniqueServiceKeyJRso.matchVersion(version,si.getKey().getVersion())
						&& UniqueServiceKeyJRso.matchNamespace(namespace,si.getKey().getNamespace())) {
						sets.add(si);
					}
				}
			});
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		
		return sets;
	}
	
	
	public Set<ServiceItemJRso> getItemsByInstanceName(String instanceName) {
		Set<ServiceItemJRso> sets = new HashSet<>();
		if(StringUtils.isEmpty(instanceName)) {
			logger.error("getItemsByInstanceName instance is NULL {} and return NULL items list",instanceName);
			return sets;
		}
		instanceName = instanceName.trim();
		
		ReentrantReadWriteLock.ReadLock l = path2SrvItemsLocker.readLock();
		try {
			l.lock();
			for(ServiceItemJRso i : path2SrvItems.values()) {
				if(instanceName.equals(i.getKey().getInstanceName())) {
					sets.add(i);
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		
		return sets;
	}
	
	public Set<UniqueServiceKeyJRso> getAllItems() {
		Set<UniqueServiceKeyJRso> sets = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			l.lock();
			sets.addAll(this.allPaths.values());
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return sets;
	}
	
	public Set<UniqueServiceMethodKeyJRso> getAllServiceMethods(String serviceKey) {
		Set<UniqueServiceMethodKeyJRso> set = this.service2Methods.get(serviceKey);
		if(set == null) return null;
		return Collections.unmodifiableSet(set);
	}
	
	public UniqueServiceMethodKeyJRso getServiceMethod(String serviceKey,String methodName) {
		Set<UniqueServiceMethodKeyJRso> set = this.service2Methods.get(serviceKey);
		if(set == null) return null;
		for(UniqueServiceMethodKeyJRso sm : set) {
			if(methodName.equals(sm.getMethod())) {
				return sm;
			}
		}
		return null;
	}
	
	
	public UniqueServiceMethodKeyJRso getServiceMethodKey(String fullKey,String methodName) {
		
		ReentrantReadWriteLock.ReadLock l = service2MethodsLocker.readLock();
		try {
			l.lock();
			Set<UniqueServiceMethodKeyJRso> set = this.service2Methods.get(fullKey);
			for(UniqueServiceMethodKeyJRso mk : set) {
				if(mk.getMethod().equals(methodName)) {
					return mk;
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return null;
	}
	
	public Set<String> serviceNames(String prefix,int loginAccountId) {
		Set<String> sns = new HashSet<>();
		boolean ep = Utils.isEmpty(prefix);
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			l.lock();
			for(UniqueServiceKeyJRso i : allPaths.values()) {
				if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId()) 
						&& (ep || i.getServiceName().startsWith(prefix))) {
					sns.add(i.getServiceName());
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return sns;
	}
	
	public Set<String> serviceVersions(String srvName,int loginAccountId) {
		Set<String> sns = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			l.lock();
			if(Utils.isEmpty(srvName)) {
				for(UniqueServiceKeyJRso i : allPaths.values()) {
					if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId())) {
						sns.add(i.getVersion());
					}
				}
			}else {
				for(UniqueServiceKeyJRso i : allPaths.values()) {
					if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId()) && 
							srvName.equals(i.getServiceName())) {
						sns.add(i.getVersion());
					}
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return sns;
	}
	
	public Set<String> serviceNamespaces(String srvName,int loginAccountId) {
		Set<String> sns = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			l.lock();
			if(Utils.isEmpty(srvName)) {
				for(UniqueServiceKeyJRso i : allPaths.values()) {
					if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId())) {
						sns.add(i.getNamespace());
					}
				}
			}else {
				for(UniqueServiceKeyJRso i : allPaths.values()) {
					if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId()) 
							&& srvName.equals(i.getServiceName())) {
						sns.add(i.getNamespace());
					}
				}
			}
			
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return sns;
	}
	
	public Set<String> serviceMethods(String srvName, int loginAccountId) {
		Set<String> sns = new HashSet<>();

		if (Utils.isEmpty(srvName)) {
			ReentrantReadWriteLock.ReadLock l = path2SrvItemsLocker.readLock();
			try {

				l.lock();
				// 引起全量服务加载
				if (!this.eagLoad) {
					logger.warn(
							"Do load all service data by get all service method op,loginAccountId: " + loginAccountId);
					this.eagLoad = true;
					this.refleshChildren();
				}

				Set<ServiceMethodJRso> ms = null;
				for (ServiceItemJRso i : path2SrvItems.values()) {
					if (PermissionManager.checkClientPermission(loginAccountId, i.getClientId())) {
						ms = i.getMethods();
						if (ms != null && ms.size() > 0) {
							for (ServiceMethodJRso m : ms) {
								sns.add(m.getKey().getMethod());
							}
						}
					}

				}

			} finally {
				l.unlock();
			}
		} else {

			ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
			try {

				l.lock();
				UniqueServiceKeyJRso skey = null;
				for (UniqueServiceKeyJRso siKey : allPaths.values()) {
					if (siKey.getServiceName().equals(srvName)
							&& PermissionManager.checkClientPermission(loginAccountId, siKey.getClientId())) {
						skey = siKey;
						break;
					}
				}

				if (skey == null) {
					return sns;
				}

				ServiceItemJRso si = path2SrvItems.get(skey.fullStringKey());
				if (si == null) {
					this.loadItem(skey.fullStringKey(),true);
					si = path2SrvItems.get(skey.fullStringKey());
					if (si == null) {
						return sns;
					}
				}
				Set<ServiceMethodJRso> ms = si.getMethods();
				if (ms != null && ms.size() > 0) {
					for (ServiceMethodJRso m : ms) {
						sns.add(m.getKey().getMethod());
					}
				}
			} finally {
				l.unlock();
			}
		}
		return sns;
	}
	
	public Set<String> serviceInstances(String srvName,int loginAccountId) {
		Set<String> sns = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = allPathsLocker.readLock();
		try {
			l.lock();
			boolean f = Utils.isEmpty(srvName);
			for(UniqueServiceKeyJRso i : this.allPaths.values()) {
				if(PermissionManager.checkClientPermission(loginAccountId, i.getClientId())) {
					if(f) {
						sns.add(i.getInstanceName());
					}else if(srvName.equals(i.getServiceName())){
						sns.add(i.getInstanceName());
					}
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		return sns;
	}
	
	public boolean exist(String path) {
		if(!path.startsWith(parent)) {
			path = path(path);
		}
		return this.op.exist(path);
	}
	
	public void breakService(String methodKey) {
		UniqueServiceMethodKeyJRso usm = UniqueServiceMethodKeyJRso.fromKey(methodKey);
		breakService(usm);
	}
	
	public void breakService(UniqueServiceMethodKeyJRso usm) {
		String siKey = usm.getUsk().fullStringKey();
		ServiceItemJRso item = this.path2SrvItems.get(siKey);
		if(item == null) {
			 this.loadItem(siKey,true);
			 item = this.path2SrvItems.get(siKey);
		}
		
		if(item == null) return;
		
		ServiceMethodJRso sm = item.getMethod(usm.getMethod(), usm.getParamsStr());
		sm.setBreaking(true);
		this.updateOrCreate(item, siKey, true);
	}
	
	private ServiceMethodJRso getSM(ServiceMethodJRso m) {
		String child = m.getKey().getUsk().fullStringKey();
		ServiceItemJRso item = this.path2SrvItems.get(child);
		if(item == null) {
			loadItem(child,true);
			item = this.path2SrvItems.get(child);
			if(item == null) {
				logger.error("Service [{}] not found",child);
				return null;
			}
		}
		return item.getMethod(m.getKey().getMethod(), m.getKey().getParamsStr());
	}
	
	public void setMonitorable(ServiceMethodJRso sm,int isMo) {
		ServiceMethodJRso sm1 = this.getSM(sm);
		if(sm1 != null) {
			if(isMo != sm1.getMonitorEnable()) {
				sm1.setMonitorEnable(isMo);
				String child = sm1.getKey().getUsk().fullStringKey();
				ServiceItemJRso item = this.path2SrvItems.get(child);
				String path = path(child);
				if(op.exist(path)){
					op.setData(path,JsonUtils.getIns().toJson(item));
				} 
			}
		}
	}
	 
	public ServiceItemJRso getServiceByKey(String key) {
		ServiceItemJRso item = this.path2SrvItems.get(key);
		if(item == null) {
			loadItem(key,true);
		}
		return this.path2SrvItems.get(key);
	}
	
	public ServiceItemJRso getServiceByKey(UniqueServiceKeyJRso siKey) {
		return getServiceByKey(siKey.fullStringKey());
	}
	
	public ServiceItemJRso getServiceByServiceMethod(ServiceMethodJRso sm) {
		String path = sm.getKey().getUsk().fullStringKey();
		ServiceItemJRso item = this.path2SrvItems.get(path);
		return item;
	}
	
	/*
	自身实例的服务，全部要缓存起来
	 */
	private boolean myService(UniqueServiceKeyJRso uk/*,boolean external*/) {
		return uk != null && uk.getInstanceName().equals(Config.getInstanceName());
	}
	
	/*
	 * API网关模式，并且服务外部可访问，则要缓存
	 */
	private boolean apiGateway(boolean external) {
		return this.gatewayModel && external;
	}
	
	private void serviceRemove(String child) {
		
		UniqueServiceKeyJRso uk = this.allPaths.remove(child);
		
		//boolean isMy = this.myService(uk);
		
		ServiceItemJRso si = path2SrvItems.remove(child);
		
		Set<UniqueServiceMethodKeyJRso> methods = service2Methods.remove(child);
		
		if(uk == null) {
			return;
		}
		
		String fpath = path(child);
		ReentrantReadWriteLock.WriteLock l = service2MethodsLocker.writeLock();
		
		try {
			l.lock();
			if(si != null && methods != null && !methods.isEmpty() && (this.myService(uk) || this.gatewayModel)) {
				//存储时已经保证不存在全局性重复hash
				/*Set<UniqueServiceKeyJRso> items = this.getServiceItems(si.getKey().getServiceName(),
						si.getKey().getNamespace(), si.getKey().getVersion());*/
				for(UniqueServiceMethodKeyJRso sm : methods) {
					ServiceMethodJRso smj = this.methodHash2Method.remove(sm.getSnvHash());
					if(smj != null && !Utils.isEmpty(smj.getHttpPath())) {
						this.httpPath2Method.remove(smj.getHttpPath());
					}
				}
			}
		} finally {
			if(l != null) {
				l.unlock();
			}
		}
		
		//logger.warn("Remove service:{}",path);
		//path = si.path(Config.ServiceRegistDir);
		notifyServiceChange(IServiceListener.REMOVE, si.getKey(),si, child);
		op.removeDataListener(fpath, dataListener);
		
		//this.dataOperator.removeDataListener(si.path(Config.ServiceItemCofigDir), cfgDataListener);
		//this.dataOperator.removeNodeListener(path, this.nodeListener);
	}
	
	/**
	 * 配置改变，服务配置信息改变时，从配置信息合并到服务信息，反之则存储到配置信息
	 * @param path
	 * @param data
	 * @param isConfig true全局服务配置信息改变， false 服务实例信息改变
	 */
	private void updateItemData(String child, String data, boolean isConfig) {
		ServiceItemJRso si = this.fromJson(data);

		if(!PermissionManager.checkClientPermission(Config.getClientId(),si.getClientId())) {
			return;
		}
		
		child = subPath(child);
		
		ServiceItemJRso srvItem = this.path2SrvItems.get(child);
		
		if(srvItem == null) {
			childrenAdd(child);
		} else {
			srvItem.formPersisItem(si);
			if(this.isChange(data, child,false)) {
				//this.updateOrCreate(srvItem, srvPath, true);
				si = srvItem;
				this.notifyServiceChange(IServiceListener.DATA_CHANGE, srvItem.getKey(),srvItem,child);
			}
		}
		
	}
	
	private String subPath(String child) {
		if(!child.startsWith(parent)) return child;
		return child.substring(parent.length()+1);
	}

	private ServiceItemJRso fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItemJRso.class);
	}
	
	private  boolean isChange(String ndata,String child,boolean forceLoad) {
		
		Integer nhash = HashUtils.FNVHash1(ndata);
		
		ServiceItemJRso osi = this.path2SrvItems.get(child);
		Integer ohash = 0;
		String odata = null;
		if(osi != null) {
			 odata = JsonUtils.getIns().toJson(osi);
			 ohash = HashUtils.FNVHash1(odata);
		}
		
		if(ohash == nhash) {
			return false;
		}
		
		ServiceItemJRso nsi = JsonUtils.getIns().fromJson(ndata, ServiceItemJRso.class);
		logger.info("Service added, Code: " + nhash + ", Service: " + nsi.getKey().toSnv());
		
		boolean isMy = this.myService(nsi.getKey());
		
		ReentrantReadWriteLock.WriteLock l = methodHash2MethodLocker.writeLock();
		try {
			l.lock();
			
			if(forceLoad || isMy || this.gatewayModel && nsi.isExternal()) {
				for(ServiceMethodJRso sm : nsi.getMethods()) {
					int h = sm.getKey().getSnvHash();
					if(!this.methodHash2Method.containsKey(h)) {
						//logger.info("Hash: " + h + " => " + sm.getKey().toKey(true, true, true));
						this.methodHash2Method.put(h, sm);
					} else {
						//检查是否是方法Hash冲突
						String smKey = sm.getKey().methodID();
						UniqueServiceMethodKeyJRso conflichMethod = this.checkConflictServiceMethodByHash(h, smKey);
						if(conflichMethod != null) {
							String msg = "Service method hash conflict: [" + smKey + 
									"] with exist sm [" + conflichMethod.fullStringKey()+"] fail to load service!";
							//logger.error(msg);
							LG.logWithNonRpcContext(MC.LOG_ERROR, ServiceManager.class,msg,MC.MT_DEFAULT,true);
							throw new CommonException(msg);
						}
						//同一个方法，保存最新的方法
						this.methodHash2Method.put(h, sm);
					}
					
					if(this.gatewayModel && !Utils.isEmpty(sm.getHttpPath())) {
						String trimPath = sm.getHttpPath().trim();
						if(httpPath2Method.containsKey(trimPath)) {
							String emk = httpPath2Method.get(trimPath).getKey().methodID();
							if(!emk.equals(sm.getKey().methodID())) {
								//相同的HTT路径，不能存在两个处理方法
								String msg = "Exist HTTP path: "+sm.getHttpPath()+", NMK: " + sm.getKey().methodID()+", EMK: " + emk ;
								LG.logWithNonRpcContext(MC.LOG_ERROR, ServiceManager.class,msg,MC.MT_DEFAULT,true);
								throw new CommonException(msg);
							}
						}
						httpPath2Method.put(sm.getHttpPath(), sm);
					}
					
				}
			}
			
			//this.path2Hash.put(path, hash);
			this.path2SrvItems.put(child, nsi);
		} finally {
			l.unlock();
		}
		return true;
	}
	
	private void refleshOneService(String child,String data) {
		
		ServiceItemJRso i = this.fromJson(data);
		if(i == null){
			logger.warn("path:"+child+", data: "+data);
			return;
		}
		
		//从配置服务合并
		//this.persisFromConfig(i);
		
		boolean flag = this.path2SrvItems.containsKey(child);
		
		if(!this.isChange(data, child,false)) {
			logger.warn("Service Item no change {}",child);
			return;
		}
				
		//this.path2SrvItems.put(path, i);
		
		if(flag) {
			this.notifyServiceChange(IServiceListener.DATA_CHANGE, i.getKey(),i,child);
		} else {
			//logger.debug("Service Add: {}",path);
			this.notifyServiceChange(IServiceListener.ADD, i.getKey(),i,child);
			//dataOperator.addNodeListener(path, nodeListener);
			op.addDataListener(path(child), this.dataListener);
			//dataOperator.addDataListener(i.path(Config.ServiceItemCofigDir), this.cfgDataListener);
		}
	}
	
	/**
	 * 
	 * @param type
	 * @param item
	 */
	private void notifyServiceChange(int type,UniqueServiceKeyJRso itemKey,ServiceItemJRso item,String path){
		Set<IServiceListener> ls = this.serviceListeners.get(path);
		if(ls != null && !ls.isEmpty()){
			//服务名称，名称空间，版本 三维一体服务监听
			synchronized(ls) {
				for(IServiceListener l : ls){
					l.serviceChanged(type,itemKey,item);
				}
			}
		}
		
		ls = this.listeners;
		if(ls != null && !ls.isEmpty()){
			synchronized(ls) {
				for(IServiceListener l : ls){
					//服务运行实例监听器
					l.serviceChanged(type,itemKey, item);
				}
			}
		}
	}
	
	public Set<String> getAllTopic() {
		Set<String> topics = new HashSet<>();
		ReentrantReadWriteLock.ReadLock l = path2SrvItemsLocker.readLock();
		try {
			l.lock();
			
			//引起全量服务加载
			if(!this.eagLoad) {
				logger.warn("Do load all service data by getAllTopic method op");
				this.eagLoad = true;
				this.refleshChildren();
			}
			
			for(ServiceItemJRso si : this.path2SrvItems.values()) {
				for(ServiceMethodJRso sm : si.getMethods()) {
					if(StringUtils.isNotEmpty(sm.getTopic())) {
						topics.add(sm.getTopic());
					}
				}
			}
		} finally {
			l.unlock();
		}
		
		return topics;
	}
	
	public boolean containTopic(String topic) {
		if(StringUtils.isEmpty(topic)) {
			return false; 
		}
		
		ReentrantReadWriteLock.ReadLock l = path2SrvItemsLocker.readLock();
		try {
			l.lock();
			
			//引起全量服务加载
			if(!this.eagLoad) {
				logger.warn("Do load all service data by containTopic method op:topic:"+topic);
				this.eagLoad = true;
				this.refleshChildren();
			}
			
			for(ServiceItemJRso si : this.path2SrvItems.values()) {
				for(ServiceMethodJRso sm : si.getMethods()) {
					if(StringUtils.isNotEmpty(sm.getTopic())) {
						if(sm.getTopic().indexOf(topic) > -1) {
							return true;
						}
					}
				}
			}
		} finally {
			l.unlock();
		}
		
		return false;
	}
	
}
