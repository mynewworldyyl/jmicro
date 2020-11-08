package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ICommonManager;
import cn.jmicro.api.mng.IMonitorTypeService;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MCConfig;
import cn.jmicro.api.monitor.MCTypesManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace = "mng", version = "0.0.1", debugMode = 1,timeout=10000,
monitorEnable = 0, logLevel = MC.LOG_ERROR, retryCnt = 0,external=true,showFront=false)
public class MonitorTypeServiceImpl implements IMonitorTypeService {

	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private MCTypesManager mtm;
	
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<MCConfig>> getAllConfigs() {
		Resp<List<MCConfig>> resp = new Resp<List<MCConfig>>();
		List<MCConfig> l = new ArrayList<>();
		Set<MCConfig> rst = mtm.getAll();
		resp.setData(l);
		
		if(rst == null || rst.isEmpty()) {
			resp.setCode(2);
			resp.setKey("NoData");
			resp.setMsg("No data");
		} else {
			l.addAll(rst);
			l.sort(com);
			resp.setCode(Resp.CODE_SUCCESS);
		}
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> update(MCConfig mc) {
		Resp<Void> resp = new Resp<Void>();
		if(!PermissionManager.isCurAdmin()) {
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> delete(short type) {
		Resp<Void> resp = new Resp<Void>();
		if(!PermissionManager.isCurAdmin()) {
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> add(MCConfig mc) {
		Resp<Void> resp = new Resp<Void>();
		if(!PermissionManager.isCurAdmin()) {
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
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
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<Short>> getConfigByMonitorKey(String key) {
		List<Short> l = getTypeByKey(Config.MonitorTypesDir + "/" + key);
		Resp<List<Short>> resp = new Resp<List<Short>>();
		resp.setData(l);
		return resp;
	}

	private Resp<Void> add2Monitor(String parentDir,String key, Short[] types) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		String path = parentDir + "/" + key;
		List<Short> l = getTypeByKey(path);
		
		
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

	private Resp<Void> removeFromMonitor(String parentDir,String key, Short[] types) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		String path = parentDir + "/" + key;
		List<Short> l = getTypeByKey(path);
		
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
			sb.delete(sb.length()-1, sb.length());
		}
		
		op.setData(path, sb.toString());
		
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> updateMonitorTypes(String key, Short[] adds, Short[] dels) {
		Resp<Void> resp = new Resp<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		Resp<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(Config.MonitorTypesDir,key,adds);
		}
		
		Resp<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(Config.MonitorTypesDir,key,dels);
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

	private List<Short> getTypeByKey(String path) {
		List<Short> l = new ArrayList<>();
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

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<Short>> getConfigByServiceMethodKey(String key) {
		if(key.contains("/")) {
			key = key.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		List<Short> l = getTypeByKey(Config.MonitorServiceMethodTypesDir + "/" + key);
		Resp<List<Short>> resp = new Resp<List<Short>>();
		resp.setData(l);
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> updateServiceMethodMonitorTypes(String key, Short[] adds, Short[] dels) {

		Resp<Void> resp = new Resp<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		if(key.contains("/")) {
			key = key.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		Resp<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(Config.MonitorServiceMethodTypesDir,key,adds);
		}
		
		Resp<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(Config.MonitorServiceMethodTypesDir,key,dels);
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

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<MCConfig>> getAllConfigsByGroup(String[] groups) {
		Resp<List<MCConfig>> resp = new Resp<List<MCConfig>>();
		if(groups == null || groups.length == 0) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Group value is NULL");
			return resp;
		}
		
		for(int i = 0; i < groups.length; i++) {
			if(StringUtils.isEmpty(groups[i])) {
				resp.setCode(Resp.CODE_FAIL);
				resp.setMsg("Group index ["+i+"] value is NULL");
				return resp;
			}
		}
		
		List<MCConfig> l = new ArrayList<>();
		Set<MCConfig> rst = mtm.getAll();
		if(rst != null) {
			Iterator<MCConfig> cfgs = rst.iterator();
			for(;cfgs.hasNext();) {
				MCConfig mc = cfgs.next();
				boolean f = false;
				for(String g : groups) {
					if(g.equals(mc.getGroup())) {
						f = true;
						break;
					}
				}
				if(!f) {
					cfgs.remove();
				}
			}
			l.addAll(rst);
			l.sort(com);
		}
		resp.setCode(Resp.CODE_SUCCESS);
		resp.setData(l);
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<String>> getNamedList() {
		Set<String> ls = op.getChildren(Config.NamedTypesDir,false);
		Resp<List<String>> resp = new Resp<>();
		List<String> l = new ArrayList<>();
		l.addAll(ls);
		resp.setData(l);
		return resp;
	}

	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<List<Short>> getTypesByNamed(String name) {
		if(name.contains("/")) {
			name = name.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		List<Short> l = getTypeByKey(Config.NamedTypesDir + "/" + name);
		Resp<List<Short>> resp = new Resp<List<Short>>();
		resp.setData(l);
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> updateNamedTypes(String name, Short[] adds, Short[] dels) {

		Resp<Void> resp = new Resp<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		if(name.contains("/")) {
			name = name.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		Resp<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(Config.NamedTypesDir,name,adds);
		}
		
		Resp<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(Config.NamedTypesDir,name,dels);
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
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public Resp<Void> addNamedTypes(String name) {
		 Resp<Void> resp = new Resp<>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(Resp.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		 String key0 = name;
		 if(name.contains("/")) {
			 key0 = name.replaceAll("/", Constants.PATH_EXCAPE);
		 }
		
		 resp.setCode(Resp.CODE_SUCCESS);
		 
		 String p = Config.NamedTypesDir + "/" + key0;
		 if(op.exist(p)) {
			 resp.setCode(Resp.CODE_FAIL);
			 resp.setMsg("exist:　" + name);
		 } else {
			 op.createNodeOrSetData(p, "", IDataOperator.PERSISTENT);
		 }
		 
		 return resp;
		
	}
	
}
