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
package cn.jmicro.gateway.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.IExecutorInfoJMSrv;
import cn.jmicro.api.monitor.IMonitorAdapterJMSrv;
import cn.jmicro.api.monitor.IMonitorDataSubscriberJMSrv;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

/**
 * 专用于HTTP服务网关，维护Http方法列表
 *
 * @author Yulei Ye
 * @date 2022年12月15日 下午1:29:39
 */
@Component(limit2Packages="cn.jmicro")
public class HttpServiceManager {

	private final static Logger logger = LoggerFactory.getLogger(HttpServiceManager.class);
	
	private static final Set<String> excludeServices = new HashSet<>();
	
	static {
		excludeServices.add(IExecutorInfoJMSrv.class.getName());
		excludeServices.add("cn.jmicro.api.choreography.IAgentProcessService");
		excludeServices.add(IMonitorDataSubscriberJMSrv.class.getName());
		excludeServices.add(IMonitorAdapterJMSrv.class.getName());
	}
	
	private String parent = null;
	
	//private Map<Integer,ServiceMethodJRso> methodHash2Method = new HashMap<>();
	
	private Map<Integer,Map<String,ServiceMethodJRso>> httpPath2Method = new HashMap<>();
	
	private Map<String,ServiceMethodJRso> globalHttpPath2Method = new HashMap<>();
	
	private Map<String,Set<ServiceMethodJRso>> path2Methods = new HashMap<>();
	
	@Inject
	private IDataOperator op;
	
	@Cfg(value="/includeServices",toValType=Cfg.VT_SPLIT)
	private Set<String> includeServices = new HashSet<>();
	
	@Cfg("/openDebug")
	private boolean openDebug = false;
	
	//动态服务数据监听器
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateItemData(path,data);
		}
	};
	
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
				op.addDataListener(path(child), this.dataListener);
			}else if(IListener.REMOVE == type) {
				logger.debug("Service remove, path:{}",child);
				childrenRemove(child);
				String fpath = path(child);
				op.removeDataListener(fpath, dataListener);
			}else if(IListener.DATA_CHANGE == type){
				logger.debug("Invalid service data change event, path:{}",child);
			}
		});
		//refleshChildren();
	}
	
	private void childrenRemove(String child) {
		Set<ServiceMethodJRso> set = path2Methods.remove(child);
		if(set == null || set.isEmpty()) {
			return;
		}
		
		ServiceMethodJRso sm = set.iterator().next();
		
		Map<String,ServiceMethodJRso> ms = this.httpPath2Method.get(sm.getKey().getUsk().getClientId());
		if(ms == null || ms.isEmpty()) return;
		
		for(ServiceMethodJRso m : set) {
			ms.remove(m.getHttpPath());
			globalHttpPath2Method.remove(m.getHttpPath());
		}
		
	}
	
	private void refleshChildren() {
		
		Set<String> children = this.op.getChildren(Config.getRaftBasePath(parent),true);
		
		logger.warn("clean cache data!");

		httpPath2Method.clear();
		
		for(String child : children) {
			childrenAdd(child);
		}
	}
	
	private void updateItemData(String child, String data) {
		ServiceItemJRso si = this.fromJson(data);
		if(!si.isExternal()) {
			return;
		}
		
		Set<ServiceMethodJRso> ms = path2Methods.get(child);
		if(ms == null ) {
			ms = new HashSet<ServiceMethodJRso>();
			path2Methods.put(child, ms);
		}
	    
		for(ServiceMethodJRso sm : si.getMethods()) {
			if(Utils.isEmpty(sm.getHttpPath())) {
				continue;
			}
			if(sm.isHttpGlobal()) {
				ServiceMethodJRso esm = this.globalHttpPath2Method.get(sm.getHttpMethod());
				if(esm != null) {
					throw new CommonException("Global http method repeat: "+esm.getKey().fullStringKey()+"==>"+sm.getKey().fullStringKey());
				}
				this.globalHttpPath2Method.put(sm.getHttpPath(), sm);
			} else {
				Map<String,ServiceMethodJRso> msset = this.httpPath2Method.get(si.getClientId());
				if(msset == null) {
					msset = new HashMap<>();
					this.httpPath2Method.put(si.getClientId(), msset);
				}
				
				ServiceMethodJRso esm = msset.get(sm.getHttpMethod());
				if(esm != null) {
					throw new CommonException("Http method repeat: "+esm.getKey().fullStringKey()+" ==> "
							+sm.getKey().fullStringKey());
				}
				
				msset.put(sm.getHttpPath(), sm);
				ms.add(sm);
			}
			
		}
	}

	protected void childrenAdd(String child) {
		//已经加载，判断是否需要更新
		String path = path(child);
		String data = this.op.getData(path);
		updateItemData(child,data);
		
	}
	
	public void setDataOperator(IDataOperator dataOperator) {
		this.op = dataOperator;
	}
	
	/**
	 * 查找路径最佳匹配的服务方法
	 * @param path
	 * @param httpMethod
	 * @param reqContentType
	 * @return
	 */
	public ServiceMethodJRso getMethodByHttpPath(String httpPath,Integer clientId) {
		if(Utils.isEmpty(httpPath)) {
			return null;
		}
		
		Map<String,ServiceMethodJRso> ms = null;
		if(clientId == null) {
			ms = this.globalHttpPath2Method;
		} else {
			ms = this.httpPath2Method.get(clientId);
		}
		
		if(ms == null) {
			logger.warn("Http path method:" + httpPath+"] method not found for client: " + clientId);
			return null;
		}
		
		httpPath = httpPath.trim();
		if(ms.containsKey(httpPath)) {
			return ms.get(httpPath);
		}
		
		// /a/b/c/*   /a/b/* /a/*
		// /a/b/c/cdd  匹配  /a/b/c/*
		String curMathPath = null;
		for(String p : ms.keySet()) {
			if(!p.endsWith("*")) {
				continue;
			}

			String prefix = p.substring(0,p.length()-1);
			if(!httpPath.startsWith(prefix)) {
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
			return ms.get(curMathPath);
		}
		
		logger.warn("Http path method:" + httpPath+"] method not found for client: " + clientId);
		return null;
	}
	
	private String path(String path) {
		if(path.startsWith(parent)) return path;
		return parent+"/"+path;
	}
	
	private String subPath(String child) {
		if(!child.startsWith(parent)) return child;
		return child.substring(parent.length()+1);
	}

	private ServiceItemJRso fromJson(String data){
		return JsonUtils.getIns().fromJson(data, ServiceItemJRso.class);
	}
	
}
