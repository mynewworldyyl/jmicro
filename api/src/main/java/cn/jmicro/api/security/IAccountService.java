package cn.jmicro.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountService {

	Resp<ActInfo> getAccount(String loginKey);
	
	Resp<String> getNameById(Integer id);
	
	//Resp<Boolean> checkAccountExist(String actName);
	
	Resp<Boolean> resetPwdEmail(String actName,String checkCode);
	
	Resp<Boolean> resetPwd(String actName, String token, String newPwd);
	
	Resp<Boolean> activeAccount(String actName, String token);
	
	Resp<Boolean> resendActiveEmail(String actName);
	
	Resp<ActInfo> login(String actName,String pwd);
	
	Resp<Boolean> logout();
	
	Resp<Boolean> isLogin(String loginKey);
	
	//Resp<ActInfo> getAccountByLoginkey(String loginKey);
	
	Resp<Boolean> regist(String actName, String pwd,String mail,String mobile);
	
	Resp<Boolean> updatePwd(String newPwd,String oldPwd);
	
	Resp<Boolean> changeAccountStatus(String actName);
	
	Resp<Boolean> checkAccountExist(String actName);
	
	//Resp<Boolean> updatePermissions(String actName,Set<String> adds,Set<String> dels);
	Resp<Boolean> updateActPermissions(String actName,Set<Integer> adds,Set<Integer> dels);
	
	Resp<Integer> countAccount(Map<String, Object> queryConditions);
	
	Resp<List<ActInfo>> getAccountList(Map<String, Object> queryConditions, int pageSize, int curPage);
	
	Resp<Map<String, Set<Permission>>> getPermissionsByActName(String actName);

	Resp<Map<String, Set<Permission>>> getAllPermissions();
}
