package cn.jmicro.mng.impl;

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
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.ILogMonitorServer;
import cn.jmicro.api.monitor.LogWarningConfig;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.mng.api.ILogWarningConfig;

@Component
@Service(namespace="mng", version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class LogWarningConfigImpl implements ILogWarningConfig {

	private final static Logger logger = LoggerFactory.getLogger(LogWarningConfigImpl.class);
	
	private static final String ROOT = ILogMonitorServer.LOG_WARNING_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=256,downSsl=true,encType=0,upSsl=true)
	public Resp<List<LogWarningConfig>> query() {
		Resp<List<LogWarningConfig>> r = new Resp<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		List<LogWarningConfig> ll = new ArrayList<>();
		r.setData(ll);
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			LogWarningConfig lw = JsonUtils.getIns().fromJson(data, LogWarningConfig.class);
			ll.add(lw);
		}
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<Boolean> update(LogWarningConfig cfg) {
		
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + cfg.getId();
		String data = op.getData(path);
		LogWarningConfig lw = JsonUtils.getIns().fromJson(data, LogWarningConfig.class);
		
		if(!ExpUtils.isValid(cfg.getExpStr())) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("Invalid Expression");
			return r;
		}
		
		if(LogWarningConfig.TYPE_SAVE_DB == cfg.getType() && Utils.isEmpty(cfg.getCfgParams())) {
			cfg.setCfgParams("rpc_log");
		}
		
		if((LogWarningConfig.TYPE_FORWARD_SRV == cfg.getType()
				|| LogWarningConfig.TYPE_SAVE_DB == cfg.getType()
				|| LogWarningConfig.TYPE_SAVE_FILE == cfg.getType()) 
				&& Utils.isEmpty(cfg.getCfgParams())) {
			logger.error("Config param is NULL: " + cfg.getId());
			r.setCode(Resp.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid config params");
			return r;
		}
		
		lw.setExpStr(cfg.getExpStr());
		lw.setMinNotifyInterval(cfg.getMinNotifyInterval());
		lw.setCfgParams(cfg.getCfgParams());
		lw.setTag(cfg.getTag());
		lw.setType(cfg.getType());
		lw.setEnable(cfg.isEnable());

		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		return r;
		
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=256)
	public Resp<Boolean> delete(String id) {
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
	public Resp<LogWarningConfig> add(LogWarningConfig cfg) {
		Resp<LogWarningConfig> r = new Resp<>();
		
		if(!ExpUtils.isValid(cfg.getExpStr())) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid Expression");
			return r;
		}
		
		if((LogWarningConfig.TYPE_FORWARD_SRV == cfg.getType()
				|| LogWarningConfig.TYPE_SAVE_DB == cfg.getType()
				|| LogWarningConfig.TYPE_SAVE_FILE == cfg.getType()) 
				&& Utils.isEmpty(cfg.getCfgParams())) {
			logger.error("Config param is NULL: " + cfg.getId());
			r.setCode(Resp.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid config params");
			return r;
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		cfg.setClientId(ai.getId());
		cfg.setId(this.idGenerator.getStringId(LogWarningConfig.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

}
