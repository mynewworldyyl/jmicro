package cn.jmicro.api.security;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.idgenerator.ComponentIdServer;

@Component
@Service(namespace="act", version="0.0.1")
public class AccountServiceImpl implements IAccountService {

	private static final ActInfo admin = new ActInfo("admin","0", 0);
	
	private static final ActInfo act0 = new ActInfo("user1","0", 10);
	
	private static final ActInfo act1 = new ActInfo("user2","0", 100);
	
	private ActInfo[] accounts = new ActInfo[] {admin,act0,act1};
	
	@Inject
	private ICache cache;
	
	@Inject
	private ComponentIdServer idGenerator;

	@Override
	public ActInfo login(String actName, String pwd) {
		ActInfo rst = null;
		
		for(ActInfo ai : accounts) {
			if(ai.getActName().equals(actName) && ai.getPwd().equals(pwd)) {
				try {
					rst = ai.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(rst == null) {
			rst = new ActInfo();
			rst.setSuccess(false);
		} else {
			rst.setLoginKey(JMicroContext.CACHE_LOGIN_KEY + this.idGenerator.getStringId(ActInfo.class));
			rst.setSuccess(true);
			cache.put(rst.getLoginKey(), rst);
		}
		
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
