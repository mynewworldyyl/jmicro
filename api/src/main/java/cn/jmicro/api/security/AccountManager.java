package cn.jmicro.api.security;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.genclient.IAccountService$JMAsyncClient;
import cn.jmicro.api.service.IServiceAsyncResponse;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class AccountManager {

	private final Logger logger = LoggerFactory.getLogger(AccountManager.class);
	
	private static final ActInfo admin = new ActInfo("jmicro","jmicro123", 0);
	
	private static final ActInfo act0 = new ActInfo("jmicro0","jmicro123", 10);
	
	private static final ActInfo act1 = new ActInfo("jmicro1","jmicro123", 100);
	
	@Reference(namespace="*",version="*")
	private IAccountService$JMAsyncClient as;
	
	@Inject
	private ICache cache;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	public void ready() {
		Set<String> acts = op.getChildren(Config.AccountDir, false);
		if(acts == null || acts.isEmpty()) {
			String p = Config.AccountDir +"/"+ admin.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(admin), IDataOperator.PERSISTENT);
			p = Config.AccountDir +"/"+ act0.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(act0), IDataOperator.PERSISTENT);
			p = Config.AccountDir +"/"+ act1.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(act1), IDataOperator.PERSISTENT);
		}
	}

	public ActInfo login(String actName, String pwd) {
		if(as == null || !as.isReady()) {
			ActInfo ai = getAccountFromZK(actName);
			if(ai != null && ai.getPwd().equals(pwd)) {
				String akey = JMicroContext.CACHE_LOGIN_KEY + ai.getActName();
				ActInfo la = cache.get(akey);
				if(la == null) {
					la = ai;
					ai.setLoginKey(JMicroContext.CACHE_LOGIN_KEY + this.idGenerator.getStringId(ActInfo.class));
					cache.put(akey, ai);
					cache.put(ai.getLoginKey(), ai);
				}
				la.setSuccess(true);
				return la;
			} else {
				ActInfo rst = new ActInfo();
				rst.setSuccess(false);
				rst.setMsg("Account not exist or password error!");
				return rst;
			}
		} else {
			
			JMicroContext cxt = JMicroContext.get();
			
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			
			if(cxt.isAsync() && cb != null) {
				as.loginJMAsync(null, actName, pwd)
				.then((ai,fail,cxt0)->{
					if(fail == null) {
						cb.result(ai);
					} else {
						logger.error("Login error: " + fail.toString());
						cb.result(null);
					}
				});
				return null;
			} else {
				return as.login(actName, pwd).getData();
			}
			
		}
	}
	
	public ActInfo getAccount(String loginKey) {
		ActInfo ai = cache.get(loginKey);
		return ai;
	}

	public boolean logout(String loginKey) {
		ActInfo ai = this.getAccount(loginKey);
		if(ai != null) {
			cache.del(loginKey);
			cache.del(JMicroContext.CACHE_LOGIN_KEY + ai.getActName());
		}
		return true;
	}
	
	public boolean isLogin(String loginKey) {
		return cache.exist(loginKey);
	}
	
	public ActInfo getAccountFromZK(String actName) {
		String p = Config.AccountDir +"/"+ actName;
		String data = op.getData(p);
		if(StringUtils.isNotEmpty(data)) {
			ActInfo ai = JsonUtils.getIns().fromJson(data, ActInfo.class);
			return ai;
		}
		return null;
	}

}
