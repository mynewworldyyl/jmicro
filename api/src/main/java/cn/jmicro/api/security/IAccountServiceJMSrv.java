package cn.jmicro.api.security;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountServiceJMSrv {

	IPromise<RespJRso<ActInfoJRso>> login(String actName, String pwd, String code,String codeId);
	
	IPromise<RespJRso<ActInfoJRso>> loginWithId(int id,String pwd);
	
	IPromise<RespJRso<ActInfoJRso>> loginWithClientToken(String token);
	
	IPromise<RespJRso<ActInfoJRso>>  loginByWeixin(String code, int shareUserId);

	RespJRso<Boolean> hearbeat(String loginKey);
	
	RespJRso<Map<String, Set<PermissionJRso>>> getCurActPermissionDetail();
	
	RespJRso<String> getCode(int type);
	
	IPromise<RespJRso<ActInfoJRso>> changeCurClientId(int clientId);
	
	RespJRso<Map<Integer,String>> clientList();
	
	/**
	 * 绑定手机号
	 * @param mobile
	 * @return
	 */
	IPromise<RespJRso<Boolean>>  bindMobile(String mobile,String vcode);
	
	/**
	 * 绑定手机邮箱
	 * @param mobile
	 * @return
	 */
	IPromise<RespJRso<Boolean>>  bindMail(String mail,String vcode);
	
	/**
	 * 实名认证
	 * @param name
	 * @param idNo
	 * @param faceImage
	 * @return
	 */
	IPromise<RespJRso<Boolean>>  realNameVerify(String name, String idNo,String faceImage,String vcode);
	
	public default String key(String subfix) {
		return JMicroContext.CACHE_LOGIN_KEY + subfix;
	}
}
