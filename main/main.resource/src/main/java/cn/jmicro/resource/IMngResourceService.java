package cn.jmicro.resource;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.monitor.ResourceMonitorConfig;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMngResourceService {

	Resp<List<ResourceMonitorConfig>> query();
	
	Resp<Boolean> enable(Integer id);
	
	Resp<Boolean> update(ResourceMonitorConfig cfg);
	
	Resp<Boolean> delete(int id);
	
	Resp<ResourceMonitorConfig> add(ResourceMonitorConfig cfg);
	 
}
