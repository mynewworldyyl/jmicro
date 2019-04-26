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
package org.jmicro.choreography.agent;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.choreography.agent.IServiceAgent;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.choreography.base.InstanceInfo;
import org.jmicro.api.choreography.base.SchedulerResult;
import org.jmicro.common.Constants;


/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:41:17
 */
@Service(namespace="choreography.agent", version="0.0.1", maxSpeed=-1,
	baseTimeUnit=Constants.TIME_SECONDS, timeout=0,retryCnt=0)
@Component
public class ServiceAgentImpl implements IServiceAgent{

	@Inject
	private LocalProcessManager pm;
	
	private AgentInfo agentInfo;
	
	public void init() {
	}
	
	@Override
	@SMethod(baseTimeUnit=Constants.TIME_SECONDS, timeout=60,retryCnt=0)
	public SchedulerResult startService(String processId,String classpaths, 
			String mainClazz,String[] args) {
		return pm.startService(processId,classpaths,mainClazz,args);
	}

	@Override
	public InstanceInfo checkService(String processId) {
		return pm.checkService(processId);
	}

	@Override
	public SchedulerResult stopService(String processId) {
		return pm.stopService(processId);
	}

	@Override
	public AgentInfo info() {
		return agentInfo;
	}
	
}
