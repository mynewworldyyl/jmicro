package cn.jmicro.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISecurityPersistServiceJMSrv {

	boolean saveAccount(ActInfoJRso ai);
	
	boolean updatePwdById(long actId,String token,byte tokenType, String pwd);
	
	boolean updateEmailById(long actId,String email);
	
	boolean updateMobileById(long actId,String mobile);
	
	ActInfoJRso getAccountById(long actId);
	
	ActInfoJRso getAccountByActName(String actName);
	
	boolean updateStatuCode(long id, String token, byte tokenType,byte statuCode);
	
	boolean existAccount(String actId);
	
	boolean existEmail(String email);
	
	boolean existMobile(String actName);
	
	int countAccount(Map<String, Object> queryConditions);
	
	List<ActInfoJRso> getAccountList(Map<String, Object> queryConditions, int pageSize, int curPage);
	
	Map<String, Set<PermissionJRso>> getPermissionsByActId(Integer actId,boolean allPer);
	
	Map<String, Set<PermissionJRso>> getAllPermissions();
	
}
