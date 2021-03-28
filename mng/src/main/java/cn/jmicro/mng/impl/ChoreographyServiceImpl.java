package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
@Service( version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false)
public class ChoreographyServiceImpl implements IChoreographyService {

	private final static Logger logger = LoggerFactory.getLogger(ChoreographyServiceImpl.class);
	
	private final static Class<?> TAG = ChoreographyServiceImpl.class;
	
	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Reference(namespace="repository",version="*")
	private IResourceResponsitory respo;
	
	@Inject
	private IConfigManager configManager;
	
	@Inject
	private InstanceManager insManager;
	
	@Inject
	private ICommonManager commonManager;
	
	//private Set<PackageResource> packageResources = new HashSet<>();
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=8092,logLevel=MC.LOG_INFO)
	public Resp<Deployment> addDeployment(Deployment dep) {
		Resp<Deployment> resp = new Resp<>();
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			String msg = "Jar file cannot be null when do add deployment: " +dep.toString();
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		/*if(!checkPackageResource(dep.getJarFile())) {
			String msg = "PackageResource [" + dep.getJarFile()+"] not found!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}*/
		
		final ActInfo ai = JMicroContext.get().getAccount();
		
		if(ai.isGuest() && !Utils.isEmpty(dep.getArgs())) {
			String msg = "Guest account deployment cannot contain any args!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		if(!PermissionManager.isCurAdmin()) {
			List<Deployment> l = getDeploymentList().getData();
			long cnt = l.stream().filter(e -> {return e.getClientId() == ai.getId();}).count();
			if(ai.isGuest() && cnt >= 5) {
				String msg = "Guest account max deployment count is 5 pls regist normal account if you want more";
				resp.setCode(1);
				resp.setMsg(msg);
				return resp;
			}else if(cnt >= 10) {
				String msg = "Your acount max deployment count is 10, pls contact admin if you want more";
				resp.setCode(1);
				resp.setMsg(msg);
				return resp;
			}
			
			if(dep.getStatus() == Deployment.STATUS_ENABLE) {
				long ecnt = l.stream().filter(e -> {return e.getClientId() == ai.getId() && e.getStatus() == Deployment.STATUS_ENABLE;}).count();
				if(ai.isGuest() && ecnt >= 1) {
					String msg = "Guest account max enable deployment count is 1 pls regist normal account if you want more";
					resp.setCode(1);
					resp.setMsg(msg);
					return resp;
				}else if(ecnt >= 3) {
					String msg = "Your acount max enable deployment count is 3, pls contact admin if you want more";
					resp.setCode(1);
					resp.setMsg(msg);
					return resp;
				}
			}
		}
		
		String msg = checkProgramArgs(dep.getArgs());
		if(msg != null) {
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		msg = checkAndSetJVMArgs(dep,null);
		if(msg != null) {
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		
		if(dep.getStatus() == Deployment.STATUS_ENABLE && !PermissionManager.isCurAdmin()) {
			 //非Admin添加的部署需要严格验证参数
			 checkNonAdminProgramArgs(dep);
		}
		
		dep.setClientId(ai.getId());
		
		String id = idServer.getStringId(Deployment.class);
		dep.setId(id);
		
		if(dep.getStatus() == Deployment.STATUS_ENABLE) {
			setEnableDepArgs(dep);
		}
		
		dep.setCreatedTime(TimeUtils.getCurTime());
		dep.setUpdatedTime(TimeUtils.getCurTime());
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/"+id, JsonUtils.getIns().toJson(dep), false);
		
		resp.setData(dep);

		String data = JsonUtils.getIns().toJson(dep);
		LG.log(MC.LOG_INFO, TAG,"Add: " +data);
		
		return resp;
	}

	private String checkAndSetJVMArgs(Deployment dep,Deployment oldDep) {
		
		String[] jvmArgs = null;
		
		if(Utils.isEmpty(dep.getJvmArgs())) {
			jvmArgs = new String[0];
		}else {
			jvmArgs = dep.getJvmArgs().split("\\s+");
			for(String a : jvmArgs) {
				if(!a.startsWith("-X")) {
					return "Invalid JVM arg: " + a;
				}
			}
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		if(!ai.isAdmin() && jvmArgs.length > 0) {
			if(oldDep == null || !dep.getJvmArgs().equals(oldDep.getJvmArgs())) {
				dep.setStatus(Deployment.STATUS_CHECK);
				dep.setDesc("Need system admin check with jvm args, you can wait for approving or delete jvm args to enable this deployment!");
				return null;
			}
		}
		
		if(jvmArgs != null && jvmArgs.length > 0) {
			int xmsArg = getIntSizeJvmArg(jvmArgs,"-Xms");
			if(xmsArg==0) {
				dep.setJvmArgs(dep.getJvmArgs() + " -Xms16M");
			}
			
			int XmxArg = getIntSizeJvmArg(jvmArgs,"-Xmx");
			if(XmxArg==0) {
				dep.setJvmArgs(dep.getJvmArgs() + " -Xmx32M");
			}
		} else {
			dep.setJvmArgs(dep.getJvmArgs() + " -Xms16M");
			dep.setJvmArgs(dep.getJvmArgs() + " -Xmx32M");
		}
	
		return null;
	}

	private String checkProgramArgs(String argStr) {
		
		if(Utils.isEmpty(argStr)) {
			return null;
		}
		String[] args = argStr.split("\\s+");
		for(String arg : args){
			if(Utils.isEmpty(arg)) {
				continue;
			}
			if(!arg.startsWith("-D")){
				return "Invalid program: " + arg;
			}
		}
		return null;
	}

	private void setEnableDepArgs(Deployment dep) {
		dep.setDesc("");
		Map<String, String> params = IAssignStrategy.parseProgramArgs(dep.getArgs());
		if(!params.containsKey(Constants.CLIENT_ID)) {
			dep.setArgs(dep.getArgs() + " -D"+Constants.CLIENT_ID + "="+ dep.getClientId());
		}
		
		if(!params.containsKey(Constants.ADMIN_CLIENT_ID)) {
			dep.setArgs(dep.getArgs() + " -D"+Constants.ADMIN_CLIENT_ID + "=" + Config.getAdminClientId());
		}
	}

	private int getIntSizeJvmArg(String[] jvmArgs, String keyPrefix) {
		String val = getJvmArg(jvmArgs,keyPrefix);
		if(Utils.isEmpty(val)) {
			return 0;
		}
		
		int size = 0;
		if(val.endsWith("b") || val.endsWith("B")) {
			size = Integer.parseInt(val.substring(0,val.length()-1));
		}else if(val.endsWith("K") || val.endsWith("k")) {
			size = Integer.parseInt(val.substring(0,val.length()-1))*1024;
		}else if(val.endsWith("M") || val.endsWith("m")) {
			size = Integer.parseInt(val.substring(0,val.length()-1))*1024*1024;
		}
		return size;
	}
	
	private String getJvmArg(String[] jvmArgs, String keyPrefix) {
		for(String e: jvmArgs) {
			if(e.startsWith(keyPrefix) && e.length() > keyPrefix.length()) {
				int idx = e.indexOf(":");
				if(idx > 0) {
					return e.substring(idx+1);
				}else {
					return e.substring(keyPrefix.length());
				}
			}
		}
		return null;
	}

	private void checkNonAdminProgramArgs(Deployment dep) {
		
		Map<String, String> params = null;
		
		if(Utils.isEmpty(dep.getArgs())) {
			 params = new HashMap<>();
		}else {
			 params = IAssignStrategy.parseProgramArgs(dep.getArgs());
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		for(Map.Entry<String, String> e : params.entrySet()) {
			String key = e.getKey();
			
			if(key.equals(Constants.ADMIN_CLIENT_ID) ) {
				int adminClient = Integer.parseInt(e.getValue());
				if(adminClient != Config.getAdminClientId()) {
					dep.setStatus(Deployment.STATUS_CHECK);
					dep.setDesc("Need system admin check with ["+Constants.ADMIN_CLIENT_ID+"] args, you can wait for approving or delete the args to enable this deployment!");
				}
			}
			
			if(key.equals(Constants.CLIENT_ID)) {
				int clientId = Integer.parseInt(e.getValue());
				if(clientId != ai.getId()) {
					dep.setStatus(Deployment.STATUS_CHECK);
					dep.setDesc("Need system admin check with ["+Constants.CLIENT_ID+"] args, you can wait for approving or delete the args to enable this deployment!");
				}
			}
		}
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
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
			if(dep != null && PermissionManager.isOwner(dep.getClientId())) {
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
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Boolean> deleteDeployment(String id) {
		Resp<Boolean> resp = new Resp<>(0);
		
		Deployment dep = this.getDeploymentById(id);
		if(dep == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +id);
			return resp;
		}
		
		if(!PermissionManager.isOwner(dep.getClientId())) {
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
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=8092,logLevel=MC.LOG_INFO)
	public Resp<Deployment> updateDeployment(Deployment dep) {
		Resp<Deployment> resp = new Resp<>(0);
		
		Deployment d = this.getDeploymentById(dep.getId());
		if(d == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +dep.getId());
			return resp;
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		if(ai.isGuest() && (!Utils.isEmpty(dep.getArgs()) || !Utils.isEmpty(dep.getJvmArgs()))) {
			String msg = "Guest account deployment cannot contain any args!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		if(ai.isGuest() && dep.getStatus() == Deployment.STATUS_CHECK) {
			dep.setStatus(Deployment.STATUS_DRAFT);
		}
		
		if(!PermissionManager.isOwner(d.getClientId())) {
			String msg = "";
			msg += ai.getActName() + " has not permission to update deployment: " + dep.toString(); 
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
		
		if(!PermissionManager.isCurAdmin() && dep.getStatus() == Deployment.STATUS_ENABLE) {
			List<Deployment> l = this.getDeploymentList().getData();
			long ecnt = l.stream().filter(e -> {return e.getClientId() == dep.getClientId() && e.getStatus() == Deployment.STATUS_ENABLE;}).count();
			if(ai.isGuest() && ecnt >= 1) {
				String msg =  "Guest ID ["+dep.getClientId()+"] enable deployment count is 1 pls regist normal account if you want more";
				resp.setCode(1);
				resp.setMsg(msg);
				return resp;
			}else if(ecnt >= 3) {
				String msg = "Client ID ["+dep.getClientId()+"] max enable deployment count is 3, pls contact admin if you want more";
				resp.setCode(1);
				resp.setMsg(msg);
				return resp;
			}
		}
		
		String msg = checkProgramArgs(dep.getArgs());
		if(msg != null) {
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		msg = checkAndSetJVMArgs(dep,d);
		if(msg != null) {
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		Map<String, String> params = IAssignStrategy.parseProgramArgs(dep.getArgs());
		if(dep.getStatus() == Deployment.STATUS_ENABLE && !PermissionManager.isCurAdmin()) {
			//非Admin添加的部署需要严格验证参数
			checkNonAdminProgramArgs(dep);
		}
		
		if(dep.getStatus() == Deployment.STATUS_ENABLE) {
			setEnableDepArgs(dep);
		}
		
		dep.setClientId(d.getClientId());
		dep.setUpdatedTime(TimeUtils.getCurTime());
		
		String data = JsonUtils.getIns().toJson(dep);
		LG.log(MC.LOG_INFO, TAG,"Update: "+data);
		op.setData(ChoyConstants.DEP_DIR+"/"+dep.getId(), data);
		resp.setData(dep);
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<ProcessInfo>> getProcessInstanceList(boolean all) {
		Resp<List<ProcessInfo>> resp = new Resp<>(0);
		Set<ProcessInfo> set = this.insManager.getProcesses(all);
		if (set == null || set.isEmpty()) {
			return resp;
		}

		List<ProcessInfo> result = new ArrayList<>();
		for (ProcessInfo pi : set) {
			if (PermissionManager.isOwner(pi.getClientId())) {
				result.add(pi);
			}
		}

		result.sort((o1, o2) -> {
			return o1.getId() > o2.getId() ? 1 : o1.getId() == o2.getId() ? 0 : -1;
		});
		resp.setData(result);
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=8194)
	public Resp<Boolean> updateProcess(ProcessInfo updatePi) {
		Resp<Boolean> resp = new Resp<>(0);
		ProcessInfo pi = this.insManager.getProcessesByInsId(updatePi.getId(), true);
		if(pi == null) {
			resp.setData(true);
			return resp;
		}
		
		if(PermissionManager.isOwner(pi.getClientId())) {
			String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
			pi.setLogLevel(updatePi.getLogLevel());
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			ActInfo ai = JMicroContext.get().getAccount();
			String msg = "Update process by Account [" + ai.getActName() + "], Process: " + data;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			return resp;
		} else {
			String msg = "";
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				msg = "No permission to update process [" + ai.getActName() + "], Process ID: " + updatePi.getId();
			}else {
				msg = "Not login account to update process ID: " + updatePi.getId();
			}
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
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
		
		if(PermissionManager.isOwner(pi.getClientId())) {
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
		Resp<ConfigNode[]> agents = this.configManager.getChildren(ChoyConstants.ROOT_AGENT, true);
		if(agents == null || agents.getCode() != Resp.CODE_SUCCESS || agents.getData() == null) {
			resp.setMsg("no data");
			return resp;
		}
		
		List<AgentInfoVo> aivs = new ArrayList<>();
		
		for(ConfigNode cn : agents.getData()) {
			
			String acPath = ChoyConstants.ROOT_ACTIVE_AGENT + "/" + cn.getName();
			boolean isActive = op.exist(acPath);
			if(!showAll && !isActive) {
				continue;
			}
			
			AgentInfoVo av = new AgentInfoVo();
			aivs.add(av);
			
			AgentInfo ai = JsonUtils.getIns().fromJson(cn.getVal(), AgentInfo.class);
			if(!PermissionManager.checkAccountClientPermission(ai.getClientId())) {
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
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
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
	
}
