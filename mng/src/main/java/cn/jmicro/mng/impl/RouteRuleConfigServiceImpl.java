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
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.route.RouteRule;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.mng.api.IRouteRuleConfigService;

@Component
@Service(version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class RouteRuleConfigServiceImpl implements IRouteRuleConfigService {

	private final static Logger logger = LoggerFactory.getLogger(RouteRuleConfigServiceImpl.class);
	
	private static final String ROOT = Config.getRaftBasePath("") + "/routeRules";
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=256,downSsl=true,encType=0,upSsl=true)
	public Resp<List<RouteRule>> query() {
		Resp<List<RouteRule>> r = new Resp<>();
		Set<String> insNames = op.getChildren(ROOT, false);
		if(insNames == null || insNames.isEmpty()) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		List<RouteRule> l  = new ArrayList<>();
		r.setData(l);
		
		boolean isAll = PermissionManager.isCurAdmin();
		
		for(String ns : insNames) {
			String path = ROOT + "/" + ns;
			Set<String> ids = op.getChildren(path,false);
			if(ids == null || ids.isEmpty()) {
				continue;
			}
			
			for(String id : ids) {
				String p = path + "/" + id;
				String data = op.getData(p);
				RouteRule lw = JsonUtils.getIns().fromJson(data, RouteRule.class);
				if(isAll) {
					l.add(lw);
				}else if(PermissionManager.checkAccountClientPermission(lw.getClientId())) {
					l.add(lw);
				}
			}
		}
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<Boolean> update(RouteRule cfg) {
		
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + cfg.getForIns() + "/" + cfg.getUniqueId();
		String data = op.getData(path);
		RouteRule lw = JsonUtils.getIns().fromJson(data, RouteRule.class);
		
		if(!PermissionManager.checkAccountClientPermission(cfg.getClientId())) {
			r.setCode(Resp.CODE_NO_PERMISSION);
			r.setMsg("Permission reject");
			r.setData(false);
			return r;
		}
		
		if(lw.getClientId() != cfg.getClientId()) {
			if(!PermissionManager.checkAccountClientPermission(lw.getClientId())) {
				r.setCode(Resp.CODE_NO_PERMISSION);
				r.setMsg("Target permission reject");
				r.setData(false);
				return r;
			}
			lw.setClientId(cfg.getClientId());
		}
		
		String msg = checkValid(cfg);
		if(msg != null) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg(msg);
			r.setData(false);
			return r;
		}
		
		lw.setEnable(cfg.isEnable());
		lw.setFrom(cfg.getFrom());
		lw.setPriority(cfg.getPriority());
		lw.setTargetType(cfg.getTargetType());
		lw.setTargetVal(cfg.getTargetVal());
		lw.setFrom(cfg.getFrom());
		lw.setUpdatedBy(JMicroContext.get().getAccount().getId());
		lw.setUpdatedTime(TimeUtils.getCurTime());

		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		return r;
		
	}

	private String checkValid(RouteRule cfg) {
		if(Utils.isEmpty(cfg.getForIns())) {
			return "Instance cannot be NULL";
		}
		
		if(cfg.isEnable()) {
			if(Utils.isEmpty(cfg.getFrom().getType())) {
				return "Route rule from type cannot be NULL";
			}
			
			if(Utils.isEmpty(cfg.getTargetType())) {
				return "Route rule target type cannot be NULL";
			}
			
			if(Utils.isEmpty(cfg.getTargetVal())) {
				return "Route rule target ["+cfg.getTargetType()+"] value cannot be NULL";
			}
			
			if(Utils.isEmpty(cfg.getFrom().getServiceName())) {
				return "Route rule service name cannot be NULL";
			}
			
			/*if(Utils.isEmpty(cfg.getFrom().getNamespace())) {
				return "Route rule namespace cannot be NULL";
			}
			if(Utils.isEmpty(cfg.getFrom().getVersion())) {
				return "Route rule version cannot be NULL";
			}
			if(Utils.isEmpty(cfg.getFrom().getMethod())) {
				return "Route rule method cannot be NULL";
			}*/
			
			switch(cfg.getFrom().getType()) {
			
			case RouteRule.TYPE_FROM_TAG_ROUTER:
				if(Utils.isEmpty(cfg.getFrom().getTagKey())) {
					return "Route rule tag key cannot be NULL";
				}
				if(Utils.isEmpty(cfg.getFrom().getVal())) {
					return "Route rule tag value cannot be NULL";
				}
			break;
			default:
				if(Utils.isEmpty(cfg.getFrom().getVal())) {
					return "Route rule type ["+cfg.getFrom().getType()+"] from value cannot be NULL";
				}
			break;
			}
		}
		
		return null;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=256)
	public Resp<Boolean> delete(String insName, int id) {
		Resp<Boolean> r = new Resp<>();
		String path = ROOT + "/" + insName + "/" + id;
		String data = op.getData(path);
		
		RouteRule lw = JsonUtils.getIns().fromJson(data, RouteRule.class);
		if(!PermissionManager.checkAccountClientPermission(lw.getClientId())) {
			r.setCode(Resp.CODE_NO_PERMISSION);
			r.setMsg("Target permission reject");
			r.setData(false);
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
	public Resp<RouteRule> add(RouteRule cfg) {
		Resp<RouteRule> r = new Resp<>();
		
		ActInfo ai = JMicroContext.get().getAccount();
		if(cfg.getClientId() != ai.getId()) {
			if(!PermissionManager.checkAccountClientPermission(cfg.getClientId())) {
				r.setCode(Resp.CODE_NO_PERMISSION);
				r.setMsg("Target permission reject");
				return r;
			}
		}
		
		String msg = checkValid(cfg);
		if(msg != null) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg(msg);
			r.setData(null);
			return r;
		}

		if(cfg.getClientId() != ai.getId() && !PermissionManager.isCurAdmin()) {
			r.setCode(Resp.CODE_NO_PERMISSION);
			r.setMsg("Permission reject to specify clientId");
			return r;
		}
		
		cfg.setClientId(ai.getId());
		
		cfg.setUniqueId(this.idGenerator.getIntId(RouteRule.class));
		cfg.setCreatedTime(TimeUtils.getCurTime());
		cfg.setUpdatedBy(ai.getId());
		cfg.setUpdatedTime(TimeUtils.getCurTime());
		cfg.setCreatedBy(ai.getId());
		
		String path = ROOT + "/" + cfg.getForIns() + "/" + cfg.getUniqueId();
		
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}

}
