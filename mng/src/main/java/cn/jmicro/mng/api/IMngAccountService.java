package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.Permission;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMngAccountService {

	Resp<Boolean> checkAccountExist(String actName);
	
	Resp<Boolean> resetPwdEmail(String actName,String checkCode);
	
	Resp<Boolean> resetPwd(String actName, String token, String newPwd);
	
	Resp<Boolean> activeAccount(String actName, String token);
	
	Resp<Boolean> resendActiveEmail(String actName);
	
	Resp<ActInfo> login(String actName,String pwd);
	
	Resp<Boolean> logout();
	
	Resp<Boolean> isLogin(String loginKey);
	
	Resp<ActInfo> getAccount(String loginKey);
	
	Resp<Boolean> regist(String actName, String pwd,String mail,String mobile);
	
	Resp<Boolean> updateActPermissions(String actName,Set<String> adds,Set<String> dels);
	
	Resp<Boolean> updatePwd(String newPwd,String oldPwd);
	
	Resp<Boolean> changeAccountStatus(String actName);
	
	Resp<Integer> countAccount(Map<String, String> queryConditions);
	
	Resp<List<ActInfo>> getAccountList(Map<String, String> queryConditions, int pageSize, int curPage);
	
	Resp<Map<String, Set<Permission>>> getPermissionsByActName(String actName);
	
	Resp<Map<String, Set<Permission>>> getAllPermissions();
	
}
