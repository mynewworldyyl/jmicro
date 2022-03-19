package cn.jmicro.resource.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceDataJRso;
import cn.jmicro.api.monitor.ResourceMonitorConfigJRso;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.resource.IMngResourceServiceJMSrv;
import cn.jmicro.resource.ResourceDataReqJRso;
import cn.jmicro.resource.ResourceMonitorServer;

@Component
@Service(version="0.0.1",external=true,timeout=10000,debugMode=1,showFront=false)
public class MngResourceServiceImpl implements IMngResourceServiceJMSrv{

	private final static Logger logger = LoggerFactory.getLogger(MngResourceServiceImpl.class);
	
	private static final String ROOT = ResourceMonitorConfigJRso.RES_MONITOR_CONFIG_ROOT;
	
	private JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ResourceMonitorServer mserver;
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=1024,downSsl=true,encType=0,upSsl=true)
	public RespJRso<List<ResourceMonitorConfigJRso>> query() {
		RespJRso<List<ResourceMonitorConfigJRso>> r = new RespJRso<>();
		Set<String> ids = op.getChildren(ROOT, false);
		if(ids == null || ids.isEmpty()) {
			r.setCode(RespJRso.CODE_FAIL);
			r.setMsg("NoData");
			return r;
		}
		
		boolean isAdmin = PermissionManager.isCurAdmin(Config.getClientId());
	
		List<ResourceMonitorConfigJRso> ll = new ArrayList<>();
		r.setData(ll);
		
		for(String id : ids) {
			String path = ROOT + "/" + id;
			String data = op.getData(path);
			ResourceMonitorConfigJRso lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfigJRso.class);
			if(lw != null) {
				if(isAdmin || PermissionManager.checkAccountClientPermission(lw.getClientId())) {
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
		
		ResourceMonitorConfigJRso lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getClientId()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to enable resource monitor config: " + lw.getId()+", target clientId: " + lw.getClientId());
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
		
		lw.setEnable(!lw.isEnable());
		
		op.setData(path, JsonUtils.getIns().toJson(lw));
		r.setData(true);
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=1,maxPacketSize=4096,downSsl=true,encType=0,upSsl=true)
	public RespJRso<Boolean> update(ResourceMonitorConfigJRso cfg) {
		
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
		
		ResourceMonitorConfigJRso lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfigJRso.class);
		
		if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getClientId()))) {
			r.setCode(RespJRso.CODE_NO_PERMISSION);
			r.setData(false);
			r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to update resource monitor config: " + lw.getId()+", target clientId: " + lw.getClientId());
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
			lw.setClientId(JMicroContext.get().getAccount().getClientId());
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
	public RespJRso<Boolean> delete(int id) {
		RespJRso<Boolean> r = new RespJRso<>();
		String path = ROOT + "/" + id;
		if(op.exist(path)) {
			String data = op.getData(path);
			ResourceMonitorConfigJRso lw = JsonUtils.getIns().fromJson(data, ResourceMonitorConfigJRso.class);
			if(!(PermissionManager.isCurAdmin(Config.getClientId()) || PermissionManager.checkAccountClientPermission(lw.getClientId()))) {
				r.setCode(RespJRso.CODE_NO_PERMISSION);
				r.setData(false);
				r.setMsg(JMicroContext.get().getAccount().getActName()+" have no permissoin to delete resource monitor config: " + lw.getId()+", target clientId: " + lw.getClientId());
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
	public RespJRso<ResourceMonitorConfigJRso> add(ResourceMonitorConfigJRso cfg) {
		RespJRso<ResourceMonitorConfigJRso> r = new RespJRso<>();
		
		RespJRso<Boolean> rr = this.checkAndSet(cfg);
		if(rr != null) {
			r.setCode(rr.getCode());
			r.setMsg(rr.getMsg());
			return r;
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			cfg.setClientId(ai.getClientId());
		}
		
		cfg.setCreatedByAct(ai.getActName());
		cfg.setCreatedBy(ai.getId());
		cfg.setId(this.idGenerator.getIntId(ResourceMonitorConfigJRso.class));
		
		String path = ROOT + "/" + cfg.getId();
		op.createNodeOrSetData(path, JsonUtils.getIns().toJson(cfg), false);
		r.setData(cfg);
		
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public RespJRso<Set<CfgMetadataJRso>> getResourceMetadata(String resName) {
		RespJRso<Set<CfgMetadataJRso>> r = new RespJRso<>();
		r.setData(mserver.getResMetadata(resName));
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public RespJRso<Map<String,Map<String,Set<CfgMetadataJRso>>>> getInstanceResourceList() {
		RespJRso<Map<String,Map<String,Set<CfgMetadataJRso>>>> r = new RespJRso<>();
		r.setData(mserver.getInstanceResourceList());
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=4096)
	public IPromise<RespJRso<Map<String,List<ResourceDataJRso>>>> getInstanceResourceData(ResourceDataReqJRso req) {
		
		Promise<RespJRso<Map<String,List<ResourceDataJRso>>>> p = new Promise<>();
		
		RespJRso<Map<String,List<ResourceDataJRso>>> r = new RespJRso<> ();
		p.setResult(r);
		
		String[] resNames = req.getResNames();
		String[] insNames = req.getInsNames();
		if((resNames == null || resNames.length == 0) && (insNames == null || insNames.length == 0)) {
			r.setMsg("Resource name and instance name have to select one!");
			r.setCode(RespJRso.CODE_FAIL);
			p.done();
			return p;
		}
		
		/*String groupBy = params.get("groupBy");
		if(StringUtils.isEmpty(groupBy)) {
			groupBy = "ins";
		}*/
		switch(req.getToType()) {
		case StatisConfigJRso.TO_TYPE_DIRECT:
			return mserver.getDirectResourceData(req);
		case StatisConfigJRso.TO_TYPE_DB:
			return queryFromDb(req);
		}
		return p;
	}

	private IPromise<RespJRso<Map<String, List<ResourceDataJRso>>>> queryFromDb(ResourceDataReqJRso req) {

		Document qryMatch = this.getLogCondtions(req);
		
		Document match = new Document("$match", qryMatch);
		//Document unwind = new Document("$unwind", "$items");
		
		int ps = req.getPageSize() > 0? req.getPageSize():10;
		int cp = req.getCurPage() >= 0? req.getCurPage():0;
		
		Document sort = new Document("$sort", new Document("time", -1));
		Document skip = new Document("$skip", ps*cp);
		Document limit = new Document("$limit", ps);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);
		
		MongoCollection<ResourceDataJRso> rpcLogColl = 
				mongoDb.getCollection(ResourceMonitorConfigJRso.DEFAULT_RESOURCE_TABLE_NAME,ResourceDataJRso.class);
		AggregateIterable<ResourceDataJRso> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<ResourceDataJRso> cursor = resultset.iterator();
		
		RespJRso<Map<String, List<ResourceDataJRso>>> resp = new RespJRso<>();
		Map<String, List<ResourceDataJRso>> maps = new HashMap<>();
		resp.setData(maps);
		
		try {
			while(cursor.hasNext()) {
				ResourceDataJRso mi = cursor.next();
				/*String json = log.toJson(settings);
				ResourceData mi = fromJson(json);*/
				if(!maps.containsKey(mi.getBelongInsName())) {
					maps.put(mi.getBelongInsName(), new ArrayList<ResourceDataJRso>());
				}
				maps.get(mi.getBelongInsName()).add(mi);
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}
		
		Promise<RespJRso<Map<String, List<ResourceDataJRso>>>> p = new Promise<>();
		p.setResult(resp);
		p.done();
		
		return p;
	}
	
	private Document getLogCondtions(ResourceDataReqJRso req) {
		Document match = new Document();;
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			 match.put("clientId", JMicroContext.get().getAccount().getClientId());
		}
		
		if(req.getStartTime() > 0) {
			Document st = new Document();
			st.put("$gte", req.getStartTime());
			match.put("time", st);
		}
		
		if(req.getEndTime() > 0) {
			Document st = new Document();
			st.put("$lte", req.getEndTime());
			match.put("time", st);
		}
		
		if(req.getResNames() != null && req.getResNames().length > 0) {
			Document st = new Document();
			st.put("$in", Arrays.asList(req.getResNames()));
			match.put("resName", st);
		}

		if(req.getInsNames() != null && req.getInsNames().length > 0) {
			Document st = new Document();
			st.put("$in", Arrays.asList(req.getInsNames()));
			match.put("belongInsName", st);
		}
		
		if(StringUtils.isNotEmpty(req.getTag())) {
			match.put("tag", req.getTag());
		}
		
		return match;
	}
	
	private RespJRso<Boolean> checkAndSet(ResourceMonitorConfigJRso cfg) {
		
		RespJRso<Boolean> r = new RespJRso<>();
		
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
	
	public String checkConfig(ResourceMonitorConfigJRso lw) {
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

	private String checkToType(ResourceMonitorConfigJRso lw) {
		String msg = null;
		try {
			if(lw.getToType() <= 0) {
				msg = "To key type invalid: " + lw.getToType()+ " for id: " + lw.getId();
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
				if(StringUtils.isEmpty(lw.getToParams())) {
					lw.setToParams(ResourceMonitorConfigJRso.DEFAULT_RESOURCE_TABLE_NAME);
				}
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
			}else if(StatisConfigJRso.TO_TYPE_EMAIL == lw.getToType()) {
				if(!Utils.getIns().checkEmail(lw.getToParams())) {
					msg = "Email format invalid: " + lw.getToParams();
					return msg;
				}
			}
		} finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, this.getClass() ,msg,MC.MT_DEFAULT,true);
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
				LG.logWithNonRpcContext(MC.LOG_WARN, this.getClass(), msg,MC.MT_DEFAULT,true);
			}
		}
		
	}
	
}
