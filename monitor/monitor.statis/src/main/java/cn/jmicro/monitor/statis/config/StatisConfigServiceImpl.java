package cn.jmicro.monitor.statis.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.StatisIndex;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.monitor.statis.api.IStatisConfigService;

@Component
@Service(version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class StatisConfigServiceImpl implements IStatisConfigService {

	private final static Logger logger = LoggerFactory.getLogger(StatisConfigServiceImpl.class);
	
	private static final String ROOT = StatisConfig.STATIS_CONFIG_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public Resp<List<StatisConfig>> query() {
		Resp<List<StatisConfig>> r = new Resp<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		List<StatisConfig> ll = new ArrayList<>();
		r.setData(ll);
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			StatisConfig lw = JsonUtils.getIns().fromJson(data, StatisConfig.class);
			ll.add(lw);
		}
		
		return r;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public Resp<Boolean> enable(Integer id) {
		
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + id;
		String data = op.getData(path);
		
		if(Utils.isEmpty(data)) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("更新配置已经不存在");
			return r;
		}
		
		StatisConfig lw = JsonUtils.getIns().fromJson(data, StatisConfig.class);
		
		if(!lw.isEnable()) {
			//从禁用到启用需要检测数据合法性
			Resp<Boolean> rr = this.checkAndSet(lw);
			if(rr != null) {
				return rr;
			}
		}
		
		lw.setEnable(!lw.isEnable());
		
		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096,downSsl=true,encType=0,upSsl=true)
	public Resp<Boolean> update(StatisConfig cfg) {
		
		Resp<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			return rr;
		}
		
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + cfg.getId();
		String data = op.getData(path);
		
		if(Utils.isEmpty(data)) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("更新配置已经不存在");
			return r;
		}
		
		StatisConfig lw = JsonUtils.getIns().fromJson(data, StatisConfig.class);
		if(lw.isEnable()) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("启用中的配置不能更新");
			return r;
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
	public Resp<Boolean> delete(int id) {
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + id;
		if(op.exist(path)) {
			op.deleteNode(path);
		}
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<StatisConfig> add(StatisConfig cfg) {
		Resp<StatisConfig> r = new Resp<>();
		
		Resp<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			r.setCode(rr.getCode());
			r.setMsg(rr.getMsg());
			return r;
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		cfg.setCreatedBy(ai.getId());
		cfg.setId(this.idGenerator.getIntId(StatisConfig.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

	private Resp<Boolean> checkAndSet(StatisConfig cfg) {
		
		Resp<Boolean> r = new Resp<>();
		
		String msg = checkConfig(cfg);
		if(msg != null) {
			r.setMsg(msg);
			r.setCode(1);
			r.setData(false);
			return r;
		}
		
		if(cfg.getByType() <= 0 || cfg.getByType() > StatisConfig.BY_TYPE_ACCOUNT) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计类型不能为空");
			return r;
		}
		
		if(Utils.isEmpty(cfg.getByKey())) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计键值不能为空");
			return r;
		}
		
		if(cfg.getStatisIndexs() == null || cfg.getStatisIndexs().length == 0) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("统计指标不能为空");
			return r;
		}
		
		if(cfg.getToType() <= 0 || cfg.getToType() > StatisConfig.TO_TYPE_MESSAGE+10) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("目标类型不能为空");
			return r;
		}
		
		if(Utils.isEmpty(cfg.getTimeUnit())) {
			cfg.setTimeUnit(StatisConfig.UNIT_MU);
		}
		
		if(cfg.getTimeCnt() <= 0) {
			cfg.setTimeCnt(1);
		}
		
		return null;
	}
	
	public String checkConfig(StatisConfig lw) {
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
		
		StatisIndex[] sis = lw.getStatisIndexs();
		
		for(int i = 0; i < sis.length; i++ ) {
			StatisIndex si = sis[i];
            if(si.getType() < StatisConfig.PREFIX_TOTAL || si.getType() > StatisConfig.PREFIX_CUR_PERCENT) {
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
			String p = Config.NamedTypesDir+"/"+lw.getNamedType();
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

	private String checkToType(StatisConfig lw) {
		String msg = null;
		try {
			if(lw.getToType() <= 0) {
				msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
				return msg;
			}
			
			if(StatisConfig.TO_TYPE_SERVICE_METHOD == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To key params cannot be null for service [" + StatisConfig.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
					return msg;
				}
				
				String[] ps = lw.getToParams().split(UniqueServiceKey.SEP);
				if(ps == null || ps.length < 7) {
					msg = "To param ["+lw.getToParams()+"] invalid [" + StatisConfig.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
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
				
			}else if(StatisConfig.TO_TYPE_DB == lw.getToType()) {
				lw.setToParams(StatisConfig.DEFAULT_DB);
			}else if(StatisConfig.TO_TYPE_FILE == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To file cannot be NULL for id: " + lw.getId();
					return msg;
				}
			}else if(StatisConfig.TO_TYPE_MONITOR_LOG == lw.getToType()) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "Tag cannot be null: " + lw.getId();
					return msg;
				}
			}else if(StatisConfig.TO_TYPE_MESSAGE == lw.getToType()) {
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
	
	private String checkByType(StatisConfig lw) {
		boolean suc = false;
		int tt = lw.getByType();
		String msg = null;
		
		try {
			if(tt <= 0) {
				msg = "By type value cannot be null for: " + lw.getId();
			} else {
				switch(tt) {
					case StatisConfig.BY_TYPE_SERVICE_METHOD:
					case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
					case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
					{
						String[] srvs = lw.getByKey().split(UniqueServiceKey.SEP);
						if(srvs.length < 3) {
							msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
							return msg;
						}
						
						suc = checkByService(lw.getId(),srvs);
						
						if(suc) {
							 lw.setBysn(srvs[UniqueServiceKey.INDEX_SN]);
							 lw.setByns(srvs[UniqueServiceKey.INDEX_NS]);
							 lw.setByver(srvs[UniqueServiceKey.INDEX_VER]);
							 lw.setByins(srvs[UniqueServiceKey.INDEX_INS]);
							 lw.setByme(srvs[UniqueServiceKey.INDEX_METHOD]);
							
							switch(tt) {
							case StatisConfig.BY_TYPE_SERVICE_METHOD:
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
								if(srvs.length < 4 || Utils.isEmpty(srvs[3])) {
									msg = "By instance name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
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
					case StatisConfig.BY_TYPE_INSTANCE:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By instance name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					case StatisConfig.BY_TYPE_ACCOUNT:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By account name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					/*case StatisConfig.BY_TYPE_EXP:
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
