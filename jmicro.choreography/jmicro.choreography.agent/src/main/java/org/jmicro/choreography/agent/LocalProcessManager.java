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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.choreography.base.AgentInfo;
import org.jmicro.api.choreography.base.InstanceInfo;
import org.jmicro.api.choreography.base.SchedulerResult;
import org.jmicro.api.choreography.controller.AgentManager;
import org.jmicro.api.choreography.controller.IInstanceListener;
import org.jmicro.api.choreography.controller.InstanceManager;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:03
 */
@Component
public class LocalProcessManager {

	private final static Logger logger = LoggerFactory.getLogger(LocalProcessManager.class);
	
	private final Map<String,ProcessInfo> processes = new ConcurrentHashMap<>();
	
	@Cfg("/LocalProcessManager/workDir")
	private String workDir = System.getProperty("user.dir")+"/servers";
	
	@Cfg(value="/LocalProcessManager/checkInfo",changeListener="checkInfoChange")
	private boolean checkInfo = false;
	
	@Cfg(value="/LocalProcessManager/checkInterval")
	private long checkInterval = 5000;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private ComponentIdServer idServer;
	
	private Runnable checkInfoRunner = ()-> {
		if(checkInterval < 1000) {
			//不能小于1秒，否则太快没有意义，
			checkInterval = 1000;
		}
		
		while(checkInfo) {
			logger.info("==================================================");
			for(ProcessInfo pi : this.processes.values()) {
				InstanceInfo ii = checkService(pi.getId());
				logger.info("{}",ii);
			}
		}
		try {
			Thread.sleep(this.checkInterval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};
	
	public void checkInfoChange() {
		if(checkInfo) {
			new Thread(checkInfoRunner).start();
		}
	}
	
	private IInstanceListener insListener = (type,ii) -> {
		if(type == IListener.ADD) {
			logger.info("Start Instance from ZK [{}]",ii);
			startService(ii.getProcessId(),ii.getClasspath(),ii.getMainClazz(),ii.getArgs());
		} else if(type == IListener.REMOVE) {
			logger.info("Stop Instance from ZK [{}]",ii);
			stopService(ii.getProcessId());
		}else if(type == IListener.DATA_CHANGE) {
			logger.info("Restart Instance from ZK [{}]",ii);
			stopService(ii.getProcessId());
			long ticker = 1000*10L;
			TimerTicker.getDefault(ticker).addListener("LocalProcessManagerTimer",
					(key,i)->{
						startService(ii.getProcessId(),ii.getClasspath(),ii.getMainClazz(),ii.getArgs());
						TimerTicker.getDefault(ticker).removeListener("LocalProcessManagerTimer", true);
					},ii);
		}
	};
	
	private AgentInfo createAgent() {
		
		AgentInfo ai = new AgentInfo();
		ai.setId(idServer.getStringId(AgentInfo.class));
		ai.setCmd("java -jar test.jar org.jmicro.TestMain");
		ai.setName(Config.getInstanceName());
		ai.setStartTime(System.currentTimeMillis()+"");
		
		String path = AgentManager.ROOT_AGENT + "/" + ai.getName() + ai.getId();
		String jsonData = JsonUtils.getIns().toJson(ai);
		
		if(!dataOperator.exist(path)) {
			dataOperator.createNode(path, jsonData, true);
		}else {
			dataOperator.setData(path, jsonData);
		}
		
		return ai;
	}

	public void init() {
		long ticker = 1000*30L;
		TimerTicker.getDefault(ticker).addListener("LocalProcessManagerCreateAgentTimer",
				(key,i)->{
					createAgent();
					TimerTicker.getDefault(ticker).removeListener("LocalProcessManagerCreateAgentTimer", true);
				},null);
		insManager.addInstanceListener(insListener);
	}
	
	public SchedulerResult startService(String processId,String classpaths, String mainClazz,String[] args) {

		SchedulerResult result = new SchedulerResult();
		try {
			
			if(StringUtils.isEmpty(processId)) {
				result.setSuccess(false);
				result.setPid(processId);
				result.setMsg("Process ID cannot be null");
				result.setCode("ProcessIDIsNull");
				return result;
			}
			
			//防止重复请求
			if(processes.containsKey(processId)) {
				ProcessInfo pi = processes.get(processId);
				result.setSuccess(false);
				result.setPid(processId);
				result.setMsg("Service is started Active:" + pi.getProcess().isAlive());
				result.setCode("ProcessExistsWithSameId");
				return result;
			}
			
			List<String> list = new ArrayList<String>();
			list.add("java");
			list.add("-cp");
			list.add(classpaths);
			list.add(mainClazz);
			
			ProcessBuilder pb = new ProcessBuilder(list);

			File wd = new File(workDir + "/" + processId);
			wd.mkdirs();

			pb.directory(wd);

			Process p = pb.start();
			
			ProcessInfo pi = new ProcessInfo();
			pi.setArgs(args);
			pi.setClasspath(classpaths);
			pi.setMainClazz(mainClazz);
			pi.setProcess(p);
			pi.setId(processId);
			
			processes.put(processId, pi);
			
			result.setSuccess(true);
			result.setPid(processId);

		} catch (Exception e) {
			logger.error("classpath:{},mainClass:{},args:{}",classpaths,mainClazz/*,Arrays.asList(args).toString()*/);
			logger.error("startService",e);
			result.setMsg(e.getMessage());
			result.setSuccess(false);
		}

		return result;
	}

	public InstanceInfo checkService(String processId) {
		InstanceInfo si = new InstanceInfo();
		ProcessInfo pi = processes.get(processId);
		if(pi == null) {
			si.setAlive(false);
		} else {
			si.setAlive(true);
			si.setArgs(pi.getArgs());
			si.setClasspath(pi.getClasspath());
			si.setMainClazz(pi.getMainClazz());
			si.setProcessId(pi.getId());
		}
		return si;
	}

	public SchedulerResult stopService(String processId) {
		SchedulerResult result = new SchedulerResult();
		ProcessInfo pi = processes.get(processId);
		if(pi != null) {
			pi.getProcess().destroy();
			result.setSuccess(true);
			processes.remove(processId);
			logger.warn("Stop pid:{},mainClazz:{},args:{}",processId,
					pi.getClass(),Arrays.asList(pi.getArgs()).toString());
		} else {
			result.setSuccess(false);
			result.setMsg("Process processId ["+processId+"] not exists");
			logger.warn("Stop pid[{}] not found",processId);
		}
		return result;
	}
	
}
