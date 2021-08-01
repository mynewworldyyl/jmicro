package cn.jmicro.api.security;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountServiceJMSrv {

	RespJRso<ActInfoJRso> login(String actName, String pwd, String code,String codeId);
	
	RespJRso<ActInfoJRso> loginWithId(int id,String pwd);
	
	RespJRso<ActInfoJRso> loginWithClientToken(String token);
	
	RespJRso<ActInfoJRso>  loginByWeixin(String code, int shareUserId);

	RespJRso<Boolean> hearbeat(String loginKey);
	
	RespJRso<Map<String, Set<PermissionJRso>>> getCurActPermissionDetail();
	
	RespJRso<String> getCode(int type);
	
	RespJRso<ActInfoJRso> changeCurClientId(int clientId);
	
	RespJRso<Map<Integer,String>> clientList();
	
	public default String key(String subfix) {
		return JMicroContext.CACHE_LOGIN_KEY+subfix;
	}
}
