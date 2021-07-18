package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.annotation.SO;

@SO
public class LogFileEntryJRso {

	private String agentId;
	
	private String instanceName;
	
	private int processId;
	
	private List<String> logFileList;
	
	private boolean active;

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public int getProcessId() {
		return processId;
	}

	public void setProcessId(int processId) {
		this.processId = processId;
	}

	public List<String> getLogFileList() {
		return logFileList;
	}

	public void setLogFileList(List<String> logFileList) {
		this.logFileList = logFileList;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	
}
