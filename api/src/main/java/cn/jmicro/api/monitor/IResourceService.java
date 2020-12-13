package cn.jmicro.api.monitor;

import java.util.Map;
import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IResourceService {

	Set<ResourceData> getResource(Map<String,Object> params);
	
}
