package cn.jmicro.choreography.api;

import java.util.List;

public interface IDeploymentService {

	 Deployment addDeployment(Deployment dep);
	 
	 List<Deployment> getDeploymentList();
	 
	 boolean deleteDeployment(int id);
	 
	 boolean updateDeployment(Deployment dep);
}
