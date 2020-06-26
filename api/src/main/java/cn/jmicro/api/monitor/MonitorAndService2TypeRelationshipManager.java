package cn.jmicro.api.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
public class MonitorAndService2TypeRelationshipManager {

	public static final String TYPE_SPERATOR = ",";
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IRegistry registry;
	
	private Map<String, Set<Short>> monitorKey2Types = new HashMap<>();
	
	private Set<Short> monitorTypes = new HashSet<>();
	
	private Set<String> activeMonitors = new HashSet<>();
	
	private Map<String,Set<Short>> srvKey2Types = new HashMap<>();
	
	private Map<String,Boolean> sm2Enable = new HashMap<>();
	
	private Map<String,Set<String>> ins2sms = new HashMap<>();
	
	private Map<String,Set<String>> srv2sms = new HashMap<>();
	
	private IDataListener monitorTypeDataChangeListener = (path,data) -> {
		String skey = path.substring(Config.MonitorTypesDir.length()+1);
		doMonitorMoTypeUpdate(skey,data);
	};
	
	private IChildrenListener monitorTypeChildrenListener = (type,parentDir,mokey,data)->{
		if(type == IListener.ADD) {
			doMonitorMoTypeAdd(mokey,data);
			op.addDataListener(parentDir + "/" + mokey, monitorTypeDataChangeListener);
		} else if(type == IListener.REMOVE) {
			doMonitorTypeDelete(mokey,data);
			op.removeDataListener(parentDir + "/" + mokey, monitorTypeDataChangeListener);
		}
	};
	
	private IDataListener srvTypeDataChangeListener = (path,data) -> {
		String skey = path.substring(Config.MonitorServiceMethodTypesDir.length()+1);
		doSrvMoTypeUpdate(skey,data);
	};
	
	private IChildrenListener srvChildrenListener = (type,parentDir,mokey,data)->{
		if(type == IListener.ADD) {
			doSrvMoTypeAdd(mokey,data);
			op.addDataListener(parentDir + "/" + mokey, srvTypeDataChangeListener);
		} else if(type == IListener.REMOVE) {
			doSrvMoTypeDelete(mokey,data);
			op.removeDataListener(parentDir + "/" + mokey, srvTypeDataChangeListener);
		}
	};
	
	/*private IDataListener monitorActiveStateDataChangeListener = (path,data) -> {
		monitorActiveStateChange(data);
	};*/
	
	private IServiceListener monigotDataSubscribeListener = (type,si) -> {
		if(type == IListener.ADD) {
			monitorDataSubscribeAdd(si);
		}else if(type == IListener.REMOVE) {
			monitorDataSubscribeRemove(si);
		}
	};
	
	
	public void ready() {
		registry.addServiceNameListener("cn.jmicro.api.monitor.IMonitorDataSubscriber", monigotDataSubscribeListener);
		op.addChildrenListener(Config.MonitorTypesDir,monitorTypeChildrenListener);
		op.addChildrenListener(Config.MonitorServiceMethodTypesDir,srvChildrenListener);
	}
	
	public Map<String, Set<Short>> getMkey2Types() {
		return Collections.unmodifiableMap(monitorKey2Types);
	}
	
	public Set<Short> intrest(String mkey) {
		return this.monitorKey2Types.get(mkey);
	}
	
	private void doMonitorTypeDelete(String mokey, String data) {
		Set<Short> ts = this.monitorKey2Types.remove(mokey);
		deleteMonitorTypes(ts);
	}
	
	private void deleteMonitorTypes(Set<Short> ts) {
		if(ts == null || ts.isEmpty()) {
			return;
		}
		for(Short t : ts) {
			if(!monitorTypes.contains(t)) {
				continue;
			}
			
			boolean f = false;
			for(String k : activeMonitors) {
				if(monitorKey2Types.containsKey(k) && monitorKey2Types.get(k).contains(t)) {
					f = true;
					break;
				}
			}
			
			if(!f) {
				monitorTypes.remove(t);
			}
		}
	}

	private void monitorDataSubscribeRemove(ServiceItem si) {
		String snvKey = si.getKey().toSnv();
		if(!activeMonitors.contains(snvKey)) {
			return;
		}
		
		activeMonitors.remove(snvKey);
		
		Set<Short> ts = monitorKey2Types.get(snvKey);
		deleteMonitorTypes(ts);
		
	}

	private void monitorDataSubscribeAdd(ServiceItem si) {
		String snvKey = si.getKey().toSnv();
		if(activeMonitors.contains(snvKey)) {
			return;
		}
		
		activeMonitors.add(snvKey);
		Set<Short> ts = monitorKey2Types.get(snvKey);
		if(ts == null || ts.isEmpty()) {
			return;
		}
		
		monitorTypes.addAll(ts);
	}

	public boolean canSubmit(ServiceMethod sm , Short t) {
		boolean en = nonRpcContext(t);
		if(!en || sm == null) {
			return en;
		}
		
		String mkey = sm.getKey().toKey(false, false, false);
		if(sm2Enable.containsKey(mkey)) {
			return sm2Enable.get(mkey);
		}
		
		compute(mkey,sm,t);
		
		return sm2Enable.get(mkey);
		
	}
	
