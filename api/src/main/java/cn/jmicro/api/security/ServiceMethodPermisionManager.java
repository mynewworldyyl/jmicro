package cn.jmicro.api.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.Permission;
import cn.jmicro.api.service.ServiceManager;

@Component
public class ServiceMethodPermisionManager {

	@Inject
	private ServiceManager sm;
	
	private Map<String,Set<Permission>> pers = new HashMap<>();
	
	public void ready() {
		sm.addListener((type,si)->{
			if(type == IServiceListener.ADD) {
				serviceAdd(si);
			}
		});
	}

	private void serviceAdd(ServiceItem si) {
		Set<Permission> ps = new HashSet<>();
		for(ServiceMethod sm : si.getMethods()) {
			if(sm.isPerType()) {
				Permission p = new Permission();
				//p.setPid(sm.getKey().toKey(false, false, false));
				p.setLabel(sm.getKey().getMethod());
				p.setModelName(si.getKey().toSnv());
				p.setDesc(sm.getKey().getMethod()+"(" +sm.getKey().getParamsStr()+")");
				p.setActType(Permission.ACT_INVOKE);
				ps.add(p);
			}
		}
		if(!ps.isEmpty()) {
			pers.put(si.getKey().toSnv(), ps);
		}
	}
	
	
	public Map<String,Set<Permission>> getAllPermission() {
		return Collections.unmodifiableMap(pers);
	}
	
}
