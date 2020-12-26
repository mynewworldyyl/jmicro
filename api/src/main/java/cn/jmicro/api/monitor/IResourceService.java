package cn.jmicro.api.monitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IResourceService {

	List<ResourceData> getResource(Set<String> resNames, Map<String,Object> params, Map<String,String> exps);
	
}
