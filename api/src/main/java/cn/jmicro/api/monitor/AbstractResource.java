package cn.jmicro.api.monitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.StringUtils;

public abstract class AbstractResource implements IResource {

	private ResourceData data = new ResourceData();
	
	@Inject
	protected ProcessInfo pi;
	
	@Cfg(value="/enable")
	private boolean enable = false;
	
	public void ready() {
		data.setBelongInsId(pi.getId());
		data.setBelongInsName(pi.getInstanceName());
		data.setResName(this.getResourceName());
	}
	
	@Override
	public ResourceData getResource(Map<String,Object> params,String expStr) {
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
	public Map<String,Set<CfgMetadata>> metaData() {
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

	public ResourceData getData() {
		data.setTime(TimeUtils.getCurTime());
		return data;
	}
	
}
