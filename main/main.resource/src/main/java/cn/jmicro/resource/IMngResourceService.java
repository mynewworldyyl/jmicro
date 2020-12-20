package cn.jmicro.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.Resp;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.monitor.ResourceMonitorConfig;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMngResourceService {

	Resp<List<ResourceMonitorConfig>> query();
	
	Resp<Boolean> enable(Integer id);
	
	Resp<Boolean> update(ResourceMonitorConfig cfg);
	
	Resp<Boolean> delete(int id);
	
	Resp<ResourceMonitorConfig> add(ResourceMonitorConfig cfg);
	
	Resp<Set<CfgMetadata>> getResourceMetadata(String resName,boolean refreshCache);
	 
	Resp<Map<String,Map<String,Set<CfgMetadata>>>> getInstanceResourceList();
	
	Resp<Map<String,Set<ResourceData>>> getInstanceResourceData(Map<String,String> params);
	
}
