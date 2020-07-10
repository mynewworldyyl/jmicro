package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.choreography.AgentInfoVo;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IChoreographyService {

	 //Deployment
	 Deployment addDeployment(Deployment dep);
	 
	 List<Deployment> getDeploymentList();
	 
	 boolean deleteDeployment(int id);
	 
	 boolean updateDeployment(Deployment dep);
	 
	 //Agent
	 List<AgentInfoVo> getAgentList(boolean showAll);
	 
	 //Process instance
	 List<ProcessInfo> getProcessInstanceList(boolean all);
	 
	 boolean stopProcess(String insId);
	 
	 String changeAgentState(String agentId);
	 
}
