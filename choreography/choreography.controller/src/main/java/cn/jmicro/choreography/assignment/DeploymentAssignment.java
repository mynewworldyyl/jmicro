package cn.jmicro.choreography.assignment;

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
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.agent.AgentManager;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component(level=30)
public class DeploymentAssignment {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentAssignment.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private AgentManager agentManager;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private ILockerManager lockMgn;
	
	//Agent to fail instance
	private Map<String,Set<String>> fails = new HashMap<>();
	
	private Set<Assign> assigns = new HashSet<>();
	
	private Map<String,Deployment> deployments = new HashMap<>();
	
	private Map<String,Long> nextDeployTimeout = new HashMap<>();
	
	private IDataListener deploymentDataListener = (path,data)->{
		deploymentDataChanged(path.substring(ChoyConstants.DEP_DIR.length()+1),data);
	};
	
	public void ready() {
		op.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				registListener();
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				registListener();
			}
		});
		
		TimerTicker.getDefault(1000*3L).addListener("", (key,att)->{
			try {
				doChecker();
			} catch (Throwable e) {
				logger.error("doChecker",e);
			}
		}, null);
		
	}
	
	
	private void registListener() {
		op.addChildrenListener(ChoyConstants.DEP_DIR, (type,p,c,data)->{
			if(type == IListener.ADD) {
				String path = ChoyConstants.DEP_DIR + "/" + c;
				op.addDataListener(path, deploymentDataListener);
				deploymentAdded(c,data);
			}else if(type == IListener.REMOVE) {
				String path = ChoyConstants.DEP_DIR + "/" + c;
				op.removeDataListener(path, deploymentDataListener);
				deploymentRemoved(c,data);
			}
		});
		
		insManager.addListener((type,pi) -> {
			if(type == IListener.ADD) {
				instanceAdded(pi);
			} else if(type == IListener.REMOVE) {
				if(pi != null) {
					instanceRemoved(pi.getId());
				}
			}
		});
		
	}

	private void doChecker() {
		 Set<Assign> ass = new HashSet<>();
		 ass.addAll(this.assigns);
		 
		 long curTime = System.currentTimeMillis();
		 
		 //检测分配后1分钟内有没有启动成功，如果没有启动成功，则取消分配
		 for(Assign a : ass) {
			 if(a.insId == null && curTime - a.assignTime > 120000) {
				 cancelAssign(a);
				 continue;
			 }
			 
			 if(a.insId != null) {
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.insId;
				 if(!op.exist(piPath)) {
					 instanceRemoved(a.insId);
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
		
	}
	
	private void instanceRemoved(String insId) {
		Assign a = this.getAssignByInfoId(insId);
		if(a != null) {
			logger.info("Instance remove: " + a.toString());
			cancelAssign(a);
		}
	}
	
	private void instanceAdded(ProcessInfo pi) {
		
		if(pi == null || StringUtils.isEmpty(pi.getAgentId())) {
			return;
		}
		
		Assign a = this.getAssignByDepIdAndAgentId(pi.getDepId(),pi.getAgentId());
		if(a == null) {
			//初次启动时，对已经存在的实例做实例化
			logger.info("Instance add for origint: " + pi.toString());
			a = new Assign(pi.getDepId(),pi.getAgentId());
			a.assignTime = System.currentTimeMillis();
			this.assigns.add(a);
		} else {
			logger.info("Instance start success: " + pi.toString());
		}
		
		a.insId = pi.getId();
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
			logger.info("Cancel assign: "+a.toString());
			cancelAssign(a);
		}
	}
	
	private void cancelAssign(Assign a) {
		logger.info("Cancel dep ["+a.depId+"], agentId [" + a.agentId+"]");
		
		String path = ChoyConstants.ROOT_AGENT+"/"+a.agentId+"/"+a.depId;
		if(op.exist(path)) {
			op.deleteNode(path);
		}
		
		if(!fails.containsKey(a.agentId)) {
			fails.put(a.agentId, new HashSet<String>());
		}
		fails.get(a.agentId).add(a.depId);
		
		this.assigns.remove(a);
		nextDeployTimeout.put(a.depId, System.currentTimeMillis());
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
		
		Set<AgentInfo> agentInfo = this.agentManager.getAllAgentInfo();
		if(agentInfo == null || agentInfo.isEmpty()) {
			return ;
		}
		
		int cnt = dep.getInstanceNum() - ass.size();
		if(cnt > 0) {
			//增加运行实例
			doAddAssign(agentInfo,dep,cnt);
		}else if(cnt < 0) {
			//减少运行实例
			doDecAssign(agentInfo,dep,cnt);
		}
		
	}
	
	private void doDecAssign(Set<AgentInfo> agentInfo, Deployment dep, int cnt) {
		
		long curTime = System.currentTimeMillis();
		
		List<AgentInfo> sortList = new LinkedList<>();
		sortList.addAll(agentInfo);
		
		Iterator<AgentInfo> ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(curTime - ai.getStartTime() < 60000) {
				//启动后1分钟内不给做任务分配，以使Agent达到稳定状态
				ite.remove();
				continue;
			}
		}
		
		if(sortList.size() == 0) {
			return ;
		}
		
		if(sortList.size() > 1) {
			sortList.sort((AgentInfo o1, AgentInfo o2)->{
				//运行实例最多的排前头
				return o1.getCprRate() > o2.getCprRate() ? 1: o1.getCprRate() == o2.getCprRate()?0:-1;
			});
		}
		
		ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo aif = ite.next();
			Assign a = this.getAssignByDepIdAndAgentId(dep.getId(), aif.getId());
			if(a != null) {
				this.cancelAssign(a);
				cnt++;
				if(cnt >= 0) {
					break;
				}
			}
		}
		
	}

	private void doAddAssign(Set<AgentInfo> agentInfo, Deployment dep,int cnt) {

		long curTime = System.currentTimeMillis();
		
		List<AgentInfo> sortList = new LinkedList<>();
		sortList.addAll(agentInfo);
		
		Iterator<AgentInfo> ite = sortList.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(curTime - ai.getStartTime() < 60000) {
				//启动后1分钟内不给做任务分配，以使Agent达到稳定状态
				ite.remove();
				continue;
			}
			Assign a = this.getAssignByDepIdAndAgentId(dep.getId(), ai.getId());
			if(a != null) {
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
				doAddAssign(agentInfo,dep,cnt);
			}
			return ;
		}
		
		if(sortList.size() > 1) {
			sortList.sort((AgentInfo o1, AgentInfo o2)->{
				//Cpu利用率小的排前头
				return o1.getCprRate() > o2.getCprRate() ? -1: o1.getCprRate() == o2.getCprRate()?0:1;
			});
		}
		
		ite = sortList.iterator();
		
		while(ite.hasNext()) {
			AgentInfo aif = ite.next();
			
			String path = ChoyConstants.ROOT_AGENT+"/"+aif.getId()+"/"+dep.getId();
			if(op.exist(path)) {
				Assign a = new Assign(dep.getId(),aif.getId());
				this.assigns.add(a);
			} else {
				op.createNodeOrSetData(path, "", IDataOperator.PERSISTENT);
				
				aif.setAssignTime(curTime);
				String dd = JsonUtils.getIns().toJson(aif);
				op.setData(ChoyConstants.ROOT_AGENT+"/"+aif.getId(), dd);
				
				Assign a = new Assign(dep.getId(),aif.getId());
				this.assigns.add(a);
				a.assignTime = curTime;
				
				logger.warn("Assign deployment: "+ dep.toString());
				logger.info("Assign: "+dep.getId() +" to "+ dd);
				
				nextDeployTimeout.put(dep.getId(), curTime);

				if(--cnt == 0) {
					return;
				}
			}
			
		}
	}


	private class Assign {
		public Assign(String depId,String agentId) {
			this.depId = depId;
			this.agentId = agentId;
		}
		public String depId;
		public String agentId;
		public String insId;
		
		public long assignTime;
		
		@Override
		public int hashCode() {
			String cid = this.depId + this.agentId;
			return cid.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Assign)) {
				return false;
			}
			return hashCode() == obj.hashCode();
		}
		
		@Override
		public String toString() {
			return "Assign [depId=" + depId + ", agentId=" + agentId + ", insId=" + insId + "]";
		}
		
		
	}
	
	private Assign getAssignByInfoId(String id) {
		for(Assign a : this.assigns) {
			if(a.insId == null) {
				continue;
			}
			if(id.equals(a.insId)) {
				return a;
			}
		}
		return null;
	}
	
	private Set<Assign> getAssignByDepId(String depId) {
		if(StringUtils.isEmpty(depId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		for(Assign a : this.assigns) {
			if(depId.equals(a.depId)) {
				s.add(a);
			}
		}
		return s;
	}
	
	
	private Set<Assign> getAssignByAgentId(String agentId) {
		if(StringUtils.isEmpty(agentId)) {
			return Collections.EMPTY_SET;
		}
		Set<Assign> s = new HashSet<>();
		for(Assign a : this.assigns) {
			if(agentId.equals(a.agentId)) {
				s.add(a);
			}
		}
		return s;
	}
	
	private Assign getAssignByDepIdAndAgentId(String depId, String agentId) {
		
		if(StringUtils.isEmpty(agentId) || StringUtils.isEmpty(depId) ) {
			return null;
		}
		
		for(Assign a : this.assigns) {
			if(agentId.equals(a.agentId) && depId.equals(a.depId)) {
				return a;
			}
		}
		return null;
	}
	
}
