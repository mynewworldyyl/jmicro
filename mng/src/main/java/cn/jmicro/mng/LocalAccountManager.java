package cn.jmicro.mng;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionJRso;

@Component
public class LocalAccountManager {

	@Inject
	private AccountManager am;
	
	public Map<String,Set<PermissionJRso>> getPermissionByAccountName(String actName) {
		Map<String,Set<PermissionJRso>> rst = new HashMap<>();
		ActInfoJRso ai = am.getAccountFromZK(actName);
		if(ai.getPers() != null && !ai.getPers().isEmpty()) {
			for(Integer mk : ai.getPers()) {
				UniqueServiceMethodKeyJRso k = UniqueServiceMethodKeyJRso.fromKey(mk+"");
				PermissionJRso p = new PermissionJRso();
				//p.setPid(k.toKey(false, false, false));
				p.setLabel(k.getMethod());
				p.setModelName(k.getUsk().toSnv());
				//p.setDesc(k.getMethod()+"(" +k.getParamsStr()+")");
				p.setActType(PermissionJRso.ACT_INVOKE);
				if(!rst.containsKey(p.getModelName())) {
					rst.put(p.getModelName(),new HashSet<>());
				}
				rst.get(p.getModelName()).add(p);
			}
		}
		return rst;
	}
	
	
}
