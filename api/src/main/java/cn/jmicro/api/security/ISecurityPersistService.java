package cn.jmicro.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISecurityPersistService {

	boolean saveAccount(ActInfo ai);
	
	boolean updatePwdById(long actId,String token,byte tokenType, String pwd);
	
	boolean updateEmailById(long actId,String email);
	
	boolean updateMobileById(long actId,String mobile);
	
	ActInfo getAccountById(long actId);
	
	ActInfo getAccountByActName(String actName);
	
	boolean updateStatuCode(long id, String token, byte tokenType,byte statuCode);
	
	boolean existAccount(String actName);
	
	boolean existEmail(String actName);
	
	boolean existMobile(String actName);
	
	int countAccount(Map<String, Object> queryConditions);
	
	List<ActInfo> getAccountList(Map<String, Object> queryConditions, int pageSize, int curPage);
	
	Map<String, Set<Permission>> getPermissionsByActName(String actName);
	
	Map<String, Set<Permission>> getAllPermissions();
	
}
