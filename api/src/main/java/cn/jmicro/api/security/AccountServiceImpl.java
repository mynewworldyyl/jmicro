package cn.jmicro.api.security;

import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace="act", version="0.0.1",external=true)
public class AccountServiceImpl implements IAccountService {

	private static final ActInfo admin = new ActInfo("admin","0", 0);
	
	private static final ActInfo act0 = new ActInfo("user1","0", 10);
	
	private static final ActInfo act1 = new ActInfo("user2","0", 100);
	
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

	@Override
	public ActInfo login(String actName, String pwd) {
		
		String p = Config.AccountDir +"/"+ admin.getActName();
		String data = op.getData(p);
		if(StringUtils.isNotEmpty(data)) {
			ActInfo ai = JsonUtils.getIns().fromJson(data, ActInfo.class);
			if(ai != null && ai.getPwd().equals(pwd)) {
				ai.setLoginKey(JMicroContext.CACHE_LOGIN_KEY + this.idGenerator.getStringId(ActInfo.class));
				ai.setSuccess(true);
				cache.put(ai.getLoginKey(), ai);
				return ai;
			}
		}
		
		ActInfo rst = new ActInfo();
		rst.setSuccess(false);
		rst.setMsg("Account not exist or password error!");
		
		return rst;
	}
	
	@Override
	public ActInfo getAccount(String loginKey) {
		ActInfo ai = cache.get(loginKey);
		return ai;
	}

	@Override
	public boolean logout(String loginKey) {
		cache.del(loginKey);
		return true;
	}
	
	@Override
	public boolean isLogin(String loginKey) {
		return cache.exist(loginKey);
	}
	
}
