package cn.jmicro.mng.api;

import java.util.List;
import java.util.Set;

import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.mng.api.choreography.AgentInfoVo;

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
