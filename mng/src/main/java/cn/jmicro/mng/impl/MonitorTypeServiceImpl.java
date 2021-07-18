package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ICommonManagerJMSrv;
import cn.jmicro.api.mng.IMonitorTypeServiceJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MCConfigJRso;
import cn.jmicro.api.monitor.MCTypesManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(version = "0.0.1", debugMode = 1,logLevel=MC.LOG_NO,timeout=10000,
monitorEnable = 0,  retryCnt = 0,external=true,showFront=false)
public class MonitorTypeServiceImpl implements IMonitorTypeServiceJMSrv {

	@Cfg(value="/adminPermissionLevel",defGlobal=true)
	private int adminPermissionLevel = 0;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private MCTypesManager mtm;
	
	@Inject
	private ICommonManagerJMSrv commonManager;
	
	public void ready() {
		mtm.enable();
	}
	
	private Comparator<MCConfigJRso> com = (o1, o2)-> {
		int f = o1.getGroup().compareTo(o2.getGroup());
		if(f != 0) {
			return f;
		}
		return o1.getType().compareTo(o2.getType());
	};
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<List<MCConfigJRso>> getAllConfigs() {
		RespJRso<List<MCConfigJRso>> resp = new RespJRso<List<MCConfigJRso>>();
		List<MCConfigJRso> l = new ArrayList<>();
		Set<MCConfigJRso> rst = mtm.getAll();
		resp.setData(l);
		
		if(rst == null || rst.isEmpty()) {
			resp.setCode(2);
			resp.setKey("NoData");
			resp.setMsg("No data");
		} else {
			l.addAll(rst);
			l.sort(com);
			resp.setCode(RespJRso.CODE_SUCCESS);
		}
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> update(MCConfigJRso mc) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		boolean rst = mtm.updateMConfig(mc);
		if(rst) {
			resp.setCode(RespJRso.CODE_SUCCESS);
		} else {
			resp.setCode(RespJRso.CODE_FAIL);
		}
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> delete(short type) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		if(type <= MC.KEEP_MAX_VAL) {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("System type cannot delete!");
			return resp;
		}
		if(mtm.deleteType(type)) {
			resp.setCode(RespJRso.CODE_SUCCESS);
		}else {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("Delete fail!");
		}
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> add(MCConfigJRso mc) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		if(StringUtils.isEmpty(mc.getFieldName())) {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("Field name cannot be NULL");
			return resp;
		}
		
		if(mtm.getByFieldName(mc.getFieldName()) != null) {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("Field name exist: " + mc.getFieldName());
			return resp;
		}
		
		if(this.mtm.createMConfig(mc)) {
			resp.setCode(RespJRso.CODE_SUCCESS);
		} else {
			resp.setCode(RespJRso.CODE_FAIL);
		}
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Map<String,String>> getMonitorKeyList() {
		RespJRso<Map<String,String>> resp = new RespJRso<>();
		Map<String,String> key2val = new HashMap<>();
		String ppath = Config.getRaftBasePath(Config.MonitorTypesDir);
		Set<String> keys = op.getChildren(ppath, false);
		if(keys != null) {
			for(String k :keys) {
				String data = op.getData(ppath+"/"+k);
				key2val.put(k, data);
			}
		}
		resp.setData(key2val);
		resp.setCode(RespJRso.CODE_SUCCESS);
		return resp;
	}


	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<List<Short>> getConfigByMonitorKey(String key) {
		List<Short> l = getTypeByKey(Config.getRaftBasePath(Config.MonitorTypesDir) + "/" + key);
		RespJRso<List<Short>> resp = new RespJRso<List<Short>>();
		resp.setData(l);
		return resp;
	}

	private RespJRso<Void> add2Monitor(String parentDir,String key, Short[] types) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
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
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg(sb.toString() + " exist type for: " + key);
		}else {
			resp.setCode(RespJRso.CODE_SUCCESS);
		}
		
