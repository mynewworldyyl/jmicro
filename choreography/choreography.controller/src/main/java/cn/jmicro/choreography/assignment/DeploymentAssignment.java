package cn.jmicro.choreography.assignment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IConnectionStateChangeListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.choreography.agent.AgentManager;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.api.IInstanceListener;
import cn.jmicro.choreography.assign.Assign;
import cn.jmicro.choreography.assign.AssignState;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component(level=30)
public class DeploymentAssignment {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentAssignment.class);
	
	private static final Class<?> TAG = DeploymentAssignment.class;
	
	/*@Cfg("/enableMasterSlaveModel")
	private boolean isMasterSlaveModel = false;*/
	
	@Inject
	private AssignManager assingManager;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private ILockerManager lockMgn;
	
	@Inject("defautAssignStrategy")
	private IAssignStrategy defaultAssignStrategy; 
	
	@Inject
	private IObjectFactory of; 
	
	@Inject
	private ProcessInfo processInfo;
	
	@Inject
	private Config cfg;
	
	@Inject
	private ComponentIdServer idServer;
	
	//@Inject
	//private ComponentIdServer idServer;
	
	//Agent to fail instance
	private Map<String,Set<String>> fails = new HashMap<>();
	
	private Map<String,Deployment> deployments = new HashMap<>();
	
	private Map<String,Long> nextDeployTimeout = new HashMap<>();
	
	private long lastCheckTime = 0;
	
	private IDataListener deploymentDataListener = (path,data)->{
		deploymentDataChanged(path.substring(ChoyConstants.DEP_DIR.length()+1),data);
	};
	
	private IChildrenListener depListener = (type,p,c,data)->{
		if(type == IListener.ADD) {
			String path = ChoyConstants.DEP_DIR + "/" + c;
			op.addDataListener(path, deploymentDataListener);
			deploymentAdded(c,data);
		}else if(type == IListener.REMOVE) {
			String path = ChoyConstants.DEP_DIR + "/" + c;
			op.removeDataListener(path, deploymentDataListener);
			deploymentRemoved(c,data);
		}
	};
	
	private IInstanceListener insListener = (type,pi) -> {
		if(type == IListener.ADD) {
			instanceAdded(pi);
		} else if(type == IListener.REMOVE) {
			if(pi != null) {
				instanceRemoved(pi.getId());
			}
		}
	};
	
	private IConnectionStateChangeListener connListener = (state)->{
		if(Constants.CONN_CONNECTED == state) {
			logger.info("CONNECTED, reflesh children");
			registListener();
		}else if(Constants.CONN_LOST == state) {
			logger.warn("DISCONNECTED");
		}else if(Constants.CONN_RECONNECTED == state) {
			logger.warn("Reconnected,reflesh children");
			registListener();
		}
	}; 
	
	private String actKey = null;
	
	private ITickerAction act =  (key,att)->{
		try {
			doChecker();
		} catch (Throwable e) {
			logger.error("doChecker",e);
		}
	};
	
	/*private IMasterChangeListener mcl = (type,isMaster)->{
		if(isMaster && ( IMasterChangeListener.MASTER_ONLINE == type 
				|| IMasterChangeListener.MASTER_NOTSUPPORT == type )) {
			 //参选成功
			 logger.info(Config.getInstanceName() + " got as master");
			 isMaster = true;
			 ready0();
		} else {
			 //参选失败
			 isMaster = false;
			 lostMaster();
		}
	};*/
	
	public void ready() {
		
		boolean initGatewayAndMng = cfg.getBoolean("initGatewayAndMng", false);
		if(initGatewayAndMng) {
			createInitDeployment();
		}
		
		if(!op.exist(ChoyConstants.ID_PATH)) {
			op.createNodeOrSetData(ChoyConstants.ID_PATH, "0", IDataOperator.PERSISTENT);
		}
		
		ready0();
		
		/*if(isMasterSlaveModel) {
			of.masterSlaveListen(mcl);
		} else {
			ready0();
		}*/
		
	}
	
	 private void createInitDeployment() {
		 Set<String> children = op.getChildren(ChoyConstants.DEP_DIR, false);
		
		 Deployment mngDep = null;
		 Deployment apiGatewayDep = null;
		 
		 for(String c : children) {
			String data = op.getData(ChoyConstants.DEP_DIR+"/" + c);
			if(StringUtils.isNotEmpty(data)) {
				Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
				if(dep.getJarFile().startsWith("jmicro-main.mng-") && dep.isEnable()) {
					mngDep = dep;
				}else if(dep.getJarFile().startsWith("jmicro-main.apigateway-")  && dep.isEnable()) {
					apiGatewayDep = dep;
				}
			}
		 }
		 
		 if(mngDep == null) {
			 mngDep = new Deployment();
			 String id = idServer.getStringId(Deployment.class);
			 mngDep.setId(id);
			 mngDep.setArgs("-Xmx128m -Xms32m -DenableMasterSlaveModel=true -DsysLogLevel=5  -DclientId=0 -DadminClientId=0");
			 mngDep.setAssignStrategy("defautAssignStrategy");
			 mngDep.setEnable(true);
			 mngDep.setForceRestart(false);
			 mngDep.setInstanceNum(1);
			 
			 if(StringUtils.isNotEmpty(cfg.getString("mngJarFile", null))) {
				 mngDep.setJarFile(cfg.getString("mngJarFile", null));
			 } else {
				 mngDep.setJarFile("jmicro-main.mng-"+Constants.JMICRO_VERSION+"-"+Constants.JMICRO_RELEASE_LABEL+"-jar-with-dependencies.jar");
			 }
			 
			 mngDep.setStrategyArgs("-DsortPriority=instanceNum");
			 String jo = JsonUtils.getIns().toJson(mngDep);
			 //logger.info("Create origin mng Deployment: " + jo);
			 LG.log(MC.LOG_INFO, TAG, "Create origin mng Deployment: " + jo);
			 op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/"+id, jo , false);
		 }
		 
		 if(apiGatewayDep == null) {
			 apiGatewayDep = new Deployment();
			 String id = idServer.getStringId(Deployment.class);
			 apiGatewayDep.setId(id);
			 
			 String exportHttpIp = cfg.getString("apiGatewayExportHttpIP", null);
			 if(StringUtils.isEmpty(exportHttpIp)) {
				 throw new CommonException("apiGatewayExportHttpIP" + " cannot be null when create inti api gateway service!");
			 }
			 
			 String mngCxtRoot = cfg.getString("/StaticResourceHttpHandler/staticResourceRoot_mng", null);
			 if(StringUtils.isEmpty(mngCxtRoot)) {
				 throw new CommonException("/StaticResourceHttpHandler/staticResourceRoot_mng cannot be null when create inti api gateway service!");
			 }
			 
			 apiGatewayDep.setArgs("-DsysLogLevel=5  -DclientId=0 -DadminClientId=0 -Xmx128m -Xms32m -DinstanceName=apigateway -DlistenHttpIP=0.0.0.0 -DexportHttpIP="+exportHttpIp+" -DnettyHttpPort=9090 -D/StaticResourceHttpHandler/staticResourceRoot_mng="+mngCxtRoot);
			 apiGatewayDep.setAssignStrategy("defautAssignStrategy");
			 apiGatewayDep.setEnable(true);
			 apiGatewayDep.setForceRestart(false);
			 apiGatewayDep.setInstanceNum(1);
			 
			 if(StringUtils.isNotEmpty(cfg.getString("apiGatewayJarFile", null))) {
				 apiGatewayDep.setJarFile(cfg.getString("apiGatewayJarFile", null));
			 } else {
				 apiGatewayDep.setJarFile("jmicro-main.apigateway-"+Constants.JMICRO_VERSION+"-"+Constants.JMICRO_RELEASE_LABEL+"-jar-with-dependencies.jar");
			 }
			 
			 apiGatewayDep.setStrategyArgs("-DsortPriority=instanceNum");
			 String jo = JsonUtils.getIns().toJson(apiGatewayDep);
			 //logger.info("Create origin api gateway Deployment: " + jo);
			 LG.log(MC.LOG_INFO, TAG, "Create origin api gateway Deployment: " + jo);
			 op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/"+id, jo , false);
		 }
		
	}

	private void ready0() {
		 String conRootPath = ChoyConstants.ROOT_CONTROLLER + "/" + this.processInfo.getId();
		 if(op.exist(conRootPath)) {
			 op.deleteNode(conRootPath);
		 }
		 op.createNodeOrSetData(conRootPath, this.processInfo.getId()+"", IDataOperator.EPHEMERAL);
		 actKey = Config.getInstanceName() + "_DeploymentAssignmentChecker";
		 op.addListener(connListener);
		 TimerTicker.getDefault(5000L).addListener(actKey,null,act);
		 LG.log(MC.LOG_INFO, TAG, "Controller with PID [" + this.processInfo.getId()+"] started!");
	 }
	
	/*private void lostMaster() {
		 
		 logger.warn(Config.getInstanceName() + " lost master resposibility");
		 TimerTicker.getDefault(1000*5L).removeListener(actKey, true);
		 op.removeChildrenListener(ChoyConstants.DEP_DIR, depListener);
		 op.removeListener(connListener);
		 insManager.removeListener(insListener);
		 
		 if(!deployments.isEmpty()) {
			 for(String d : this.deployments.keySet()) {
				 String path = ChoyConstants.DEP_DIR + "/" + d;
				 op.removeDataListener(path, deploymentDataListener);
			 }
		 }
		 deployments.clear();
		 nextDeployTimeout.clear();
	 }*/
	
	private void registListener() {
		op.addChildrenListener(ChoyConstants.DEP_DIR, depListener);
		insManager.addListener(insListener);
	}
	
	private void doChecker() {
		 
		 long curTime = TimeUtils.getCurTime();
		 if(curTime - lastCheckTime < 3000) {
			 return;
		 }
		 
		 Set<Assign> ass = this.assingManager.getAll();
		 
		 //检测分配后2分钟内有没有启动成功，如果没有启动成功，则取消分配
		 for(Assign a : ass) {
			 if((a.state == AssignState.INIT || a.state == AssignState.STARTING 
					 || a.state == AssignState.DOWNLOAD_RES) && curTime - a.opTime > 120000) {
				 //starting timeout
				 logger.error("Starting timeout: " + a.toString());
				 //SF.event(MonitorConstant.Ms_PROCESS_LOG, TAG, "Timeout: "+ a.toString());
				 LG.log(MC.LOG_WARN, TAG, "Starting timeout and cacel it: "+ a.toString());
				 cancelAssign(a);
				 continue;
			 }
			 
			 if(a.state == AssignState.STOPING && curTime - a.opTime > 15000) {
				 //stoping timeout
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.getInsId();
				 if(!op.exist(piPath)) {
					 //logger.error("Delete invalid assign: " + a.toString());
					 LG.log(MC.LOG_WARN, TAG, "Delete invalid assign: " + a.toString());
					 this.assingManager.remove(a);
				 } else {
					 String data = op.getData(piPath);
					 if(StringUtils.isEmpty(data)) {
						 //logger.warn("Delete invalid process node: " + piPath);
						 LG.log(MC.LOG_WARN, TAG, "Delete invalid process node: " + piPath);
						 op.deleteNode(piPath);
						 this.assingManager.remove(a);
					 } else {
						 if(a.checkTime > 100) {
							 op.deleteNode(piPath);
							 this.assingManager.remove(a);
							 //logger.error("Process stop exception Assign: " + a.toString());
							 //logger.error("Process stop exception processInfo: " + data);
							 //logger.error("You should check it by your hand, sorry for this case!");
							 LG.log(MC.LOG_WARN, TAG, "Process exception for check count too exceep: " + data);
						 } else {
							 ProcessInfo pi = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
							 a.checkTime++;
							 this.assingManager.update(a);
							 if(pi.isActive()) {
								 //logger.warn("Do cancel time["+a.checkTime+"] again: " + data);
								 LG.log(MC.LOG_WARN, TAG, "Do cancel time["+a.checkTime+"] again: " + data);
								 this.cancelAssign(a);
							 } else {
								 //logger.warn("Process in stoping time["+a.checkTime+"] state: " + data);
								 LG.log(MC.LOG_WARN, TAG, "Process in stoping time["+a.checkTime+"] state: " + data);
							 }
						 }
					 }
				 }
				 
				 continue;
			 }
			 
			 if(a.state == AssignState.STARTED && curTime - a.opTime > 120000) {
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.getInsId();
				 if(!op.exist(piPath)) {
					 //实例已经不存在
					 //logger.error("Process exit: " + piPath);
					 LG.log(MC.LOG_ERROR, TAG, "Process started timeout and remove it: " + a.toString());
					 instanceRemoved(a.getInsId());
					 continue;
				 }
			 }
		 }
		 
		 Set<Deployment> deps = new HashSet<>();
		 deps.addAll(this.deployments.values());
		 
		 for(Deployment dep : deps) {
			 if(!dep.isEnable()) {
				 //logger.warn("Stop deployment: "+ dep.toString());
				 stopDeployment(dep.getId());
			 }else if(dep.isForceRestart()) {
				 //logger.warn("Force restart deployment: "+ dep.toString());
				 LG.log(MC.LOG_INFO, TAG, "Force restart deployment: "+ dep.toString());
				 stopDeployment(dep.getId());
				 dep.setForceRestart(false);
			 } else if(dep.isEnable()) {
				 if(nextDeployTimeout.containsKey(dep.getId())) {
					 if(curTime - nextDeployTimeout.get(dep.getId()) < 10000 ) {
						 //两次分配动作之间最少等待一分钟
						 continue;
					 } else {
						 nextDeployTimeout.remove(dep.getId());
					 }
				 }
				 this.doAssgin(dep);
			 }
		 }
		 lastCheckTime = TimeUtils.getCurTime();
	}
	
	private void instanceRemoved(Integer insId) {
		Assign a = this.assingManager.getAssignByInfoId(insId);
		if(a != null) {
			LG.log(MC.LOG_WARN, TAG,"Instance remove: "+JsonUtils.getIns().toJson(a));
			cancelAssign(a);
			assingManager.remove(a);
		}else {
			LG.log(MC.LOG_WARN, TAG,"Remove instance not exist: "+insId);
		}
	}
	
	private void instanceAdded(ProcessInfo pi) {
		
		if(pi == null || StringUtils.isEmpty(pi.getAgentId())) {
			return;
		}
		
		Assign a = assingManager.getAssignByInfoId(pi.getId());
		if(a == null) {
			//初次启动时，对已经存在的实例做实例化
			LG.log(MC.LOG_INFO, TAG, "Instance add for origint: " + pi.toString());
			a = new Assign(pi.getDepId(),pi.getAgentId(),pi.getId());
		} else {
			LG.log(MC.LOG_INFO, TAG, "Instance start success: " + pi.toString());
		}
		
		//LG.log(MC.LOG_WARN, TAG,  "Process success started: "+JsonUtils.getIns().toJson(pi));
		
		a.opTime = TimeUtils.getCurTime();
		a.state = AssignState.STARTED;
		this.assingManager.add(a);
	}

	private void deploymentRemoved(String d, String data) {
		LG.log(MC.LOG_WARN, TAG,data);
		deployments.remove(d);
		stopDeployment(d);
	}
	
	private void stopDeployment(String depId) {
		Set<Assign> ownerAgents = assingManager.getAssignByDepId(depId);
		if(ownerAgents == null || ownerAgents.isEmpty()) {
			return;
		}
		/*if(logger.isInfoEnabled()) {
			logger.info("Stop deployment: "+depId);
		}*/
		LG.log(MC.LOG_INFO, TAG, "Stop deployment: "+depId);
		
		for(Assign a : ownerAgents) {
			if(a.state == AssignState.STARTING || a.state == AssignState.STARTED ||
					a.state == AssignState.INIT || a.state == AssignState.DOWNLOAD_RES) {
				/*if(logger.isInfoEnabled()) {
					logger.info("Cancel assign: "+a.toString());
				}*/
				LG.log(MC.LOG_WARN, TAG,"To cancel assign: "+a.toString());
				cancelAssign(a);
			}
		}
	}
	
	private void cancelAssign(Assign a) {
		if(a.state == AssignState.STOPING) {
			if(null == insManager.getProcessesByInsId(a.getInsId(),false)) {
				assingManager.remove(a);
			}
			String msg = "Assign is on stoping state: depId: " + a.toString();
			/*logger.warn(msg);*/
			LG.log(MC.LOG_WARN, TAG,msg);
			return;
		}
		
		/*String msg = "Cancel :"+a.toString();
		LG.log(MC.LOG_WARN, TAG,msg);
		logger.info(msg);*/
		
		Set<Assign> set = assingManager.getAssignByDepIdAndAgentId(a.getDepId(), a.getAgentId());
		String path = ChoyConstants.ROOT_AGENT+"/" + a.getAgentId() + "/" + a.getInsId();
		if(set.size() > 1 || !this.agentManager.isActive(a.getAgentId()) || !op.exist(path)) {
			//Agent挂机状态，直接关闭服务进程
			ProcessInfo pi = this.insManager.getProcessesByInsId(a.getInsId(),false);
			String p = ChoyConstants.INS_ROOT + "/" + a.getInsId();
			if(op.exist(p) && pi.isActive()) {
				pi.setActive(false);
				String data = JsonUtils.getIns().toJson(pi);
				op.setData(p, data);
				//logger.info("Stop process: " + data);
				LG.log(MC.LOG_INFO,TAG,"Stop process: " + data);
			} else {
				assingManager.remove(a);
			}
		} else {
			//logger.debug("Delete deploy: " + path);
			a.state = AssignState.STOPING;
			a.opTime = TimeUtils.getCurTime();
			assingManager.update(a);
			LG.log(MC.LOG_INFO,TAG,"Cammand agent to stop process: " + a.toString());
		}
		
		if(!fails.containsKey(a.getAgentId())) {
			fails.put(a.getAgentId(), new HashSet<String>());
		}
		fails.get(a.getAgentId()).add(a.getDepId());
		
		nextDeployTimeout.put(a.getDepId(), TimeUtils.getCurTime());
	}

	private void deploymentDataChanged(String depId, String data) {
		Deployment newDep = JsonUtils.getIns().fromJson(data, Deployment.class);
		if(newDep != null) {
			deployments.put(depId, newDep);
		} else {
			 LG.log(MC.LOG_ERROR, TAG, "Deployment data invalid" + data);
		}
	}

	private void deploymentAdded(String depId, String data) {
		deploymentDataChanged(depId,data);
	}

	private void doAssgin(Deployment dep) {
		if(!dep.isEnable()) {
			return;
		}
		Set<Assign> ass = assingManager.getAssignByDepId(dep.getId());
		//filterState(ass,AssignState.STARTED,AssignState.STARTING);
		this.includeByState(ass, AssignState.STARTED,AssignState.STARTING,AssignState.INIT);
		
		Set<AgentInfo> agentInfo = this.agentManager.getAllAgentInfo();
		
		int cnt = dep.getInstanceNum() - ass.size();
		if(cnt > 0) {
			//增加运行实例
			if(agentInfo != null && !agentInfo.isEmpty()) {
				doAddAssign(agentInfo,dep,cnt);
			}
		}else if(cnt < 0) {
			//减少运行实例
			doDecAssign(agentInfo,dep,cnt);
		}
		
	}
	
	private void includeByState(Set<Assign> ass, AssignState... states) {
		Iterator<Assign> ite = ass.iterator();
		while(ite.hasNext()) {
			Assign a = ite.next();
			
			boolean f = false;
			for(AssignState as : states) {
				if(as == a.state) {
					f = true;
					break;
				}
			}
			
			if(!f) {
				ite.remove();
			}
			
		}
		
	}

	private void doDecAssign(Set<AgentInfo> agentInfo, Deployment dep, int cnt) {
		
		Set<ProcessInfo> processes = this.insManager.getProcessesByDepId(dep.getId());
		if(processes == null || processes.isEmpty()) {
			logger.info("No agents for dep: " + dep.toString());
			return;
		}
		
		List<AgentInfo> sortList = new LinkedList<>();
		sortList.addAll(agentInfo);
		
		Iterator<AgentInfo> ite = sortList.iterator();
		
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			
			boolean found = false;
			for(ProcessInfo pi : processes) {
				if(pi.getAgentId().equals(ai.getId())) {
					found = true;
					break;
				}
			}
			
			if(!found) {
				ite.remove();
			}
			
		}
		
		if(sortList.isEmpty()) {
			logger.warn("No agent responsibe this dep: " + dep.toString());
			return;
		}
		
		String agentIds = null;
		if(StringUtils.isNotEmpty(dep.getStrategyArgs())) {
			Map<String,String> params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
			agentIds = params.get(IAssignStrategy.AGENT_ID);
		}
		
		if(StringUtils.isNotEmpty(agentIds)) {
			filterByAgentId(sortList,dep,cnt,agentIds);
			if(sortList == null || sortList.isEmpty()) {
				logger.debug("doDecAssign: Agent ID: " + agentIds + " not online for dep: " + dep.toString());
				return;
			}
		} else { 
			if(!sortList.isEmpty()) {
				filterAgentByStrategy(sortList,dep,cnt);
			}
		}
		
		for(int i = sortList.size() - 1; i >= 0; i-- ) {
			AgentInfo aif = sortList.get(i);
			Set<Assign> as = assingManager.getAssignByDepIdAndAgentId(dep.getId(), aif.getId());
			
			if(!as.isEmpty()) {
				
				Assign a = getByState(as,AssignState.INIT);
				if(a == null) {
					a =  getByState(as,AssignState.STARTING);
				}
				
				if(a == null) {
					getByState(as,AssignState.STARTED);
				}
				
				if(a != null) {
					as.remove(a);
					this.cancelAssign(a);
					cnt++;
					if(cnt >= 0) {
						break;
					}
				}
				
			}
		}
		
		if(cnt < 0) {
			forceStopIns(dep,cnt);
		}
		
	}

	private Assign getByState(Set<Assign> as, AssignState s) {
		for(Assign a : as) {
			if(a.state == s) {
				return a;
			}
		}
		return null;
	}


	private void forceStopIns(Deployment dep, int cnt) {
		if(cnt >= 0) {
			return;
		}
		
		String msg = "Force direct stop [" + dep.getId() + "], count [" + (-cnt) + "] ";
		logger.warn(msg);
		
		LG.log(MC.LOG_WARN, TAG,msg);
		MT.nonRpcEvent(Config.getInstanceName(), MC.MT_ASSIGN_REMOVE);
		
		Set<Assign> as = assingManager.getAssignByDepId(dep.getId());
		if(as.size() > 0) {
			Iterator<Assign> ite = as.iterator();
			while(cnt < 0 && ite.hasNext()) {
				cnt++;
				this.cancelAssign(ite.next());
			}
		}
	}


	private void doAddAssign(Collection<AgentInfo> agentInfo, Deployment dep, int cnt) {

		long curTime = TimeUtils.getCurTime();
		
		List<AgentInfo> sortList = new LinkedList<>();
		sortList.addAll(agentInfo);
		
		//Agent已经启动对应部署
		filterByCurProcess(sortList,dep);
		
		String agentIds = null;
		if(StringUtils.isNotEmpty(dep.getStrategyArgs())) {
			Map<String,String> params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
			//指定Agent运行部署
			agentIds = params.get(IAssignStrategy.AGENT_ID);
		}
		
		if(StringUtils.isNotEmpty(agentIds)) {
			//只保留指定的Agent，其他的删除
			filterByAgentId(sortList,dep,cnt,agentIds);
			if(sortList == null || sortList.isEmpty()) {
				//logger.debug("doAddAssign: Agent ID: " + agentIds + " not on line for dep: " + dep.toString());
				LG.log(MC.LOG_INFO, TAG, "doAddAssign: Specify agent ID: " + agentIds + " not started for dep: " + dep.toString());
				return;
			}
		} else { 
			if(!sortList.isEmpty()) {
				//由策略负责分配
				filterAgentByStrategy(sortList,dep,cnt);
			}
		}
		
		if(sortList.isEmpty()) {
			//logger.error("No agent for assign dep [" + dep.toString() + "]");
			LG.log(MC.LOG_WARN, TAG, "No agent for assign dep [" + dep.toString() + "]");
			return;
		}
		
		Iterator<AgentInfo> ite = sortList.iterator();
		
		while(ite.hasNext()) {
			AgentInfo aif = ite.next();
			
			//String path = ChoyConstants.ROOT_AGENT + "/" + aif.getId() + "/"+dep.getId();

			//String pid = idServer.getStringId(ProcessInfo.class);
			Integer pid = Integer.parseInt(op.getData(ChoyConstants.ID_PATH)) +1;
			op.setData(ChoyConstants.ID_PATH, pid+"");
			
			Assign a = new Assign(dep.getId(),aif.getId(),pid);
			a.opTime = curTime;
			a.state = AssignState.INIT;
			assingManager.add(a);
			
			String msg = "Assign dep: "+ a.toString();
			LG.log(MC.LOG_INFO, TAG,msg);
			MT.nonRpcEvent(MC.MT_ASSIGN_ADD);
			
			//logger.info("Assign: " + msg);
			
			nextDeployTimeout.put(dep.getId(), curTime);

			if(--cnt == 0) {
				break;
			}
		
		}
	}

	private void filterByCurProcess(Collection<AgentInfo> agentInfo, Deployment dep) {
		Set<ProcessInfo> processes = this.insManager.getProcessesByDepId(dep.getId());
		if(processes == null || processes.isEmpty()) {
			return;
		}

		Iterator<AgentInfo> ite = agentInfo.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			ProcessInfo pi = this.insManager.getProcessesByAgentIdAndDepid(ai.getId(), dep.getId());
			if(pi != null) {
				ite.remove();
			}
		}
	}

	private void filterByAgentId(List<AgentInfo> sortList, Deployment dep, int cnt, String agentIds) {
		
		String[] aids = agentIds.split(",");
		
		Iterator<AgentInfo> ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			boolean f = false;
			for(String aid : aids) {
				if(ai.getId().equals(aid)) {
					f = true;
				}
			}
			
			if(!f) {
				ite.remove();
			}
		}
		
	}

	private void filterAgentByStrategy(List<AgentInfo> sortList, Deployment dep,int cnt) {
		
		if(sortList == null || sortList.isEmpty()) {
			return;
		}
		
		Iterator<AgentInfo> ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			//去除私有的Agent
			if(ai.isPrivat()) {
				ite.remove();
			}
		}
		
		long curTime = TimeUtils.getCurTime();
		ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(curTime - ai.getStartTime() < 15000) {
				//启动后15秒内不给做任务分配，以使Agent达到稳定状态
				//logger.info(ai.getId() + " exclude since start time less than 15 seconds!");
				LG.log(MC.LOG_DEBUG, TAG,ai.getId() + " exclude since start time less than 15 seconds!");
				ite.remove();
				continue;
			}
			
			Set<Assign> as = assingManager.getAssignByDepIdAndAgentId(dep.getId(), ai.getId());
			if(!as.isEmpty()) {
				/*if(logger.isDebugEnabled()) {
					logger.debug(dep.getJarFile() + " is assigned: " + ai.getId() );
				}*/
				LG.log(MC.LOG_DEBUG, TAG,dep.getJarFile() + " have been assigned to agent " + ai.getId() );
				ite.remove();
				continue;
			}
			
			if(fails.containsKey(ai.getId())) {
				if(fails.get(ai.getId()).contains(dep.getId())) {
					/*if(logger.isDebugEnabled()) {
						logger.debug(dep.getJarFile() + " is failure in recent for: " + ai.getId() );
					}*/
					LG.log(MC.LOG_DEBUG, TAG,dep.getJarFile() + " is failure in recent by agent: " + ai.getId());
					ite.remove();
					continue;
				}
			}
			
		}
		
		if(sortList.size() == 0) {
			boolean flag = false;
			for(Set<String> set : fails.values()) {
				if(set.contains(dep.getId())) {
					set.remove(dep.getId());
					flag = true;
				}
			}
			
			if(flag) {
				LG.log(MC.LOG_DEBUG, TAG,dep.getJarFile() + "Do reassign depId: " + dep.getId());
				//logger.info("Do reassign depId: " + dep.getId());
				doAddAssign(sortList,dep,cnt);
			}
			return ;
		}
		
		IAssignStrategy s = null;
		
		if(StringUtils.isEmpty(dep.getAssignStrategy()) 
				|| "defautAssignStrategy".equals(dep.getAssignStrategy())) {
			s = this.defaultAssignStrategy;
		} else {
			s = of.getByName(dep.getAssignStrategy());
			if(s == null) {
				s = this.defaultAssignStrategy;
				//logger.error("Assign strategy [" + dep.getAssignStrategy() + "] not found, use default strategy");
				LG.log(MC.LOG_DEBUG, TAG,"Assign strategy [" + dep.getAssignStrategy() + "] not found, use default strategy");
			}
		}
		
		if(!s.doStrategy(sortList, dep)) {
			//logger.error("Assign fail with strategy [" + dep.getAssignStrategy() + "]");
			LG.log(MC.LOG_DEBUG, TAG,"Assign fail with strategy [" + dep.getAssignStrategy() + "]");
			return;
		}
	}

	
	
}
