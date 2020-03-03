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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.INodeListener;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:52
 */
@Component
public class AgentManager {
	
	private final static Logger logger = LoggerFactory.getLogger(AgentManager.class);
	
	private final static String AGENT_IS_DESABLE = "AgentManager is disable by [\"/AgentManager/enable\"]";
	
	public static final String ROOT_AGENT = Config.ChoreographyDir+"/agents";
	
	//public static final String ROOT_CONTROLLER = Config.ChoreographyDir+"/controllers";
	
	@Cfg(value="/AgentManager/enable", defGlobal=false)
	private boolean enable = false;
	
	@Inject
	private IDataOperator dataOperator;
	
	//路径到Agent映射
	private Map<String,AgentInfo> path2Agents = new ConcurrentHashMap<>();
	
	//ID到Agent映射
	private Map<String,AgentInfo> id2Agents = new ConcurrentHashMap<>();
	
	private Set<IAgentListener> agentListeners = new HashSet<>();
	
	//Agent结点监听器
	private INodeListener agentNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == IListener.ADD){
				//agentAdd(path,data);
				logger.error("NodeListener Agent add "+type+",path: "+path);
			} else if(type == IListener.REMOVE) {
				removed(path);
				logger.error("Agent remove:"+type+",path: "+path);
			} else {
				logger.error("Receive invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	//Agent结点数据监听器
	private IDataListener agentDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			updateAgentData(path,data,false);
		}
	};
	
	public void init() {
		
		if(!this.enable) {
			logger.info(AGENT_IS_DESABLE);
			return;
		}
		
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				Set<String> children = this.dataOperator.getChildren(ROOT_AGENT,true);
				refleshAgent(children);
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				Set<String> children = this.dataOperator.getChildren(ROOT_AGENT,true);
				refleshAgent(children);
			}
		});
		
		logger.info("add listener");
		dataOperator.addChildrenListener(ROOT_AGENT, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent, String child,String data) {
				if(type == IListener.ADD) {
					added(parent + "/" + child,data);
				}else if(type == IListener.REMOVE) {
					removed(parent + "/" + child);
				}
			}
		});
	}
	
	public Set<AgentInfo> getAllAgentInfo() {
		Set<AgentInfo> agents = new HashSet<>();
		agents.addAll(this.path2Agents.values());
		return agents;
	}
	
	public void addAgentListener(IAgentListener l) {
		if(!this.enable) {
			throw new CommonException(AGENT_IS_DESABLE);
		}
		if( this.agentListeners.contains(l) ) {
			return;
		}
		this.agentListeners.add(l);
		if(!path2Agents.isEmpty()) {
			for(AgentInfo ai: path2Agents.values()) {
				l.agentChanged(IAgentListener.ADD, ai);
			}
		}
	}
	
	private void added(String path,String data) {
		
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		if(!path2Agents.containsKey(path)) {
			dataOperator.addDataListener(path, agentDataListener);
			dataOperator.addNodeListener(path, this.agentNodeListener);
			notifyAgentListener(IAgentListener.ADD,ai);
		}
		path2Agents.put(path, ai);
		id2Agents.put(ai.getId(), ai);
	}
	
	
	private void refleshAgent(Set<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		
		for(String c : children) {
			String path = ROOT_AGENT+"/"+c;
			String data = this.dataOperator.getData(path);
			added(path,data);
		}
		
	}

	private void updateAgentData(String path, String data, boolean b) {
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		path2Agents.put(path, ai);
		id2Agents.put(ai.getId(), ai);
		notifyAgentListener(IAgentListener.DATA_CHANGE,ai);
	}

	private void removed(String path) {
		if(path2Agents.containsKey(path)) {
			dataOperator.removeDataListener(path, agentDataListener);
			dataOperator.removeNodeListener(path, this.agentNodeListener);
			AgentInfo ai = this.path2Agents.remove(path);
			if(ai != null) {
				id2Agents.remove(ai.getId());
				notifyAgentListener(IAgentListener.REMOVE,ai);
			}
		}
	}

	private void notifyAgentListener(int type,AgentInfo ai) {
		if(this.agentListeners.isEmpty()) {
			return;
		}
		
		for(IAgentListener l : this.agentListeners) {
			l.agentChanged(type, ai);
		}
		
	}
	
}
