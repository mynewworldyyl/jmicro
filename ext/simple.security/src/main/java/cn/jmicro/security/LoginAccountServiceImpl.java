package cn.jmicro.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.IAccountServiceJMSrv;
import cn.jmicro.api.security.PermissionJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,debugMode=1,
showFront=false,clientId=Constants.NO_CLIENT_ID)
public class LoginAccountServiceImpl implements IAccountServiceJMSrv {

	private final Logger logger = LoggerFactory.getLogger(LoginAccountServiceImpl.class);
	
	private Map<String,ActInfoJRso> accounts = new HashMap<>();
	
	private Map<Integer,String> id2ActName = new HashMap<>();
	
	@Inject
	private AccountManager am;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICache cache;
	
	public void jready() {
		initAccount();
	}
	
	@Override
	public String activeAccount(Integer aid, String token) {
		return null;
	}

	@Override
	public IPromise<RespJRso<Boolean>> activeSomething(String token) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPromise<RespJRso<Boolean>> updateAttr(String fieldName, String val) {
		return null;
	}

	@Override
	public IPromise<Map<String,Object>> loginByDevice(String actName,String pwd) {
		
		return null;
	}

	@Override
	public IPromise<RespJRso<ActInfoJRso>> login(Integer loginClientId,String actName, String pwd,String code,String codeId) {

		return new Promise<RespJRso<ActInfoJRso>>((suc,fail)->{
			RespJRso<ActInfoJRso> r = new RespJRso<ActInfoJRso>();
			ActInfoJRso ai = accounts.get(actName);
			if(ai == null) {
				r.setCode(RespJRso.CODE_FAIL);
				r.setMsg("Account invalid now!");
				suc.success(r);
				return;
			}
			
			if(ai.getPwd().equals(pwd)) {
				String akey = key(ai.getActName());
				
				String oldLk = null;
				if(cache.exist(akey)) {
					oldLk = cache.get(akey,String.class);
				}
				
				if(Utils.isEmpty(oldLk)) {
					int seed = HashUtils.FNVHash1(TimeUtils.getCurTime() + "_" + this.idGenerator.getStringId(ActInfoJRso.class));
					if(seed < 0) {
						seed = -seed;
					}
					oldLk = key(""+ seed);
				}
				
				ai.setLastLoginTime(0);
				ai.setLoginNum(0);
				
				long curTime = TimeUtils.getCurTime();
				ai.setLoginKey(oldLk);
				ai.setLastActiveTime(curTime);
				ai.setAdmin(ai.getClientId() == Config.getClientId());
				
				cache.put(ai.getLoginKey(), ai,1800000);
				cache.put(akey, oldLk,1800000);
				cache.put(key(ai.getId()+""),oldLk,1800000);
				
				ai.setAdmin(ai.getClientId() == Config.getClientId());
				r.setCode(RespJRso.CODE_SUCCESS);
				r.setData(ai);
				suc.success(r);
				return;
			} else {
				r.setCode(RespJRso.CODE_FAIL);
				r.setMsg("Account not exist or password error!");
				suc.success(r);
				return;
			}
		});
	}

	@Override
	public IPromise<RespJRso<ActInfoJRso>> loginWithId(Integer loginClientId,int id, String pwd) {
		if(id2ActName.containsKey(id)) {
			return login(loginClientId,this.id2ActName.get(id),pwd,"","");
		} else {
			return new Promise<RespJRso<ActInfoJRso>>((suc,fail)->{
				RespJRso<ActInfoJRso> r = new RespJRso<ActInfoJRso>(RespJRso.CODE_FAIL,"Account not found!");
				suc.success(r);
			});
		}
	}
	

