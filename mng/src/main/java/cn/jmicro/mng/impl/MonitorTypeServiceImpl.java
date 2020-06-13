package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MCConfig;
import cn.jmicro.api.monitor.MonitorTypesManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.ICommonManager;
import cn.jmicro.mng.api.IMonitorTypeService;

@Component
@Service(namespace = "mng", version = "0.0.1", debugMode = 0,
monitorEnable = 0, logLevel = MC.LOG_ERROR, retryCnt = 0)
public class MonitorTypeServiceImpl implements IMonitorTypeService {

	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private MonitorTypesManager mtm;
	
	@Inject
	private ICommonManager commonManager;
	
	public void ready() {
		mtm.enable();
	}
	
	private Comparator<MCConfig> com = (o1, o2)-> {
		int f = o1.getGroup().compareTo(o2.getGroup());
		if(f != 0) {
			return f;
		}
		return o1.getType().compareTo(o2.getType());
	};
	
	@Override
	public Resp<List<MCConfig>> getAllConfigs() {
		Resp<List<MCConfig>> resp = new Resp<List<MCConfig>>();
		List<MCConfig> l = new ArrayList<>();
		Set<MCConfig> rst = mtm.getAll();
		if(rst != null) {
			l.addAll(rst);
			l.sort(com);
		}
		resp.setCode(Resp.CODE_SUCCESS);
		resp.setData(l);
		return resp;
	}

	@Override
	public Resp<Void> update(MCConfig mc) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		boolean rst = mtm.updateMConfig(mc);
		if(rst) {
			resp.setCode(Resp.CODE_SUCCESS);
		} else {
			resp.setCode(Resp.CODE_FAIL);
		}
		return resp;
	}

	@Override
	public Resp<Void> delete(short type) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		if(type <= MC.KEEP_MAX_VAL) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("System type cannot delete!");
			return resp;
		}
		if(mtm.deleteType(type)) {
			resp.setCode(Resp.CODE_SUCCESS);
		}else {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Delete fail!");
		}
		return resp;
	}

	@Override
	public Resp<Void> add(MCConfig mc) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		if(StringUtils.isEmpty(mc.getFieldName())) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Field name cannot be NULL");
			return resp;
		}
		
		if(mtm.getByFieldName(mc.getFieldName()) != null) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Field name exist: " + mc.getFieldName());
			return resp;
		}
		
		if(this.mtm.createMConfig(mc)) {
			resp.setCode(Resp.CODE_SUCCESS);
		} else {
			resp.setCode(Resp.CODE_FAIL);
		}
		return resp;
	}
	
	@Override
	public Resp<Map<String,String>> getMonitorKeyList() {
		Resp<Map<String,String>> resp = new Resp<>();
		Map<String,String> key2val = new HashMap<>();
		Set<String> keys = op.getChildren(Config.MonitorTypesDir, false);
		if(keys != null) {
			for(String k :keys) {
				String data = op.getData(Config.MonitorTypesDir+"/"+k);
				key2val.put(k, data);
			}
		}
		resp.setData(key2val);
		resp.setCode(Resp.CODE_SUCCESS);
		return resp;
	}


	@Override
	public Resp<List<Short>> getConfigByMonitorKey(String key) {
		List<Short> l = getTypeByKey(key);
		Resp<List<Short>> resp = new Resp<List<Short>>();
		resp.setData(l);
		return resp;
	}

	@Override
	public Resp<Void> add2Monitor(String key, Short[] types) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		String path = Config.MonitorTypesDir + "/" + key;
		List<Short> l = getTypeByKey(key);
		
		
		StringBuffer nsb = new StringBuffer();
		
		StringBuffer sb = null;
		
		for(Short t : types) {
			boolean f = false;
			for(Short nt : l) {
				if(nt == t) {
					f = true;
					break;
				}
			}
			
			if(f) {
				if(sb == null) {
					sb = new StringBuffer();
				}
				sb.append(t).append(",");
			} else {
				nsb.append(t).append(",");
			}
		}
		
		if(nsb.length() > 0) {
			String data = op.getData(path);
			if(StringUtils.isNotEmpty(data)) {
				nsb.append(data);
			}else {
				nsb.delete(nsb.length()-1, nsb.length());
			}
			op.createNodeOrSetData(path, nsb.toString(), IDataOperator.PERSISTENT);
		}
		
		if(sb != null) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg(sb.toString() + " exist type for: " + key);
		}else {
			resp.setCode(Resp.CODE_SUCCESS);
		}
		
		return resp;
	}

	@Override
	public Resp<Void> removeFromMonitor(String key, Short[] types) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		List<Short> l = getTypeByKey(key);
		
		StringBuffer sb = new StringBuffer();
		
		for(short t: l) {
			boolean f = false;
			for(Short nt : types) {
				if(nt == t) {
					f = true;
					break;
				}
			}
			
			if(!f) {
				sb.append(t).append(",");
			}
		}
		
		if(sb.length() > 0) {
			String path = Config.MonitorTypesDir + "/" + key;
			sb.delete(sb.length()-1, sb.length());
			op.setData(path, sb.toString());
		}
		
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}
	
	
	
	@Override
	public Resp<Void> updateMonitorTypes(String key, Short[] adds, Short[] dels) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		Resp<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(key,adds);
		}
		
		Resp<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(key,dels);
		}
		
		if(rsp == null) {
			return rsp0;
		}else if(rsp0 == null) {
			return rsp;
		}else {
			if(rsp.getCode() == 0 && rsp0.getCode() == 0) {
				return rsp;
			} else {
				rsp.setMsg(rsp.getMsg() + ": " + rsp0.getMsg());
			}
			return rsp;
		}
	}

	private List<Short> getTypeByKey(String key) {
		List<Short> l = new ArrayList<>();
		String path = Config.MonitorTypesDir + "/" + key;
		if(op.exist(path)) {
			String data = op.getData(path);
			if(StringUtils.isNotEmpty(data)) {
				String[] ts = data.split(",");
				for(String t : ts) {
					Short type = Short.parseShort(t);
					l.add(type);
				}
			}		
		}
		return l;
	}

}
