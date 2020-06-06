package cn.jmicro.mng.api.choreography;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.choreography.base.AgentInfo;

@SO
public class AgentInfoVo {

	private AgentInfo agentInfo;
	
	private String[] depIds;
	
	private String[] intIds;

	public AgentInfo getAgentInfo() {
		return agentInfo;
	}

	public void setAgentInfo(AgentInfo agentInfo) {
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
