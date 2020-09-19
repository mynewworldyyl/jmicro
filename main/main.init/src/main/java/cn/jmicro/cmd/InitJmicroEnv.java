package cn.jmicro.cmd;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class InitJmicroEnv {

	@Inject
	private IDataOperator op;
	
	@Inject
	private Config cfg;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private AccountManager am;
	
	@Inject
	private CmdProcessor cp;
	
	public static void main(String[] args) {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		CmdProcessor cp = of.get(CmdProcessor.class);
		cp.cmdLoop(true);
	}
	
	public void ready() {
		
		String initDepTypeStr = this.cfg.getString("initDepTypes",null);
		if(StringUtils.isEmpty(initDepTypeStr)) {
			return;
		}
		
		String[] initDepTypes = initDepTypeStr.split(",");
		
		for(String t : initDepTypes) {
			Module m = Module.valueOf(t);
			if(Module.pubsub == m) {
				createPubsubDeployment();
			} else if(Module.repository == m) {
				createResposityDeployment();
			} else if(Module.controller== m) {
				createControllerDeployment();
			} else if(Module.gateway == m) {
				createGatewayDeployment();
			} else if(Module.breaker == m) {
				createBreakerDeployment();
			} else if(Module.monitor == m) {
				createMonitorDeployment();
			}else {
				System.out.println("Invalid module name: " + t);
			}
		}
		
		
	}
	
	private void createMonitorDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		
		String jar = cfg.getString(Module.monitor.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-monitor-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		
		respDep.setJarFile(jar);
		
		respDep.setArgs("-DenableMasterSlaveModel=true");
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}

	private void createBreakerDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		
		String jar = cfg.getString(Module.breaker.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-breaker-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		respDep.setJarFile(jar);
		
		respDep.setArgs("-DenableMasterSlaveModel=true");
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}

	private void createPubsubDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);
		
		String jar = cfg.getString(Module.pubsub.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-pubsub-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		respDep.setJarFile(jar);
		
		respDep.setArgs("-DenableMasterSlaveModel=true");
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}

	private void createGatewayDeployment() {
		Deployment respDep = new Deployment();
		String id = idServer.getStringId(Deployment.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setEnable(true);
		respDep.setInstanceNum(2);

		String jar = cfg.getString(Module.gateway.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-example.provider-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		respDep.setJarFile(jar);
		
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
		
		String jar = cfg.getString(Module.controller.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-choreography.controller-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		respDep.setJarFile(jar);
		
		
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
		
		String jar = cfg.getString(Module.repository.name()+".jar", null);
		if(jar == null) {
			jar = "jmicro-choreography.repository-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		}
		respDep.setJarFile(jar);
		
		op.createNodeOrSetData(ChoyConstants.DEP_DIR+"/" + respDep.getId(), 
				JsonUtils.getIns().toJson(respDep), IDataOperator.PERSISTENT);
	}
	
	public enum Module {
		pubsub,breaker,monitor,repository,controller,gateway
	}
	
}


