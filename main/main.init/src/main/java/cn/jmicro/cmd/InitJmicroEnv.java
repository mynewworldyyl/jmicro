package cn.jmicro.cmd;

import java.lang.reflect.InvocationTargetException;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.DeploymentJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
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
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		Object of = JMicro.getObjectFactoryAndStart(args);
		try {
			CmdProcessor cp = (CmdProcessor)of.getClass().getMethod("get", Class.class).invoke(of, CmdProcessor.class);
			cp.cmdLoop(true);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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
		DeploymentJRso respDep = new DeploymentJRso();
		String id = idServer.getStringId(DeploymentJRso.class);
		respDep.setId(id);
		respDep.setAssignStrategy("defautAssignStrategy");
		respDep.setStrategyArgs("-DsortPriority=maxCPURate,minFreeMemory,coreNum -DagentId=0,1");
		respDep.setStatus(DeploymentJRso.STATUS_ENABLE);
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


