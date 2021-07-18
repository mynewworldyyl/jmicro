package cn.jmicro.api.choreography;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;

@SO
@IDStrategy(1)
public class DeploymentJRso {
	
	public static final int STATUS_DRAFT = 1;

	public static final int STATUS_ENABLE = 2;
	
	public static final int STATUS_CHECK = 3;
	
	//public static final int STATUS_INIT = 1;
	
	private String id;
	
	private int resId;
	
	private int clientId = -1000;
	
	private String jarFile;
	
	private int instanceNum;
	
	private String args;
	
	private int status = STATUS_DRAFT;
	
	private String desc;
	
	private boolean forceRestart;
	
	private String assignStrategy="defautAssignStrategy";
	
	private String strategyArgs;
	
	private String jvmArgs;
	
	private long createdTime;
	
	private long updatedTime;

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

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
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
		return "Deployment [id=" + id + ", clientId=" + clientId + ", jarFile=" + jarFile + ", instanceNum="
				+ instanceNum + ", args=" + args + ", status=" + status + ", desc=" + desc + ", forceRestart="
				+ forceRestart + ", assignStrategy=" + assignStrategy + ", strategyArgs=" + strategyArgs + "]";
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getJvmArgs() {
		return jvmArgs;
	}

	public void setJvmArgs(String jvmArgs) {
		this.jvmArgs = jvmArgs;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(long updatedTime) {
		this.updatedTime = updatedTime;
	}
	
}
