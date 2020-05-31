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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.sysstatis.SystemStatisManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.base.AgentInfo;
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
@Component
public class ServiceAgent {

	private static final Logger logger = LoggerFactory.getLogger(ServiceAgent.class);
	
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
	
	//private Map<String,Deployment> deployments = new HashMap<>();
	
	//private Map<String,Long> processTimeout = new HashMap<>();
	
	private Map<String,ProcessInfo> processInfos = new HashMap<>();
	//private Map<String,ProcessInfo> dep2Process = new HashMap<>();
	
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
		
		agentInfo.setName(Config.getInstanceName());
		agentInfo.setStartTime(System.currentTimeMillis());
		agentInfo.setAssignTime(agentInfo.getAssignTime());
		agentInfo.setHost(Config.getHost());
		agentInfo.setSs(ssm.getStatis());
		
		String agJson = JsonUtils.getIns().toJson(agentInfo);
		SystemUtils.setFileString(infoFile, agJson);
		
		path = ChoyConstants.ROOT_AGENT + "/" + agentInfo.getId();
		activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+ agentInfo.getId();
		
		op.createNodeOrSetData(path, agJson , IDataOperator.PERSISTENT); //
		op.createNodeOrSetData(activePath,"true",IDataOperator.EPHEMERAL); //代表此Agent的存活标志
		//op.createNodeOrSetData(assignPath, "" , false); //分配给此Agent的全部部署ID
		
		insManager.filterByAgent(agentInfo.getId());
		
		insManager.addListener((type,pi) -> {
			if(type == IListener.ADD) {
				instanceAdded(pi);
			} else if(type == IListener.REMOVE) {
				instanceRemoved(pi);
			}
		});
		
		 //本Agent退出
		/* op.addDataListener(activePath, (path,data) -> {
			 if("fasle".equals(data)) {
				 Set<ProcessInfo> ids = new HashSet<>();
				 ids.addAll(this.processInfos.values());
				 for(ProcessInfo id : ids) {
					this.instanceRemoved(id);
				 }
			 }
		 });*/
		 
		 //监控分配新的应用
		op.addChildrenListener(path, (type,parent,depId,data)->{
			if(type == IListener.ADD) {
				ProcessInfo pi = this.insManager.getProcessesByAgentIdAndDepid(agentInfo.getId(), depId);
				if(pi != null) {
					//第一次启动时，已经存在对应的应用
					instanceAdded(pi);
				} else {
					deploymentAdded(depId);
				}
			}else if(type == IListener.REMOVE) {
				this.deploymentRemoved(depId);
			}
		});
		 
