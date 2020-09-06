package cn.jmicro.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountService {

	Resp<ActInfo> login(String actName,String pwd);
	
	boolean logout(String loginKey);
	
	boolean isLogin(String loginKey);
	
	ActInfo getAccount(String loginKey);
	
	Resp<Boolean> regist(String actName, String pwd,String mail,String mobile);
	
	Resp<Boolean> updatePwd(String newPwd,String oldPwd);
	
	Resp<Boolean> changeAccountStatus(String actName,boolean enableStatus);
	
	boolean checkActNameExist(String actName);
	
	Resp<Boolean> updatePermissions(String actName,Set<String> adds,Set<String> dels);
	
	Resp<Integer> countAccount(Map<String, String> queryConditions);
	
	Resp<List<ActInfo>> getAccountList(Map<String, String> queryConditions, int pageSize, int curPage);
	
	Resp<Map<String, Set<Permission>>> getPermissionsByActName(String actName);

	Resp<Map<String, Set<Permission>>> getAllPermissions();
}
