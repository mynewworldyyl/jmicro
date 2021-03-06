package cn.jmicro.api.choreography;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.security.ActInfo;

@SO
@IDStrategy(1)
public class ProcessInfo {

	private int id;
	
	private int clientId;
	
	private String actName;
	
	private String host;
	
	private String osName;
	
	private String instanceName;
	
	private String agentHost;
	
	private String agentInstanceName;
	
	private String depId;
	
	private String agentId;
	
	private String agentProcessId;
	
	private String pid;
	
	private String cmd;
	
	private String workDir;
	
	private String infoFilePath;
	
	private boolean active;
	
	private long opTime;
	
	private long timeOut;
	
	private long startTime;
	
	private boolean haEnable = false;
	
	private boolean master = false;
	
	private transient Process process;
	
	private transient ActInfo ai;
	
	private boolean monitorable = false;
	
	private byte logLevel = MC.LOG_INFO;
	
	protected Map<String,Set<CfgMetadata>> metadatas = new HashMap<>();

	public boolean isLogin() {
		return ai != null;
	}
	
	public Set<CfgMetadata> getMetadatas(String resName) {
		return metadatas.get(resName);
	}

	public void setMetadatas(String resName,Set<CfgMetadata> metadatas) {
		this.metadatas.put(resName, metadatas);
	}
	
	public void setMetadatas(Map<String,Set<CfgMetadata>> metadatas) {
		this.metadatas = metadatas;
	}
	
	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public ActInfo getAi() {
		return ai;
	}

	public void setAi(ActInfo ai) {
		this.ai = ai;
	}

	public Map<String,Set<CfgMetadata>> getMetadatas() {
		return metadatas;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isHaEnable() {
		return haEnable;
	}

	public void setHaEnable(boolean haEnable) {
		this.haEnable = haEnable;
	}

	public boolean isMaster() {
		return master;
	}

	public String getInfoFilePath() {
		return infoFilePath;
	}

	public void setInfoFilePath(String infoFilePath) {
		this.infoFilePath = infoFilePath;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
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

	public long getOpTime() {
		return opTime;
	}

	public void setOpTime(long opTime) {
		this.opTime = opTime;
	}

	public long getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public boolean isMonitorable() {
		return monitorable;
	}

	public void setMonitorable(boolean monitorable) {
		this.monitorable = monitorable;
	}

	public String getActName() {
		return actName;
	}

	public void setActName(String actName) {
		this.actName = actName;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ProcessInfo)) {
			return false;
		}
		return this.hashCode() == obj.hashCode();
	}

	public byte getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(byte logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public String toString() {
		return "ProcessInfo [id=" + id + ", host=" + host + ", instanceName=" + instanceName + ", agentHost="
				+ agentHost + ", agentInstanceName=" + agentInstanceName + ", depId=" + depId + ", agentId=" + agentId
				+ ", agentProcessId=" + agentProcessId + ", pid=" + pid + ", cmd=" + cmd + ", workDataDir="
				+ workDir + ", active=" + active + "]";
	}

	

}