	@Override
	public RespJRso<String> getCode(int type, String vcode, String codeId, String mobile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPromise<RespJRso<ActInfoJRso>> changeCurClientId(int clientId) {
		return null;
	}

	@Override
	public RespJRso<Map<Integer, String>> clientList() {
		return null;
	}

	@Override
	public RespJRso<Map<String, Set<PermissionJRso>>> getCurActPermissionDetail() {
		return null;
	}

	@Override
	//@SMethod(forType=Constants.FOR_TYPE_SYS)
	public RespJRso<Boolean> hearbeat(String loginKey) {
		ActInfoJRso ai = am.getAccount(loginKey);
		if(ai != null) {
			return RespJRso.d(RespJRso.CODE_SUCCESS,true);
		}else {
			return RespJRso.d(RespJRso.CODE_FAIL,false);
		}
		
	}
	
	private void initAccount() {
		for(String js : JSACTS) {
			ActInfoJRso ai = JsonUtils.getIns().fromJson(js, ActInfoJRso.class);
			accounts.put(ai.getActName(), ai);
			id2ActName.put(ai.getId(), ai.getActName());
		}
	}
	
	private String[] JSACTS= {
			"{" + 
			"    \"id\" : 0," + 
			"    \"actName\" : \"jmicro\"," + 
			"    \"admin\" : true," + 
			"    \"email\" : \"test@sina.com\"," + 
			"    \"guest\" : false,\r\n" + 
			"    \"lastActiveTime\" : 0," + 
			"    \"lastLoginTime\" : 1618825388425," + 
			"    \"loginNum\" : 0," + 
			"    \"mobile\" : \"\"," + 
			"    \"pwd\" : \"0\"," + 
			"    \"registTime\" : 0," + 
			"    \"statuCode\" : 2," + 
			"    \"token\" : \"\"," + 
			"    \"tokenType\" : 0," + 
			"    \"createdTime\" :0," + 
			"    \"updatedTime\" : 0" + 
			"}",
			"{" + 
			"    \"id\" : 25500," + 
			"    \"actName\" : \"test00\"," + 
			"    \"admin\" : false," + 
			"    \"email\" : \"test00@sina.com\"," + 
			"    \"guest\" : false,\r\n" + 
			"    \"lastActiveTime\" : 0," + 
			"    \"lastLoginTime\" : 1618825388425," + 
			"    \"loginNum\" : 0," + 
			"    \"mobile\" : \"\"," + 
			"    \"pwd\" : \"1\"," + 
			"    \"registTime\" : 0," + 
			"    \"statuCode\" : 2," + 
			"    \"token\" : \"\"," + 
			"    \"tokenType\" : 0," + 
			"    \"createdTime\" :0," + 
			"    \"updatedTime\" : 0" + 
			"}",
			"{" + 
			"    \"id\" : 25501," + 
			"    \"actName\" : \"test01\"," + 
			"    \"admin\" : false," + 
			"    \"email\" : \"test01@sina.com\"," + 
			"    \"guest\" : false,\r\n" + 
			"    \"lastActiveTime\" : 0," + 
			"    \"lastLoginTime\" : 1618825388425," + 
			"    \"loginNum\" : 0," + 
			"    \"mobile\" : \"\"," + 
			"    \"pwd\" : \"1\"," + 
			"    \"registTime\" : 0," + 
			"    \"statuCode\" : 2," + 
			"    \"token\" : \"\"," + 
			"    \"tokenType\" : 0," + 
			"    \"createdTime\" :0," + 
			"    \"updatedTime\" : 0" + 
			"}"
	};

	@Override
	public IPromise<RespJRso<ActInfoJRso>> loginWithClientToken(Integer loginClientId,String token) {
		return null;
	}

	@Override
	public IPromise<RespJRso<ActInfoJRso>> loginByWeixin(Integer loginClientId,String code, int shareUserId) {
		return null;
	}

	@Override
	public IPromise<RespJRso<Boolean>> bindMobile(String mobile, String vcode) {
		return null;
	}

	@Override
	public IPromise<RespJRso<Boolean>> bindMail(String mail, String vcode,String codeId) {
		return null;
	}

	@Override
	public IPromise<RespJRso<Boolean>> submitAttachmentInfo(String metadata, Byte type, 
			String fileId, String vcode, String vcodeId,String remark) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPromise<RespJRso<Map<String, Object>>> userInfo(Integer uid, String verifyCode) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
