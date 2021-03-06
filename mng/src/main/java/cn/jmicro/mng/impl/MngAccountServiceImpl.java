package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.email.genclient.IEmailSender$JMAsyncClient;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
import cn.jmicro.api.security.Permission;
import cn.jmicro.api.utils.SystemUtils;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.Md5Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.LocalAccountManager;

//@Component
//@Service(namespace="mng", version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false)
public class MngAccountServiceImpl implements IAccountService {

	private final Logger logger = LoggerFactory.getLogger(MngAccountServiceImpl.class);
	
	@Cfg(value="/accountActivePage",defGlobal=true)
	private String accountActivePage="http://192.168.56.1:9090/activeAccount.html?";
	
	private static final String EMAIL_CHECKER = "\\w+(\\.\\w)*@\\w+(\\.\\w{2,3}){1,3}";
	
	private static final Pattern HK_PATTERN = Pattern.compile("^(5|6|8|9)\\d{7}$");
    private static final Pattern CHINA_PATTERN = Pattern.compile("^((13[0-9])|(14[0,1,4-9])|(15[0-3,5-9])|(16[2,5,6,7])|(17[0-8])|(18[0-9])|(19[0-3,5-9]))\\d{8}$");
	private static final Pattern NUM_PATTERN = Pattern.compile("[0-9]+");
	
	private Random r = new Random();
	
	@Reference
	private IEmailSender$JMAsyncClient mailSender;
	
	@Inject
	private LocalAccountManager lam;
	
	@Inject
	private AccountManager am;

	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	public Resp<ActInfo> login(String actName, String pwd) {
		return am.login(actName, pwd);
	}

	@Override
	public Resp<ActInfo> loginWithId(int id, String pwd) {
		return null;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=3)
	public Resp<Boolean> logout() {
		ActInfo ai = JMicroContext.get().getAccount();
		Resp<Boolean> r = new Resp<>();
		if(ai != null) {
			Boolean rst = am.logout(ai.getLoginKey());
			r.setData(rst);
		}
		r.setCode(0);
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(maxSpeed=100)
	public Resp<Boolean> isLogin(String loginKey) {
		Boolean rst = am.isLogin(loginKey);
		Resp<Boolean> r = new Resp<>();
		r.setCode(0);
		r.setData(rst);
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=1)
	public Resp<Boolean> checkAccountExist(String actName) {
		Resp<Boolean> r = new Resp<>();
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(am.existActName(actName));
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=1)
	public Resp<Boolean> resetPwdEmail(String actName,String checkCode) {
		Resp<Boolean> r = new Resp<>();
		boolean rst = am.existActName(actName);
		if(!rst) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account not exist");
			r.setKey("accountExist");
			return r;
		}
		
		ActInfo ai = am.getAccountFromZK(actName);
		
		if(ActInfo.TOKEN_INVALID == ai.getTokenType()) {
			ai.setToken(SystemUtils.getRandomStr(6));
			ai.setTokenType(ActInfo.TOKEN_RESET_PWD);
			String p = AccountManager.ActDir + "/" + actName;
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		}
		
		if(ActInfo.TOKEN_RESET_PWD == ai.getTokenType()) {
			StringBuffer sb = new StringBuffer();
			sb.append("JMicro密码重置校验码如下：\n")
			.append(ai.getToken());
			//.send(ai.getEmail(), "密码重置", sb.toString())
			this.mailSender.sendJMAsync(ai.getEmail(), "密码重置", sb.toString(), ai)
			.success((succe,cxt)->{
				logger.debug("reset pwd email send success: " + ai.getActName());
			})
			.fail((code,msg,cxt)->{
				logger.error("reset pwd email send fail: " + ai.getActName());
			});
			
			r.setCode(Resp.CODE_SUCCESS);
			r.setData(true);
		} else {
			r.setMsg("Invalid account status!");
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
		}
		
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=10)
	public Resp<Boolean> resetPwd(String actName, String token, String newPwd) {
		Resp<Boolean> r = new Resp<>();
		ActInfo ai = this.am.getAccountFromZK(actName);
		if(ai == null || StringUtils.isEmpty(actName) || StringUtils.isEmpty(token) 
				|| StringUtils.isEmpty(ai.getToken())|| !token.equals(ai.getToken()) 
				|| ai.getStatuCode() != ActInfo.SC_ACTIVED 
				|| ai.getTokenType() != ActInfo.TOKEN_RESET_PWD) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Invalid account or status!");
			return r;
		}
		
