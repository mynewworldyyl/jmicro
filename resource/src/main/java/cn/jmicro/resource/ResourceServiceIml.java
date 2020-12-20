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
	public Set<ResourceData> getResource(Set<String> resNames, 
			Map<String,Object> params, Map<String,String> exps) {
		Set<ResourceData> rs = new HashSet<>();
		for(IResource r : resources) {
			String resName = r.getResourceName();
			if(r.isEnable() && resNames.contains(resName)) {
				ResourceData rd = r.getResource(params,exps.get(resName));
				if(rd != null) {
					rs.add(rd);
				}
			}
		}
		return rs;
	}
	
	public void ready() {}
	
}
