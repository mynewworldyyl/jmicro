package cn.jmicro.api.monitor;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;

public abstract class AbstractResource implements IResource {

	protected ResourceData data = new ResourceData();
	
	@Inject
	protected ProcessInfo pi;
	
	@Cfg(value="/enable")
	private boolean enable = false;
	
	public void ready() {
		data.setBelongInsId(pi.getId());
		data.setBelongInsName(pi.getInstanceName());
	}
	
	@Override
	public ResourceData getResource(Map<String,Object> params) {
		return data;
	}

	@Override
	public boolean isEnable() {
		return this.enable;
	}

	@Override
	public void setEnable(boolean en) {
		this.enable = en;
	}

	@Override
	public Set<CfgMetadata> metaData() {
		return pi.getMetadatas();
	}
	
}
