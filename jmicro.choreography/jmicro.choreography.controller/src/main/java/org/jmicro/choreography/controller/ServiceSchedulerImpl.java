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
package org.jmicro.choreography.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.choreography.agent.IServiceAgent;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.choreography.base.SchedulerResult;
import org.jmicro.api.choreography.cmd.Cmd;
import org.jmicro.api.choreography.controller.AgentManager;
import org.jmicro.api.choreography.controller.IAgentListener;
import org.jmicro.api.choreography.controller.IServiceController;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月24日 上午9:11:23
 */
@Service(namespace="choreography.scheduler", version="0.0.1", maxSpeed=-1,
baseTimeUnit=Constants.TIME_SECONDS, timeout=0, retryCnt=0)
@Component
public class ServiceSchedulerImpl implements IServiceController {

	@Reference
	private Set<IServiceAgent> serviceAgents = Collections.synchronizedSet(new HashSet<>());
	
	private Map<String,AgentInfo> agentInfos = new ConcurrentHashMap<>();
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private IDataOperator dataOperator;
	
	public void init() {
		agentManager.addAgentListener((type,ai)->{
			if(type == IAgentListener.SERVICE_ADD) {
				agentInfos.put(ai.getId(), ai);
			}else if(type == IAgentListener.SERVICE_DATA_CHANGE) {
				agentInfos.put(ai.getId(), ai);
			}else if(type == IAgentListener.SERVICE_REMOVE) {
				agentInfos.remove(ai.getId());
			}
		});
		
		for(AgentInfo ai: agentManager.getAllAgentInfo()) {
			agentInfos.put(ai.getId(),ai);
		}
		
	}

	@Override
	public SchedulerResult startByCmd(String cmd, int count, String... args) {
		SchedulerResult r = new SchedulerResult();
		if(agentInfos.isEmpty()) {
			r.setCode("NoAgent");
			r.setSuccess(false);
			r.setMsg("No agent to run instance");
			return r;
		}
		
		Random rand = new Random();
		int b =(int) System.currentTimeMillis() % agentInfos.size();
		int i = rand.nextInt(b);
		
		int c = 0;
		
		AgentInfo ai = null;
		
		Iterator<AgentInfo> ite = agentInfos.values().iterator();
		while(ite.hasNext()) {
			if(i == c) {
				ai = ite.next();
			}else {
				ite.next();
			}
		}
		
		Cmd cd = new Cmd();
		cd.setArgs(args);
		cd.setCmd(cmd);
		cd.setCmdName(cmd);
		cd.setCount(count);
		
		String path = AgentManager.ROOT_AGENT + "/" + ai.getName() + ai.getId()+"/cmds/"+cd.getCmdName();
		this.dataOperator.createNode(path, JsonUtils.getIns().toJson(cd), false);
		
		
		return r;
	}

	@Override
	public SchedulerResult stopById(String id) {
		
		return null;
	}

	@Override
	public SchedulerResult lsServices(String matcher) {
		
		return null;
	}

}
