package cn.jmicro.api.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IRoleServiceJMSrv {

	RespJRso<RoleJRso> addRole(RoleJRso role);
	
	RespJRso<List<RoleJRso>> listRoles(Map<String,Object> queryConditions,Integer pageSize,Integer curPage);
	
	RespJRso<Boolean> updateRole(RoleJRso role);
	
	//RespJRso<Boolean> addRolePermission(Integer roleId, Set<Integer> perIds);
	
	//RespJRso<Boolean> deleteRolePermission(Integer roleId, Set<Integer> perIds);
	
	RespJRso<Map<String,Set<PermissionJRso>>> getRolePermissions(Integer roleId);
	
	RespJRso<List<RoleJRso>> listRoleByRoleIds(Set<Integer> roleIds);
	
	RespJRso<String> updateRolePermissions(Integer actId,Set<Integer> adds,Set<Integer> dels);
	 
}
