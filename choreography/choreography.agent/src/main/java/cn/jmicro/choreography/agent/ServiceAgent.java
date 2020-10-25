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
import cn.jmicro.api.Resp;
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
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.sysstatis.SystemStatisManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.SystemUtils;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.api.genclient.IResourceResponsitory$JMAsyncClient;
import cn.jmicro.choreography.assign.Assign;
import cn.jmicro.choreography.assign.AssignState;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:41:17
 */
@Component(level = 100)
public class ServiceAgent {

	private static final Logger logger = LoggerFactory.getLogger(ServiceAgent.class);

	private static final Class<?> TAG = ServiceAgent.class;

	// @Cfg("/ServiceAgent/workDir")
	private String workDir;
	
	//private byte logLevel = LG.SYSTEM_LOG_LEVEL;

	// @Cfg("/ServiceAgent/resourceDir")
	private String resourceDir; // = System.getProperty("user.dir") + "/resourceDir";

	//@Cfg(value = "/ServiceAgent/javaAgentJarFile", defGlobal = true)
	private String javaAgentJarFile = "jmicro-agent-"+Constants.VERSION+"-"+Constants.JMICRO_RELEASE_LABEL+".jar";

	@Cfg(value = "/ResourceReponsitoryService/uploadBlockSize", defGlobal = true)
	private int uploadBlockSize = 65300;// 1024*1024;

	// will be cancel the start process when start time
	// will be force stop the process when process not response to stop action
	// before timeout
	@Cfg(value = "/ServiceAgent/processOpTimeout", defGlobal = true)
	private int processOpTimeout = 2 * 60 * 1000 + 20 * 1000; // 20秒是ZK结点超时时间

	@Inject
	private Config cfg;

	private File workDirFile = null;

	private File resourceDirFile = null;

	private Map<Integer, ProcessInfo> startingProcess = new HashMap<>();

	private Map<Integer, ProcessInfo> stopingProcess = new HashMap<>();

	private Map<Integer, Process> sysProcess = new HashMap<>();

	private Queue<Assign> startAssigns = new ConcurrentLinkedQueue<>();

	@Reference(namespace = "rrs", version = "*")
	private IResourceResponsitory$JMAsyncClient respo;

	@Inject
	private InstanceManager insManager;

	@Inject
	private IDataOperator op;

	@Inject
	private ComponentIdServer idServer;

	@Inject
	private SystemStatisManager ssm;

	@Inject
	private AssignManager assignManager;

	private AgentInfo agentInfo;

	@Inject
	private ILockerManager lockMgn;

	private String path;

	private String activePath;

	private String[] initDepIds;

	private IAssignListener assignListener = new IAssignListener() {
		@Override
		public void change(int type, Assign as) {
			if (type == IListener.ADD) {
				LG.log(MC.LOG_DEBUG, TAG, "Got assign add: " + as.toString());
				assignAdded(as);
			} else if (type == IListener.REMOVE) {
				LG.log(MC.LOG_DEBUG, TAG, "Got assign remove: " + as.toString());
				assignRemoved(as.getInsId());
			} else if (type == IListener.DATA_CHANGE) {
				LG.log(MC.LOG_DEBUG, TAG, "Got assign change: " + as.toString());
				assignDataChange(as);
			}
		}
	};

