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
package org.jmicro.choreography.assignment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.choreography.ChoyConstants;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.choreography.base.AgentInfo;
import org.jmicro.choreography.controller.IAgentListener;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:52
 */
@Component(level=20)
public class AgentManager {
	
	private final static Logger logger = LoggerFactory.getLogger(AgentManager.class);
	
	private final static String AGENT_IS_DESABLE = "AgentManager is disable by [\"/AgentManager/enable\"]";
	
	@Inject
	private IDataOperator dataOperator;
	
	//路径到Agent映射
	private Map<String,AgentInfo> id2Agents = new ConcurrentHashMap<>();
	
	private Set<IAgentListener> agentListeners = new HashSet<>();
	
	//Agent结点数据监听器
	private IDataListener agentDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			String id = path.substring(ChoyConstants.ROOT_AGENT.length()+1);
			updateAgentData(id,data);
		}
	};
	
	public void ready() {
		
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				Set<String> children = this.dataOperator.getChildren(ChoyConstants.ROOT_AGENT,true);
				refleshAgent(children);
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				Set<String> children = this.dataOperator.getChildren(ChoyConstants.ROOT_AGENT,true);
				refleshAgent(children);
			}
		});
		
		logger.info("add listener");
		dataOperator.addChildrenListener(ChoyConstants.ROOT_AGENT, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent, String child,String data) {
				if(type == IListener.ADD) {
					added(child,data);
				}else if(type == IListener.REMOVE) {
					removed(child);
				}
			}
		});
	}
	
	public Set<AgentInfo> getAllAgentInfo() {
		Set<String> children = this.dataOperator.getChildren(ChoyConstants.ROOT_AGENT,true);
		refleshAgent(children);
		
		Set<AgentInfo> agents = new HashSet<>();
		agents.addAll(this.id2Agents.values());
		return agents;
	}
	
	public void addAgentListener(IAgentListener l) {

		if( this.agentListeners.contains(l) ) {
			return;
		}
		this.agentListeners.add(l);
		if(!id2Agents.isEmpty()) {
			for(AgentInfo ai: id2Agents.values()) {
				l.agentChanged(IAgentListener.ADD, ai);
			}
		}
	}
	
	private void added(String c,String data) {
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		id2Agents.put(c, ai);
		if(!id2Agents.containsKey(c)) {
			String p = ChoyConstants.ROOT_AGENT+"/"+c;
			dataOperator.addDataListener(p, agentDataListener);
			notifyAgentListener(IAgentListener.ADD,ai);
		}
	}
	
	
	private void refleshAgent(Set<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		
		for(String c : children) {
			String path = ChoyConstants.ROOT_AGENT+"/"+c;
			String data = this.dataOperator.getData(path);
			added(c,data);
		}
		
	}

	private void updateAgentData(String c, String data) {
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		id2Agents.put(c, ai);
		notifyAgentListener(IAgentListener.DATA_CHANGE,ai);
	}

	private void removed(String c) {
		if(id2Agents.containsKey(c)) {
			String path = ChoyConstants.ROOT_AGENT+"/"+c;
			dataOperator.removeDataListener(path, agentDataListener);
			AgentInfo ai = this.id2Agents.remove(c);
			if(ai != null) {
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
