package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.Permission;
import cn.jmicro.api.security.genclient.IAccountService$JMAsyncClient;
import cn.jmicro.api.service.IServiceAsyncResponse;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Md5Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.LocalAccountManager;
import cn.jmicro.mng.api.IMngAccountService;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false)
public class MngAccountServiceImpl implements IMngAccountService {

	private final Logger logger = LoggerFactory.getLogger(MngAccountServiceImpl.class);
	
	@Inject
	private LocalAccountManager lam;
	
	@Inject
	private AccountManager am;

	@Reference(namespace="*",version="*")
	private IAccountService$JMAsyncClient as;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	public ActInfo login(String actName, String pwd) {
		return am.login(actName, pwd);
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
	@SMethod(maxSpeed=100)
	public Resp<ActInfo> getAccount(String loginKey) {
		ActInfo ai = am.getAccount(loginKey);
		Resp<ActInfo> r = new Resp<>();
		r.setCode(0);
		r.setData(ai);
		return r;
	}
	
	@Override
	@SMethod(maxSpeed=10)
	public Resp<Boolean> regist(String actName, String pwd) {
		
		if(StringUtils.isEmpty(pwd)) {
			Resp<Boolean> r = new Resp<>();
			r.setCode(1);
			r.setData(false);
			r.setKey("PwdIsNull");
			r.setMsg("New password cannot be null!");
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
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.registJMAsync(actName,pwd).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				r = null;
			} else {
				return as.regist(actName,pwd);
			}
		} else {
			r.setCode(0);
			String p = Config.AccountDir +"/"+ ai.getActName();
			ai.setEnable(true);
			ai.setClientId(this.idGenerator.getIntId(ActInfo.class));
			if(op.exist(p)) {
				r.setCode(1);
				r.setData(false);
				r.setMsg("Account exist");
				r.setKey("accountExist");
			} else {
				op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
				r.setCode(0);
				r.setData(true);
			}
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
		
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.updatePwdJMAsync(newPwd, oldPwd).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				r = null;
			} else {
				return as.updatePwd(newPwd,oldPwd);
			}
		} else {
			ai.setPwd(newPwd);
			String p = Config.AccountDir +"/"+ ai.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
			r.setCode(0);
			r.setData(true);
		}
		
		return r;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Boolean> changeAccountStatus(String actName,boolean enableStatus) {
		Resp<Boolean> r = new Resp<>();
		
		if(StringUtils.isEmpty(actName)) {
			r.setCode(1);
			r.setData(false);
			r.setKey("ActNameIsNull");
			r.setMsg("Account name cannot be null!");
			return r;
		}
		
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.changeAccountStatusJMAsync(actName, enableStatus).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				r = null;
			} else {
				return as.changeAccountStatus( actName, enableStatus);
			}
		} else {
			 r = new Resp<>();
			ActInfo ai = am.getAccountFromZK(actName);
			if(ai == null) {
				r.setCode(1);
				r.setData(false);
				r.setMsg("Account not exist");
				r.setKey("accountExist");
				return r;
			}
			
			if(ai.isEnable() == enableStatus) {
				r.setCode(0);
				r.setData(true);
				return r;
			}
			
			ai.setEnable(enableStatus);
			
			String p = Config.AccountDir +"/"+ ai.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
			r.setCode(0);
			r.setData(true);
			return r;
		}
		
		return r;
	}
	
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=2048)
	public Resp<Boolean> updateActPermissions(String actName,Set<String> adds,Set<String> dels) {
		
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.updatePermissionsJMAsync(actName,adds,dels).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
			    return null;
			} else {
				return as.updatePermissions(actName,adds,dels);
			}
		} else {
			Resp<Boolean> r = new Resp<>();
			ActInfo ai = am.getAccountFromZK(actName);
			if(ai == null) {
				r.setCode(1);
				r.setData(false);
				r.setMsg("Account not exist");
				r.setKey("accountExist");
				return r;
			}
			
			Set<String> pers = ai.getPers();
			if(pers == null) {
				pers = new HashSet<>();
				ai.setPers(pers);
			}
			
			if(dels != null && !dels.isEmpty()) {
				pers.removeAll(dels);
			}
			
			if(adds != null && !adds.isEmpty()) {
				pers.addAll(adds);
			}
			
			String p = Config.AccountDir +"/"+ ai.getActName();
			op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
			r.setCode(0);
			r.setData(true);
			return r;
			
		}
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Integer> countAccount(Map<String, String> queryConditions) {
		
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.countAccountJMAsync(queryConditions).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				return null;
			} else {
				return as.countAccount(queryConditions);
			}
		} else {
			Set<String> acts = op.getChildren(Config.AccountDir, false);
			Resp<Integer> r = new Resp<Integer>();
			r.setCode(0);
			r.setData(acts.size());
			return r;
		}
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<ActInfo>> getAccountList(Map<String, String> queryConditions, int pageSize, int curPage) {
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.getAccountListJMAsync(queryConditions,pageSize,curPage).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Boolean> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(false);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				return null;
			} else {
				return as.getAccountList(queryConditions,pageSize,curPage);
			}
		} else {
			List<ActInfo> rst = new ArrayList<>();
			Set<String> acts = op.getChildren(Config.AccountDir, false);
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
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=2048)
	public Resp<Map<String, Set<Permission>>> getPermissionsByActName(String actName) {
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.getPermissionsByActNameJMAsync(actName).then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Map<String, Set<Permission>>> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(null);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				return null;
			} else {
				return as.getPermissionsByActName(actName);
			}
		} else {
			Resp<Map<String, Set<Permission>>> rst = new Resp<>();
			rst.setCode(0);
			rst.setData(lam.getPermissionByAccountName(actName));
			return rst;
		}
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Map<String, Set<Permission>>> getAllPermissions() {
		if(as != null && as.isReady()) {
			JMicroContext cxt = JMicroContext.get();
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			if(cxt.isAsync() && cb == null) {
				logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
			}
			if(cb != null) {
				as.getAllPermissionsJMAsync().then((rst,fail,rcxt)->{
					if(fail == null) {
						cb.result(rst);
					} else {
						Resp<Map<String, Set<Permission>>> rr = new Resp<>();
						rr.setCode(1);
						rr.setData(null);
						rr.setMsg(fail.toString());
						cb.result(rr);
					}
				});
				//异步请求同步返回NULL
				return null;
			} else {
				return as.getAllPermissions();
			}
		} else {
			Resp<Map<String, Set<Permission>>> rst = new Resp<>();
			rst.setCode(0);
			rst.setData(lam.getServiceMethodPermissions());
			return rst;
		}
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
