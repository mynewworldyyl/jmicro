package cn.jmicro.api.security;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountService {

	ActInfo login(String actName,String pwd);
	
	boolean logout(String loginKey);
	
	boolean isLogin(String loginKey);
	
	ActInfo getAccount(String loginKey);
	
}
