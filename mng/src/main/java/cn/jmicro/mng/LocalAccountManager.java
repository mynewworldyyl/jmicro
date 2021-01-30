package cn.jmicro.mng;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.Permission;
import cn.jmicro.api.security.ServiceMethodPermisionManager;

@Component
public class LocalAccountManager {

	@Inject
	private AccountManager am;
	
	@Inject
	private ServiceMethodPermisionManager pm;
	
	public Map<String,Set<Permission>> getPermissionByAccountName(String actName) {
		Map<String,Set<Permission>> rst = new HashMap<>();
		ActInfo ai = am.getAccountFromZK(actName);
		if(ai.getPers() != null && !ai.getPers().isEmpty()) {
			for(Integer mk : ai.getPers()) {
				UniqueServiceMethodKey k = UniqueServiceMethodKey.fromKey(mk+"");
				Permission p = new Permission();
				//p.setPid(k.toKey(false, false, false));
				p.setLabel(k.getMethod());
				p.setModelName(k.getUsk().toSnv());
				p.setDesc(k.getMethod()+"(" +k.getParamsStr()+")");
				p.setActType(Permission.ACT_INVOKE);
				if(!rst.containsKey(p.getModelName())) {
					rst.put(p.getModelName(),new HashSet<>());
				}
				rst.get(p.getModelName()).add(p);
			}
		}
		return rst;
	}
	
	public Map<String,Set<Permission>> getServiceMethodPermissions() {
		return pm.getAllPermission();
	}
	
	
	
}