	private IDataListener cmdListener = (path0, cmd) -> {
		switch (cmd) {
		case ChoyConstants.AGENT_CMD_STARTING_TIMEOUT:
		case ChoyConstants.AGENT_CMD_STOPING_TIMEOUT:
			logger.warn("Reset deployment listenr for cmd: " + cmd);
			// op.removeChildrenListener(path,deploymentListener);
			// op.addChildrenListener(path, deploymentListener);
			break;
		case ChoyConstants.AGENT_CMD_CLEAR_LOCAL_RESOURCE:
			String msg = "Clear resource by cmd: " + cmd;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG, msg);
			clearLocalRes();
			break;
		case ChoyConstants.AGENT_CMD_STOP_ALL_INSTANCE:
			 msg = "Stop all process by cmd: " + cmd;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG, msg);
			stopAllInstance();
			break;
		}
	};

	private IDataListener agentDataListener = (path0, data) -> {
		agentInfo = JsonUtils.getIns().fromJson(data, AgentInfo.class);
	};

	public void ready() {

		workDir = cfg.getString(Constants.INSTANCE_DATA_DIR, "") + File.separatorChar + "agentInstanceDir";
		workDirFile = new File(workDir);
		if (!workDirFile.exists()) {
			workDirFile.mkdir();
		}

		resourceDir = cfg.getString(Constants.INSTANCE_DATA_DIR, "") + File.separatorChar + "resourceDir";
		resourceDirFile = new File(resourceDir);
		if (!resourceDirFile.exists()) {
			resourceDirFile.mkdir();
		}

		File infoFile = new File(cfg.getString(Constants.INSTANCE_DATA_DIR, ""), "agent.json");
		if (infoFile.exists()) {
			String existAgentJson = SystemUtils.getFileString(infoFile);
			if (StringUtils.isNotEmpty(existAgentJson)) {
				agentInfo = JsonUtils.getIns().fromJson(existAgentJson, AgentInfo.class);
				activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/" + agentInfo.getId();
				if (op.exist(activePath)) {
					logger.warn("Only one agent can be exist for one resourceDir: " + resourceDir);
					System.exit(0);
					return;
				}
			}
		}

		if (agentInfo == null) {
			agentInfo = new AgentInfo();
			//agentInfo.setId(idServer.getStringId(AgentInfo.class));
		}

		boolean privat = cfg.getBoolean(ChoyConstants.ARG_AGENT_PRIVATE, false);
		agentInfo.setPrivat(privat);

		String deps = cfg.getString(ChoyConstants.ARG_INIT_DEP_IDS, null);
		if (privat && StringUtils.isEmpty(deps)) {
			throw new CommonException("Private agent init depId cannot be NULL");
		}

		//agentInfo.setName(Config.getInstanceName());
		agentInfo.setId(Config.getInstanceName());
		agentInfo.setStartTime(System.currentTimeMillis());
		agentInfo.setAssignTime(agentInfo.getAssignTime());
		agentInfo.setHost(Config.getExportSocketHost());
		agentInfo.setSs(ssm.getStatis());
		agentInfo.setInitDepIds(deps);

		String agJson = JsonUtils.getIns().toJson(agentInfo);
		SystemUtils.setFileString(infoFile, agJson);

		path = ChoyConstants.ROOT_AGENT + "/" + agentInfo.getId();
		activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/" + agentInfo.getId();

		// run specify deployments
		if (StringUtils.isNotEmpty(deps)) {
			setInitDeps(deps);
		}

		//op.createNodeOrSetData(assignPath, "" , false); //分配给此Agent的全部部署ID

		insManager.filterByAgent(agentInfo.getId());
		insManager.addListener((type, pi) -> {
			if(type == IListener.ADD) {
				instanceAdded(pi);
			}else if (type == IListener.REMOVE) {
				String msg = "Instance remove: " + pi.toString();
				LG.log(MC.LOG_WARN, TAG, msg);
				logger.info(msg);
				instanceRemoved(pi);
			}
		});

		op.createNodeOrSetData(path, agJson, IDataOperator.PERSISTENT); //
		if (op.exist(activePath)) {
			op.deleteNode(activePath);
		} else {
			op.createNodeOrSetData(activePath, ChoyConstants.AGENT_CMD_NOP, IDataOperator.EPHEMERAL); // 代表此Agent的存活标志
		}

		this.assignManager.doInit(assignListener, agentInfo.getId());

		// 监控分配新的应用
		// op.addChildrenListener(path,deploymentListener);

		op.addDataListener(path, agentDataListener);

		op.addDataListener(activePath, cmdListener);

		TimerTicker.doInBaseTicker(5, Config.getInstanceName() + "_ServiceAgent", null, (key, att) -> {
			try {
				/*
				 * if(System.currentTimeMillis() - agentInfo.getStartTime() < 5000) { return; }
				 */
				checkStatus();
				LG.log(MC.LOG_TRANCE, TAG, "Do one check");
			} catch (Throwable e) {
				logger.error("doChecker", e);
				LG.log(MC.LOG_ERROR, TAG, "doChecker", e);
			}
		});
	}

	private void stopAllInstance() {
		Set<ProcessInfo> ps = this.insManager.getProcessesByAgentId(this.agentInfo.getId());
		if (ps != null && !ps.isEmpty()) {
			for (ProcessInfo pi : ps) {
				stopProcess(pi);
			}
		} else {
			String msg = "No process to stop by stop command";
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG, msg);
		}
	}

	private void clearLocalRes() {
		File[] fs = new File(resourceDir).listFiles();
		for (File f : fs) {
			if (f.isFile()) {
				f.delete();
			}
		}
	}

	private boolean hasController() {
		if (!op.exist(ChoyConstants.ROOT_CONTROLLER)) {
			return false;
		}
		Set<String> controllers = op.getChildren(ChoyConstants.ROOT_CONTROLLER, false);
		return controllers != null && controllers.size() > 0;
	}

	private void setInitDeps(String deps) {
		String[] depIds = deps.split(",");
		if (depIds == null || depIds.length == 0) {
			return;
		}

		initDepIds = depIds;

		if (this.hasController()) {
			// manager by controllers
			return;
		}

		// start instances specify by depId
		for (String depId : depIds) {
			startInitDeployment(depId);
		}
	}

	private void assignDataChange(Assign as) {
		if (as.state == AssignState.STOPING) {
			this.stopProcess(this.insManager.getProcessesByInsId(as.getInsId(), false));
		}
	}

	private void startInitDeployment(String depId) {
		if (StringUtils.isEmpty(depId)) {
			return;
		}
		depId = depId.trim();
		Deployment dep = this.getDeployment(depId);
		if (dep == null || !dep.isEnable()) {
			return;
		}

		Map<String, String> params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
		String agentIds = params.get(IAssignStrategy.AGENT_ID);
		if (StringUtils.isEmpty(agentIds)) {
			String msg = "Deployment ID [" + depId + "] not contain angent ID [" + agentInfo.getId() + "]";
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			return;
		}

		boolean f = false;
		String[] agids = agentIds.split(",");
		for (String aid : agids) {
			if (aid.equals(agentInfo.getId())) {
				// deployment descriptor argument with name agentId equals this agentId
				f = true;
				break;
			}
		}

		if (f) {
			Integer pid = Integer.parseInt(op.getData(ChoyConstants.ID_PATH)) + 1;
			op.setData(ChoyConstants.ID_PATH, pid+"");
			Assign a = new Assign(dep.getId(), this.agentInfo.getId(), pid);
			a.opTime = System.currentTimeMillis();
			a.state = AssignState.INIT;
			op.createNodeOrSetData(path + "/" + depId, JsonUtils.getIns().toJson(a), IDataOperator.PERSISTENT);
		} else {
			String msg = "Deployment ID [" + depId + "] not contain init angent Id [" + agentInfo.getId() + "]";
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
		}

	}

	private void recreateAgentInfo() {
		agentInfo.setAssignTime(System.currentTimeMillis());
		String data = JsonUtils.getIns().toJson(agentInfo);
		
		String msg = "Recreate angent info: " + data;
		LG.log(MC.LOG_WARN, TAG, msg);
		logger.warn(msg);
		
		op.createNodeOrSetData(path, data, IDataOperator.PERSISTENT);
		// op.addChildrenListener(path,deploymentListener);
		op.addDataListener(path, agentDataListener);
	}

	private void setStatisData() {
		agentInfo.setSs(ssm.getStatis());

		String data = JsonUtils.getIns().toJson(agentInfo);
		op.setData(path, data);
	}

	private void checkStatus() {
		long curTime = System.currentTimeMillis();
		if (curTime - agentInfo.getAssignTime() < 5000) {
			return;
		}

		if (!op.exist(path)) {
			recreateAgentInfo();
			return;
		}

		if (!op.exist(activePath)) {
			op.createNodeOrSetData(activePath, ChoyConstants.AGENT_CMD_NOP, IDataOperator.EPHEMERAL); // 代表此Agent的存活标志
			op.addDataListener(activePath, cmdListener);
		}

		setStatisData();

		if (!startAssigns.isEmpty()) {
			Assign as = null;
			while ((as = startAssigns.poll()) != null) {
				startDep(as);
			}
		}

		if (!startingProcess.isEmpty()) {
			Set<Integer> keySet = new HashSet<>();
			keySet.addAll(startingProcess.keySet());
			for (Integer k : keySet) {
				ProcessInfo pi = startingProcess.get(k);
				if (curTime - pi.getOpTime() > pi.getTimeOut()) {
					// 已经命令进程停止，并且超过超时时间还没成功退出
					
					String msg = "Start timeout: " + pi.toString();
					LG.log(MC.LOG_WARN, TAG, msg);
					
					logger.warn(msg);
					startingProcess.remove(k);
					forceStopProcess(k);
					deleteAssignDepNode(pi.getId());
				}
			}
		}

		if (!stopingProcess.isEmpty()) {
			Set<Integer> keySet = new HashSet<>();
			keySet.addAll(stopingProcess.keySet());
			for (Integer k : keySet) {
				ProcessInfo pi = stopingProcess.get(k);
				//String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
				if (curTime - pi.getOpTime() > pi.getTimeOut()) {
					// 已经命令进程停止，并且超过超时时间还没成功退出
					String msg = "Stop timeout and force stop it: " + pi.toString();
					LG.log(MC.LOG_WARN, TAG, msg);
					logger.warn(msg);
					stopingProcess.remove(k);
					forceStopProcess(k);
					deleteAssignDepNode(pi.getId());
				}
			}
		}

		if (this.initDepIds != null && this.initDepIds.length > 0 && !this.hasController()) {
			for (String depId : this.initDepIds) {
				int size = this.insManager.getProcessSizeByDepId(depId);
				if (size == 0) {
					startInitDeployment(depId);
				}
			}
		}

	}

	private void forceStopProcess(Integer pid) {

		Process p = sysProcess.get(pid);
		if (p != null) {
			
			String msg = "Force kill process " + pid;
			LG.log(MC.LOG_WARN, TAG, msg);
			
			logger.warn(msg);
			p.destroyForcibly();
			sysProcess.remove(pid);
		} else {
			String cmd = "kill -15 " + pid;
			if (SystemUtils.isWindows()) {
				cmd = "taskkill /f /pid " + pid;
			}

			try {
				
				String msg = "Force kill with command: " + cmd;
				LG.log(MC.LOG_WARN, TAG, msg);
				
				logger.warn(msg);
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				String msg = "fail to stop process : " + pid;
				LG.log(MC.LOG_ERROR, TAG, msg,e);
				logger.error(msg,e);
			}
		}
	}

	private void instanceRemoved(ProcessInfo pi) {
		if (this.stopingProcess.containsKey(pi.getId())) {
			stopingProcess.remove(pi.getId());
		}

		if (sysProcess.containsKey(pi.getId())) {
			sysProcess.remove(pi.getId());
		}

		deleteAssignDepNode(pi.getId());
	}

	private void instanceAdded(ProcessInfo pi) {
		if (pi == null) {
			
			String msg = "ProcessInfo is NULL";
			LG.log(MC.LOG_ERROR, TAG, msg);
			
			logger.error(msg);
			return;
		}
		if (StringUtils.isEmpty(pi.getAgentId())) {
			// 非Agent环境下启动的实例
			return;
		}
		if (pi.getAgentId().equals(this.agentInfo.getId())) {
			// processInfos.put(pi.getId(), pi);
			// 启动成功后，实例由InstanceManager管理
			startingProcess.remove(pi.getId());
			
			String msg = "Insatnce add: " + pi.toString();
			LG.log(MC.LOG_INFO, TAG, msg);
			
			logger.info(msg);
		}
	}

	private void assignRemoved(Integer insId) {
		ProcessInfo p = this.insManager.getProcessesByInsId(insId, false);
		if (p != null) {
			stopProcess(p);
		} else {
			String msg = "Process for instance: " + insId + " not found!";
			LG.log(MC.LOG_WARN, TAG, msg);
			
			logger.warn(msg);
		}
	}

	private void assignAdded(Assign as) {
		if (this.agentInfo.isPrivat()) {
			if (this.initDepIds == null || this.initDepIds.length == 0) {
				
				String msg = "Private agent but initDepIds is null!";
				LG.log(MC.LOG_ERROR, TAG, msg);
				
				logger.error(msg);
				return;
			} else {
				boolean f = false;
				for (String did : this.initDepIds) {
					if (did.equals(as.getDepId())) {
						f = true;
						break;
					}
				}

				if (!f) {
					String msg = "Private agent not responsible for dep:[" + as.getDepId() + "] initDepIds : "
							+ Arrays.toString(this.initDepIds);
					LG.log(MC.LOG_ERROR, TAG, msg);
					
					logger.error(msg);
					return;
				}
			}
		}

		Deployment dep = getDeployment(as.getDepId());
		if (dep == null) {
			String msg = "Deployment not found for ID:" + as.getDepId() + ", initDepIds: "
					+ Arrays.toString(this.initDepIds);
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			return;
		}

		if (!dep.isEnable()) {
			deleteAssignDepNode(as.getInsId());
			return;
		}

		Set<ProcessInfo> pis = this.insManager.getProcessesByDepId(as.getDepId());
		if (pis != null && !pis.isEmpty()) {
			ProcessInfo pi = pis.iterator().next();
			String msg = "Assign process exist: " + pi.toString();
			LG.log(MC.LOG_INFO, TAG, msg);
			logger.info(msg);
			return;
		}
		
		if(this.insManager.isExistByProcessId(as.getInsId())) {
			LG.log(MC.LOG_WARN, TAG, "Assing have been exist: " + as.toString());
			return;
		}

		startAssigns.offer(as);

		/*
		 * new Thread(()-> { startDep(dep); }).start();
		 */

	}

	private void deleteAssignDepNode(Integer insId) {
		String assignDepPath = path + "/" + insId;
		if (op.exist(assignDepPath)) {
			String msg = "Delete deployment node: " + assignDepPath;
			LG.log(MC.LOG_WARN, TAG, msg);
			logger.warn(msg);
			op.deleteNode(assignDepPath);
		}
	}

	private Deployment getDeployment(String depId) {
		String depPath = ChoyConstants.DEP_DIR + "/" + depId;
		String data = op.getData(depPath);
		Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
		return dep;
	}

	private boolean startDep(Assign as) {
		if (as == null) {
			String msg = "Invalid dep for NULL!";
			LG.log( MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			return false;
		}
		
		Deployment dep = getDeployment(as.getDepId());
		/*if(!dep.getJarFile().contains(Constants.VERSION) 
				|| !dep.getJarFile().contains(Constants.JMICRO_RELEASE_LABEL)) {
			
		}*/
		
		boolean doContinue = true;
		if (!checkRes(dep.getJarFile())) {
			// Jar文件还不存在，先下载资源
			//String msg = "Begin download: " + dep.getJarFile();
			//LG.log(MC.LOG_INFO, TAG, msg);
			//logger.info(msg);
			doContinue = downloadJarFile(dep.getJarFile(), as);
		}

		if (!checkRes(this.javaAgentJarFile)) {
			// Jar文件还不存在，先下载资源
			//String msg = "Begin download: " + this.javaAgentJarFile;
			//LG.log(MC.LOG_INFO, TAG, msg);
			//logger.info(msg);
			doContinue = downloadJarFile(this.javaAgentJarFile, as);
		}

		if (!doContinue) {
			String msg = "Start deployment fail pls check yourself dep: " + dep.toString();
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			deleteAssignDepNode(as.getInsId());
			return false;
		}

		updateAssign(as, AssignState.STARTING);

		String assignData = op.getData(this.path + "/" + as.getInsId()); // idServer.getStringId(ProcessInfo.class);
		if (StringUtils.isEmpty(assignData)) {
			String msg = "Assign data is null when start dep: " + dep.getId();
			LG.log(MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}

		Assign a = JsonUtils.getIns().fromJson(assignData, Assign.class);

		String args = dep.getArgs();

		List<String> list = new ArrayList<String>();

		list.add("java");
		list.add("-javaagent:" + this.resourceDir + File.separatorChar + this.javaAgentJarFile);

		list.add("-jar");
		list.add(this.resourceDir + File.separatorChar + dep.getJarFile());

		if (StringUtils.isNotEmpty(args)) {
			list.add(args);
		}

		if(SystemUtils.isWindows()) {

		} else if (SystemUtils.isLinux()) {

		} else {
			String msg = "Not support operation system:" + SystemUtils.getOSname();
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			return false;
		}

		list.add("-D" + ChoyConstants.ARG_INSTANCE_ID + "=" + a.getInsId());
		list.add("-D" + ChoyConstants.ARG_MYPARENT_ID + "=" + SystemUtils.getProcessId());
		list.add("-D" + ChoyConstants.ARG_DEP_ID + "=" + dep.getId());
		list.add("-D" + ChoyConstants.ARG_AGENT_ID + "=" + this.agentInfo.getId());
		list.add("-D" + Constants.CLIENT_ID + "=" + dep.getClientId());
		list.add("-D" + Constants.ADMIN_CLIENT_ID + "=" + Config.getAdminClientId());
		
		String msg = "Start process args: " + dep.getArgs();
		LG.log(MC.LOG_INFO, TAG, msg);
		logger.info(msg);
		
		// list.add(dep.getArgs());
		if (StringUtils.isNotEmpty(dep.getArgs())) {
			Map<String, String> params = IAssignStrategy.parseArgs(dep.getArgs());
			for (Map.Entry<String, String> e : params.entrySet()) {
				if (logger.isDebugEnabled()) {
					logger.debug(e.getKey() + "=" + e.getValue());
				}
				list.add("-D" + e.getKey() + "=" + e.getValue());
			}
		}

		ProcessBuilder pb = new ProcessBuilder(list);
		File wd = new File(workDir + "/" + a.getInsId());
		wd.mkdirs();
		pb.directory(wd);

		File logDir = new File(wd, "logs");
		if (!logDir.exists()) {
			logDir.mkdir();
		}

		File logFile = new File(logDir, "output.log");
		pb.redirectError(logFile);
		// File outputFile = new File(wd,"output.log");
		pb.redirectOutput(logFile);

		File processInfoData = new File(wd, "processInfo.json");

		list.add("-D" + ChoyConstants.PROCESS_INFO_FILE + "=" + processInfoData.getAbsolutePath());

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
			pi.setAgentHost(Config.getExportSocketHost());
			pi.setAgentInstanceName(Config.getInstanceName());
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(processOpTimeout);
			// pi.setStartTime(pi.getOpTime());

			// 通过文件传递给子进程，再由子进程在启动后存入ZK
			String data = JsonUtils.getIns().toJson(pi);
			if (!SystemUtils.setFileString(processInfoData, data)) {
				
				msg = "Write file error: " + processInfoData.getAbsolutePath();
				LG.log(MC.LOG_ERROR, TAG, msg);
				
				throw new CommonException(msg);
			}

			Process p = pb.start();
			pi.setProcess(p);
			sysProcess.put(pi.getId(), p);

			this.startingProcess.put(pi.getId(), pi);
			 msg = "Start process: " + data;
			 LG.log(MC.LOG_INFO, TAG, msg);
			logger.info(msg);
			return true;
		} catch (IOException e) {
			LG.log(MC.LOG_ERROR, TAG, "", e);
			logger.error("", e);
			return false;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void updateAssign(Assign as, AssignState s) {
		String data = op.getData(this.path + "/" + as.getInsId());
		Assign a = JsonUtils.getIns().fromJson(data, Assign.class);
		if (a == null) {
			String msg = "Assign not found dor insId: " + as.getInsId();
			LG.log(MC.LOG_ERROR, TAG, msg);
			throw new CommonException(msg);
		}
		a.opTime = System.currentTimeMillis();
		if (s != null) {
			a.state = s;
		}
		op.setData(this.path + "/" + as.getInsId(), JsonUtils.getIns().toJson(a));
		return;
	}

	private boolean downloadJarFile(String jarFile, Assign as) {
		final Resp<Integer> resp = respo.initDownloadResource(jarFile);
		if (resp.getCode() != 0) {
			String msg = "Download [" + jarFile + "] fail with error: " + resp.getMsg();
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.error(msg);
			return false;
		}

		final long[] curTime = new long[] { System.currentTimeMillis() };

		updateAssign(as, AssignState.DOWNLOAD_RES);
		
		//final FileOutputStream fos;
		File f = new File(this.resourceDir, jarFile);
		try {
			f.createNewFile();
		} catch (IOException e1) {
			logger.error("",e1);
			LG.log(MC.LOG_ERROR, TAG, "create file error",e1);
			return false;
		}
		
		boolean[] rst = new boolean[] {true};
		
		try(FileOutputStream fos = new FileOutputStream(f)) {

			String msg = "Begin download: " + jarFile;
			LG.log(MC.LOG_INFO, TAG, msg);
			
			//logger.info(msg);
			
			boolean[] loop = new boolean[] {true};
			
			while (loop[0]) {

				byte[] data = respo.downResourceData(resp.getData(), 0);

				long ctime = System.currentTimeMillis();
				if (ctime - curTime[0] > 3000) {
					//通知Controller分配还在下载数据，不要超时关停此分配
					updateAssign(as, null);
					curTime[0] = ctime;
				}

				if (data != null && data.length > 0) {
					String msg0 = "Got one block: " + data.length + "B";
					LG.logWithNonRpcContext(MC.LOG_DEBUG, TAG, msg0);
					try {
						fos.write(data, 0, data.length);
					} catch (IOException e) {
						LG.logWithNonRpcContext(MC.LOG_ERROR, TAG, "Write jar file error", e);
						f.delete();
						return false;
					}
				}

				if (data == null || data.length == 0 || data.length < this.uploadBlockSize) {
					String msg0 = "Finish download: " + jarFile + " with size: " + f.length();
					LG.logWithNonRpcContext(MC.LOG_INFO, TAG, msg0);
					return true;
				}
				
				/*respo.downResourceDataJMAsync(resp.getData(), 0)
				.success((data,cxt)->{

					long ctime = System.currentTimeMillis();
					if (ctime - curTime[0] > 3000) {
						//通知Controller分配还在下载数据，不要超时关停此分配
						updateAssign(as, null);
						curTime[0] = ctime;
					}

					if (data != null && data.length > 0) {
						String msg0 = "Got one block: " + data.length + "B";
						LG.logWithNonRpcContext(MC.LOG_DEBUG, TAG, msg0);
						try {
							fos.write(data, 0, data.length);
						} catch (IOException e) {
							LG.logWithNonRpcContext(MC.LOG_ERROR, TAG, "Write jar file error", e);
							rst[0] = false;
							loop[0] = false;
						}
					}

					if (data == null || data.length == 0 || data.length < this.uploadBlockSize) {
						String msg0 = "Finish download: " + jarFile + " with size: " + f.length();
						LG.logWithNonRpcContext(MC.LOG_INFO, TAG, msg0);
						rst[0] = true;
						loop[0] = false;
					}
					
					synchronized(resp) {
						resp.notify();
					}
				
				})
				.fail((code,err,cxt)->{
					LG.logWithNonRpcContext(MC.LOG_ERROR, TAG, "Req jar file data error: " + jarFile+" with " + err);
					rst[0] = false;
					loop[0] = false;
					synchronized(resp) {
						resp.notify();
					}
				});
				
				synchronized(resp) {
					try {
						resp.wait(8000);
					} catch (InterruptedException e1) {
						LG.log(MC.LOG_ERROR, TAG, "Req jar file data error: " + jarFile,e1);
						rst[0] = false;
						loop[0] = false;
					}
				}*/
			}
		} catch (Throwable e) {
			f.delete();
			String msg = "Fail to download [" + jarFile + "]";
			LG.log(MC.LOG_ERROR, TAG, msg);
			//logger.error(msg, e);
			rst[0] = false;
		} /*finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error("", e);
				}
			}
		}*/
		return rst[0];
	}

	private boolean checkRes(String jarFile) {
		return new File(this.resourceDir, jarFile).exists();
	}

	private void stopProcess(ProcessInfo pi) {
		if (pi == null) {
			String msg = "Receive NULL process info for stop deployment";
			LG.log(MC.LOG_ERROR, TAG, msg);
			logger.warn(msg);
			return;
		}

		String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
		if (op.exist(p) && pi.isActive()) {
			pi.setActive(false);
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(processOpTimeout);
			this.stopingProcess.put(pi.getId(), pi);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			
			String msg = "Stop process: " + data;
			LG.log(MC.LOG_INFO, TAG, msg);
			
			logger.info(msg);
		}
	}

}
