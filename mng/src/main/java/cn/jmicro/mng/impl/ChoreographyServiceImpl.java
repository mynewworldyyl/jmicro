package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.AgentInfoVo;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.mng.IChoreographyService;
import cn.jmicro.api.mng.ICommonManager;
import cn.jmicro.api.mng.IConfigManager;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.api.PackageResource;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false)
public class ChoreographyServiceImpl implements IChoreographyService {

	private final static Logger logger = LoggerFactory.getLogger(ChoreographyServiceImpl.class);
	
	private final static Class<?> TAG = ChoreographyServiceImpl.class;
	
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=8092)
	public Resp<Deployment> addDeployment(Deployment dep) {
		Resp<Deployment> resp = new Resp<>();
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			String msg = "Jar file cannot be null when do add deployment: " +dep.toString();
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		if(!checkPackageResource(dep.getJarFile())) {
			String msg = "PackageResource [" + dep.getJarFile()+"] not found!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		String id = idServer.getStringId(Deployment.class);
		dep.setId(id);
		
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai.getClientId() != Config.getAdminClientId() || 
				(ai.getClientId() == Config.getAdminClientId() && dep.getClientId() < 0 )) {
			dep.setClientId(ai.getClientId());
		}
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/"+id, JsonUtils.getIns().toJson(dep), false);
		
		resp.setData(dep);
		
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<Deployment>> getDeploymentList() {
		Resp<List<Deployment>> resp = new Resp<>();
		Set<String> children = op.getChildren(ChoyConstants.DEP_DIR, false);
		if(children == null) {
			resp.setCode(0);
			resp.setMsg("no data");
			return resp;
		}
		
		List<Deployment> result = new ArrayList<>();
		
		for(String c : children) {
			Deployment dep = this.getDeploymentById(c);
			if(dep != null && PermissionManager.checkAccountClientPermission(dep.getClientId())) {
				result.add(dep);
			}
		}
		
		result.sort((o1,o2)->{
			int id1 = Integer.parseInt(o1.getId());
			int id2 = Integer.parseInt(o2.getId());
			return id1 > id2 ? 1 : id1 == id2 ? 0 :-1;
		});
		
		resp.setData(result);
		
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Boolean> deleteDeployment(String id) {
		Resp<Boolean> resp = new Resp<>(0);
		
		Deployment dep = this.getDeploymentById(id);
		if(dep == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +id);
			return resp;
		}
		
		if(!PermissionManager.checkAccountClientPermission(dep.getClientId())) {
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "";
			
			if(ai != null) {
				msg += "Account " + ai.getActName() + " has not permission to delete deployment: " + id; 
			} else {
				msg += "No login account to update deployment: " + id; 
			}
			LG.log(MC.LOG_WARN, TAG,msg);
			logger.warn(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		op.deleteNode(ChoyConstants.DEP_DIR+"/" + id);
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=8092)
	public Resp<Boolean> updateDeployment(Deployment dep) {
		Resp<Boolean> resp = new Resp<>(0);
		
		Deployment d = this.getDeploymentById(dep.getId());
		if(d == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +dep.getId());
			return resp;
		}
		
		if(!PermissionManager.checkAccountClientPermission(d.getClientId())) {
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "";
			
			if(ai != null) {
				msg += ai.getActName() + " has not permission to update deployment: " + dep.toString(); 
			} else {
				msg += "No login account to update deployment: " + dep.toString(); 
			}
			LG.log(MC.LOG_ERROR, TAG,msg);
			logger.warn(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			String msg = "Jar file cannot be null when do update: " +dep.toString();
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		if(!dep.getJarFile().equals(d.getJarFile())) {
			//更新了JarFile，判断更新的JAR是否存在
			if(!checkPackageResource(dep.getJarFile())) {
				String msg = "Jar file cannot not found: " +dep.getJarFile();
				logger.error(msg);
				resp.setCode(1);
				resp.setMsg(msg);
				return resp;
			}
		}
		dep.setClientId(d.getClientId());
		
		String data = JsonUtils.getIns().toJson(dep);
		LG.log(MC.LOG_INFO, TAG,data);
		op.setData(ChoyConstants.DEP_DIR+"/"+dep.getId(), data);
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<ProcessInfo>> getProcessInstanceList(boolean all) {
		Resp<List<ProcessInfo>> resp = new Resp<>(0);
		Set<ProcessInfo> set = this.insManager.getProcesses(all);
		 if(set == null || set.isEmpty()) {
			 return resp;
		 }
		 List<ProcessInfo> result = new ArrayList<>();
		 for(ProcessInfo pi : set) {
			 if(PermissionManager.checkAccountClientPermission(pi.getClientId())) {
				 result.add(pi);
			 }
		 }
		 
		result.sort((o1,o2)->{
			return o1.getId() > o2.getId() ? 1 : o1.getId() == o2.getId() ? 0 :-1;
		});
		resp.setData(result);
		return resp;
		
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Boolean> stopProcess(Integer insId) {
		Resp<Boolean> resp = new Resp<>(0);
		ProcessInfo pi = this.insManager.getProcessesByInsId(insId, true);
		if(pi == null) {
			resp.setData(true);
			return resp;
		}
		
		if(PermissionManager.checkAccountClientPermission(pi.getClientId())) {
			String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
			pi.setActive(false);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "Stop process by Account [" + ai.getActName() + "], Process: " + data;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			return resp;
		} else {
			String msg = "";
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				msg = "No permission to stop process [" + ai.getActName() + "], Process ID: " + insId;
			}else {
				msg = "Nor login account to stop process ID: " + insId;
			}
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
	}
	
	//Agent
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<AgentInfoVo>> getAgentList(boolean showAll) {
		Resp<List<AgentInfoVo>> resp = new Resp<>(0);
		ConfigNode[] agents = this.configManager.getChildren(ChoyConstants.ROOT_AGENT, true);
		if(agents == null || agents.length == 0) {
			resp.setMsg("no data");
			return resp;
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
			if(PermissionManager.checkAccountClientPermission(ai.getClientId())) {
				continue;
			}
			
			av.setAgentInfo(ai);
			ai.setActive(isActive);
			
			 Set<ProcessInfo> pros = this.insManager.getProcessesByAgentId(ai.getId());
			 if(pros != null && pros.size() > 0) {
				 String[] depids = new String[pros.size()];
				 String[] intids = new String[pros.size()];
			     Iterator<ProcessInfo> ite = pros.iterator();
				 for(int i = 0; ite.hasNext(); i++) {
					 ProcessInfo pi = ite.next();
					 intids[i] = pi.getId()+"";
					 depids[i] = pi.getDepId();
				 }
				 av.setIntIds(intids);
				 av.setDepIds(depids);
			 }
		}
		
		aivs.sort((o1,o2)->{
			return o1.getAgentInfo().getId().compareTo(o2.getAgentInfo().getId());
		});
		resp.setData(aivs);
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Boolean> stopAllInstance(String agentId) {
		Resp<Boolean> resp = new Resp<>(0);
		
		String activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+ agentId;
		if(!op.exist(activePath)) {
			resp.setCode(1);
			resp.setMsg("Offline agent: " + agentId);
			resp.setData(false);
			return resp;
		}
		
		AgentInfo ai = getAgentById(agentId);
		if(PermissionManager.checkAccountClientPermission(ai.getClientId())) {
			op.setData(activePath, ChoyConstants.AGENT_CMD_STOP_ALL_INSTANCE);
			resp.setData(true);
		} else {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("no permission");
		}
		return resp;
	}
	
	public Resp<Boolean> clearResourceCache(String agentId) {
		Resp<Boolean> resp = new Resp<>(0);
		if(commonManager.hasPermission(this.adminPermissionLevel)) {
			String activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+ agentId;
			if(op.exist(activePath)) {
				op.setData(activePath, ChoyConstants.AGENT_CMD_CLEAR_LOCAL_RESOURCE);
				resp.setData(true);
			}else {
				resp.setCode(1);
				resp.setMsg("Offline agent: " + agentId);
				resp.setData(false);
			}
			return resp;
		}
		resp.setData(false);
		resp.setCode(1);
		resp.setMsg("no permission");
		return resp;
	}
	
	public AgentInfo getAgentById(String agentId) {
		String apath = ChoyConstants.ROOT_AGENT + "/" + agentId;
		String data = op.getData(apath);
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		return ai;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<String> changeAgentState(String agentId) {
		Resp<String> resp = new Resp<>(0);
		
		AgentInfo ai = this.getAgentById(agentId);
		
		if(ai == null) {
			resp.setCode(1);
			resp.setMsg("Agent ID : "+agentId + " is NULL data!");
			return resp;
		}
		
		if(ai != null && PermissionManager.checkAccountClientPermission(ai.getClientId())) {
			String apath = ChoyConstants.ROOT_AGENT + "/" + agentId;
			if(!op.exist(apath)) {
				resp.setCode(1);
				resp.setMsg("Agent ID : "+agentId + " not found!");
				return resp;
			}
			
			
			if(!ai.isPrivat() && StringUtils.isEmpty(ai.getInitDepIds())) {
				resp.setCode(1);
				resp.setMsg("Private agent initDepIds cannot be NULL: "+agentId);
				return resp;
			}
			
			int size = this.insManager.getProcessSizeByAgentId(agentId);
            if(size > 0) {
            	resp.setCode(1);
				resp.setMsg("Agent ID : " + agentId + " has process runngin so cannot change state!");
				return resp;
            }
            
            ai.setPrivat(!ai.isPrivat());
			String data = JsonUtils.getIns().toJson(ai);
			op.setData(apath, data);
			return resp;
		}
		resp.setData("No permission");
		return resp;		
	}
	
	private Deployment getDeploymentById(String depId) {
		String data = op.getData(ChoyConstants.DEP_DIR+"/" + depId);
		if(StringUtils.isEmpty(data)) {
			return null;
		}
		return JsonUtils.getIns().fromJson(data, Deployment.class);
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
