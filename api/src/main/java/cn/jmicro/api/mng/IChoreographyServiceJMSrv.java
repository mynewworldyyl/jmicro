package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.choreography.AgentInfo0JRso;
import cn.jmicro.api.choreography.DeploymentJRso;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IChoreographyServiceJMSrv {

	 //Deployment
	 RespJRso<DeploymentJRso> addDeployment(DeploymentJRso dep);
	 
	 RespJRso<List<DeploymentJRso>> getDeploymentList();
	 
	 RespJRso<Boolean> deleteDeployment(String id);
	 
	 RespJRso<DeploymentJRso> updateDeployment(DeploymentJRso dep);
	 
	 //Agent
	 RespJRso<List<AgentInfo0JRso>> getAgentList(boolean showAll);
	 
	 RespJRso<Boolean> clearResourceCache(String resId);
	 
	 //Process instance
	 RespJRso<List<ProcessInfoJRso>> getProcessInstanceList(boolean all);
	 
	 RespJRso<Boolean> stopProcess(Integer insId);
	 RespJRso<Boolean> updateProcess(ProcessInfoJRso pi);
	 
	 RespJRso<String> changeAgentState(String agentId);
	 
	 RespJRso<Boolean> stopAllInstance(String agentId);
	 
}
