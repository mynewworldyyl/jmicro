package cn.jmicro.resource.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.monitor.ResourceMonitorConfig;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.resource.IMngResourceService;
import cn.jmicro.resource.ResourceMonitorServer;

@Component
@Service(namespace="mng", version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class MngResourceServiceImpl implements IMngResourceService{

	private final static Logger logger = LoggerFactory.getLogger(MngResourceServiceImpl.class);
	
	private static final String ROOT = ResourceMonitorConfig.RES_MONITOR_CONFIG_ROOT;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ResourceMonitorServer mserver;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public Resp<List<ResourceMonitorConfig>> query() {
		Resp<List<ResourceMonitorConfig>> r = new Resp<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(Resp.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		List<ResourceMonitorConfig> ll = new ArrayList<>();
		r.setData(ll);
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			ResourceMonitorConfig lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfig.class);
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
		
		ResourceMonitorConfig lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfig.class);
		
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
	public Resp<Boolean> update(ResourceMonitorConfig cfg) {
		
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
		
		ResourceMonitorConfig lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfig.class);
		if(lw.isEnable()) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(false);
			r.setMsg("启用中的配置不能更新");
			return r;
		}
		
		lw.setEnable(cfg.isEnable());
		lw.setExtParams(cfg.getExtParams());
		lw.setMonitorInsName(cfg.getMonitorInsName());
		lw.setT(cfg.getT());
		lw.setToParams(cfg.getToParams());
		lw.setToType(cfg.getToType());
		lw.setExpStr(cfg.getExpStr());
		lw.setResName(cfg.getResName());

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
	public Resp<ResourceMonitorConfig> add(ResourceMonitorConfig cfg) {
		Resp<ResourceMonitorConfig> r = new Resp<>();
		
		Resp<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			r.setCode(rr.getCode());
			r.setMsg(rr.getMsg());
			return r;
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		cfg.setCreatedByAct(ai.getActName());
		cfg.setCreatedBy(ai.getClientId());
		cfg.setId(this.idGenerator.getIntId(ResourceMonitorConfig.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<Set<CfgMetadata>> getResourceMetadata(String resName,boolean refreshCache) {
		Resp<Set<CfgMetadata>> r = new Resp<>();
		r.setData(mserver.getResMetadata(resName));
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<Map<String,Map<String,Set<CfgMetadata>>>> getInstanceResourceList() {
		Resp<Map<String,Map<String,Set<CfgMetadata>>>> r = new Resp<>();
		r.setData(mserver.getInstanceResourceList());
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public Resp<Map<String,Set<ResourceData>>> getInstanceResourceData(Map<String,String> params) {
		
		Resp<Map<String,Set<ResourceData>>> r = new Resp<> ();
		
		String resName = params.get("resName");
		String insName = params.get("insName");
		if(StringUtils.isEmpty(resName) && StringUtils.isEmpty(insName)) {
			r.setMsg("Resource name and instance name have to select one!");
			r.setCode(Resp.CODE_FAIL);
			return r;
		}
		
		int toType = StatisConfig.TO_TYPE_DIRECT;
		if(params.containsKey("toType")) {
			toType = Integer.parseInt(params.get("toType"));
		}
		
		/*String groupBy = params.get("groupBy");
		if(StringUtils.isEmpty(groupBy)) {
			groupBy = "ins";
		}*/
		
		if(toType == StatisConfig.TO_TYPE_DIRECT) {
			return mserver.getDirectResourceData(insName,resName,params);
		}
		
		return r;
		//return mserver.getInstanceResourceData(insName);
	}

	private Resp<Boolean> checkAndSet(ResourceMonitorConfig cfg) {
		
		Resp<Boolean> r = new Resp<>();
		
		String msg = checkToType(cfg);
		
		if(msg == null && StringUtils.isEmpty(cfg.getResName())) {
			msg = "资源名称不能为空";
		}
		
		if(!Utils.isEmpty(cfg.getExpStr())) {
			List<String> suffixExp = ExpUtils.toSuffix(cfg.getExpStr());
			if(!ExpUtils.isValid(suffixExp)) {
				 msg = "Invalid exp: " + cfg.getId() + ", exp: " + cfg.getExpStr();
			}
		}
		
		if(msg != null) {
			r.setMsg(msg);
			r.setCode(1);
			r.setData(false);
			return r;
		}
		
		return null;
	}
	
	public String checkConfig(ResourceMonitorConfig lw) {
		String msg = checkToType(lw);
		if(msg != null) {
			return msg;
		}
		
		if(Utils.getIns().checkEmail(lw.getToParams())) {
			msg = "Email format invalid: " + lw.getToParams()+ " for id: " + lw.getId();
			return msg;
		}
		
		return msg;
	}

	private String checkToType(ResourceMonitorConfig lw) {
		String msg = null;
		try {
			if(lw.getToType() <= 0) {
				msg = "To key type invalid: " + lw.getToType()+ " for id: " + lw.getId();
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
			}else if(StatisConfig.TO_TYPE_EMAIL == lw.getToType()) {
				if(!Utils.getIns().checkEmail(lw.getToParams())) {
					msg = "Email format invalid: " + lw.getToParams();
					return msg;
				}
			}
		} finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, this.getClass() , msg);
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
				LG.logWithNonRpcContext(MC.LOG_WARN, this.getClass(), msg);
			}
		}
		
	}
	
}
