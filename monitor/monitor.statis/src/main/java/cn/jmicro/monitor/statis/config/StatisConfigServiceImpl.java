package cn.jmicro.monitor.statis.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.monitor.StatisIndexJRso;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.monitor.statis.api.IStatisConfigServiceJMSrv;

@Component
@Service(version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class StatisConfigServiceImpl implements IStatisConfigServiceJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(StatisConfigServiceImpl.class);
	
	private static final String ROOT = StatisConfigJRso.STATIS_CONFIG_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public RespJRso<List<StatisConfigJRso>> query() {
		RespJRso<List<StatisConfigJRso>> r = new RespJRso<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		List<StatisConfigJRso> ll = new ArrayList<>();
		r.setData(ll);
		
		boolean isAdmin = PermissionManager.isCurAdmin(Config.getClientId());
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			StatisConfigJRso lw = JsonUtils.getIns().fromJson(data, StatisConfigJRso.class);
			
			if(lw != null) {
				if(isAdmin || PermissionManager.checkAccountClientPermission(lw.getCreatedBy())) {
					ll.add(lw);
				}
			}
		}
		
		return r;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public RespJRso<Boolean> enable(Integer id) {
		
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + id;
		String data = op.getData(path);
		
		if(Utils.isEmpty(data)) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("更新配置已经不存在");
			return r;
		}
		
		StatisConfigJRso lw = JsonUtils.getIns().fromJson(data, StatisConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getCreatedBy()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to enable statis monitor config: " + lw.getId()+", target clientId: " + lw.getCreatedBy());
			LG.log(MC.LOG_WARN, this.getClass(), r.getMsg());
			return r;
		}
		
		if(!lw.isEnable()) {
			//从禁用到启用需要检测数据合法性
			RespJRso<Boolean> rr = this.checkAndSet(lw);
			if(rr != null) {
				return rr;
			}
		}
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			lw.setToParams(StatisConfigJRso.DEFAULT_DB);
		}else if(Utils.isEmpty(lw.getToParams())) {
			lw.setToParams(StatisConfigJRso.DEFAULT_DB);
		}
		
		lw.setEnable(!lw.isEnable());
		
		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096,downSsl=true,encType=0,upSsl=true)
	public RespJRso<Boolean> update(StatisConfigJRso cfg) {
		
		RespJRso<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			return rr;
		}
		
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + cfg.getId();
		String data = op.getData(path);
		
		if(Utils.isEmpty(data)) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("更新配置已经不存在");
			return r;
		}
		
		StatisConfigJRso lw = JsonUtils.getIns().fromJson(data, StatisConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getCreatedBy()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to update statis config: " + lw.getId()+", clientId: " + lw.getCreatedBy());
			LG.log(MC.LOG_WARN, this.getClass(), r.getMsg());
			return r;
		}
		
		if(lw.isEnable()) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("启用中的配置不能更新");
			return r;
		}
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			
			if(cfg.getToType() == StatisConfigJRso.TO_TYPE_CONSOLE ||
					cfg.getToType() == StatisConfigJRso.TO_TYPE_FILE) {
				r.setCode(RespJRso.CODE_FAIL);
				r.setData(false);
				r.setMsg("无权限使用此种目标类型");
				return r;
			}
			
			cfg.setToParams(StatisConfigJRso.DEFAULT_DB);
		}else if(Utils.isEmpty(cfg.getToParams())) {
			cfg.setToParams(StatisConfigJRso.DEFAULT_DB);
		}
		
		lw.setByKey(cfg.getByKey());
		lw.setByType(cfg.getByType());
		lw.setActName(cfg.getActName());
		lw.setStatisIndexs(cfg.getStatisIndexs());
		lw.setTimeCnt(cfg.getTimeCnt());
		lw.setTimeUnit(cfg.getTimeUnit());
		lw.setToParams(cfg.getToParams());
		lw.setToType(cfg.getToType());
		//lw.setEnable(cfg.isEnable());
		lw.setTag(cfg.getTag());
		lw.setExpStr(cfg.getExpStr());
		lw.setCounterTimeout(cfg.getCounterTimeout());
		lw.setNamedType(cfg.getNamedType());
		lw.setMinNotifyTime(cfg.getMinNotifyTime());
		lw.setExpStr1(cfg.getExpStr1());

		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		return r;
		
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public RespJRso<Boolean> delete(int id) {
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + id;
		if(op.exist(path)) {
			
			String data = op.getData(path);
			StatisConfigJRso lw = JsonUtils.getIns().fromJson(data, StatisConfigJRso.class);
			
			if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getCreatedBy()))) {
				r.setCode(RespJRso.CODE_NO_PERMISSION);
				r.setData(false);
				r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to delete statis config: " + lw.getId()+", target clientId: " + lw.getCreatedBy());
				LG.log(MC.LOG_WARN, this.getClass(), r.getMsg());
				return r;
			}
			
			op.deleteNode(path);
		}
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public RespJRso<StatisConfigJRso> add(StatisConfigJRso cfg) {
		RespJRso<StatisConfigJRso> r = new RespJRso<>();
		
		RespJRso<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			r.setCode(rr.getCode());
			r.setMsg(rr.getMsg());
			return r;
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			if(cfg.getToType() == StatisConfigJRso.TO_TYPE_CONSOLE ||
					cfg.getToType() == StatisConfigJRso.TO_TYPE_FILE) {
				r.setCode(RespJRso.CODE_FAIL);
				r.setMsg("无权限使用此种目标类型");
				return r;
			}
			cfg.setClientId(ai.getClientId());
		}
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			cfg.setToParams(StatisConfigJRso.DEFAULT_DB);
		}else if(Utils.isEmpty(cfg.getToParams())) {
			cfg.setToParams(StatisConfigJRso.DEFAULT_DB);
		}
		
		cfg.setCreatedBy(ai.getId());
		cfg.setId(this.idGenerator.getIntId(StatisConfigJRso.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

	private RespJRso<Boolean> checkAndSet(StatisConfigJRso cfg) {
		
		RespJRso<Boolean> r = new RespJRso<>();
		
		String msg = checkConfig(cfg);
		if(msg != null) {
			r.setMsg(msg);
			r.setCode(1);
			r.setData(false);
			return r;
		}
		
		if(cfg.getByType() <= 0 || cfg.getByType() > StatisConfigJRso.BY_TYPE_ACCOUNT) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计类型不能为空");
			return r;
		}
		
		if(Utils.isEmpty(cfg.getByKey())) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计键值不能为空");
			return r;
		}
		
		if(cfg.getStatisIndexs() == null || cfg.getStatisIndexs().length == 0) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计指标不能为空");
			return r;
		}
		
		if(cfg.getToType() <= 0 || cfg.getToType() > StatisConfigJRso.TO_TYPE_MESSAGE+10) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("目标类型不能为空");
			return r;
		}
		
		if(Utils.isEmpty(cfg.getTimeUnit())) {
			cfg.setTimeUnit(StatisConfigJRso.UNIT_MU);
		}
		
		if(cfg.getTimeCnt() <= 0) {
			cfg.setTimeCnt(1);
		}
		
		return null;
	}
	
	public String checkConfig(StatisConfigJRso lw) {
		String msg = checkByType(lw);
		if(msg != null) {
			return msg;
		}
		
		msg = checkToType(lw);
		if(msg != null) {
			return msg;
		}
		
		if(lw.getStatisIndexs() == null || lw.getStatisIndexs().length == 0) {
			 msg = "Statis index cannot be null for config id: " + lw.getId();
			logger.error(msg);
			LG.logWithNonRpcContext(MC.LOG_ERROR, StatisConfigServiceImpl.class, msg,MC.MT_DEFAULT,true);
			return msg;
		}
		
		StatisIndexJRso[] sis = lw.getStatisIndexs();
		
		for(int i = 0; i < sis.length; i++ ) {
			StatisIndexJRso si = sis[i];
            if(si.getType() < StatisConfigJRso.PREFIX_TOTAL || si.getType() > StatisConfigJRso.PREFIX_CUR_PERCENT) {
            	msg = "统计指标类型不合法" +  si.getType();
            	return msg;
            }

            if(Utils.isEmpty(si.getName())) {
            	msg = "统计指标名称不能为空";
            	return msg;
            }

            if(si.getNums() == null || si.getNums().length == 0) {
            	msg  = "统计指标分子值不能为空";
            	return msg;
            }

            if((si.getType()== 2 || si.getType() == 5) && (si.getDens() == null || si.getDens().length == 0)) {
            	msg =  "统计指标分母值不能为空";
            	return msg;
            }

        }
		
		if(!Utils.isEmpty(lw.getNamedType())) {
			String p = Config.getRaftBasePath(Config.NamedTypesDir)+"/"+lw.getNamedType();
			if(!op.exist(p)) {
				 msg = "NamedType ["+lw.getNamedType()+"] not exist for config id: " + lw.getId();
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_ERROR, StatisConfigServiceImpl.class, msg,MC.MT_DEFAULT,true);
				return msg;
			}
		}
		
		if(!Utils.isEmpty(lw.getExpStr())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr());
			if(!ExpUtils.isValid(suffixExp)) {
				 msg = "Invalid exp0: " + lw.getId() + "---> " + lw.getExpStr();
			}
		}
		
		if(!Utils.isEmpty(lw.getExpStr1())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr1());
			if(!ExpUtils.isValid(suffixExp)) {
				 msg = "Invalid exp1: " + lw.getId() + " ---> " + lw.getExpStr1();
			}
		}
		
		return msg;
	}

	private String checkToType(StatisConfigJRso lw) {
		String msg = null;
		try {
			if(lw.getToType() <= 0) {
				msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
				return msg;
			}
			
			if(StatisConfigJRso.TO_TYPE_SERVICE_METHOD == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To key params cannot be null for service [" + StatisConfigJRso.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
					return msg;
				}
				
				String[] ps = lw.getToParams().split(UniqueServiceKeyJRso.SEP);
				if(ps == null || ps.length < 7) {
					msg = "To param ["+lw.getToParams()+"] invalid [" + StatisConfigJRso.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
					return msg;
				}
				
				boolean suc = checkByService(lw.getId(),ps);
				if(!suc) {
					return msg;
				}
				
				if(Utils.isEmpty(ps[6])) {
					msg = "To service method cannot be NULL for id: " + lw.getId();
					return msg;
				}
				
				lw.setToSn(ps[0]);
				lw.setToNs(ps[1]);
				lw.setToVer(ps[2]);
				lw.setToMt(ps[6]);
				
			}else if(StatisConfigJRso.TO_TYPE_DB == lw.getToType()) {
				lw.setToParams(StatisConfigJRso.DEFAULT_DB);
			}else if(StatisConfigJRso.TO_TYPE_FILE == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To file cannot be NULL for id: " + lw.getId();
					return msg;
				}
			}else if(StatisConfigJRso.TO_TYPE_MONITOR_LOG == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "Tag cannot be null: " + lw.getId();
					return msg;
				}
			}else if(StatisConfigJRso.TO_TYPE_MESSAGE == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "Message topic cannot be null: " + lw.getId();
					return msg;
				}
			}
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, StatisConfigServiceImpl.class, msg,MC.MT_DEFAULT,true);
			}
		}
		
		return msg;
	}
	
	private boolean checkByService(int cfgId, String[] srvs) {
		String msg = null;
		try {
			if(Utils.isEmpty(srvs[0])) {
				msg = "By service name cannot be NULL for id: " + cfgId;
				return false;
			}
			
			if(Utils.isEmpty(srvs[1])) {
				msg = "By namespace cannot be NULL for id: " + cfgId;
				return false;
			}
			
			if(Utils.isEmpty(srvs[2])) {
				msg = "By version cannot be NULL for id: " + cfgId;
				return false;
			}
			
			return true;
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, StatisConfigServiceImpl.class, msg,MC.MT_DEFAULT,true);
			}
		}
		
	}
	
	private String checkByType(StatisConfigJRso lw) {
		boolean suc = false;
		int tt = lw.getByType();
		String msg = null;
		
		try {
			if(tt <= 0) {
				msg = "By type value cannot be null for: " + lw.getId();
			} else {
				switch(tt) {
					case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
					case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
					case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
					{
						String[] srvs = lw.getByKey().split(UniqueServiceKeyJRso.SEP);
						if(srvs.length < 3) {
							msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
							return msg;
						}
						
						suc = checkByService(lw.getId(),srvs);
						
						if(suc) {
							 lw.setBysn(srvs[UniqueServiceKeyJRso.INDEX_SN]);
							 lw.setByns(srvs[UniqueServiceKeyJRso.INDEX_NS]);
							 lw.setByver(srvs[UniqueServiceKeyJRso.INDEX_VER]);
							 lw.setByins(srvs[UniqueServiceKeyJRso.INDEX_INS]);
							 lw.setByme(srvs[UniqueServiceKeyJRso.INDEX_METHOD]);
							
							switch(tt) {
							case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
								if(srvs.length < 4 || Utils.isEmpty(srvs[3])) {
									msg = "By instance name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								
								if(Utils.isEmpty(lw.getActName())) {
									msg = "By account name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							}
						}
						break;
					}
					case StatisConfigJRso.BY_TYPE_INSTANCE:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By instance name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					case StatisConfigJRso.BY_TYPE_ACCOUNT:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By account name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					/*case StatisConfigJRso.BY_TYPE_EXP:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "Expression cannot be NULL for id: " + lw.getId();
							return msg;
						}
						
						if(!ExpUtils.isValid(lw.getByKey())) {
							msg = "Expression is invalid for id: " + lw.getId();
							return msg;
						}
						break;*/
				}
			}
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, StatisConfigServiceImpl.class, msg,MC.MT_DEFAULT,true);
			}
		}
		
		return msg;
	}
}