		TimerTicker.getDefault(5000L).addListener("", (key,att)->{
				try {
					if(System.currentTimeMillis() - agentInfo.getStartTime() < 1000*30) {
						return;
					}
					checkStatus();
				} catch (Throwable e) {
					logger.error("doChecker",e);
				}
			}, null);
		 
	}
	
	private void recreateAgentInfo() {
		agentInfo.setAssignTime(System.currentTimeMillis());
		String data = JsonUtils.getIns().toJson(agentInfo);
		logger.warn("Recreate angent info: " + data);
		op.createNodeOrSetData(path,data ,IDataOperator.PERSISTENT);
	}
	
	private void setStatisData() {
		agentInfo.setSs(ssm.getStatis());
		
		String data = JsonUtils.getIns().toJson(agentInfo);
		op.setData(path, data);
	}

	private void checkStatus() {
		long curTime = System.currentTimeMillis();
		if( curTime - agentInfo.getAssignTime() < 10000) {
			return;
		}

		if(!op.exist(path)) {
			recreateAgentInfo();
			return;
		}
		
		setStatisData();
		
		Set<String> keySet = new HashSet<>();
		keySet.addAll(processInfos.keySet());
		for(String k : keySet) {
			ProcessInfo pi = processInfos.get(k);
			String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
			if(op.exist(p)) {
				if(!pi.isActive() && curTime - pi.getOpTime() > pi.getTimeOut()) {
					//已经命令进程停止，并且超过超时时间还没成功退出
					processInfos.remove(k);
					if(StringUtils.isNotEmpty(pi.getPid())) {
						forceStopProcess(pi.getPid());
					}
				} 
			} else {
				//进程节点不存在
				//pi.getPid() != null: 进程已经停止
				//进程启动超时: curTime - pi.getOpTime() > pi.getTimeOut()
				if(pi.getPid() != null || (pi.getPid() == null && curTime - pi.getOpTime() > pi.getTimeOut())) {
					//进程ID为空，启动中，进程还没起来
					//超时时间已经到了
					processInfos.remove(k);
				}
			}
		}
		
	}

	private void forceStopProcess(String pid) {
		String cmd = "kill -9 "+pid;
		if(SystemUtils.isWindows()) {
			 cmd = "taskkill /f /pid "+pid;
		} else {
			 //类unix系统停止进程
			 cmd = "kill -9 "+pid;
		}
		
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			logger.error("fail to stop process : " + pid,e);
		}
		
	}


	private void instanceRemoved(ProcessInfo pi) {
		if(processInfos.containsKey(pi.getId())) {
			 processInfos.remove(pi.getId());
			 //stopProcess(pi);
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
			processInfos.put(pi.getId(), pi);
			logger.info("Insatnce add: " + pi.toString());
		}
	}

	private void deploymentRemoved(String depId) {
		ProcessInfo p = null;
		for(ProcessInfo pi : processInfos.values()) {
			if(depId.equals(pi.getDepId())) {
				p = pi;
				break;
			}
		}
		if(p != null) {
			stopProcess(p);
		}
		
	}

	private boolean deploymentAdded(String depId) {
		Deployment dep = getDeployment(depId);
		boolean f = startDep(dep);
		return f;
	}

	private Deployment getDeployment(String depId) {
		String depPath = ChoyConstants.DEP_DIR + "/" + depId;
		String data = op.getData(depPath);
		Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
		return dep;
	}

	private boolean startDep(Deployment dep) {
		
		boolean doContinue = true;
		if(!checkRes(dep.getJarFile()) ) {
			//Jar文件还不存在，先下载资源
			doContinue = downloadJarFile(dep.getJarFile());
		}
		
		if(!checkRes(this.javaAgentJarFile) ) {
			//Jar文件还不存在，先下载资源
			doContinue = downloadJarFile(this.javaAgentJarFile);
		}
		
		if(!doContinue) {
			logger.error("Start deployment fail pls check yourself");
			return false;
		}
		
		String processId = idServer.getStringId(ProcessInfo.class);
		
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
		
		list.add("-D" + ChoyConstants.ARG_INSTANCE_ID+"=" + processId);
		list.add("-D" + ChoyConstants.ARG_MYPARENT_ID+"=" + SystemUtils.getProcessId());
		list.add("-D" + ChoyConstants.ARG_DEP_ID+"=" + dep.getId());
		list.add("-D" + ChoyConstants.ARG_AGENT_ID+"=" + this.agentInfo.getId());
		
		ProcessBuilder pb = new ProcessBuilder(list);
		File wd = new File(workDir + "/" + processId);
		wd.mkdirs();
		pb.directory(wd);
		
		File errorFile = new File(wd,"error.log");
		pb.redirectError(errorFile);
		
		File outputFile = new File(wd,"nohup.log");
		pb.redirectOutput(outputFile);
		
		File processInfoData = new File(wd,processId+".json");
		
		list.add("-D"+ChoyConstants.PROCESS_INFO_FILE + "=" + processInfoData.getAbsolutePath());
		
		OutputStream os = null;

		try {
			
			ProcessInfo pi = new ProcessInfo();
			pi.setActive(false);
			pi.setAgentId(this.agentInfo.getId());
			pi.setCmd(list.toString());
			pi.setDepId(dep.getId());
			pi.setId(processId);
			pi.setDataDir(wd.getAbsolutePath());
			pi.setAgentProcessId(SystemUtils.getProcessId());
			pi.setAgentHost(Config.getHost());
			pi.setAgentInstanceName(Config.getInstanceName());
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(this.processOpTimeout);
			
			//通过文件传递给子进程，再由子进程在启动后存入ZK
			String data = JsonUtils.getIns().toJson(pi);
			if(!SystemUtils.setFileString(processInfoData,data )) {
				throw new CommonException("Write file error: " + processInfoData.getAbsolutePath());
			}
			
			Process p = pb.start();
			pi.setProcess(p);
			
			this.processInfos.put(pi.getId(), pi);
			//this.dep2Process.put(pi.getDepId(), pi);
			
			logger.info("Start process: " + data);
			return true;
		} catch (IOException e) {
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
	
	
	private boolean downloadJarFile(String jarFile) {
		int resId = respo.initDownloadResource(jarFile);
		if(resId <= 0) {
			String msg = "Download ["+jarFile+"] fail with resource id: "+ resId;
			logger.error(msg);
			return false;
		}
		
		FileOutputStream fos = null;
		try {
			File f = new File(this.resourceDir,jarFile);
			f.createNewFile();
			
			fos = new FileOutputStream(f);
			logger.info("Begin download: "+jarFile);
			while(true) {
				byte[] data = respo.downResourceData(resId, 0);
				if(data != null && data.length > 0) {
					logger.info("Got one block: " + data.length + "B");
					fos.write(data, 0, data.length);
				}
				
				if(data == null || data.length < this.uploadBlockSize) {
					logger.info("Finish download: "+jarFile+" with size: " + f.length());
					return true;
				}
			}
		} catch (IOException e) {
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
			logger.warn("Receive NULL dep for stop deployment");
			return;
		}
		
		String p = ChoyConstants.INS_ROOT+"/"+pi.getId();
		if(op.exist(p) && pi.isActive()) {
			pi.setActive(false);
			pi.setOpTime(System.currentTimeMillis());
			pi.setTimeOut(processOpTimeout);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			logger.info("Stop process: " + data);
		}
	}
}
