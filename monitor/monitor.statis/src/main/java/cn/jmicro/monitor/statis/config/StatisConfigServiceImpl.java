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
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MonitorStatisConfigManager;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.monitor.statis.api.IStatisConfigService;

@Component
@Service(namespace="mng", version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class StatisConfigServiceImpl implements IStatisConfigService {

	private final static Logger logger = LoggerFactory.getLogger(StatisConfigServiceImpl.class);
	
	private static final String ROOT = MonitorStatisConfigManager.STATIS_WARNING_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private MonitorStatisConfigManager mcm;
	
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
		
		cfg.setCreatedBy(ai.getClientId());
		cfg.setId(this.idGenerator.getIntId(StatisConfig.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

	private Resp<Boolean> checkAndSet(StatisConfig cfg) {
		
		Resp<Boolean> r = new Resp<>();
		
		String msg = mcm.checkConfig(cfg);
		if(msg != null) {
			r.setMsg(msg);
			r.setCode(1);
			r.setData(false);
			return r;
		}
		
		if(Utils.isEmpty(cfg.getByType())) {
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
		
		if(Utils.isEmpty(cfg.getToType())) {
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
}