	private synchronized void compute(String mkey, ServiceMethod sm, Short type) {
		boolean en = false;
		
		Set<Short> ts = srvKey2Types.get(mkey);
		
		String insName = sm.getKey().getUsk().getInstanceName();
		if(!ins2sms.containsKey(insName)) {
			ins2sms.put(insName, new HashSet<>());
		}
		
		ins2sms.get(insName).add(mkey);
		
		String srvKey = sm.getKey().getUsk().toSnv();
		if(!srv2sms.containsKey(srvKey)) {
			srv2sms.put(srvKey, new HashSet<>());
		}
		srv2sms.get(srvKey).add(mkey);
		
		if(ts != null) {
			en = ts.contains(type);
			if(en) {
				sm2Enable.put(mkey, true);
				return;
			}
		}
		
		//主机级
		srvKey2Types.get(sm.getKey().getUsk().getInstanceName());
		if(ts != null) {
			en = ts.contains(type);
			if(en) {
				sm2Enable.put(mkey, true);
				return;
			}
		}
		
		//服务级
		ts = srvKey2Types.get(srvKey);
		if(ts != null) {
			en = ts.contains(type);
			if(en) {
				sm2Enable.put(mkey, true);
				return;
			}
		}
		
		//方法级
		ts = srvKey2Types.get(sm.getKey().toKey(false, false, false));
		if(ts != null) {
			en = ts.contains(type);
			if(en) {
				sm2Enable.put(mkey, true);
				return;
			}
		}
		
		sm2Enable.put(mkey, false);
	}

	private boolean nonRpcContext(Short t) {
		return monitorTypes.contains(t);
	}

	private void doSrvMoTypeDelete(String skey, String data) {
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		Map<String,Set<Short>> key2Types = srvKey2Types;
		key2Types.remove(skey);
		clearCache(skey);
	}
	
	private void doSrvMoTypeUpdate(String skey, String data) {

		if(StringUtils.isEmpty(data)) {
			doSrvMoTypeDelete(skey,data);
			return;
		}
		
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		
		Map<String,Set<Short>> key2Types = srvKey2Types;
		
		Set<Short> oldTs = key2Types.get(skey);
		if(oldTs == null || oldTs.isEmpty()) {
			doSrvMoTypeAdd(skey,data);
			return;
		}
		
		Set<Short> newTs = new HashSet<Short>();
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			newTs.add(v);
		}
		
		Set<Short> delTs0 = new HashSet<Short>();
		delTs0.addAll(oldTs);
		
		//计算被删除的类型，delTs0中剩下的就是被删除元素
		delTs0.removeAll(newTs);
		
		//计算新增的类型，newTs中剩下都是新增类型 
		newTs.removeAll(oldTs);
		
		//作为新增元素加到集合中
		oldTs.addAll(newTs);
		oldTs.removeAll(delTs0);
		
		clearCache(skey);
	}

	private void doMonitorMoTypeUpdate(String skey, String data) {

		if(StringUtils.isEmpty(data)) {
			doMonitorTypeDelete(skey,data);
			return;
		}
		
		Map<String,Set<Short>> key2Types = monitorKey2Types;
		Set<Short> oldTs = key2Types.get(skey);
		if(oldTs == null || oldTs.isEmpty()) {
			doMonitorMoTypeAdd(skey,data);
			return;
		}
		
		
		Set<Short> newTs = new HashSet<Short>();
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			newTs.add(v);
		}
		
		Set<Short> delTs0 = new HashSet<Short>();
		delTs0.addAll(oldTs);
		
		//计算被删除的类型，delTs0中剩下的就是被删除元素
		delTs0.removeAll(newTs);
		
		//计算新增的类型，newTs中剩下都是新增类型 
		newTs.removeAll(oldTs);
		
		//作为新增元素加到集合中
		oldTs.addAll(newTs);
		oldTs.removeAll(delTs0);
		
		//利用集合自动重
		if(this.activeMonitors.contains(skey)) {
			this.monitorTypes.addAll(newTs);
			this.deleteMonitorTypes(delTs0);
		}
		
	}

	private void doSrvMoTypeAdd(String skey, String data) {
		
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		
		Map<String,Set<Short>> key2Types = srvKey2Types;
		key2Types.put(skey, new HashSet<Short>());
		if(StringUtils.isEmpty(data)) {
			return;
		}
		
		Set<Short> ts = new HashSet<Short>();
		key2Types.put(skey, ts);
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			ts.add(v);
		}
		
		clearCache(skey);
		
	}
	
	private void doMonitorMoTypeAdd(String skey, String data) {
		
		Map<String,Set<Short>> key2Types = monitorKey2Types;
		
		if(StringUtils.isEmpty(data)) {
			key2Types.put(skey, new HashSet<Short>());
			return;
		}
		
		Set<Short> ts = new HashSet<Short>();
		key2Types.put(skey, ts);
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			ts.add(v);
		}
		
		if(this.activeMonitors.contains(skey)) {
			this.monitorTypes.addAll(ts);
		}
		
	}
	
	private synchronized void clearCache(String key) {
		if(this.sm2Enable.containsKey(key)) {
			this.sm2Enable.remove(key);
			return;
		}
		
		if(this.ins2sms.containsKey(key)) {
			Set<String> mkeys = this.ins2sms.get(key);
			for(String mk: mkeys) {
				if(this.sm2Enable.containsKey(mk)) {
					this.sm2Enable.remove(mk);
				}
			}
		}
		
		if(this.srv2sms.containsKey(key)) {
			Set<String> mkeys = this.srv2sms.get(key);
			for(String mk: mkeys) {
				if(this.sm2Enable.containsKey(mk)) {
					this.sm2Enable.remove(mk);
				}
			}
		}
		
	}
	
}
