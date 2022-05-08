package cn.jmicro.mng.impl;

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
import cn.jmicro.api.monitor.ILogMonitorServerJMSrv;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.LogWarningConfigJRso;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.mng.Namespace;
import cn.jmicro.mng.api.ILogWarningConfigJMSrv;

@Component
@Service(version="0.0.1",namespace=Namespace.NS,external=true,timeout=10000,debugMode=1,showFront=false,logLevel=MC.LOG_NO)
public class LogWarningConfigImpl implements ILogWarningConfigJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(LogWarningConfigImpl.class);
	
	private static final String ROOT = ILogMonitorServerJMSrv.LOG_WARNING_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=256,downSsl=true,encType=0,upSsl=true)
	public RespJRso<List<LogWarningConfigJRso>> query() {
		RespJRso<List<LogWarningConfigJRso>> r = new RespJRso<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		boolean isAdmin = PermissionManager.isCurAdmin(Config.getClientId());
		
		List<LogWarningConfigJRso> ll = new ArrayList<>();
		r.setData(ll);
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			LogWarningConfigJRso lw = JsonUtils.getIns().fromJson(data, LogWarningConfigJRso.class);
			if(lw != null) {
				if(isAdmin || PermissionManager.checkAccountClientPermission(lw.getCreatedBy())) {
					ll.add(lw);
				}
			}
		}
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public RespJRso<Boolean> update(LogWarningConfigJRso cfg) {
		
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + cfg.getId();
		String data = op.getData(path);
		LogWarningConfigJRso lw = JsonUtils.getIns().fromJson(data, LogWarningConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getCreatedBy()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to update log warning config: " + lw.getId()+", clientId: " + lw.getClientId());
			LG.log(MC.LOG_WARN, this.getClass(), r.getMsg());
			return r;
		}
		
		if(!ExpUtils.isValid(cfg.getExpStr())) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(false);
			r.setMsg("Invalid Expression");
			return r;
		}
		
		if(LogWarningConfigJRso.TYPE_SAVE_DB == cfg.getType()) {
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				cfg.setCfgParams(JMLogItemJRso.TABLE);
			}else if(Utils.isEmpty(cfg.getCfgParams())) {
				cfg.setCfgParams(JMLogItemJRso.TABLE);
			}
		}
		
		if((LogWarningConfigJRso.TYPE_FORWARD_SRV == cfg.getType()
				|| LogWarningConfigJRso.TYPE_SAVE_FILE == cfg.getType()) 
				&& Utils.isEmpty(cfg.getCfgParams())) {
			logger.error("Config param is NULL: " + cfg.getId());
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid config params");
			return r;
		}
		
		lw.setClientId(cfg.getClientId());
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
	public RespJRso<Boolean> delete(String id) {
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + id;
		
		String data = op.getData(path);
		LogWarningConfigJRso lw = JsonUtils.getIns().fromJson(data, LogWarningConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getCreatedBy()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to delete log warning config: " + lw.getId()+", target clientId: " + lw.getClientId());
			LG.log(MC.LOG_WARN, this.getClass(), r.getMsg());
			return r;
		}
		
		if(op.exist(path)) {
			op.deleteNode(path);
		}
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public RespJRso<LogWarningConfigJRso> add(LogWarningConfigJRso cfg) {
		RespJRso<LogWarningConfigJRso> r = new RespJRso<>();
		
		if(!ExpUtils.isValid(cfg.getExpStr())) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid Expression");
			return r;
		}
		
		if(LogWarningConfigJRso.TYPE_SAVE_DB == cfg.getType()) {
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				cfg.setCfgParams(JMLogItemJRso.TABLE);
			}else if(Utils.isEmpty(cfg.getCfgParams())) {
				cfg.setCfgParams(JMLogItemJRso.TABLE);
			}
		}
		
		if((LogWarningConfigJRso.TYPE_FORWARD_SRV == cfg.getType()
				|| LogWarningConfigJRso.TYPE_SAVE_FILE == cfg.getType()) 
				&& Utils.isEmpty(cfg.getCfgParams())) {
			logger.error("Config param is NULL: " + cfg.getId());
			r.setCode(RespJRso.CODE_FAIL);
			r.setData(null);
			r.setMsg("Invalid config params");
			return r;
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			cfg.setClientId(ai.getClientId());
		}
		
		//cfg.setClientId(ai.getId());
		cfg.setId(this.idGenerator.getStringId(LogWarningConfigJRso.class));
		cfg.setCreatedBy(ai.getId());
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

}
