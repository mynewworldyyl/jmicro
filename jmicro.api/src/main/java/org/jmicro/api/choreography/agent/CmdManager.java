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
package org.jmicro.api.choreography.agent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.choreography.base.InstanceInfo;
import org.jmicro.api.choreography.cmd.Cmd;
import org.jmicro.api.choreography.controller.AgentManager;
import org.jmicro.api.choreography.controller.IAgentListener;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.INodeListener;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 运行于Agent端，监听Agent相关的指令，并根据指令要求运行指令实例
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:21:05
 */
@Component
public class CmdManager {
	
	private final static Logger logger = LoggerFactory.getLogger(CmdManager.class);
	
	private final static String AGENT_IS_DESABLE = "InstanceManager is disable by [\"/InstanceManager/enable\"]";
	
	private final static String CMD_DIR = "/cmds";
	
	public String root = null;
	
	@Cfg(value="/CmdManager/enable", defGlobal=false, required=true)
	private boolean enable = false;
	
	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private ComponentIdServer idServer;
	
	private AgentInfo agent;
	
	//路径到Cmd映射
	private Map<String,ICmdListener> path2CmdListeners = new ConcurrentHashMap<>();
	
	//ID到Instance映射
	private Map<String,Cmd> path2Cmds = new ConcurrentHashMap<>();
	
	private Set<ICmdListener> cmdListeners = new HashSet<>();
	
	//Cmd结点监听器
	private INodeListener agentNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == IListener.SERVICE_ADD){
				//agentAdd(path,data);
				logger.error("NodeListener Instance add "+type+",path: "+path);
			} else if(type == IListener.SERVICE_REMOVE) {
				//cmdRemoved(path);
				logger.error("Instance remove:"+type+",path: "+path);
			} else {
				logger.error("Receive invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	public void init() {
		
		if(!this.enable) {
			logger.info(AGENT_IS_DESABLE);
			return;
		}
		
		//this.agentName = agentName;
		this.root = AgentManager.ROOT_AGENT + "/" + 
				agent.getName()+ "_" + agent.getId() + CMD_DIR;
		
		dataOperator.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				Set<String> children = this.dataOperator.getChildren(root);
				refleshCmds(children);
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				Set<String> children = this.dataOperator.getChildren(root);
				refleshCmds(children);
			}
		});
		
		logger.info("add listener");
		dataOperator.addChildrenListener(root, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent,String child,String data) {
				if(type == IListener.SERVICE_ADD) {
					cmdAdded(parent+"/"+child,data);
				}else if(type == IListener.SERVICE_REMOVE) {
					cmdRemoved(parent+"/"+child);
				}
			}
		});
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
	
	public void addCmdListener(ICmdListener l) {
		if(!this.enable) {
			throw new CommonException(AGENT_IS_DESABLE);
		}
		if( this.cmdListeners.contains(l) ) {
			return;
		}
		this.cmdListeners.add(l);
		if(!path2Cmds.isEmpty()) {
			for(Cmd ai: path2Cmds.values()) {
				l.agentChanged(IAgentListener.SERVICE_ADD, ai);
			}
		}
	}
	
	private void refleshCmds(Set<String> children) {
		if(children == null || children.isEmpty()) {
			return;
		}
		for(String c : children) {
			String path = root+"/"+c;
			cmdAdded(path,null);
		}
	}
	
	private void cmdAdded(String path,String data) {
		if(data == null) {
			data = this.dataOperator.getData(path);
		}
		Cmd cmd = JsonUtils.getIns().fromJson(data, Cmd.class);
		path2Cmds.put(path,cmd);
		if(!path2CmdListeners.containsKey(path)) {
			notifyCmdListener(IAgentListener.SERVICE_ADD,cmd);
		}
	}

	private void cmdRemoved(String path) {
		if(path2Cmds.containsKey(path)) {
			Cmd cmd = this.path2Cmds.remove(path);
			if(cmd != null) {
				notifyCmdListener(IAgentListener.SERVICE_REMOVE,cmd);
			}
		}
	}

	private void notifyCmdListener(int type,Cmd cmd) {
		if(this.cmdListeners.isEmpty()) {
			return;
		}
		
		for(ICmdListener l : this.cmdListeners) {
			l.agentChanged(type, cmd);
		}
		
	}

	public AgentInfo getAgent() {
		return agent;
	}

	public void setAgent(AgentInfo agent) {
		this.agent = agent;
	}
	
}
