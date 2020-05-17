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
import java.util.Iterator;
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
import cn.jmicro.api.cache.lock.ILocker;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.base.AgentInfo;
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
	private String javaAgentJarFile="jmicro.agent-0.0.1-SNAPSHOT.jar";
	
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 65300;//1024*1024;
	
	@Inject
	private Config cfg;
	
	private File workDirFile = null;
	
	private File resourceDirFile = null;
	
	//private Map<String,Deployment> deployments = new HashMap<>();
	
	private Map<String,Long> processTimeout = new HashMap<>();
	
	private Map<String,ProcessInfo> processInfos = new HashMap<>();
	private Map<String,ProcessInfo> dep2Process = new HashMap<>();
	
	@Reference(namespace="rrs",version="*")
	private IResourceResponsitory respo;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	private AgentInfo agentInfo;
	
	@Inject
	private ILockerManager lockMgn;
	
	private String path;
	
	private IDataListener processInfoDataListener = (path,data)-> {
		if(StringUtils.isEmpty(data)) {
			return;
		}
		ProcessInfo pi = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
		if(pi.isActive() && processInfos.containsKey(pi.getId())) {
			processInfos.put(pi.getId(), pi);
		}
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
		
		if(checkExist(infoFile)) {
			logger.warn("Only one agent can be exist for one resourceDir: " + resourceDir);
			System.exit(0);
			return;
		}
		
		if(infoFile.exists()) {
			String existAgentJson = SystemUtils.getFileString(infoFile);
			if(existAgentJson != null) {
				agentInfo = JsonUtils.getIns().fromJson(existAgentJson, AgentInfo.class);
			}
		}
		
		if(agentInfo == null) {
			String id = idServer.getStringId(AgentInfo.class);
			agentInfo = new AgentInfo();
			agentInfo.setId(id);
		}
		
		agentInfo.setName(Config.getInstanceName());
		agentInfo.setStartTime(System.currentTimeMillis());
		agentInfo.setAssignTime(agentInfo.getAssignTime());
		agentInfo.setHost(Config.getHost());
		
		String agJson = JsonUtils.getIns().toJson(agentInfo);
		SystemUtils.setFileString(infoFile, agJson);
		
		path = ChoyConstants.ROOT_AGENT + "/" + agentInfo.getId();
		op.createNode(path,agJson , true);
		
		/*op.addNodeListener(path, (int type, String p,String data)->{
			//防止被误删除，只要此进程还在，此结点就不应该消失
			if(type == IListener.REMOVE) {
				recreateAgentInfo();
			}
		});*/
		
		op.addDataListener(path, (path,data0)->{
			checkStatus();
		});
		
		 op.addChildrenListener(ChoyConstants.INS_ROOT, (type,parent,insId,data)->{
			if(type == IListener.ADD) {
				instanceAdded(insId,data);
			}else if(type == IListener.REMOVE) {
				instanceRemoved(insId);
			}
		});
		 
		 TimerTicker.getDefault(1000*3L).addListener("", (key,att)->{
				try {
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
		op.createNode(path,data ,true);
	}

	private void checkStatus() {
		long curTime = System.currentTimeMillis();
		if( curTime - agentInfo.getAssignTime() < 10000) {
			return;
		}
		
		doInlocker(path,()->{
			if(!op.exist(path)) {
				recreateAgentInfo();
				return;
			}
			
			String data = op.getData(path);
			
			AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
			if(ai != null) {
				boolean doUpdate = false;
				if(!ai.getRunningDeps().isEmpty()) {
					Iterator<String> ite = ai.getRunningDeps().iterator();
					for(;ite.hasNext();) {
						ProcessInfo pi = this.dep2Process.get(ite.next());
						if( pi == null ) {
							doUpdate = true;
							ite.remove();
						}
					}
				}
				
				if(!ai.getAssignDeps().isEmpty()) {
					for(String d : ai.getAssignDeps()) {
						if(deploymentAdded(d)) {
							ai.getRunningDeps().add(d);
						}
					}
					ai.getAssignDeps().clear();
					doUpdate = true;
				}
				
				if(!ai.getDeleteDeps().isEmpty()) {
					for(String d : ai.getDeleteDeps()) {
						deploymentRemoved(d);
						ai.getRunningDeps().remove(d);
					}
					ai.getDeleteDeps().clear();
					doUpdate = true;
				}

				Set<String> keySet = new HashSet<>();
				keySet.addAll(processInfos.keySet());
				for(String k : keySet) {
					ProcessInfo pi = processInfos.get(k);
					String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
					if(!op.exist(p)) {
						deploymentRemoved(pi.getDepId());
						ai.getRunningDeps().remove(pi.getDepId());
						processInfos.remove(k);
						dep2Process.remove(pi.getDepId());
						doUpdate = true;
					}
				}
				
				if(doUpdate) {
					agentInfo = ai;
					ai.setAssignTime(curTime);//删除成功
					op.setData(path, JsonUtils.getIns().toJson(ai));
				}
			}
		});		
	}

	private boolean checkExist(File infoFile) {
		
		if(!infoFile.exists()) {
			return false;
		}
		
		String existAgentJson = SystemUtils.getFileString(infoFile);
		if(existAgentJson == null) {
			return false;
		}
		
		AgentInfo existAgentInfo = JsonUtils.getIns().fromJson(existAgentJson, AgentInfo.class);
		Set<String> agents = op.getChildren(ChoyConstants.ROOT_AGENT, false);
		for(String a : agents) {
			String data = op.getData(ChoyConstants.ROOT_AGENT+"/"+a);
			AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
			if(existAgentInfo.getId().equals(ai.getId())) {
				logger.warn("Agent exist: " + data);
				//return true;
			}
		
		}
		return false;
	}

	private void instanceRemoved(String insId) {
		if(processInfos.containsKey(insId)) {
			op.removeDataListener(ChoyConstants.INS_ROOT+"/" + insId, this.processInfoDataListener);
			ProcessInfo pi = processInfos.remove(insId);
			stopProcess(pi);
			dep2Process.remove(pi.getDepId());
			
			this.doInlocker(this.path, ()->{
				String data = op.getData(this.path);
				AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
				if(ai.getRunningDeps().contains(pi.getDepId())) {
					ai.getRunningDeps().remove(pi.getDepId());
					ai.setAssignTime(System.currentTimeMillis());
					op.setData(this.path, JsonUtils.getIns().toJson(ai));
				}
				logger.info("Insatnce remove: " + JsonUtils.getIns().toJson(pi));
				agentInfo = ai;
			});
			
		
		}
	}
	
	private void instanceAdded(String insId, String json) {
		ProcessInfo pi = JsonUtils.getIns().fromJson(json, ProcessInfo.class);
		if(StringUtils.isEmpty(pi.getAgentId())) {
			//非Agent环境下启动的实例
			return;
		}
		if(pi.getAgentId().equals(this.agentInfo.getId())) {
			String pipath = ChoyConstants.INS_ROOT+"/" + insId;
			op.addDataListener(pipath, processInfoDataListener);
			
			if(processInfos.containsKey(pi.getId())) {
				processInfos.put(insId, pi);
				dep2Process.put(pi.getDepId(), pi);
			} else {
				processInfos.put(insId, pi);
				dep2Process.put(pi.getDepId(), pi);
				//第一次启动时，Agent关联实例
				doInlocker(this.path,()->{
					String data = op.getData(this.path);
					AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
					if(!ai.getRunningDeps().contains(pi.getDepId())) {
						ai.getRunningDeps().add(pi.getDepId());
						ai.setAssignTime(System.currentTimeMillis());
						op.setData(path, JsonUtils.getIns().toJson(ai));
						this.agentInfo = ai;
					}
				});
			}
			
			logger.info("Insatnce add: " + json);
		}
	}

	private void deploymentRemoved(String depId) {
		stopProcess(this.dep2Process.get(depId));
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
			
			//通过文件传递给子进程，再由子进程在启动后存入ZK
			String data = JsonUtils.getIns().toJson(pi);
			if(!SystemUtils.setFileString(processInfoData,data )) {
				throw new CommonException("Write file error: " + processInfoData.getAbsolutePath());
			}
			
			Process p = pb.start();
			pi.setProcess(p);
			
			this.processInfos.put(pi.getId(), pi);
			this.dep2Process.put(pi.getDepId(), pi);
			
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
					// TODO Auto-generated catch block
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
			//logger.warn("Receive NULL dep for stop deployment");
			return;
		}
		
		String p = ChoyConstants.INS_ROOT+"/"+pi.getId();
		if(op.exist(p)) {
			pi.setActive(false);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			logger.info("Stop process: " + data);
		}
	}
	
	private void doInlocker(String lockPath,Runnable r) {
		ILocker locker = null;
		boolean success = false;
		try {
			locker = lockMgn.getLocker(lockPath);
			if(success = locker.tryLock(1000,30*1000)) {
				r.run();
			} else {
				throw new CommonException("Fail to get locker:" + lockPath);
			}
		}finally {
			if(locker != null && success) {
				locker.unLock();
			}
		}
	}
}
