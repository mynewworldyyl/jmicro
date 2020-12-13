package cn.jmicro.resource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.IResourceService;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceData;

@Component
@Service(namespace="monitorResourceService", version="0.0.1", debugMode=0,
monitorEnable=0, logLevel=MC.LOG_WARN, retryCnt=0, showFront=false, external=true)
public class ResourceServiceIml implements IResourceService {

	@Inject
	private Set<IResource> resources = new HashSet<>();
	
	@Inject
	private ProcessInfo pi;
	
	@Override
	public Set<ResourceData> getResource(Map<String,Object> params) {
		Set<ResourceData> rs = new HashSet<>();
		for(IResource r : resources) {
			if(r.isEnable()) {
				rs.add(r.getResource(params));
			}
		}
		return rs;
	}
	
	public void ready() {
		
	}
	
}
