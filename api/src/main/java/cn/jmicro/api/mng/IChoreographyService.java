package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.choreography.AgentInfoVo;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IChoreographyService {

	 //Deployment
	 Resp<Deployment> addDeployment(Deployment dep);
	 
	 Resp<List<Deployment>> getDeploymentList();
	 
	 Resp<Boolean> deleteDeployment(int id);
	 
	 Resp<Boolean> updateDeployment(Deployment dep);
	 
	 //Agent
	 Resp<List<AgentInfoVo>> getAgentList(boolean showAll);
	 
	 Resp<Boolean> clearResourceCache(String resId);
	 
	 //Process instance
	 Resp<List<ProcessInfo>> getProcessInstanceList(boolean all);
	 
	 Resp<Boolean> stopProcess(String insId);
	 
	 Resp<String> changeAgentState(String agentId);
	 
	 Resp<Boolean> stopAllInstance(String agentId);
	 
}
