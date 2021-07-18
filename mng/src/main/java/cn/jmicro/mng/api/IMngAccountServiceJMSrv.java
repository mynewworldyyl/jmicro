package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMngAccountServiceJMSrv {

	RespJRso<Boolean> checkAccountExist(String actName);
	
	RespJRso<Boolean> resetPwdEmail(String actName,String checkCode);
	
	RespJRso<Boolean> resetPwd(String actName, String token, String newPwd);
	
	RespJRso<Boolean> activeAccount(String actName, String token);
	
	RespJRso<Boolean> resendActiveEmail(String actName);
	
	RespJRso<ActInfoJRso> login(String actName,String pwd);
	
	RespJRso<Boolean> logout();
	
	RespJRso<Boolean> isLogin(String loginKey);
	
	RespJRso<ActInfoJRso> getAccount(String loginKey);
	
	RespJRso<Boolean> regist(String actName, String pwd,String mail,String mobile);
	
	RespJRso<Boolean> updateActPermissions(String actName,Set<String> adds,Set<String> dels);
	
	RespJRso<Boolean> updatePwd(String newPwd,String oldPwd);
	
	RespJRso<Boolean> changeAccountStatus(String actName);
	
	RespJRso<Integer> countAccount(Map<String, Object> queryConditions);
	
	RespJRso<List<ActInfoJRso>> getAccountList(Map<String, Object> queryConditions, int pageSize, int curPage);
	
	RespJRso<Map<String, Set<PermissionJRso>>> getPermissionsByActName(String actName);
	
	RespJRso<Map<String, Set<PermissionJRso>>> getAllPermissions();
	
}
