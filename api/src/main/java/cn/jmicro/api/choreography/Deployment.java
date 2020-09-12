package cn.jmicro.api.choreography;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;

@SO
@IDStrategy(1)
public class Deployment {

	private String id;
	
	private int clientId=1;
	
	private String jarFile;
	
	private int instanceNum;
	
	private String args;
	
	private boolean enable;
	
	private boolean forceRestart;
	
	private String assignStrategy="defautAssignStrategy";
	
	private String strategyArgs;

	public String getJarFile() {
		return jarFile;
	}

	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	public int getInstanceNum() {
		return instanceNum;
	}

	public void setInstanceNum(int instanceNum) {
		this.instanceNum = instanceNum;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public boolean isForceRestart() {
		return forceRestart;
	}

	public void setForceRestart(boolean forceRestart) {
		this.forceRestart = forceRestart;
	}

	public String getAssignStrategy() {
		return assignStrategy;
	}

	public void setAssignStrategy(String assignStrategy) {
		this.assignStrategy = assignStrategy;
	}

	public String getStrategyArgs() {
		return strategyArgs;
	}

	public void setStrategyArgs(String strategyArgs) {
		this.strategyArgs = strategyArgs;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	@Override
	public String toString() {
		return "Deployment [id=" + id + ", jarFile=" + jarFile + ", instanceNum=" + instanceNum + ", args=" + args
				+ ", enable=" + enable + ", forceRestart=" + forceRestart + ", assignStrategy=" + assignStrategy
				+ ", strategyArgs=" + strategyArgs + "]";
	}
	
}
