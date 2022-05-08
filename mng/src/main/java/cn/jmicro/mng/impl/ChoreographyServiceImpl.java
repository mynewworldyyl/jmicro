package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.AgentInfoJRso;
import cn.jmicro.api.choreography.AgentInfo0JRso;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.DeploymentJRso;
import cn.jmicro.api.choreography.IAgentProcessServiceJMSrv;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.mng.ConfigNodeJRso;
import cn.jmicro.api.mng.IChoreographyServiceJMSrv;
import cn.jmicro.api.mng.ICommonManagerJMSrv;
import cn.jmicro.api.mng.IConfigManagerJMSrv;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.Namespace;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false,logLevel=MC.LOG_NO,namespace=Namespace.NS)
public class ChoreographyServiceImpl implements IChoreographyServiceJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(ChoreographyServiceImpl.class);
	
	private final static Class<?> TAG = ChoreographyServiceImpl.class;
	
	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private IConfigManagerJMSrv configManager;
	
	@Inject
	private ProcessInstanceManager insManager;
	
	@Inject
	private ICommonManagerJMSrv commonManager;
	
	//private Set<PackageResource> packageResources = new HashSet<>();
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=8092,logLevel=MC.LOG_INFO)
	public RespJRso<DeploymentJRso> addDeployment(DeploymentJRso dep) {
		RespJRso<DeploymentJRso> resp = new RespJRso<>();
		
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
		
		final ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(ai.isGuest() && !Utils.isEmpty(dep.getArgs())) {
			String msg = "Guest account deployment cannot contain any args!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			List<DeploymentJRso> l = getDeploymentList().getData();
			long cnt = l.stream().filter(e -> {return e.getClientId() == ai.getClientId();}).count();
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
			
			if(dep.getStatus() == DeploymentJRso.STATUS_ENABLE) {
				long ecnt = l.stream().filter(e -> {return e.getClientId() == ai.getClientId() && e.getStatus() == DeploymentJRso.STATUS_ENABLE;}).count();
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
		
		
		if(dep.getStatus() == DeploymentJRso.STATUS_ENABLE 
				&& !PermissionManager.isCurAdmin(Config.getClientId())) {
			 //非Admin添加的部署需要严格验证参数
			 checkNonAdminProgramArgs(dep);
		}
		
		dep.setClientId(ai.getClientId());
		
		String id = idServer.getStringId(DeploymentJRso.class);
		dep.setId(id);
		
		if(dep.getStatus() == DeploymentJRso.STATUS_ENABLE) {
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

	private String checkAndSetJVMArgs(DeploymentJRso dep,DeploymentJRso oldDep) {
		
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
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(ai.getClientId() != Config.getAdminClientId() && jvmArgs.length > 0) {
			if(oldDep == null || !dep.getJvmArgs().equals(oldDep.getJvmArgs())) {
				dep.setStatus(DeploymentJRso.STATUS_CHECK);
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

	private void setEnableDepArgs(DeploymentJRso dep) {
		dep.setDesc("");
		Map<String, String> params = Utils.parseProgramArgs(dep.getArgs());
		if(!params.containsKey(Constants.CLIENT_ID)) {
			dep.setArgs(dep.getArgs() + " -D"+Constants.CLIENT_ID + "="+ dep.getClientId());
		}
		
		/*if(!params.containsKey(Constants.ADMIN_CLIENT_ID)) {
			dep.setArgs(dep.getArgs() + " -D"+Constants.ADMIN_CLIENT_ID + "=" + Config.getAdminClientId());
		}*/
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

	private void checkNonAdminProgramArgs(DeploymentJRso dep) {
		
		Map<String, String> params = null;
		
		if(Utils.isEmpty(dep.getArgs())) {
			 params = new HashMap<>();
		}else {
			 params = Utils.parseProgramArgs(dep.getArgs());
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		for(Map.Entry<String, String> e : params.entrySet()) {
			String key = e.getKey();
			
			/*if(key.equals(Constants.ADMIN_CLIENT_ID) ) {
				int adminClient = Integer.parseInt(e.getValue());
				if(adminClient != Config.getAdminClientId()) {
					dep.setStatus(DeploymentJRso.STATUS_CHECK);
					dep.setDesc("Need system admin check with ["+Constants.ADMIN_CLIENT_ID+"] args, you can wait for approving or delete the args to enable this deployment!");
				}
			}*/
			
			if(key.equals(Constants.CLIENT_ID)) {
				int clientId = Integer.parseInt(e.getValue());
				if(clientId != ai.getClientId()) {
					dep.setStatus(DeploymentJRso.STATUS_CHECK);
					dep.setDesc("Need system admin check with ["+Constants.CLIENT_ID+"] args, you can wait for approving or delete the args to enable this deployment!");
				}
			}
		}
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<List<DeploymentJRso>> getDeploymentList() {
		RespJRso<List<DeploymentJRso>> resp = new RespJRso<>();
		Set<String> children = op.getChildren(ChoyConstants.DEP_DIR, false);
		if(children == null) {
			resp.setCode(0);
			resp.setMsg("no data");
			return resp;
		}
		
		List<DeploymentJRso> result = new ArrayList<>();
		
		for(String c : children) {
			DeploymentJRso dep = this.getDeploymentById(c);
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
	public RespJRso<Boolean> deleteDeployment(String id) {
		RespJRso<Boolean> resp = new RespJRso<>(0);
		
		DeploymentJRso dep = this.getDeploymentById(id);
		if(dep == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +id);
			return resp;
		}
		
		if(!PermissionManager.isOwner(dep.getClientId())) {
			ActInfoJRso ai = JMicroContext.get().getAccount();
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
	public RespJRso<DeploymentJRso> updateDeployment(DeploymentJRso dep) {
		RespJRso<DeploymentJRso> resp = new RespJRso<>(0);
		
		DeploymentJRso d = this.getDeploymentById(dep.getId());
		if(d == null) {
			resp.setCode(1);
			resp.setMsg("Deployment not found " +dep.getId());
			return resp;
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(ai.isGuest() && (!Utils.isEmpty(dep.getArgs()) || !Utils.isEmpty(dep.getJvmArgs()))) {
			String msg = "Guest account deployment cannot contain any args!";
			logger.error(msg);
			resp.setCode(1);
			resp.setMsg(msg);
			return resp;
		}
		
		if(ai.isGuest() && dep.getStatus() == DeploymentJRso.STATUS_CHECK) {
			dep.setStatus(DeploymentJRso.STATUS_DRAFT);
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
		
		if(!PermissionManager.isCurAdmin(Config.getClientId()) && dep.getStatus() == DeploymentJRso.STATUS_ENABLE) {
			List<DeploymentJRso> l = this.getDeploymentList().getData();
			long ecnt = l.stream().filter(e -> {return e.getClientId() == dep.getClientId() && e.getStatus() == DeploymentJRso.STATUS_ENABLE;}).count();
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
		
		//Map<String, String> params = Utils.parseProgramArgs(dep.getArgs());
		if(dep.getStatus() == DeploymentJRso.STATUS_ENABLE 
				&& !PermissionManager.isCurAdmin(Config.getClientId())) {
			//非Admin添加的部署需要严格验证参数
			checkNonAdminProgramArgs(dep);
		}
		
		if(dep.getStatus() == DeploymentJRso.STATUS_ENABLE) {
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
	public RespJRso<List<ProcessInfoJRso>> getProcessInstanceList(boolean all) {
		RespJRso<List<ProcessInfoJRso>> resp = new RespJRso<>(0);
		List<ProcessInfoJRso> result = new ArrayList<>();
		this.insManager.forEach((pi)->{
			if (PermissionManager.isOwner(pi.getClientId())) {
				result.add(pi);
			}
		});
		result.sort((o1, o2) -> {
			return o1.getId() > o2.getId() ? 1 : o1.getId() == o2.getId() ? 0 : -1;
		});
		resp.setData(result);
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=8194)
	public RespJRso<Boolean> updateProcess(ProcessInfoJRso updatePi) {
		RespJRso<Boolean> resp = new RespJRso<>(0);
		ProcessInfoJRso pi = this.insManager.getInstanceById(updatePi.getId());
		if(pi == null) {
			resp.setData(true);
			return resp;
		}
		
		if(PermissionManager.isOwner(pi.getClientId())) {
			String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
			pi.setLogLevel(updatePi.getLogLevel());
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			ActInfoJRso ai = JMicroContext.get().getAccount();
			String msg = "Update process by Account [" + ai.getActName() + "], Process: " + data;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			return resp;
		} else {
			String msg = "";
			ActInfoJRso ai = JMicroContext.get().getAccount();
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
	public RespJRso<Boolean> stopProcess(Integer insId) {
		RespJRso<Boolean> resp = new RespJRso<>(0);
		ProcessInfoJRso pi = this.insManager.getInstanceById(insId);
		if(pi == null) {
			resp.setData(true);
			return resp;
		}
		
		if(PermissionManager.isOwner(pi.getClientId())) {
			String p = ChoyConstants.INS_ROOT + "/" + pi.getId();
			pi.setActive(false);
			String data = JsonUtils.getIns().toJson(pi);
			op.setData(p, data);
			ActInfoJRso ai = JMicroContext.get().getAccount();
			String msg = "Stop process by Account [" + ai.getActName() + "], Process: " + data;
			logger.warn(msg);
			LG.log(MC.LOG_WARN, TAG,msg);
			return resp;
		} else {
			String msg = "";
			ActInfoJRso ai = JMicroContext.get().getAccount();
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
	public RespJRso<List<AgentInfo0JRso>> getAgentList(boolean showAll) {
		RespJRso<List<AgentInfo0JRso>> resp = new RespJRso<>(0);
		RespJRso<ConfigNodeJRso[]> agents = this.configManager.getChildren(ChoyConstants.ROOT_AGENT, true);
		if(agents == null || agents.getCode() != RespJRso.CODE_SUCCESS || agents.getData() == null) {
			resp.setMsg("no data");
			return resp;
		}
		
		List<AgentInfo0JRso> aivs = new ArrayList<>();
		
		for(ConfigNodeJRso cn : agents.getData()) {
			
			String acPath = ChoyConstants.ROOT_ACTIVE_AGENT + "/" + cn.getName();
			boolean isActive = op.exist(acPath);
			if(!showAll && !isActive) {
				continue;
			}
			
			AgentInfo0JRso av = new AgentInfo0JRso();
			aivs.add(av);
			
			AgentInfoJRso ai = JsonUtils.getIns().fromJson(cn.getVal(), AgentInfoJRso.class);
			if(!PermissionManager.checkAccountClientPermission(ai.getClientId())) {
				continue;
			}
			
			av.setAgentInfo(ai);
			ai.setActive(isActive);
			
			 Set<ProcessInfoJRso> pros = new HashSet<>();
			 this.insManager.forEach((pi)->{
				 if(pi.getAgentId() != null && pi.getAgentId().equals(ai.getId())) {
					 pros.add(pi);
				 }
			 });
			 
			 if(pros != null && pros.size() > 0) {
				 String[] depids = new String[pros.size()];
				 String[] intids = new String[pros.size()];
			     Iterator<ProcessInfoJRso> ite = pros.iterator();
				 for(int i = 0; ite.hasNext(); i++) {
					 ProcessInfoJRso pi = ite.next();
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
	public RespJRso<Boolean> stopAllInstance(String agentId) {
		RespJRso<Boolean> resp = new RespJRso<>(0);
		
		String activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+ agentId;
		if(!op.exist(activePath)) {
			resp.setCode(1);
			resp.setMsg("Offline agent: " + agentId);
			resp.setData(false);
			return resp;
		}
		
		AgentInfoJRso ai = getAgentById(agentId);
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
	
	public RespJRso<Boolean> clearResourceCache(String agentId) {
		RespJRso<Boolean> resp = new RespJRso<>(0);
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
	
	public AgentInfoJRso getAgentById(String agentId) {
		String apath = ChoyConstants.ROOT_AGENT + "/" + agentId;
		String data = op.getData(apath);
		AgentInfoJRso ai = JsonUtils.getIns().fromJson(data, AgentInfoJRso.class);
		return ai;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<String> changeAgentState(String agentId) {
		RespJRso<String> resp = new RespJRso<>(0);
		
		AgentInfoJRso ai = this.getAgentById(agentId);
		
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
			
			AtomicInteger size = new AtomicInteger(0);
			this.insManager.forEach((pi)->{
				 if(pi.getAgentId() != null && pi.getAgentId().equals(ai.getId())) {
					 size.incrementAndGet();
				 }
			 });
			
			if(size.get() > 0) {
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
	
	private DeploymentJRso getDeploymentById(String depId) {
		String data = op.getData(ChoyConstants.DEP_DIR+"/" + depId);
		if(StringUtils.isEmpty(data)) {
			return null;
		}
		return JsonUtils.getIns().fromJson(data, DeploymentJRso.class);
	}
	
}
