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
package cn.jmicro.choreography.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.sysstatis.SystemStatisManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.assign.Assign;
import cn.jmicro.choreography.assign.AssignState;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.common.util.SystemUtils;


/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:41:17
 */
@Component(level=100)
public class ServiceAgent {

	private static final Logger logger = LoggerFactory.getLogger(ServiceAgent.class);
	
	private static final Class<?> TAG = ServiceAgent.class;
	
	//@Cfg("/ServiceAgent/workDir")
	private String workDir;
	
	//@Cfg("/ServiceAgent/resourceDir")
	private String resourceDir; // = System.getProperty("user.dir") + "/resourceDir";
	
	@Cfg(value = "/ServiceAgent/javaAgentJarFile", defGlobal=true)
	private String javaAgentJarFile="jmicro-agent-0.0.1-SNAPSHOT.jar";
	
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 65300;//1024*1024;
	
	//will be cancel the start process when start time
	//will be force stop the process when process not response to stop action before timeout
	@Cfg(value="/ServiceAgent/processOpTimeout", defGlobal=true)
	private int processOpTimeout = 2*60*1000 + 20*1000;  //20秒是ZK结点超时时间
	
	@Inject
	private Config cfg;
	
	private File workDirFile = null;
	
	private File resourceDirFile = null;
	
	private Map<String,ProcessInfo> startingProcess = new HashMap<>();
	
	private Map<String,ProcessInfo> stopingProcess = new HashMap<>();
	
	private Map<String,Process> sysProcess = new HashMap<>();
	
	private Queue<Deployment> startDeps = new ConcurrentLinkedQueue<>();
	
	@Reference(namespace="rrs",version="*")
	private IResourceResponsitory respo;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private SystemStatisManager ssm;
	
	private AgentInfo agentInfo;
	
	@Inject
	private ILockerManager lockMgn;
	
	private String path;
	
	private String activePath;
	
	private String[] initDepIds;
	
	private IChildrenListener deploymentListener = (type,parent,depId,data)->{
		if(type == IListener.ADD) {
			ProcessInfo pi = this.insManager.getProcessesByAgentIdAndDepid(agentInfo.getId(), depId);
			if(pi == null) {
				deploymentAdded(depId);
			} else {
				//第一次启动时，已经存在对应的应用
				logger.warn("Process exist: " + pi.toString());
			}
		}else if(type == IListener.REMOVE) {
			this.deploymentRemoved(depId);
		}
	};
	
	private IDataListener cmdListener = (path0,cmd) -> {
		switch(cmd) {
		case ChoyConstants.AGENT_CMD_STARTING_TIMEOUT:
		case ChoyConstants.AGENT_CMD_STOPING_TIMEOUT:
			logger.warn("Reset deployment listenr for cmd: " + cmd);
			op.removeChildrenListener(path,deploymentListener);
			op.addChildrenListener(this.path, deploymentListener);
			break;
		}
	};
	
	private IDataListener agentDataListener = (path0,data) -> {
		agentInfo = JsonUtils.getIns().fromJson(data, AgentInfo.class);
	};
	
