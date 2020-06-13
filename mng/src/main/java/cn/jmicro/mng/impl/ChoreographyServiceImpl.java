package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.mng.IConfigManager;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.api.PackageResource;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.IChoreographyService;
import cn.jmicro.mng.api.ICommonManager;
import cn.jmicro.mng.api.choreography.AgentInfoVo;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0)
public class ChoreographyServiceImpl implements IChoreographyService {

	private final static Logger logger = LoggerFactory.getLogger(ChoreographyServiceImpl.class);
	
	private final static String TAG = ChoreographyServiceImpl.class.getSimpleName();
	
	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Reference(namespace="rrs",version="*")
	private IResourceResponsitory respo;
	
	@Inject
	private IConfigManager configManager;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private ICommonManager commonManager;
	
	private Set<PackageResource> packageResources = new HashSet<>();
	
	@Override
	public Deployment addDeployment(Deployment dep) {
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "";
			
			if(ai != null) {
				msg += "Account " + ai.getActName() + " has not permission to add deployment: " + dep.toString(); 
			} else {
				msg += "No login account to add deployment: " + dep.toString(); 
			}
			SF.eventLog(MC.MT_DEPLOYMENT_ADD,MC.LOG_INFO, TAG,msg);
			logger.warn(msg);
			return null;
		}
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			logger.error("Jar file cannot be null when do add deployment: " +dep.toString());
			return null;
		}
		if(!checkPackageResource(dep.getJarFile())) {
			logger.error("PackageResource [" + dep.getJarFile()+"] not found!");
			return null;
		}
		
		String id = idServer.getStringId(Deployment.class);
		dep.setId(id);
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/"+id, JsonUtils.getIns().toJson(dep), false);
		
		return dep;
	}

	@Override
	public List<Deployment> getDeploymentList() {
		Set<String> children = op.getChildren(ChoyConstants.DEP_DIR, false);
		if(children == null) {
			return null;
		}
		
		List<Deployment> result = new ArrayList<>();
		
		for(String c : children) {
			String data = op.getData(ChoyConstants.DEP_DIR+"/" + c);
			if(StringUtils.isNotEmpty(data)) {
				Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
				if(dep != null) {
					result.add(dep);
				}
			}
		}
		
		result.sort((o1,o2)->{
			int id1 = Integer.parseInt(o1.getId());
			int id2 = Integer.parseInt(o2.getId());
			return id1 > id2 ? 1 : id1 == id2 ? 0 :-1;
		});
		
		return result;
	}

	@Override
	public boolean deleteDeployment(int id) {
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "";
			
			if(ai != null) {
				msg += "Account " + ai.getActName() + " has not permission to delete deployment: " + id; 
			} else {
				msg += "No login account to update deployment: " + id; 
			}
			SF.eventLog(MC.MT_DEPLOYMENT_REMOVE,MC.LOG_WARN, TAG,msg);
			logger.warn(msg);
			return false;
		}
		
		op.deleteNode(ChoyConstants.DEP_DIR+"/" + id);
		return true;
	}

	@Override
	public boolean updateDeployment(Deployment dep) {
		
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "";
			
			if(ai != null) {
				msg += ai.getActName() + " has not permission to update deployment: " + dep.toString(); 
			} else {
				msg += "No login account to update deployment: " + dep.toString(); 
			}
			SF.eventLog(MC.MT_DEPLOYMENT_LOG,MC.LOG_ERROR, TAG,msg);
			logger.warn(msg);
			return false;
		}
		String data = op.getData(ChoyConstants.DEP_DIR+"/" + dep.getId());
		if(StringUtils.isEmpty(data)) {
			logger.error("Deployment not found when do update: " +dep.toString());
			return false;
		}
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			logger.error("Jar file cannot be null when do update: " +dep.toString());
			return false;
		}
		
		Deployment d = JsonUtils.getIns().fromJson(data, Deployment.class);
		if(!dep.getJarFile().equals(d.getJarFile())) {
			//更新了JarFile，判断更新的JAR是否存在
			if(!checkPackageResource(dep.getJarFile())) {
				logger.error("Jar file cannot not found: " +dep.getJarFile());
				return false;
			}
		}
		data = JsonUtils.getIns().toJson(dep);
		SF.eventLog(MC.MT_DEPLOYMENT_LOG,MC.LOG_INFO, TAG,data);
		op.setData(ChoyConstants.DEP_DIR+"/"+dep.getId(), data);
		return true;
	}
	
	@Override
	public List<ProcessInfo> getProcessInstanceList(boolean all) {
		 Set<ProcessInfo> set = this.insManager.getProcesses(all);
		 if(set == null || set.isEmpty()) {
			 return null;
		 }
		 List<ProcessInfo> result = new ArrayList<>();
		 result.addAll(set);
		result.sort((o1,o2)->{
			int id1 = Integer.parseInt(o1.getId());
			int id2 = Integer.parseInt(o2.getId());
			return id1 > id2 ? 1 : id1 == id2 ? 0 :-1;
		});
		
		return result;
		
	}
	
	@Override
	public boolean stopProcess(String insId) {
		if(commonManager.hasPermission(this.adminPermissionLevel)) {
			ProcessInfo pi = this.insManager.getProcessesByInsId(insId,true);
			if(pi != null) {
				String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
				pi.setActive(false);
				String data = JsonUtils.getIns().toJson(pi);
				op.setData(p, data);
				ActInfo ai = JMicroContext.get().getAccount();
				String msg = "Stop process by Account [" + ai.getActName() + "], Process: " + data;
				logger.warn(msg);
				SF.eventLog(MC.MT_DEPLOYMENT_LOG,MC.LOG_WARN, TAG,msg);
				return true;
			}
		} else {
			String msg = "";
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				msg = "No permission to stop process [" + ai.getActName() + "], Process ID: " + insId;
			}else {
				msg = "Nor login account to stop process ID: " + insId;
			}
			logger.warn(msg);
			SF.eventLog(MC.MT_DEPLOYMENT_LOG,MC.LOG_WARN, TAG,msg);
		}
		
		return false;
	}
	
	//Agent
	@Override
	public List<AgentInfoVo> getAgentList(boolean showAll) {
		ConfigNode[] agents = this.configManager.getChildren(ChoyConstants.ROOT_AGENT, true);
		if(agents == null || agents.length == 0) {
			return null;
		}
		
		List<AgentInfoVo> aivs = new ArrayList<>();
		
		for(ConfigNode cn : agents) {
			
			String acPath = ChoyConstants.ROOT_ACTIVE_AGENT + "/" + cn.getName();
			boolean isActive = op.exist(acPath);
			if(!showAll && !isActive) {
				continue;
			}
			
			AgentInfoVo av = new AgentInfoVo();
			aivs.add(av);
			
			AgentInfo ai = JsonUtils.getIns().fromJson(cn.getVal(), AgentInfo.class);
			av.setAgentInfo(ai);
			ai.setActive(isActive);
			
			 Set<ProcessInfo> pros = this.insManager.getProcessesByAgentId(ai.getId());
			 if(pros != null && pros.size() > 0) {
				 String[] depids = new String[pros.size()];
				 String[] intids = new String[pros.size()];
			     Iterator<ProcessInfo> ite = pros.iterator();
				 for(int i = 0; ite.hasNext(); i++) {
					 ProcessInfo pi = ite.next();
					 intids[i] = pi.getId();
					 depids[i] = pi.getDepId();
				 }
				 av.setIntIds(intids);
				 av.setDepIds(depids);
			 }
		}
		
		aivs.sort((o1,o2)->{
			int id1 = Integer.parseInt(o1.getAgentInfo().getId());
			int id2 = Integer.parseInt(o2.getAgentInfo().getId());
			return id1 > id2 ? 1 : id1 == id2 ? 0 :-1;
		});
		 
		return aivs;
	}
	
	@Override
	public String changeAgentState(String agentId) {
		if(commonManager.hasPermission(this.adminPermissionLevel)) {
			String apath = ChoyConstants.ROOT_AGENT + "/" + agentId;
			if(!op.exist(apath)) {
				return "Agent ID : "+agentId + " not found!";
			}
			
			AgentInfo ai = JsonUtils.getIns().fromJson(op.getData(apath), AgentInfo.class);
			if(ai == null) {
				return "Agent ID : "+agentId + " is NULL data!";
			}
			
			if(!ai.isPrivat() && StringUtils.isEmpty(ai.getInitDepIds())) {
				return "Private agent initDepIds cannot be NULL: "+agentId;
			}
			
			int size = this.insManager.getProcessSizeByAgentId(agentId);
            if(size > 0) {
            	return "Agent ID : " + agentId + " has process runngin so cannot change state!";
            }
            
            ai.setPrivat(!ai.isPrivat());
			
			String data = JsonUtils.getIns().toJson(ai);
			op.setData(apath, data);
		
			return "";
		
		}
		
		return "No permission";
	}
	
	private boolean checkPackageResource(String name) {
		Iterator<PackageResource> ite = this.packageResources.iterator();
		while (ite.hasNext()) {
			if (name.equals(ite.next().getName())) {
				return true;
			}
		}

		List<PackageResource> news = respo.getResourceList(true);
		this.packageResources.addAll(news);
		Iterator<PackageResource> it = this.packageResources.iterator();
		while (it.hasNext()) {
			if (name.equals(it.next().getName())) {
				return true;
			}
		}

		return false;
	}

	

}
