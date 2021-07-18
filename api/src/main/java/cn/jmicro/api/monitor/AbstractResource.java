package cn.jmicro.api.monitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.StringUtils;

public abstract class AbstractResource implements IResource {

	private ResourceDataJRso data = new ResourceDataJRso();
	
	@Inject
	protected ProcessInfoJRso pi;
	
	//@Cfg(value="/enable")
	private boolean enable = true;
	
	public void ready0() {
		data.setBelongInsId(pi.getId());
		data.setBelongInsName(pi.getInstanceName());
		data.setResName(this.getResourceName());
		data.setOsName(System.getProperty("os.name"));
		data.setSocketHost(Config.getExportSocketHost());
		data.setHttpHost(Config.getExportHttpHost());
		data.setClientId(pi.getClientId());
	}
	
	@Override
	public ResourceDataJRso getResource(Map<String,Object> params,String expStr) {
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
	public Map<String,Set<CfgMetadataJRso>> metaData() {
		return pi.getMetadatas();
	}

	@Override
	public String getResourceName() {
		return data.getResName();
	}
	
	protected boolean compuleByExp(String expStr,Map<String,Object> cxt) {
		Exp exp = this.parseExp(expStr);
		if(exp != null) {
			return ExpUtils.compute(exp, cxt, Boolean.class);
		}
		return true;
	}
	
	protected Exp parseExp(String expStr) {
		Exp exp = null;
		if(StringUtils.isNotEmpty(expStr)) {
			//客户端必须确保表达式是正确的
			exp = new Exp();
			List<String> subfix = ExpUtils.toSuffix(expStr);
			exp.setOriEx(expStr);
			exp.setSuffix(subfix);
		}
		return exp;
	}

	public ResourceDataJRso getData() {
		data.setTime(TimeUtils.getCurTime());
		return data;
	}
	
}
