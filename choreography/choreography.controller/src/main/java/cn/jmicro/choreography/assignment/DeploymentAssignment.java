package cn.jmicro.choreography.assignment;

import java.util.Collection;
import java.util.Collections;
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
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IConnectionStateChangeListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.agent.AgentManager;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.api.IInstanceListener;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component(level=30)
public class DeploymentAssignment {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentAssignment.class);
	
	@Cfg("/enableMasterSlaveModel")
	private boolean isMasterSlaveModel = false;
	
	private boolean isMaster = true;
	
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
	
	//@Inject
	//private ComponentIdServer idServer;
	
	//Agent to fail instance
	private Map<String,Set<String>> fails = new HashMap<>();
	
	private Set<Assign> assigns = Collections.synchronizedSet(new HashSet<>());
	
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
	
	private IMasterChangeListener mcl = (type,isMaster)->{
		if(isMaster && (IMasterChangeListener.MASTER_ONLINE == type 
				|| IMasterChangeListener.MASTER_NOTSUPPORT == type)) {
			//参选成功
			 logger.info(Config.getInstanceName() + " got as master");
			 isMaster = true;
			 ready0();
		} else {
			 //参选失败
			 isMaster = false;
			 lostMaster();
		}
	};
	
	 private void ready0() {
		 String conRootPath = ChoyConstants.ROOT_CONTROLLER + "/" + this.processInfo.getId();
		 if(op.exist(conRootPath)) {
			 op.deleteNode(conRootPath);
		 }
		 op.createNodeOrSetData(conRootPath, this.processInfo.getId(), IDataOperator.EPHEMERAL);
		 actKey = Config.getInstanceName() + "_DeploymentAssignmentChecker";
		 op.addListener(connListener);
		 TimerTicker.getDefault(1000*5L).addListener(actKey,null,act);
	 }
	 
	 private void lostMaster() {
		 
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
		 assigns.clear();
		 
	 }
	
	public void ready() {
		if(!op.exist(ChoyConstants.ID_PATH)) {
			op.createNodeOrSetData(ChoyConstants.ID_PATH, "0", IDataOperator.PERSISTENT);
		}
		if(isMasterSlaveModel) {
			this.of.masterSlaveListen(mcl);
		} else {
			ready0();
		}
	}
	
	private void registListener() {
		op.addChildrenListener(ChoyConstants.DEP_DIR, depListener);
		insManager.addListener(insListener);
	}
	
	private void doChecker() {
		 
		 long curTime = System.currentTimeMillis();
		 if(curTime - lastCheckTime < 5000) {
			 return;
		 }
		 
		 Set<Assign> ass = new HashSet<>();
		 ass.addAll(this.assigns);
		 
		 //检测分配后2分钟内有没有启动成功，如果没有启动成功，则取消分配
		 for(Assign a : ass) {
			 if(a.state == AssignState.STARTING && curTime - a.opTime > 120000) {
				 //starting timeout
				 logger.error("Starting timeout: " + a.toString());
				 cancelAssign(a);
				 continue;
			 }
			 
			 if(a.state == AssignState.STOPING && curTime - a.opTime > 15000) {
				 //stoping timeout
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.getInsId();
				 if(!op.exist(piPath)) {
					 logger.error("Delete invalid assign: " + a.toString());
					 this.assigns.remove(a);
				 }else {
					 String data = op.getData(piPath);
					 if(StringUtils.isEmpty(data)) {
						 logger.warn("Delete invalid process node: " + piPath);
						 op.deleteNode(piPath);
						 this.assigns.remove(a);
					 } else {
						 
						 if(a.checkTime > 100) {
							 op.deleteNode(piPath);
							 this.assigns.remove(a);
							 logger.error("Process stop exception Assign: " + a.toString());
							 logger.error("Process stop exception processInfo: " + data);
							 logger.error("You should check it by your hand, sorry for this case!");
						 } else {
							 ProcessInfo pi = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
							 if(pi.isActive()) {
								 a.checkTime++;
								 logger.warn("Do cancel time["+a.checkTime+"] again: " + data);
								 this.cancelAssign(a);
							 } else {
								 a.checkTime++;
								 logger.warn("Process in stoping time["+a.checkTime+"] state: " + data);
							 }
						 }
						 
					 }
				 }
				 
				 String apath = ChoyConstants.ROOT_AGENT+"/"+a.getAgentId()+"/"+a.depId;
				 if(op.exist(apath)) {
					 op.deleteNode(apath);
				 }
				 
				 continue;
			 }
			 
			 if(a.state == AssignState.STARTED && curTime - a.opTime > 120000) {
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.getInsId();
				 if(!op.exist(piPath)) {
					 //实例已经不存在
					 logger.error("Process exit: " + piPath);
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
				 logger.warn("Force restart deployment: "+ dep.toString());
				 stopDeployment(dep.getId());
				 dep.setForceRestart(false);
			 } else if(dep.isEnable()) {
				 if(nextDeployTimeout.containsKey(dep.getId())) {
					 if(curTime - nextDeployTimeout.get(dep.getId()) < 60000 ) {
						 //两次分配动作之间最少等待一分钟
						 continue;
					 } else {
						 nextDeployTimeout.remove(dep.getId());
					 }
				 }
				 this.doAssgin(dep);
			 }
		 }
		 lastCheckTime = System.currentTimeMillis();
	}
	
	private void instanceRemoved(String insId) {
		Assign a = this.getAssignByInfoId(insId);
		if(a != null) {
			logger.info("Instance remove: " + a.toString());
			cancelAssign(a);
			this.assigns.remove(a);
		}
	}
	
	private void instanceAdded(ProcessInfo pi) {
		
		if(pi == null || StringUtils.isEmpty(pi.getAgentId())) {
			return;
		}
		
		Assign a = this.getAssignByInfoId(pi.getId());
		if(a == null) {
			//初次启动时，对已经存在的实例做实例化
			logger.info("Instance add for origint: " + pi.toString());
			a = new Assign(pi.getDepId(),pi.getAgentId(),pi.getId());
			this.assigns.add(a);
		} else {
			logger.info("Instance start success: " + pi.toString());
		}
		
		a.opTime = System.currentTimeMillis();
		a.state = AssignState.STARTED;
	}

	private void deploymentRemoved(String d, String data) {
		logger.info("Remove deployment ID: "+d);
		deployments.remove(d);
		stopDeployment(d);
	}
	
	private void stopDeployment(String depId) {
		Set<Assign> ownerAgents = this.getAssignByDepId(depId);
		if(ownerAgents == null || ownerAgents.isEmpty()) {
			return;
		}
		
		logger.info("Stop deployment: "+depId);
		for(Assign a : ownerAgents) {
			if(a.state == AssignState.STARTING || a.state == AssignState.STARTED) {
				logger.info("Cancel assign: "+a.toString());
				cancelAssign(a);
			}
		}
	}
	
	private void cancelAssign(Assign a) {
		if(a.state == AssignState.STOPING) {
			if(null == insManager.getProcessesByInsId(a.getInsId(),false)) {
				this.assigns.remove(a);
			}
			logger.warn("Assign is on stoping state: depId:"+a.getDepId() + ", agentId: " + a.getAgentId());
			return;
		}
		
		logger.info("Cancel dep ["+a.getDepId()+"], agentId [" + a.getAgentId()+"]");
		
		Set<Assign> set = this.getAssignByDepIdAndAgentId(a.getDepId(), a.getAgentId());
		String path = ChoyConstants.ROOT_AGENT+"/"+a.getAgentId()+"/"+a.getDepId();
		if(set.size() > 1 || !this.agentManager.isActive(a.getAgentId()) || !op.exist(path)) {
			//Agent挂机状态，直接关闭服务进程
			ProcessInfo pi = this.insManager.getProcessesByInsId(a.getInsId(),false);
			String p = ChoyConstants.INS_ROOT + "/" + a.getInsId();
			if(op.exist(p) && pi.isActive()) {
				pi.setActive(false);
				String data = JsonUtils.getIns().toJson(pi);
				op.setData(p, data);
				logger.info("Stop process: " + data);
			}
		} else {
			logger.debug("Delete deploy: " + path);
			op.deleteNode(path);
		}
		
		if(!fails.containsKey(a.getAgentId())) {
			fails.put(a.getAgentId(), new HashSet<String>());
		}
		fails.get(a.getAgentId()).add(a.getDepId());
		
		a.state = AssignState.STOPING;
		a.opTime = System.currentTimeMillis();
		
		nextDeployTimeout.put(a.getDepId(), System.currentTimeMillis());
	}

	private void deploymentDataChanged(String depId, String data) {
		Deployment newDep = JsonUtils.getIns().fromJson(data, Deployment.class);
		if(newDep != null) {
			deployments.put(depId, newDep);
		}
	}

	private void deploymentAdded(String depId, String data) {
		deploymentDataChanged(depId,data);
	}

	private void doAssgin(Deployment dep) {
		if(!dep.isEnable()) {
			return;
		}
		Set<Assign> ass = this.getAssignByDepId(dep.getId());
		//filterState(ass,AssignState.STARTED,AssignState.STARTING);
		this.fiterByState(ass, AssignState.STARTED,AssignState.STARTING);
		
		Set<AgentInfo> agentInfo = this.agentManager.getAllAgentInfo();
		
		int cnt = dep.getInstanceNum() - ass.size();
		if(cnt > 0) {
			//增加运行实例
			if(agentInfo == null || agentInfo.isEmpty()) {
				return ;
			}
			doAddAssign(agentInfo,dep,cnt);
		}else if(cnt < 0) {
			//减少运行实例
			doDecAssign(agentInfo,dep,cnt);
		}
		
	}
	
	private void fiterByState(Set<Assign> ass, AssignState... states) {
		Iterator<Assign> ite = ass.iterator();
		while(ite.hasNext()) {
			Assign a = ite.next();
			
			boolean f = false;
			for(AssignState as : states) {
				if(as == a.state) {
					f = true;
				}
			}
			
			if(!f) {
				ite.remove();
			}
			
		}
		
	}

	private void doDecAssign(Set<AgentInfo> agentInfo, Deployment dep, int cnt) {
		
		Set<ProcessInfo> depAgents = this.insManager.getProcessesByDepId(dep.getId());
		if(depAgents == null || depAgents.isEmpty()) {
			logger.info("No agents for dep: " + dep.toString());
			return;
		}
		
		List<AgentInfo> sortList = new LinkedList<>();
		for(ProcessInfo pi : depAgents) {
			Iterator<AgentInfo> ite = sortList.iterator();
			while(ite.hasNext()) {
				if(pi.getAgentId().equals(ite.next().getId())) {
					ite.remove();
					break;
				}
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
			Set<Assign> as = this.getAssignByDepIdAndAgentId(dep.getId(), aif.getId());
			
			if(!as.isEmpty()) {
				
				Assign a = getByState(as,AssignState.STARTING);
				if(a == null) {
					a =  getByState(as,AssignState.STARTED);
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
		
		logger.warn("Force direct stop [" + dep.getId() + "], count [" + (-cnt) + "] ");
		
		Set<Assign> as = this.getAssignByDepId(dep.getId());
		if(as.size() > 0) {
			Iterator<Assign> ite = as.iterator();
			while(ite.hasNext()) {
				this.cancelAssign(ite.next());
			}
		}
	}


	private void doAddAssign(Collection<AgentInfo> agentInfo, Deployment dep, int cnt) {

		long curTime = System.currentTimeMillis();
		
		List<AgentInfo> sortList = new LinkedList<>();
		sortList.addAll(agentInfo);
		
		filterByCurProcess(sortList,dep);
		
		String agentIds = null;
		if(StringUtils.isNotEmpty(dep.getStrategyArgs())) {
			Map<String,String> params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
			agentIds = params.get(IAssignStrategy.AGENT_ID);
		}
		
		if(StringUtils.isNotEmpty(agentIds)) {
			filterByAgentId(sortList,dep,cnt,agentIds);
			if(sortList == null || sortList.isEmpty()) {
				logger.debug("doAddAssign: Agent ID: " + agentIds + " not on line for dep: " + dep.toString());
				return;
			}
		} else { 
			if(!sortList.isEmpty()) {
				filterAgentByStrategy(sortList,dep,cnt);
			}
		}
		
		if(sortList.isEmpty()) {
			logger.error("No agent for assign dep [" + dep.toString() + "]");
			return;
		}
		
		Iterator<AgentInfo> ite = sortList.iterator();
		
		while(ite.hasNext()) {
			AgentInfo aif = ite.next();
			
			String path = ChoyConstants.ROOT_AGENT + "/" + aif.getId() + "/"+dep.getId();
			if(!op.exist(path)) {

				//String pid = idServer.getStringId(ProcessInfo.class);
				String pid = (Long.parseLong(op.getData(ChoyConstants.ID_PATH)) +1)+"";
				op.setData(ChoyConstants.ID_PATH, pid);
				op.createNodeOrSetData(path, pid, IDataOperator.PERSISTENT);
				
				Assign a = new Assign(dep.getId(),aif.getId(),pid);
				a.opTime = curTime;
				a.state = AssignState.STARTING;
				assigns.add(a);
				
				aif.setAssignTime(curTime);
				String dd = JsonUtils.getIns().toJson(aif);
				op.setData(ChoyConstants.ROOT_AGENT+"/"+aif.getId(), dd);
				
				logger.warn("Assign deployment: "+ dep.toString());
				logger.info("Assign: " + dep.getId() + " to " + dd);
				
				nextDeployTimeout.put(dep.getId(), curTime);

				if(--cnt == 0) {
					return;
				}
			
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
			if(ai.isPrivat()) {
				ite.remove();
			}
		}
		
		long curTime = System.currentTimeMillis();
		ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(curTime - ai.getStartTime() < 60000) {
				//启动后1分钟内不给做任务分配，以使Agent达到稳定状态
				ite.remove();
				continue;
			}
			
			Set<Assign> as = this.getAssignByDepIdAndAgentId(dep.getId(), ai.getId());
			if(!as.isEmpty()) {
				ite.remove();
				continue;
			}
			
			if(fails.containsKey(ai.getId())) {
				if(fails.get(ai.getId()).contains(dep.getId())) {
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
				logger.info("Do reassign depId: " + dep.getId());
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
				logger.error("Assign strategy [" + dep.getAssignStrategy() + "] not found, use default strategy");
			}
		}
		
		if(!s.doStrategy(sortList, dep)) {
			logger.error("Assign fail with strategy [" + dep.getAssignStrategy() + "]");
			return;
		}
		
	}

	private enum AssignState{
		INIT,STARTING,STOPING,STARTED
	}

	private class Assign {
		
		public Assign(String depId,String agentId,String insId) {
			if(StringUtils.isEmpty(insId)) {
				throw new CommonException("Process instance ID cannot be NULL");
			}
			
			if(StringUtils.isEmpty(agentId)) {
				throw new CommonException("Agent ID cannot be NULL");
			}
			
			if(StringUtils.isEmpty(depId)) {
				throw new CommonException("Deployment ID cannot be NULL");
			}
			
			this.depId = depId;
			this.agentId = agentId;
			this.insId = insId;
		}
		
		private String depId;
		private String agentId;
		private String insId;
		
		public AssignState state = AssignState.INIT;
		
		public long opTime;
		
		public int checkTime = 0;
		
		public String getDepId() {
			return depId;
		}

		public String getAgentId() {
			return agentId;
		}

		public String getInsId() {
			return insId;
		}

		@Override
		public int hashCode() {
			if(this.getInsId() == null || "".equals(this.getInsId())) {
				return (this.agentId + this.depId).hashCode();
			} else {
				return Integer.parseInt(this.getInsId());
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return hashCode() == obj.hashCode();
		}

		@Override
		public String toString() {
			return "Assign [depId=" + depId + ", agentId=" + agentId + ", insId=" + getInsId() + "]";
		}

	}
	
	private Assign getAssignByInfoId(String id) {
		for(Assign a : this.assigns) {
			if(a.getInsId() == null) {
				continue;
			}
			if(id.equals(a.getInsId())) {
				return a;
			}
		}
		return null;
	}
	
	@SuppressWarnings({"unchecked" })
	private Set<Assign> getAssignByDepId(String depId) {
		if(StringUtils.isEmpty(depId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		for(Assign a : this.assigns) {
			if(depId.equals(a.getDepId())) {
				s.add(a);
			}
		}
		return s;
	}
	
	
	@SuppressWarnings({ "unused", "unchecked" })
	private Set<Assign> getAssignByAgentId(String agentId) {
		if(StringUtils.isEmpty(agentId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		for(Assign a : this.assigns) {
			if(agentId.equals(a.getAgentId())) {
				s.add(a);
			}
		}
		return s;
	}
	
	private Set<Assign> getAssignByDepIdAndAgentId(String depId, String agentId) {
		
		if(StringUtils.isEmpty(agentId) || StringUtils.isEmpty(depId) ) {
			return null;
		}
		
		Set<Assign> s = new HashSet<>();
		for(Assign a : this.assigns) {
			if(agentId.equals(a.getAgentId()) && depId.equals(a.getDepId())) {
				s.add(a);
			}
		}
		return s;
	}
	
}
