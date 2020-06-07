package cn.jmicro.pubsub;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.common.util.JsonUtils;

@Component
public class InitJmicroEnv {

	@Inject
	private IDataOperator op;
	
	@Inject
	private Config cfg;
	
	@Inject
	private ComponentIdServer idServer;
	
	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
	}
	
	public void ready() {
		/*String en = Config.getCommandParam("InitJmicroEnv");
		if(en == null || !Boolean.parseBoolean(en)) {
			return;
		}*/
		createResposityDeployment();
		createControllerDeployment();
		createGatewayDeployment();
	}
	
	private void createGatewayDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		respDep.setJarFile("jmicro-jmicro.example.provider-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		
		respDep.setArgs("-DenableMasterSlaveModel=true");
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}

	private void createControllerDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		respDep.setArgs("-Dclient=true -DenableMasterSlaveModel=true");
		respDep.setJarFile("jmicro-choreography.controller-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}

	private void createResposityDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		respDep.setArgs("-DResourceReponsitoryService.dataDir="+"D:\\opensource\\resDataDir -DenableMasterSlaveModel=true");
		respDep.setJarFile("jmicro-choreography.repository-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}
	
}
