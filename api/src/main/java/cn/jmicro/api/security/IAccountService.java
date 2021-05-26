package cn.jmicro.api.security;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountService {

	Resp<ActInfo> login(String actName,String pwd);
	
	Resp<ActInfo> loginWithId(int id,String pwd);

	public default String key(String subfix) {
		return JMicroContext.CACHE_LOGIN_KEY+subfix;
	}
	
	Resp<Boolean> hearbeat(String loginKey);
}
