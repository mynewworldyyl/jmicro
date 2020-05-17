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
import cn.jmicro.api.cache.lock.ILocker;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.common.CommonException;
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
	private ILockerManager lockMgn;
	
	private Set<Assign> assigns = new HashSet<>();
	
	private Map<String,Deployment> deployments = new HashMap<>();
	
	private Map<String,Long> nextDeployTimeout = new HashMap<>();
	
	private IDataListener processInfoDataListener = (path,data)-> {
		ProcessInfo pi = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
		Assign a = this.getAssignByInfoId(pi.getId());
		if(a != null) {
			a.pi = pi;
		}
	};
	
	private IDataListener deploymentDataListener = (path,data)->{
		deploymentDataChanged(path.substring(ChoyConstants.DEP_DIR.length()+1),data);
	};
	
	public void ready() {
		
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
		
		agentManager.addAgentListener((t,ai) -> {
			/*if(t == IListener.REMOVE) {
				//Agent 停机，重新分配部署
				Set<Assign> aas = this.getAssignByAgentId(ai.getId());
				if(aas != null) {
					for(Assign a : aas) {
						cancelAssign(a);
					}
				}
			}*//*else if(t == IListener.ADD) {
				for(Deployment dep : deployments.values()) {
					doAssgin(dep);
				}
			}*/
		});
		
		op.addChildrenListener(ChoyConstants.INS_ROOT, (type,p,c,data)->{
			if(type == IListener.ADD) {
				instanceAdded(c,data);
			} else if(type == IListener.REMOVE) {
				instanceRemoved(c);
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

	private void doChecker() {
		 Set<Assign> ass = new HashSet<>();
		 ass.addAll(this.assigns);
		 
		 long curTime = System.currentTimeMillis();
		 //检测分配后1分钟内有没有启动成功，如果没有启动成功
		 for(Assign a : ass) {
			 if(a.pi == null && curTime - a.assignTime > 120000) {
				 cancelAssign(a);
				 continue;
			 }
			 
			 if(a.pi != null) {
				 String piPath = ChoyConstants.INS_ROOT + "/" + a.pi.getId();
				 if(!op.exist(piPath)) {
					 instanceRemoved(a.pi.getId());
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
				 nextDeployTimeout.put(dep.getId(), System.currentTimeMillis());
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

	/*private void loadDeployment() {
		Set<String> deps = op.getChildren(DEP_DIR, false);
		for(String d : deps) {
			String p = DEP_DIR + "/" + d;
			String data = op.getData(p);
			this.deploymentAdded(d, data);
		}
	}*/
	
	private void instanceRemoved(String insId) {
		Assign a = this.getAssignByInfoId(insId);
		if(a != null) {
			logger.info("Instance remove: " + a.toString());
			op.removeDataListener(ChoyConstants.INS_ROOT+"/" + insId, this.processInfoDataListener);
			cancelAssign(a);
		}
	}
	
	private void instanceAdded(String insId, String json) {
		
		ProcessInfo pi = JsonUtils.getIns().fromJson(json, ProcessInfo.class);
		if(StringUtils.isEmpty(pi.getAgentId())) {
			//非编排环境下启动的实例
			return;
		}
		
		Assign a = this.getAssignByDepIdAndAgentId(pi.getDepId(),pi.getAgentId());
		if(a == null) {
			//初次启动时，对已经存在的实例做实例化
			logger.info("Instance add for origint: " + json);
			a = new Assign(pi.getDepId(),pi.getAgentId());
			a.assignTime = System.currentTimeMillis();
			this.assigns.add(a);
		}else {
			logger.info("Instance start success: " + json);
		}
		
		a.pi = pi;
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
		
		ILocker locker = null;
		String ap = ChoyConstants.ROOT_AGENT + "/" + a.agentId;
		try {
			locker = lockMgn.getLocker(ap);
			if(locker.tryLock(3*1000)) {
				String data = op.getData(ap);
				AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
				if(ai != null && ai.getRunningDeps().contains(a.depId)) {
					ai.getDeleteDeps().add(a.depId);
					ai.setAssignTime(System.currentTimeMillis());
					op.setData(ap, JsonUtils.getIns().toJson(ai));
					nextDeployTimeout.put(a.depId, System.currentTimeMillis());
				}
				this.assigns.remove(a);
			} else {
				logger.error("Fail to get locker:" + ap);
			}
		}finally {
			if(locker != null) {
				locker.unLock();
			}
		}
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
		
		checkAssignFail(agentInfo);
		
		checkDeleteFail(agentInfo);
		
		int cnt = dep.getInstanceNum() - ass.size();
		if(cnt == 0) {
			return;
		}else if(cnt > 0) {
			//减少运行实例
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
				return o1.getRunningDeps().size() > o2.getRunningDeps().size() ? 1: o1.getRunningDeps().size() == o2.getRunningDeps().size()?0:-1;
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

	private void doAddAssign(Set<AgentInfo> agentInfo,Deployment dep,int cnt) {

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
			if(ai.getRunningDeps().contains(dep.getId()) ||
					ai.getAssignDeps().contains(dep.getId())) {
				ite.remove();
				continue;
			}
		}
		
		if(sortList.size() == 0) {
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
			AgentInfo aif0 = ite.next();
			
			String path = ChoyConstants.ROOT_AGENT+"/"+aif0.getId();
			String data = this.op.getData(path);
			if(StringUtils.isEmpty(data)) {
				continue;
			}
			
			final AgentInfo aif = JsonUtils.getIns().fromJson(data, AgentInfo.class);
			
			doInlocker(path,()->{
				if(!aif.getRunningDeps().contains(dep.getId())) {
					aif.getAssignDeps().add(dep.getId());
					aif.setAssignTime(curTime);
					String dd = JsonUtils.getIns().toJson(aif);
					op.setData(ChoyConstants.ROOT_AGENT+"/"+aif.getId(), dd);
					Assign a = new Assign(dep.getId(),aif.getId());
					this.assigns.add(a);
					a.assignTime = curTime;
					logger.warn("Assign deployment: "+ dep.toString());
					logger.info("Assign: "+dep.getId() +" to "+ data);
					nextDeployTimeout.put(dep.getId(), curTime);
				}
			});
			
			if(--cnt == 0) {
				return;
			}
			
			ILocker locker = null;
			boolean lockSucc = false;
			try {
				locker = lockMgn.getLocker(path);
				if(lockSucc = locker.tryLock(3*1000)) {} else {
					logger.error("Fail to get locker:" + path);
				}
			}finally {
				if(locker != null && lockSucc) {
					locker.unLock();
				}
			}
		}
	
		
	}

	private void checkDeleteFail(Set<AgentInfo> agentInfo) {
		long curTime = System.currentTimeMillis();
		for(AgentInfo ai : agentInfo) {
			if(ai.getDeleteDeps().isEmpty()) {
				continue;
			}
			
			String path = ChoyConstants.ROOT_AGENT+"/"+ai.getId();
			if(curTime - ai.getAssignTime() > 120000) { //两分钟内都没有分配成功
				doInlocker(path,()->{
					String data = op.getData(path);
					AgentInfo a = JsonUtils.getIns().fromJson(data, AgentInfo.class);
					if(a != null) {
						a.setAssignTime(curTime); //触发下一次删除操作
						op.setData(path, JsonUtils.getIns().toJson(a));
					}
				});
			}
		}
	}

	private void checkAssignFail(Set<AgentInfo> agentInfo) {
		long curTime = System.currentTimeMillis();
		for(AgentInfo ai : agentInfo) {
			if(ai.getAssignDeps().isEmpty()) {
				continue;
			}
			
			String path = ChoyConstants.ROOT_AGENT+"/"+ai.getId();
			if(curTime - ai.getAssignTime() > 120000) { //两分钟内都没有分配成功
				doInlocker(path,()->{
					ai.getAssignDeps().clear();
					String data = op.getData(path);
					AgentInfo a = JsonUtils.getIns().fromJson(data, AgentInfo.class);
					if(a != null) {
						a.getAssignDeps().clear();
						a.setAssignTime(0);
						op.setData(path, JsonUtils.getIns().toJson(a));
					}
				});
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
		public ProcessInfo pi;
		
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
			return "Assign [depId=" + depId + ", agentId=" + agentId + ", pi=" + pi + "]";
		}
		
		
	}
	
	private Assign getAssignByInfoId(String id) {
		for(Assign a : this.assigns) {
			if(a.pi == null) {
				continue;
			}
			if(id.equals(a.pi.getId())) {
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
