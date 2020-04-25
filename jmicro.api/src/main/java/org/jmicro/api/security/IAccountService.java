package org.jmicro.api.security;

public interface IAccountService {

	ActInfo login(String actName,String pwd);
	
	boolean logout(String loginKey);
	
	boolean isLogin(String loginKey);
	
	ActInfo getAccount(String loginKey);
	
}
