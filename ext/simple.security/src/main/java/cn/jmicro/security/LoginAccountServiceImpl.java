package cn.jmicro.security;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,debugMode=1,
showFront=false,clientId=Constants.NO_CLIENT_ID)
public class LoginAccountServiceImpl implements IAccountService {

	private final Logger logger = LoggerFactory.getLogger(LoginAccountServiceImpl.class);
	
	private Map<String,ActInfo> accounts = new HashMap<>();
	
	private Map<Integer,String> id2ActName = new HashMap<>();
	
	@Inject
	private AccountManager am;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICache cache;
	
	public void ready() {
		initAccount();
	}
	
	@Override
	public Resp<ActInfo> login(String actName, String pwd) {

		Resp<ActInfo> r = new Resp<ActInfo>();
		ActInfo ai = accounts.get(actName);
		if(ai == null) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("Account invalid now!");
			return r;
		}
		
		if(ai.getPwd().equals(pwd)) {
			String akey = key(ai.getActName());
			
			String oldLk = null;
			if(cache.exist(akey)) {
				oldLk = cache.get(akey);
			}
			
			if(Utils.isEmpty(oldLk)) {
				int seed = HashUtils.FNVHash1(TimeUtils.getCurTime() + "_" + this.idGenerator.getStringId(ActInfo.class));
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
			ai.setAdmin(ai.getId() == Config.getAdminClientId());
			
			cache.put(ai.getLoginKey(), ai,1800000);
			cache.put(akey, oldLk,1800000);
			cache.put(key(ai.getId()+""),oldLk,1800000);
			
			ai.setAdmin(ai.getId() == Config.getAdminClientId());
			r.setCode(Resp.CODE_SUCCESS);
			r.setData(ai);
			return r;
		} else {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("Account not exist or password error!");
			return r;
		}
	}

	@Override
	public Resp<ActInfo> loginWithId(int id, String pwd) {
		if(id2ActName.containsKey(id)) {
			return login(this.id2ActName.get(id),pwd);
		} else {
			Resp<ActInfo> r = new Resp<ActInfo>(Resp.CODE_FAIL,"Account not found!");
			return r;
		}
	}
	
	@Override
	//@SMethod(forType=Constants.FOR_TYPE_SYS)
	public Resp<Boolean> hearbeat(String loginKey) {
		ActInfo ai = am.getAccount(loginKey);
		if(ai != null) {
			return new Resp<>(Resp.CODE_SUCCESS,true);
		}else {
			return new Resp<>(Resp.CODE_FAIL,false);
		}
		
	}
	
	private void initAccount() {
		for(String js : JSACTS) {
			ActInfo ai = JsonUtils.getIns().fromJson(js, ActInfo.class);
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
	
}