		return resp;
	}

	private RespJRso<Void> removeFromMonitor(String parentDir,String key, Short[] types) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!commonManager.hasPermission(this.adminPermissionLevel)) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
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
		
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> updateMonitorTypes(String key, Short[] adds, Short[] dels) {
		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		RespJRso<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor( Config.getRaftBasePath(Config.MonitorTypesDir),key,adds);
		}
		
		RespJRso<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor( Config.getRaftBasePath(Config.MonitorTypesDir),key,dels);
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
	public RespJRso<List<Short>> getConfigByServiceMethodKey(String key) {
		if(key.contains("/")) {
			key = key.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		List<Short> l = getTypeByKey(Config.getRaftBasePath(Config.MonitorServiceMethodTypesDir) + "/" + key);
		RespJRso<List<Short>> resp = new RespJRso<List<Short>>();
		resp.setData(l);
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> updateServiceMethodMonitorTypes(String key, Short[] adds, Short[] dels) {

		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		if(key.contains("/")) {
			key = key.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		RespJRso<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(Config.getRaftBasePath(Config.MonitorServiceMethodTypesDir),key,adds);
		}
		
		RespJRso<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(Config.getRaftBasePath(Config.MonitorServiceMethodTypesDir),key,dels);
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
	public RespJRso<List<MCConfigJRso>> getAllConfigsByGroup(String[] groups) {
		RespJRso<List<MCConfigJRso>> resp = new RespJRso<List<MCConfigJRso>>();
		if(groups == null || groups.length == 0) {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("Group value is NULL");
			return resp;
		}
		
		for(int i = 0; i < groups.length; i++) {
			if(StringUtils.isEmpty(groups[i])) {
				resp.setCode(RespJRso.CODE_FAIL);
				resp.setMsg("Group index ["+i+"] value is NULL");
				return resp;
			}
		}
		
		List<MCConfigJRso> l = new ArrayList<>();
		Set<MCConfigJRso> rst = mtm.getAll();
		if(rst != null) {
			Iterator<MCConfigJRso> cfgs = rst.iterator();
			for(;cfgs.hasNext();) {
				MCConfigJRso mc = cfgs.next();
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
		resp.setCode(RespJRso.CODE_SUCCESS);
		resp.setData(l);
		return resp;
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<List<String>> getNamedList() {
		Set<String> ls = op.getChildren(Config.getRaftBasePath(Config.NamedTypesDir),false);
		RespJRso<List<String>> resp = new RespJRso<>();
		List<String> l = new ArrayList<>();
		l.addAll(ls);
		resp.setData(l);
		return resp;
	}

	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<List<Short>> getTypesByNamed(String name) {
		if(name.contains("/")) {
			name = name.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		List<Short> l = getTypeByKey(Config.getRaftBasePath(Config.NamedTypesDir) + "/" + name);
		RespJRso<List<Short>> resp = new RespJRso<List<Short>>();
		resp.setData(l);
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public RespJRso<Void> updateNamedTypes(String name, Short[] adds, Short[] dels) {

		RespJRso<Void> resp = new RespJRso<Void>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		if(name.contains("/")) {
			name = name.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		RespJRso<Void> rsp = null;
		if(adds != null && adds.length > 0) {
			rsp = add2Monitor(Config.getRaftBasePath(Config.NamedTypesDir),name,adds);
		}
		
		RespJRso<Void> rsp0 = null;
		if(dels != null && dels.length > 0) {
			rsp0 = removeFromMonitor(Config.getRaftBasePath(Config.NamedTypesDir),name,dels);
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
	public RespJRso<Void> addNamedTypes(String name) {
		 RespJRso<Void> resp = new RespJRso<>();
		if(!PermissionManager.isCurAdmin()) {
			resp.setCode(RespJRso.CODE_NO_PERMISSION);
			resp.setMsg("No permission!");
			return resp;
		}
		
		 String key0 = name;
		 if(name.contains("/")) {
			 key0 = name.replaceAll("/", Constants.PATH_EXCAPE);
		 }
		
		 resp.setCode(RespJRso.CODE_SUCCESS);
		 
		 String p = Config.getRaftBasePath(Config.NamedTypesDir) + "/" + key0;
		 if(op.exist(p)) {
			 resp.setCode(RespJRso.CODE_FAIL);
			 resp.setMsg("exist:　" + name);
		 } else {
			 op.createNodeOrSetData(p, "", IDataOperator.PERSISTENT);
		 }
		 
		 return resp;
		
	}
	
}
