package cn.jmicro.api.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;

@Component
public class PermisionManager {
	
	private static final Class<?> TAG = PermisionManager.class;

	private final static Logger logger = LoggerFactory.getLogger(PermisionManager.class);
	
	@Inject
	private PermisionManager pm;
	
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
		Set<Permission> pers = new HashSet<>();
		for(ServiceMethod sm : si.getMethods()) {
			if(sm.isPerType()) {
				Permission p = new Permission();
				p.setPid(sm.getKey().toKey(false, false, false));
				p.setLabel(sm.getKey().getMethod());
				p.setModelName(si.getKey().toSnv());
				p.setDesc(sm.getKey().getMethod()+"(" +sm.getKey().getParamsStr()+")");
				p.setActType(Permission.ACT_INVOKE);
				pers.add(p);
			}
		}
	}
	
	public Map<String,Set<Permission>> getAllPermission() {
		return Collections.unmodifiableMap(pers);
	}
	
	public ServerError permissionCheck(ActInfo ai,ServiceMethod sm) {
		if(ai == null && sm.isNeedLogin()){
			ServerError se = new ServerError(ServerError.SE_NOT_LOGIN,
					 "Have to login for invoking to " + sm.getKey().toKey(false, false, false));
			SF.eventLog(MC.MT_INVALID_LOGIN_INFO,MC.LOG_ERROR, TAG,se.toString());
			logger.warn(se.toString());
			return se;
		} else if(sm.isPerType() && (ai == null || ai.getPers() == null || !ai.getPers().contains(sm.getKey().toKey(false, false, false)))) {
			ServerError se = new ServerError(ServerError.SE_NO_PERMISSION,
					(ai!= null?ai.getActName():" Not login") + " permission reject to invoke "+ sm.getKey().toKey(false, false, false));
			SF.eventLog(MC.MT_INVALID_LOGIN_INFO,MC.LOG_ERROR, TAG,se.toString());
			logger.warn(se.toString());
			return se;
		}
		return null;
	}
	
}
