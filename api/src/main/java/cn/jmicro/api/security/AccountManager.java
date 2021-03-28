package cn.jmicro.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.Md5Utils;
import cn.jmicro.common.util.StringUtils;

@Component
public class AccountManager {

	private final Logger logger = LoggerFactory.getLogger(AccountManager.class);
	
	//private static ActInfo jmicro = null;
	
	public static final String ActDir = Config.getRaftBasePath(Config.AccountDir) + "/accounts";
	public static final String EmailDir = Config.getRaftBasePath(Config.AccountDir) + "/emails";
	public static final String MobileDir = Config.getRaftBasePath(Config.AccountDir) + "/mobiles";
	
	private static final long expired = 10*60*1000;
	
	private static final long updateExpired = expired >> 1;
	
	/*private static final String[] PERS = new String[] {
		"cn.jmicro.api.security.IAccountService##sec##0.0.1########updateActPermissions##Ljava/lang/String;Ljava/util/Set;Ljava/util/Set;","cn.jmicro.api.security.IAccountService##sec##0.0.1########getPermissionsByActName##Ljava/lang/String;","cn.jmicro.api.security.IAccountService##sec##0.0.1########getAccountList##Ljava/util/Map;II","cn.jmicro.api.security.IAccountService##sec##0.0.1########getAllPermissions##","cn.jmicro.api.mng.IChoreographyService##mng##0.0.1########getProcessInstanceList##Z","cn.jmicro.api.mng.IChoreographyService##mng##0.0.1########changeAgentState##Ljava/lang/String;","cn.jmicro.api.mng.IChoreographyService##mng##0.0.1########getAgentList##Z","cn.jmicro.api.mng.IChoreographyService##mng##0.0.1########updateDeployment##Lcn/jmicro/api/choreography/Deployment;","cn.jmicro.api.mng.IManageService##mng##0.0.1########getServices##","cn.jmicro.api.mng.IManageService##mng##0.0.1########updateItem##Lcn/jmicro/api/registry/ServiceItem;","cn.jmicro.api.mng.IManageService##mng##0.0.1########updateMethod##Lcn/jmicro/api/registry/ServiceMethod;","cn.jmicro.api.security.IAccountService##sec##0.0.1########countAccount##Ljava/util/Map;","cn.jmicro.api.security.IAccountService##sec##0.0.1########changeAccountStatus##Ljava/lang/String;Z"
	};*/
	
	@Inject
	private ICache cache;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	public void ready() {
		/*Set<String> acts = op.getChildren(AccountManager.ActDir, false);
		if(acts == null || acts.isEmpty()) {
			String p = AccountManager.ActDir +"/"+ jmicro.getActName();
			jmicro = new ActInfo("jmicro","jmicro123", 0);
			jmicro.setStatuCode(ActInfo.SC_ACTIVED);
			jmicro.getPers().addAll(Arrays.asList(PERS));
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(jmicro), IDataOperator.PERSISTENT);
		}*/
	}
	
	public boolean checkEmailExist(String email) {
		return op.exist(AccountManager.EmailDir +"/"+ email);
	}
	
	public boolean checkMobileExist(String mobile) {
		return op.exist(AccountManager.MobileDir +"/"+ mobile);
	}

	public Resp<ActInfo> login(String actName, String pwd) {
		Resp<ActInfo> r = new Resp<ActInfo>();

		ActInfo ai = getAccountFromZK(actName);
		
		if(ai == null) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("Account not exist or password error!");
			return r;
		}
		
		if(ai.getStatuCode() != ActInfo.SC_ACTIVED) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("Account invalid now!");
			return r;
		}
		
		if(ai.getPwd().equals(pwd) || Md5Utils.getMd5(pwd).equals(ai.getPwd())) {
			String akey = key(ai.getActName());
			
			String oldLk = null;
			if(cache.exist(akey)) {
				oldLk = cache.get(akey);
			}
			
			if(oldLk == null) {
				ai.setLoginKey(key(this.idGenerator.getStringId(ActInfo.class)));
				ai.setLastActiveTime(TimeUtils.getCurTime());
				cache.put(ai.getLoginKey(), ai,expired);
				cache.put(akey, ai.getLoginKey(),expired);
				cache.put(key(ai.getId()+""), ai.getLoginKey(),expired);
			} else {
				ai = cache.get(oldLk);
			}
			r.setCode(Resp.CODE_SUCCESS);
			r.setData(ai);
			return r;
		} else {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("Account not exist or password error!");
			return r;
		}
	
	}
	
	private String key(String subfix) {
		return JMicroContext.CACHE_LOGIN_KEY+subfix;
	}
	
	public boolean forceAccountLogout(String actName) {
		String akey = key(actName);
		if(cache.exist(akey)) {
			String lk = cache.get(akey);
			if(StringUtils.isNotEmpty(lk)) {
				logger.warn("Account "+actName+" force logout by: " + JMicroContext.get().getAccount().getActName());
				this.logout(lk);
			}else {
				cache.del(akey);
			}
			
		}
		return true;
	}
	
	public ActInfo getAccount(String loginKey) {
		if(cache.exist(loginKey)) {
			ActInfo ai = cache.get(loginKey);
			long curTime = TimeUtils.getCurTime();
			if(curTime - ai.getLastActiveTime() > updateExpired) {
				setActInfoCache(ai,curTime);
			}
			return ai;
		}
		return null;
	}
	
	private void setActInfoCache(ActInfo ai,long curTime) {
		ai.setLastActiveTime(curTime);
		cache.put(ai.getLoginKey(), ai,expired);
		cache.expire(key(ai.getActName()),expired);
		cache.expire(key(ai.getId()+""),expired);
	}

	public boolean logout(String loginKey) {
		ActInfo ai = this.getAccount(loginKey);
		if(ai != null) {
			cache.del(loginKey);
			cache.del(key(ai.getActName()));
			cache.del(key(ai.getId()+""));
		}
		return true;
	}
	
	public boolean isLogin(String loginKey) {
		return cache.exist(loginKey);
	}
	
	public ActInfo getAccountFromZK(String actName) {
		String p = AccountManager.ActDir +"/"+ actName;
		String data = op.getData(p);
		if(StringUtils.isNotEmpty(data)) {
			ActInfo ai = JsonUtils.getIns().fromJson(data, ActInfo.class);
			return ai;
		}
		return null;
	}

	public Boolean existActName(String actName) {
		return op.exist(AccountManager.ActDir +"/"+ actName);
	}
	
}