	public void ready() {
		
		workDir = cfg.getString(Constants.INSTANCE_DATA_DIR,"") + File.separatorChar + "agentInstanceDir";
		workDirFile = new File(workDir);
		if(!workDirFile.exists()) {
			workDirFile.mkdir();
		}
		
		resourceDir = cfg.getString(Constants.INSTANCE_DATA_DIR,"")  + File.separatorChar + "resourceDir";
		resourceDirFile = new File(resourceDir);
		if(!resourceDirFile.exists()) {
			resourceDirFile.mkdir();
		}
		
		File infoFile = new File(cfg.getString(Constants.INSTANCE_DATA_DIR,""),"agent.json");
		if(infoFile.exists()) {
			String existAgentJson = SystemUtils.getFileString(infoFile);
			if(StringUtils.isNotEmpty(existAgentJson)) {
				agentInfo = JsonUtils.getIns().fromJson(existAgentJson, AgentInfo.class);
				activePath = ChoyConstants.ROOT_ACTIVE_AGENT+"/" + agentInfo.getId();
				if(op.exist(activePath)) {
					logger.warn("Only one agent can be exist for one resourceDir: " + resourceDir);
					System.exit(0);
					return;
				}
			}
		}
		
		if(agentInfo == null) {
			agentInfo = new AgentInfo();
			agentInfo.setId(idServer.getStringId(AgentInfo.class));
		}
		
		boolean privat = cfg.getBoolean(ChoyConstants.ARG_AGENT_PRIVATE, false);
		agentInfo.setPrivat(privat);
		
		String deps = cfg.getString(ChoyConstants.ARG_INIT_DEP_IDS,null);
		if(privat && StringUtils.isEmpty(deps)) {
			throw new CommonException("Private agent init depId cannot be NULL");
		}
		
		agentInfo.setName(Config.getInstanceName());
		agentInfo.setStartTime(System.currentTimeMillis());
		agentInfo.setAssignTime(agentInfo.getAssignTime());
		agentInfo.setHost(Config.getHost());
		agentInfo.setSs(ssm.getStatis());
		agentInfo.setInitDepIds(deps);
		
		String agJson = JsonUtils.getIns().toJson(agentInfo);
		SystemUtils.setFileString(infoFile, agJson);
		
		path = ChoyConstants.ROOT_AGENT + "/" + agentInfo.getId();
		activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+ agentInfo.getId();
		
		op.createNodeOrSetData(path, agJson , IDataOperator.PERSISTENT); //
		if(op.exist(activePath)) {
			op.deleteNode(activePath);
		} else {
			op.createNodeOrSetData(activePath,ChoyConstants.AGENT_CMD_NOP,IDataOperator.EPHEMERAL); //代表此Agent的存活标志
		}
		
		//run specify deployments
		if(StringUtils.isNotEmpty(deps)) {
			setInitDeps(deps);
		}
		
		//op.createNodeOrSetData(assignPath, "" , false); //分配给此Agent的全部部署ID
		
		insManager.filterByAgent(agentInfo.getId());
		
		insManager.addListener((type,pi) -> {
			if(type == IListener.ADD) {
				instanceAdded(pi);
			} else if(type == IListener.REMOVE) {
				logger.info("Instance remove: " + pi.toString());
				instanceRemoved(pi);
			}
		});
		
		//监控分配新的应用
		op.addChildrenListener(path,deploymentListener);
		
		op.addDataListener(path, agentDataListener);
		
		op.addDataListener(activePath, cmdListener);
		
		TimerTicker.doInBaseTicker(5, Config.getInstanceName()+"_ServiceAgent", null, (key,att)->{
			try {
				/*if(System.currentTimeMillis() - agentInfo.getStartTime() < 5000) {
					return;
				}*/
				checkStatus();
			} catch (Throwable e) {
				logger.error("doChecker",e);
			}
		});
	}
	
	private boolean hasController() {
		if(!op.exist(ChoyConstants.ROOT_CONTROLLER)) {
			return false;
		}
		Set<String> controllers = op.getChildren(ChoyConstants.ROOT_CONTROLLER,false);
		return controllers != null && controllers.size() > 0;
	}
	
	private void setInitDeps(String deps) {
		String[] depIds = deps.split(",");
		if(depIds == null || depIds.length == 0) {
			return;
		}
		
		initDepIds = depIds;
		
		if(this.hasController()) {
			//manager by controllers
			return;
		}
		
		//start instances specify by depId
		for(String depId : depIds) {
			startInitDeployment(depId);
		}
	}

	private void startInitDeployment(String depId) {
		if(StringUtils.isEmpty(depId)) {
			return;
		}
		depId = depId.trim();
		Deployment dep = this.getDeployment(depId);
		if(dep == null || !dep.isEnable()) {
			return;
		}
		
		Map<String,String> params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
		String agentIds = params.get(IAssignStrategy.AGENT_ID);
		if(StringUtils.isEmpty(agentIds)) {
			logger.error("Deployment ID [" + depId+"] not contain angent ID [" + agentInfo.getId() + "]");
			return;
		}
		
		boolean f = false;
		String[] agids = agentIds.split(",");
		for(String aid : agids) {
			if(aid.equals(agentInfo.getId())) {
				//deployment descriptor argument with name agentId equals this agentId
				f = true;
				break;
			}
		}
		
		if(f) {
			String pid = (Long.parseLong(op.getData(ChoyConstants.ID_PATH)) +1)+"";
			op.setData(ChoyConstants.ID_PATH, pid);
			Assign a = new Assign(dep.getId(),this.agentInfo.getId(),pid);
			a.opTime = System.currentTimeMillis();
			a.state = AssignState.INIT;
			op.createNodeOrSetData(path+"/" + depId, JsonUtils.getIns().toJson(a), IDataOperator.PERSISTENT);
		} else {
			logger.error("Deployment ID [" + depId+"] not contain init angent Id [" + agentInfo.getId() + "]");
		}
		
	}