		logger.warn("Reset account: " + actName+" password");
		String p = AccountManager.ActDir + "/" + ai.getActName();
		ai.setPwd(Md5Utils.getMd5(newPwd));
		ai.setToken("");
		ai.setTokenType(ActInfo.TOKEN_INVALID);
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(true);
		return r;
	}
	

	@Override
	@SMethod(maxSpeed=100)
	public Resp<ActInfo> getAccount(String loginKey) {
		ActInfo ai = am.getAccount(loginKey);
		Resp<ActInfo> r = new Resp<>();
		r.setCode(0);
		r.setData(ai);
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=1)
	public Resp<Boolean> activeAccount(String actName, String token) {
		Resp<Boolean> r = new Resp<>();
		ActInfo ai = this.am.getAccountFromZK(actName);
		if(ai == null || StringUtils.isEmpty(actName) || StringUtils.isEmpty(token) 
				|| StringUtils.isEmpty(ai.getToken())|| !token.equals(ai.getToken()) 
				|| ai.getStatuCode() != ActInfo.SC_WAIT_ACTIVE) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Invalid info!");
			return r;
		}
		
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		ai.setStatuCode(ActInfo.SC_ACTIVED);
		ai.setToken("");
		ai.setTokenType(ActInfo.TOKEN_INVALID);
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=1)
	public Resp<Boolean> regist(String actName, String pwd,String mail,String mobile) {
		
		if(StringUtils.isEmpty(mail)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setKey("MailIsNull");
			r.setMsg("Mail address cannot be null!");
			return r;
		}
		
		mail = mail.trim();
		if(!mail.matches(EMAIL_CHECKER)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setKey("MailFormatInvalid");
			r.setMsg("Mail address format is invalid!");
			return r;
		}
		
		if(StringUtils.isEmpty(mobile)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setKey("MobileIsNull");
			r.setMsg("Mobile number cannot be null!");
			return r;
		}
		
		mobile = mobile.trim();
		Matcher m = CHINA_PATTERN.matcher(mobile);
		if(!m.matches()) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setKey("MobileFormatInvalid");
			r.setMsg("Mobile number is invalid!");
			return r;
		}
		
		if(StringUtils.isEmpty(pwd)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setKey("PwdIsNull");
			r.setMsg("New password cannot be null!");
			return r;
		}
		
		if(am.checkEmailExist(mail)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setKey("mailHaveRegist");
			r.setMsg("Your mail "+mail+" have been registed!");
			return r;
		}
		
		mobile = mobile.trim();
		if(am.checkMobileExist(mobile)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setKey("mobileHaveRegist");
			r.setMsg("Your mobile " +mobile+" have been registed!");
			return r;
		}
		
		
		ActInfo ai = new ActInfo();
		ai.setActName(actName);
		ai.setPwd(Md5Utils.getMd5(pwd));
		
		Resp<Boolean> r = checkAccountParam(ai);
		if(r != null) {
			return r;
		}
		
		r = new Resp<>();

		r.setCode(0);
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		if(op.exist(p)) {
			r.setCode(1);
			r.setData(false);
			r.setMsg("Account exist");
			r.setKey("accountExist");
		} else {
			ai.setStatuCode(ActInfo.SC_WAIT_ACTIVE);
			ai.setId(this.idGenerator.getIntId(ActInfo.class));
			ai.setEmail(mail);
			ai.setMobile(mobile);
			ai.setRegistTime(TimeUtils.getCurTime());
			
			String token = this.idGenerator.getStringId(ActInfo.class);
			token = Md5Utils.getMd5(token);
			ai.setToken(token);
			ai.setTokenType(ActInfo.TOKEN_ACTIVE_ACT);
			
			op.createNodeOrSetData(AccountManager.MobileDir+"/" + mobile, ai.getActName(), IDataOperator.PERSISTENT);
			op.createNodeOrSetData(AccountManager.EmailDir+"/" + mail, ai.getActName(), IDataOperator.PERSISTENT);
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
			
			r.setCode(0);
			r.setData(true);
			
			StringBuffer sb = new StringBuffer();
			sb.append("点击以下连接完成激活你的账号：\n").append(this.accountActivePage)
			.append("a=").append(actName)
			.append("&t=").append(token)
			.append("&y=").append(ai.getTokenType());
			
			//this.mailSender.send(mail, "账号激活", sb.toString());
			
			this.mailSender.sendJMAsync(ai.getEmail(), "账号激活", sb.toString(), ai)
			.success((succe,cxt)->{
				logger.debug("active account email send success: " + ai.getActName());
			})
			.fail((code,msg,cxt)->{
				logger.error("active account email send fail: " + ai.getActName());
			});
			
		}
	
		return r;
	}
	
	public Resp<Boolean> resendActiveEmail(String actName) {
		ActInfo ai = this.am.getAccountFromZK(actName);
		Resp<Boolean> r = new Resp<>();
		if(ai == null) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account exist");
			r.setKey("accountExist");
			return r;
		}
		
		if(ai.getStatuCode() == ActInfo.SC_ACTIVED) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account is active!");
			r.setKey("accountIsActive");
			return r;
		}
		
		if(ai.getStatuCode() == ActInfo.SC_WAIT_ACTIVE) {
			StringBuffer sb = new StringBuffer();
			sb.append("点击以下连接完成重置你的JMicro账号：\n").append(this.accountActivePage)
			.append("a=").append(actName)
			.append("&t=").append(ai.getToken())
			.append("&y=").append(ai.getTokenType());
			//this.mailSender.send(ai.getEmail(), "账号激活", sb.toString());
			this.mailSender.sendJMAsync(ai.getEmail(), "密码重置", sb.toString(), ai)
			.success((succe,cxt)->{
				logger.debug("Resend active account email send success: " + ai.getActName());
			})
			.fail((code,msg,cxt)->{
				logger.error("Resend active account email send fail: " + ai.getActName());
			});
		}else {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account statu exception!");
			r.setKey("accountStatuIsInvalid");
			return r;
		}
		
		return r;
	}
	
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5)
	public Resp<Boolean> updatePwd(String newPwd,String oldPwd) {
		Resp<Boolean> r = new Resp<>();
		ActInfo ai = JMicroContext.get().getAccount();
		
		if(ai == null) {
			r.setCode(1);
			r.setData(false);
			r.setKey("NotLogin");
			r.setMsg("Have to login before update password!");
			return r;
		}
		
		if(StringUtils.isEmpty(newPwd)) {
			r.setCode(1);
			r.setData(false);
			r.setKey("PwdIsNull");
			r.setMsg("New password cannot be null!");
			return r;
		}
		
		if(StringUtils.isEmpty(oldPwd)) {
			r.setCode(1);
			r.setData(false);
			r.setKey("PwdIsNull");
			r.setMsg("Old password cannot be null!");
			return r;
		}
		
		if(!(ai.getPwd().equals(oldPwd) || Md5Utils.getMd5(oldPwd).equals(ai.getPwd()))) {
			r.setCode(1);
			r.setData(false);
			r.setKey("PwdIsNotValid");
			r.setMsg("Old password is invalid!");
			return r;
		}
		

		ai.setPwd(Md5Utils.getMd5(newPwd));
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		r.setCode(0);
		r.setData(true);
		
		return r;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Boolean> changeAccountStatus(String actName) {
		Resp<Boolean> r = new Resp<>();
		
		if(StringUtils.isEmpty(actName)) {
			r.setCode(1);
			r.setData(false);
			r.setKey("ActNameIsNull");
			r.setMsg("Account name cannot be null!");
			return r;
		}
		
		if(actName.equals(JMicroContext.get().getAccount().getActName())) {
			r.setCode(1);
			r.setData(false);
			r.setKey("CannotFreezeCurrentLoginAccount");
			r.setMsg("Can Not Freeze Current Login Account");
			return r;
		}
		

		r = new Resp<>();
		ActInfo ai = am.getAccountFromZK(actName);
		if(ai == null) {
			r.setCode(1);
			r.setData(false);
			r.setMsg("Account not exist");
			r.setKey("accountExist");
			return r;
		}
		
		if(ai.getStatuCode() == ActInfo.SC_WAIT_ACTIVE) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account wait for mail active!");
			return r;
		}
		
		if(ai.getStatuCode() == ActInfo.SC_FREEZE) {
			logger.warn("Account "+actName+" unfreeze by: " + JMicroContext.get().getAccount().getActName());
			ai.setStatuCode(ActInfo.SC_ACTIVED);
		}else if(ai.getStatuCode() == ActInfo.SC_ACTIVED) {
			logger.warn("Account "+actName+" freeze by: " + JMicroContext.get().getAccount().getActName());
			ai.setStatuCode(ActInfo.SC_FREEZE);
		} else {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Account statu not support this operation!");
			return r;
		}
		
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		r.setCode(0);
		r.setData(true);
	
		if(ai.getStatuCode() == ActInfo.SC_FREEZE) {
			//退出重机关报登陆
			am.forceAccountLogout(actName);
		}
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=2048)
	public Resp<Boolean> updateActPermissions(String actName,Set<Integer> adds,Set<Integer> dels) {
		
		Resp<Boolean> r = new Resp<>();
		ActInfo ai = am.getAccountFromZK(actName);
		if(ai == null) {
			r.setCode(1);
			r.setData(false);
			r.setMsg("Account not exist");
			r.setKey("accountExist");
			return r;
		}
		
		Set<Integer> pers = ai.getPers();
		/*if(pers == null) {
			pers = new HashSet<>();
			ai.setPers(pers);
		}*/
		
		if(dels != null && !dels.isEmpty()) {
			pers.removeAll(dels);
		}
		
		if(adds != null && !adds.isEmpty()) {
			pers.addAll(adds);
		}
		
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
		r.setCode(0);
		r.setData(true);
		return r;
	
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Integer> countAccount(Map<String, Object> queryConditions) {

		Set<String> acts = op.getChildren(AccountManager.ActDir, false);
		Resp<Integer> r = new Resp<Integer>();
		r.setCode(0);
		r.setData(acts.size());
		return r;
	
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<ActInfo>> getAccountList(Map<String, Object> queryConditions, int pageSize, int curPage) {

		List<ActInfo> rst = new ArrayList<>();
		Set<String> acts = op.getChildren(AccountManager.ActDir, false);
		for(String an : acts) {
			ActInfo ai = am.getAccountFromZK(an);
			if(ai != null) {
				rst.add(ai);
			}
		}
		Resp<List<ActInfo>> r = new Resp<>();
		r.setCode(0);
		r.setData(rst);
		return r;
	
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=2048)
	public Resp<Map<String, Set<Permission>>> getPermissionsByActName(String actName) {
		Resp<Map<String, Set<Permission>>> rst = new Resp<>();
		rst.setCode(0);
		rst.setData(lam.getPermissionByAccountName(actName));
		return rst;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Map<String, Set<Permission>>> getAllPermissions() {
		Resp<Map<String, Set<Permission>>> rst = new Resp<>();
		rst.setCode(0);
		rst.setData(lam.getServiceMethodPermissions());
		return rst;
	
	}
	
	@Override
	public Resp<String> getNameById(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	private Resp<Boolean> checkAccountParam(ActInfo ai) {
		
		if(StringUtils.isEmpty(ai.getActName())) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setMsg("Account name is NULL");
			r.setKey("accountIsNull");
			return r;
		}
		
		if(StringUtils.isEmpty(ai.getPwd())) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setMsg("Password is NULL");
			r.setKey("passwordIsNull");
			return r;
		}
		
		return null;
	}
	
}
