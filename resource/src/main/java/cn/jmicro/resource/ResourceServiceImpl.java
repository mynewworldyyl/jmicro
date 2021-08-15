package cn.jmicro.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.IResourceServiceJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceDataJRso;

@Component
@Service(version="0.0.1", debugMode=0,
monitorEnable=0, logLevel=MC.LOG_WARN, retryCnt=0, showFront=false, external=true)
public class ResourceServiceImpl implements IResourceServiceJMSrv {

	@Inject
	private Set<IResource> resources = new HashSet<>();
	
	@Inject
	private ProcessInfoJRso pi;
	
	@Override
	public List<ResourceDataJRso> getResource(Set<String> resNames, 
			Map<String,Object> params, Map<String,String> exps) {
		List<ResourceDataJRso> rs = new ArrayList<>();
		if(exps == null) {
			exps = new HashMap<>();
		}
		if(resNames != null && !resNames.isEmpty()) {
			for(IResource r : resources) {
				String resName = r.getResourceName();
				if(r.isEnable() && resNames.contains(resName)) {
					ResourceDataJRso rd = r.getResource(params,exps.get(resName));
					if(rd != null) {
						rs.add(rd);
					}
				}
			}
		} else {
			for(IResource r : resources) {
				String resName = r.getResourceName();
				ResourceDataJRso rd = r.getResource(params,exps.get(resName));
				if(rd != null) {
					rs.add(rd);
				}
			}
		}
		
		return rs;
	}
	
	public void jready() {}
	
}