	private void recreateAgentInfo() {
		agentInfo.setAssignTime(System.currentTimeMillis());
		String data = JsonUtils.getIns().toJson(agentInfo);
		logger.warn("Recreate angent info: " + data);
		op.createNodeOrSetData(path,data ,IDataOperator.PERSISTENT);
		op.addChildrenListener(path,deploymentListener);
		op.addDataListener(path, agentDataListener);
	}
	
	private void setStatisData() {
		agentInfo.setSs(ssm.getStatis());
		
		String data = JsonUtils.getIns().toJson(agentInfo);
		op.setData(path, data);
	}

	private void checkStatus() {
		long curTime = System.currentTimeMillis();
		if( curTime - agentInfo.getAssignTime() < 5000) {
			return;
		}

		if(!op.exist(path)) {
			recreateAgentInfo();
			return;
		}
		
		if(!op.exist(activePath)) {
			op.createNodeOrSetData(activePath,ChoyConstants.AGENT_CMD_NOP,IDataOperator.EPHEMERAL); //代表此Agent的存活标志
			op.addDataListener(activePath, cmdListener);
		}
		
		setStatisData();
		
		if(!startDeps.isEmpty()) {
			Deployment dep = null;
			while((dep = startDeps.poll()) != null) {
				startDep(dep);
			}
			
		}
		
		if(!startingProcess.isEmpty()) {
			Set<String> keySet = new HashSet<>();
			keySet.addAll(startingProcess.keySet());
			for(String k : keySet) {
				ProcessInfo pi = startingProcess.get(k);
				if(curTime - pi.getOpTime() > pi.getTimeOut()) {
					//已经命令进程停止，并且超过超时时间还没成功退出
					logger.debug("Start timeout: " + pi.toString());
					startingProcess.remove(k);
					forceStopProcess(k);
					deleteAssignDepNode(pi.getDepId());
				}
			}
		}
		
		if(!stopingProcess.isEmpty()) {
			Set<String> keySet = new HashSet<>();
			keySet.addAll(stopingProcess.keySet());
			for(String k : keySet) {
				ProcessInfo pi = stopingProcess.get(k);
				String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
				if(curTime - pi.getOpTime() > pi.getTimeOut()) {
					//已经命令进程停止，并且超过超时时间还没成功退出
					logger.warn("Stop timeout and force stop it: " + pi.toString());
					stopingProcess.remove(k);
					forceStopProcess(k);
					deleteAssignDepNode(pi.getDepId());
				}
			}
		}
		
		if(this.initDepIds != null && this.initDepIds.length > 0 && !this.hasController()) {
			for(String depId : this.initDepIds) {
				int size = this.insManager.getProcessSizeByDepId(depId);
				if(size == 0) {
					startInitDeployment(depId);
				}
			}
		}
		
	}

	private void forceStopProcess(String pid) {
		
		Process p = sysProcess.get(pid);
		if(p != null) {
			logger.warn("Force kill process " + pid);
			p.destroyForcibly();
			sysProcess.remove(pid);
		}else {
			String cmd = "kill -15 "+pid;
			if(SystemUtils.isWindows()) {
				 cmd = "taskkill /f /pid "+pid;
			}
			
			try {
				logger.warn("Force kill with command: " + cmd);
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				logger.error("fail to stop process : " + pid,e);
			}
		}
	}


	private void instanceRemoved(ProcessInfo pi) {
		if(this.stopingProcess.containsKey(pi.getId())) {
			stopingProcess.remove(pi.getId());
		}
		
		if(sysProcess.containsKey(pi.getId())) {
			sysProcess.remove(pi.getId());
		}
		
		Set<ProcessInfo>  set = this.insManager.getProcessesByDepId(pi.getDepId());
		if(set == null || set.isEmpty()) {
			//删除分配
			deleteAssignDepNode(pi.getDepId());
		}
	}
	
	private void instanceAdded(ProcessInfo pi) {
		if(pi == null) {
			logger.error("ProcessInfo is NULL");
			return;
		}
		if(StringUtils.isEmpty(pi.getAgentId())) {
			//非Agent环境下启动的实例
			return;
		}
		if(pi.getAgentId().equals(this.agentInfo.getId())) {
			//processInfos.put(pi.getId(), pi);
			//启动成功后，实例由InstanceManager管理
			startingProcess.remove(pi.getId());
			logger.info("Insatnce add: " + pi.toString());
		}
	}

	private void deploymentRemoved(String depId) {
		ProcessInfo p = null;
		for( ProcessInfo pi : this.insManager.getProcessesByDepId(depId) ) {
			if(depId.equals(pi.getDepId())) {
				p = pi;
				break;
			}
		}
		if(p != null) {
			stopProcess(p);
		} else {
			logger.warn("Process for dep: " + depId + " not found!");
		}
		
	}

