package cn.jmicro.api.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceMethodJRso;
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
	
	private Map<String,Set<PermissionJRso>> pers = new HashMap<>();
	
	public static final boolean checkClientPermission(int loginAccountId,int targetDataClientId) {
		if(targetDataClientId == Constants.NO_CLIENT_ID || targetDataClientId == loginAccountId || loginAccountId == Config.getClientId()) {
			//一般账户启动的服务
			return true;
		}
		return false;
	}
	
	public static final boolean checkAccountClientPermission(int targetDataClientId) {
		return checkAccountClientPermission(Constants.FOR_TYPE_USER,targetDataClientId);
	}
	
	public static final boolean checkAccountClientPermission(int forType,int targetDataClientId) {
		ActInfoJRso ai = getAccount(forType);
		if(ai == null) {
			return false;
		}
		return checkClientPermission(ai.getClientId(),targetDataClientId);
	}
	
	public static final boolean isOwner(int targetDataClientId) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(ai == null) {
			return false;
		}
		return ai.getClientId() == targetDataClientId || ai.getClientId() == Config.getClientId();
	}
	
	public static final boolean isCurAdmin(int forType) {
		
		switch (forType) {
		case Constants.FOR_TYPE_ALL:
			ActInfoJRso ai = JMicroContext.get().getAccount();
			if (ai != null && ai.getClientId() == Config.getClientId()) {
				return true;
			}
			ai = JMicroContext.get().getSysAccount();
			if (ai != null && ai.getClientId() == Config.getClientId()) {
				return true;
			}
			return false;
		case Constants.FOR_TYPE_USER:
			ai = JMicroContext.get().getAccount();
			if (ai != null) {
				return ai.getClientId() == Config.getClientId();
			} else {
				return false;
			}

		case Constants.FOR_TYPE_SYS:
			ai = JMicroContext.get().getSysAccount();
			if (ai != null) {
				return ai.getClientId() == Config.getClientId();
			} else {
				return false;
			}
		}

		return false;
	}
	
	public static final boolean isCurDefAdmin(int forType) {
		
		switch (forType) {
		case Constants.FOR_TYPE_ALL:
			ActInfoJRso ai = JMicroContext.get().getAccount();
			if (ai != null && ai.getDefClientId() == Config.getClientId()) {
				return true;
			}
			ai = JMicroContext.get().getSysAccount();
			if (ai != null && ai.getDefClientId() == Config.getClientId()) {
				return true;
			}
			return false;
		case Constants.FOR_TYPE_USER:
			ai = JMicroContext.get().getAccount();
			if (ai != null) {
				return ai.getDefClientId() == Config.getClientId();
			} else {
				return false;
			}

		case Constants.FOR_TYPE_SYS:
			ai = JMicroContext.get().getSysAccount();
			if (ai != null) {
				return ai.getDefClientId() == Config.getClientId();
			} else {
				return false;
			}
		}

		return false;
	}
	
	public static final boolean isCurAdmin() {
		return isCurAdmin(Constants.FOR_TYPE_USER);
	}
	
	public static final boolean isCurDefAdmin() {
		return isCurDefAdmin(Constants.FOR_TYPE_USER);
	}
	
	public void ready() {
		sm.addListener((type,siKey,item)->{
			if(type == IServiceListener.ADD) {
				//serviceAdd(si);
			}
		});
	}

	/*private void serviceAdd(UniqueServiceKeyJRso si) {
		Set<PermissionJRso> pers = new HashSet<>();
		
		for(UniqueServiceKeyJRso sm : si.getMethods()) {
			if(sm.isPerType()) {
				PermissionJRso p = new PermissionJRso();
				p.setLabel(sm.getKey().getMethod());
				p.setModelName(si.getKey().toSnv());
				p.setDesc(sm.getKey().getMethod()+"(" +sm.getKey().getParamsStr()+")");
				p.setActType(PermissionJRso.ACT_INVOKE);
				pers.add(p);
			}
		}
	}*/
	
	public Map<String,Set<PermissionJRso>> getAllPermission() {
		return Collections.unmodifiableMap(pers);
	}
	
	public ServerError permissionCheck(ServiceMethodJRso sm,int srcClientId ) {
		if(isCurAdmin(sm.getForType()) || !sm.isNeedLogin() && sm.getForType() == Constants.FOR_TYPE_ALL) {
			return null;
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		ActInfoJRso sai = JMicroContext.get().getSysAccount();
		
		if(sm.isNeedLogin() && (ai == null && sai == null ||
				ai == null && Constants.FOR_TYPE_USER == sm.getForType()
				|| sai == null && Constants.FOR_TYPE_SYS == sm.getForType()) ) {
			ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,
					 "Have to login for invoking to " + sm.getKey().methodID());
			LG.log(MC.LOG_ERROR, TAG,se.toString());
			MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
			logger.warn(se.toString());
			return se;
		}
		
		if(sm.getForType() == Constants.FOR_TYPE_SYS) {
			ai = sai;
		}
		
		if(sm.isPerType() && (ai == null || ai.getPers() == null 
				|| !ai.getPers().contains(sm.getKey().getSnvHash()))) {
			ServerError se = new ServerError(MC.MT_ACT_PERMISSION_REJECT,
					(ai!= null?ai.getActName():" Not login") + " no permission for this operation ");
			LG.log(MC.LOG_ERROR, TAG,se.toString()+",SM: " + sm.getKey().fullStringKey());
			MT.rpcEvent(MC.MT_ACT_PERMISSION_REJECT);
			logger.warn(se.toString()+",SM: " + sm.getKey().fullStringKey());
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
	
	private static final ActInfoJRso getAccount(int forType) {
		if(forType == Constants.FOR_TYPE_SYS) {
			return JMicroContext.get().getSysAccount();
		} else if(forType == Constants.FOR_TYPE_USER) {
			return JMicroContext.get().getAccount();
		} else if(forType == Constants.FOR_TYPE_ALL) {
			if(JMicroContext.get().getAccount() != null) {
				return JMicroContext.get().getAccount();
			} else {
				JMicroContext.get().getSysAccount();
			}
		}
		return null;
	}
	
}
