package cn.jmicro.api.choreography;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.common.util.StringUtils;

@SO
@IDStrategy(1)
public class ProcessInfo {

	private String id;
	
	private String host;
	
	private String instanceName;
	
	private String agentHost;
	
	private String agentInstanceName;
	
	private String depId;
	
	private String agentId;
	
	private String agentProcessId;
	
	private String pid;
	
	private String cmd;
	
	private String workDataDir;
	
	private boolean active;
	
	private transient Process process;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDepId() {
		return depId;
	}

	public void setDepId(String depId) {
		this.depId = depId;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getDataDir() {
		return workDataDir;
	}

	public void setDataDir(String workDir) {
		this.workDataDir = workDir;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getAgentHost() {
		return agentHost;
	}

	public void setAgentHost(String agentHost) {
		this.agentHost = agentHost;
	}

	public String getAgentInstanceName() {
		return agentInstanceName;
	}

	public void setAgentInstanceName(String agentInstanceName) {
		this.agentInstanceName = agentInstanceName;
	}

	public String getAgentProcessId() {
		return agentProcessId;
	}

	public void setAgentProcessId(String agentProcessId) {
		this.agentProcessId = agentProcessId;
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ProcessInfo)) {
			return false;
		}
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public String toString() {
		return "ProcessInfo [id=" + id + ", host=" + host + ", instanceName=" + instanceName + ", agentHost="
				+ agentHost + ", agentInstanceName=" + agentInstanceName + ", depId=" + depId + ", agentId=" + agentId
				+ ", agentProcessId=" + agentProcessId + ", pid=" + pid + ", cmd=" + cmd + ", workDataDir="
				+ workDataDir + ", active=" + active + "]";
	}

	

}