	private void deploymentAdded(String depId) {
		if(this.agentInfo.isPrivat()) {
			if(this.initDepIds == null || this.initDepIds.length == 0) {
				logger.error("Private agent but initDepIds is null!");
				return;
			}else {
				boolean f = false;
				for(String did : this.initDepIds) {
					if(did.equals(depId)) {
						f = true;
						break;
					}
				}
				
				if(!f) {
					String msg = "Private agent not responsible for dep:[" + depId+"] initDepIds : "
							+ Arrays.toString(this.initDepIds);
					SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_WARN, TAG,msg);
					logger.error(msg);
					return;
				}
			}
		}
		
		Deployment dep = getDeployment(depId);
		if(dep == null) {
			String msg = "Deployment not found for ID:" + depId + ", initDepIds: "+ Arrays.toString(this.initDepIds);
			logger.error(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_WARN, TAG,msg);
			return;
		}
		
		if(!dep.isEnable()) {
			deleteAssignDepNode(dep.getId());
			return;
		}
		
		startDeps.offer(dep);
		
		/*new Thread(()-> {
			startDep(dep);
		}).start();*/
		
	}
	
	private void deleteAssignDepNode(String depId) {
		String assignDepPath = path + "/" + depId;
		if(op.exist(assignDepPath)) {
			String msg = "Delete deployment node: " + assignDepPath;
			logger.warn(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_WARN, TAG,msg);
			op.deleteNode(assignDepPath);
		}
	}

	private Deployment getDeployment(String depId) {
		String depPath = ChoyConstants.DEP_DIR + "/" + depId;
		String data = op.getData(depPath);
		Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
		return dep;
	}

	private boolean startDep(Deployment dep) {
		if(dep == null) {
			logger.error("Invalid dep for NULL!");
			return false;
		}
		
		boolean doContinue = true;
		if(!checkRes(dep.getJarFile()) ) {
			//Jar文件还不存在，先下载资源
			String msg = "Begin download: "+dep.getJarFile();
			logger.info(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_WARN, TAG,msg);
			doContinue = downloadJarFile(dep.getJarFile(),dep.getId());
		}
		
		if(!checkRes(this.javaAgentJarFile) ) {
			//Jar文件还不存在，先下载资源
			String msg = "Begin download: "+this.javaAgentJarFile;
			logger.info(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_WARN, TAG,msg);
			doContinue = downloadJarFile(this.javaAgentJarFile,dep.getId());
		}
		
		if(!doContinue) {
			String msg = "Start deployment fail pls check yourself dep: " + dep.toString();
			logger.error(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, TAG,msg);
			return false;
		}
		
		updateAssign(dep.getId(), AssignState.STARTING);
		
		String assignData = op.getData(this.path +"/" + dep.getId()); //idServer.getStringId(ProcessInfo.class);
		if(StringUtils.isEmpty(assignData)) {
			String msg = "Assign data is null when start dep: " + dep.getId();
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, TAG,msg);
			throw new CommonException(msg);
		}
		
		Assign a = JsonUtils.getIns().fromJson(assignData, Assign.class);
		
		String args = dep.getArgs();
		
		List<String> list = new ArrayList<String>();
		
		list.add("java");
		list.add("-javaagent:" + this.resourceDir + File.separatorChar + this.javaAgentJarFile);
		
		list.add("-jar");
		list.add(this.resourceDir + File.separatorChar + dep.getJarFile());
		
		if(StringUtils.isNotEmpty(args)) {
			list.add(args);
		}
		
		if(SystemUtils.isWindows()) {
			
		}else if(SystemUtils.isLinux()) {
			
		} else {
			logger.error("Not support operation system:" + SystemUtils.getOSname());
			return false;
		}
		
		list.add("-D" + ChoyConstants.ARG_INSTANCE_ID+"=" + a.getInsId());
		list.add("-D" + ChoyConstants.ARG_MYPARENT_ID+"=" + SystemUtils.getProcessId());
		list.add("-D" + ChoyConstants.ARG_DEP_ID+"=" + dep.getId());
		list.add("-D" + ChoyConstants.ARG_AGENT_ID+"=" + this.agentInfo.getId());
		
		logger.info("Dep args: " + dep.getArgs());
		//list.add(dep.getArgs());
		if(StringUtils.isNotEmpty(dep.getArgs())) {
			Map<String,String> params = IAssignStrategy.parseArgs(dep.getArgs());
			for(Map.Entry<String,String> e: params.entrySet()) {
				if(logger.isDebugEnabled()) {
					logger.debug(e.getKey() + "=" +e.getValue());
				}
				list.add("-D" + e.getKey()+"=" + e.getValue());
			}
		}
		
		ProcessBuilder pb = new ProcessBuilder(list);
		File wd = new File(workDir + "/" + a.getInsId());
		wd.mkdirs();
		pb.directory(wd);
		
		File errorFile = new File(wd,"error.log");
		pb.redirectError(errorFile);
		
		File outputFile = new File(wd,"output.log");
		pb.redirectOutput(outputFile);
		
		File processInfoData = new File(wd,a.getInsId()+".json");
		
		list.add("-D"+ChoyConstants.PROCESS_INFO_FILE + "=" + processInfoData.getAbsolutePath());
		
		OutputStream os = null;

		try {
			
			ProcessInfo pi = new ProcessInfo();
			pi.setActive(false);
			pi.setAgentId(agentInfo.getId());
			pi.setCmd(list.toString());
			pi.setDepId(dep.getId());
			pi.setId(a.getInsId());
			pi.setWorkDir(wd.getAbsolutePath());
			pi.setAgentProcessId(SystemUtils.getProcessId());
			pi.setAgentHost(Config.getHost());
			pi.setAgentInstanceName(Config.getInstanceName());
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(processOpTimeout);
			//pi.setStartTime(pi.getOpTime());
			
			//通过文件传递给子进程，再由子进程在启动后存入ZK
			String data = JsonUtils.getIns().toJson(pi);
			if(!SystemUtils.setFileString(processInfoData,data)) {
				throw new CommonException("Write file error: " + processInfoData.getAbsolutePath());
			}
			
			Process p = pb.start();
			pi.setProcess(p);
			sysProcess.put(pi.getId(), p);
			
			this.startingProcess.put(pi.getId(), pi);
			String msg = "Start process: " + data;
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_INFO, TAG,msg);
			logger.info(msg);
			return true;
		} catch (IOException e) {
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, this.getClass(),"",e);
			logger.error("",e);
			return false;
		} finally {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void updateAssign(String depId, AssignState s) {
		String data = op.getData(this.path + "/" + depId);
		Assign a = JsonUtils.getIns().fromJson(data, Assign.class);
		if(a == null) {
			throw new CommonException("Assign not found dor depID: " + depId);
		}
		a.opTime = System.currentTimeMillis();
		if(s != null) {
			a.state = s;
		}
	    op.setData(this.path+"/" + depId,JsonUtils.getIns().toJson(a));
		return;
	}
	
	
	private boolean downloadJarFile(String jarFile,String depId) {
		int resId = respo.initDownloadResource(jarFile);
		if(resId <= 0) {
			String msg = "Download ["+jarFile+"] fail with resource id: "+ resId;
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, TAG,msg);
			logger.error(msg);
			return false;
		}
		
		long curTime = System.currentTimeMillis();
		
		updateAssign(depId,AssignState.DOWNLOAD_RES);
		FileOutputStream fos = null;
		try {
			File f = new File(this.resourceDir,jarFile);
			f.createNewFile();
			
			fos = new FileOutputStream(f);
			String msg = "Begin download: "+jarFile;
			logger.info(msg);
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, TAG,msg);
			while(true) {
				byte[] data = respo.downResourceData(resId, 0);
				
				long ctime = System.currentTimeMillis();
				if(ctime - curTime > 10) {
					//通知Controller分配还在下载数据，不要超时关停此分配
					updateAssign(depId,null);
					curTime = ctime;
				}
				
				if(data != null && data.length > 0) {
					logger.info("Got one block: " + data.length + "B");
					fos.write(data, 0, data.length);
				}
				
				if(data == null || data.length < this.uploadBlockSize) {
					 msg = "Finish download: "+jarFile+" with size: " + f.length();
					logger.info(msg);
					SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, TAG,msg);
					return true;
				}
			}
		} catch (IOException e) {
			SF.eventLog(MC.MT_AGENT_LOG,MC.LOG_ERROR, this.getClass(),jarFile,e);
			logger.error("Download ["+jarFile+"]",e);
			return false;
		}finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("",e);
				}
			}
		}
		
	}

	private boolean checkRes(String jarFile) {
		return new File(this.resourceDir,jarFile).exists();
	}

	private void stopProcess(ProcessInfo pi) {
		if(pi == null) {
			logger.warn("Receive NULL process info for stop deployment");
			return;
		}
		
		String p = ChoyConstants.INS_ROOT+"/"+pi.getId();
		if(op.exist(p) && pi.isActive()) {
			pi.setActive(false);
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(processOpTimeout);
			this.stopingProcess.put(pi.getId(), pi);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			logger.info("Stop process: " + data);
		}
	}
}
