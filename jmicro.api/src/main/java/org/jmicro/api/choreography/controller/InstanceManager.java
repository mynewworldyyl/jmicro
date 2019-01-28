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
package org.jmicro.api.choreography.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.choreography.base.InstanceInfo;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 运行于Controller端，监听Agent相关的实例指令，并根据指令要求运行指令实例
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:21:05
 */
@Component
public class InstanceManager {
	
	private final static Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	
	private final static String AGENT_IS_DESABLE = "InstanceManager is disable by [\"/InstanceManager/enable\"]";
	
	public String root = null;
	
	@Cfg(value="/InstanceManager/enable", defGlobal=false, required=true)
	private boolean enable = false;
	
	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private ComponentIdServer idServer;
	
	//路径到Instance映射
	private Map<String,InstanceInfo> path2Instance = new ConcurrentHashMap<>();
	
	//Agent path到Instance列表映射
	private Map<String,Set<InstanceInfo>> agent2Instances = new ConcurrentHashMap<>();
	
	private Set<IInstanceListener> instanceListeners = new HashSet<>();
	
	private IAgentListener agentListener = (type,agent)->{
		if(type == IListener.SERVICE_ADD){
			agentAdded(agent);
			logger.error("Agent added:{} " + agent);
		} else if(type == IListener.SERVICE_REMOVE) {
			agentRemoved(agent);
			logger.error("Agent removed:{} " + agent);
		} else {
			logger.error("Invalid event type:{}, agnet:{}",type,agent);
		}
	};
	
	//Instance结点监听器
	private IChildrenListener agent2InstanceListener = new IChildrenListener(){
		public void childrenChanged(int type,String parent, String child,String data){
			if(type == IListener.SERVICE_ADD){
				instanceAdded(parent,child,data);
				logger.error("NodeListener Instance add parent:{}, child:{},data:{}",parent,child,data);
			} else if(type == IListener.SERVICE_REMOVE) {
				instanceRemoved(parent,child);
				logger.error("NodeListener Instance remove parent:{}, child:{},data:{}",parent,child);
			} else {
				logger.error("Receive invalid Node event type:{}, parent:{}, child:{},data:{} ",type,parent,child,data);
			}
		}
	};
	
	public void init() {
		
		if(!this.enable) {
			logger.info(AGENT_IS_DESABLE);
			return;
		}
		
		this.agentManager.addAgentListener(agentListener);
		
	}
	
	public Set<InstanceInfo> getAllAgentInfo() {
		Set<InstanceInfo> agents = new HashSet<>();
		agents.addAll(this.path2Instance.values());
		return agents;
	}
	
	public void createInstance(InstanceInfo ai,boolean isElp) {
		
		if(StringUtils.isEmpty(ai.getProcessId())) {
			logger.error("Instance ID cannot be NULL: {}",ai);
			throw new CommonException("Instance ID cannot be NULL:"+ai);
		}
		
		String path = AgentManager.ROOT_AGENT + "/" + ai.getAgentName() +"_"
		+ ai.getAgentId()+"/"+ai.getProcessId();
		
		String jsonData = JsonUtils.getIns().toJson(ai);
		
		if(!this.dataOperator.exist(path)) {
			this.dataOperator.createNode(path, jsonData, isElp);
		}else {
			this.dataOperator.setData(path, jsonData);
		}
	}
	
	public void addInstanceListener(IInstanceListener l) {
		if(!this.enable) {
			throw new CommonException(AGENT_IS_DESABLE);
		}
		if( this.instanceListeners.contains(l) ) {
			return;
		}
		this.instanceListeners.add(l);
		if(!path2Instance.isEmpty()) {
			for(InstanceInfo ai: path2Instance.values()) {
				l.agentChanged(IAgentListener.SERVICE_ADD, ai);
			}
		}
	}
	
	
	private void refleshInstance(List<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		
		List<String> children = this.dataOperator.getChildren(root);
		
		for(String c : children) {
			String path = root+"/"+c;
			String data = this.dataOperator.getData(path);
			InstanceInfo ai = JsonUtils.getIns().fromJson(data, InstanceInfo.class);
			if(!path2Instance.containsKey(path)) {
				dataOperator.addDataListener(path, agentDataListener);
				dataOperator.addNodeListener(path, this.agentNodeListener);
				notifyInstanceListener(IAgentListener.SERVICE_ADD,ai);
			}
			path2Instance.put(path, ai);
			id2Instance.put(ai.getProcessId(), ai);
		}
		
	}

	private void updateInstanceData(String path, String data, boolean b) {
		InstanceInfo ai = JsonUtils.getIns().fromJson(data, InstanceInfo.class);
		path2Instance.put(path, ai);
		id2Instance.put(ai.getProcessId(), ai);
		notifyInstanceListener(IAgentListener.SERVICE_DATA_CHANGE,ai);
	}
	
	private void agentAdded(AgentInfo agent) {
		String path = agentPath(agent);
		if(agent2Instances.containsKey(path)) {
			dataOperator.removeDataListener(path, agentDataListener);
			dataOperator.removeNodeListener(path, this.agentNodeListener);
			InstanceInfo ai = this.path2Instance.remove(path);
			if(ai != null) {
				id2Instance.remove(ai.getProcessId());
				notifyInstanceListener(IAgentListener.SERVICE_REMOVE,ai);
			}
		}
	}
	
	private void agentRemoved(AgentInfo agent) {
		String path = agentPath(agent);
		if(this.agent2Instances.containsKey(path)) {
			this.dataOperator.removeChildrenListener(path, this.agent2InstanceListener);
			
		}
	}
	
	private String agentPath(AgentInfo agent) {
		return AgentManager.ROOT_AGENT+"/"+agent.getName()+"_"+agent.getId();
	}

	private void instanceAdded(String agentPath,String instance, String data) {
		String path = agentPath + "/"+ instance;
		if(!path2Instance.containsKey(path)) {
			InstanceInfo ai = JsonUtils.getIns().fromJson(data, InstanceInfo.class);
			this.path2Instance.put(path,ai);
			Set<InstanceInfo> iis = this.agent2Instances.get(agentPath);
			if(iis == null) {
				this.agent2Instances.put(agentPath, new HashSet<>());
			}
			iis.add(ai);
		}
	}
	
	private void instanceRemoved(String agentPath,String instance) {
		String path = agentPath + "/"+ instance;
		if(path2Instance.containsKey(path)) {
			InstanceInfo ai = this.path2Instance.remove(path);
			this.agent2Instances.remove(agentPath);
			if(ai != null) {
				notifyInstanceListener(IAgentListener.SERVICE_REMOVE,ai);
			}
		}
	}

	private void notifyInstanceListener(int type,InstanceInfo ii) {
		if(this.instanceListeners.isEmpty()) {
			return;
		}
		
		for(IInstanceListener l : this.instanceListeners) {
			l.agentChanged(type, ii);
		}
		
	}

}
