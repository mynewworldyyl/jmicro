package cn.jmicro.api.security;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAccountServiceJMSrv {

	public String activeAccount(Integer aid, String token);
	
	IPromise<RespJRso<ActInfoJRso>> login(Integer loginClientId,String actName, String pwd, String code,String codeId);
	
	IPromise<RespJRso<ActInfoJRso>> loginWithId(Integer loginClientId,int id,String pwd);
	
	IPromise<RespJRso<ActInfoJRso>> loginWithClientToken(Integer loginClientId,String token);
	
	IPromise<RespJRso<ActInfoJRso>>  loginByWeixin(Integer loginClientId,String code, int shareUserId);
	
	//物联网设备登录
	IPromise<Map<String,Object>>  loginByDevice(String userName, String pwd);

	RespJRso<Boolean> hearbeat(String loginKey);
	
	RespJRso<Map<String, Set<PermissionJRso>>> getCurActPermissionDetail();
	
	RespJRso<String> getCode(int type,String vcode,String codeId,String mobile);
	
	IPromise<RespJRso<ActInfoJRso>> changeCurClientId(int clientId);
	
	RespJRso<Map<Integer,String>> clientList();
	
	IPromise<RespJRso<Boolean>> activeSomething(String token);
	 
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
	IPromise<RespJRso<Boolean>>  bindMail(String mail, String vcode,String codeId);
	
	/**
	 * 实名认证通过
	 * @param name
	 * @param idNo
	 * @param faceImage
	 * @return
	 */
	/*IPromise<RespJRso<Boolean>> realNameVerify(String name, String idNo, String faceImageId,
			String idCardFileId, String vcode);*/
	
	/**
	 * 接收通过实名认证接口
	 * @param actId
	 * @return
	 */
	//public IPromise<RespJRso<Boolean>> approveRealname(Integer actId);
	
	/**
	 * 提交认证附件信息
	 * @param name
	 * @param idNo
	 * @param idCardFileId
	 * @param vcode
	 * @return
	 */
	IPromise<RespJRso<Boolean>> submitAttachmentInfo(String metadata, Byte type, String fileId, String vcode
			,String codeId, String remark/*,Byte appType*/);
	
	/**
	 * 
	 * @param fieldName
	 * @param val
	 * @return
	 */
	IPromise<RespJRso<Boolean>>  updateAttr(String fieldName, String val);
	
	/**
	   * 取得用户基本信息，如nickname,头像等
	 * @param uid
	 * @return
	 */
	IPromise<RespJRso<Map<String,Object>>> userInfo(Integer uid,String verifyCode);
	
	public default String key(String subfix) {
		return JMicroContext.CACHE_LOGIN_KEY + subfix;
	}
}
