package cn.jmicro.api.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;

@Component
public class PermissionManager {
	
	private static final Class<?> TAG = PermissionManager.class;

	private final static Logger logger = LoggerFactory.getLogger(PermissionManager.class);
	
	@Inject
	private PermissionManager pm;
	
	@Inject
	private ServiceManager sm;
	
	private Map<String,Set<Permission>> pers = new HashMap<>();
	
	public static final boolean checkClientPermission(int loginAccountId,int targetDataClientId) {
		if(targetDataClientId == Constants.NO_CLIENT_ID || targetDataClientId == loginAccountId || loginAccountId == Config.getAdminClientId()) {
			//一般账户启动的服务
			return true;
		}
		return false;
	}
	
	public static final boolean checkAccountClientPermission(int targetDataClientId) {
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai == null) {
			return false;
		}
		return checkClientPermission(ai.getId(),targetDataClientId);
	}
	
	public static final boolean isCurAdmin() {
		
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai != null) {
			return ai.getId() == Config.getAdminClientId();
		}
		
		return false;
	}
	
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
	
	public ServerError permissionCheck(ActInfo ai,ServiceMethod sm,int srcClientId ) {
		if(ai == null && sm.isNeedLogin()){
			ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,
					 "Have to login for invoking to " + sm.getKey().toKey(false, false, false));
			LG.log(MC.LOG_ERROR, TAG,se.toString());
			MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
			logger.warn(se.toString());
			return se;
		} else if(sm.isPerType() && (ai == null || ai.getPers() == null || !ai.getPers().contains(sm.getKey().toKey(false, false, false)))) {
			ServerError se = new ServerError(MC.MT_ACT_PERMISSION_REJECT,
					(ai!= null?ai.getActName():" Not login") + " no permission for this operation ");
			LG.log(MC.LOG_ERROR, TAG,se.toString()+",SM: " + sm.getKey().toKey(true, true, true));
			MT.rpcEvent(MC.MT_ACT_PERMISSION_REJECT);
			logger.warn(se.toString()+",SM: " + sm.getKey().toKey(true, true, true));
			return se;
		}/*else if(checkAccountClientPermission(srcClientId)) {
			ServerError se = new ServerError(ServerError.SE_NO_PERMISSION,
					(ai!= null?ai.getActName():" Not login") + " permission reject to invoke "+ srcClientId);
			SF.eventLog(MC.MT_CLIENT_ID_REJECT,MC.LOG_ERROR, TAG,se.toString());
			logger.warn(se.toString());
			return se;
		}*/
		return null;
	}
	
}
