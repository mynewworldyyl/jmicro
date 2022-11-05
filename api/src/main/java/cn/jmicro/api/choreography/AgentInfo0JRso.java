package cn.jmicro.api.choreography;

import lombok.Serial;

@Serial
public class AgentInfo0JRso {
	  
	  
	private AgentInfoJRso agentInfo;
	
	private String[] depIds;
	
	private String[] intIds;

	public AgentInfoJRso getAgentInfo() {
		return agentInfo;
	}

	public void setAgentInfo(AgentInfoJRso agentInfo) {
		this.agentInfo = agentInfo;
	}

	public String[] getDepIds() {
		return depIds;
	}

	public void setDepIds(String[] depIds) {
		this.depIds = depIds;
	}

	public String[] getIntIds() {
		return intIds;
	}

	public void setIntIds(String[] intIds) {
		this.intIds = intIds;
	}
	
}
