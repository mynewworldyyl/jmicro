package cn.jmicro.api.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
public class NamedTypeManager {

	private final static Logger logger = LoggerFactory.getLogger(NamedTypeManager.class);
	
	private static final String TYPE_SPERATOR = MonitorAndService2TypeRelationshipManager.TYPE_SPERATOR; 
	
	@Inject
	private IDataOperator op;
	
	private Map<String,Set<Short>> named2Types = new HashMap<>();
	
	private Map<String,Set<INamedTypeListener>> listeners = new HashMap<>();
	
	private IDataListener srvTypeDataChangeListener = (path,data) -> {
		String skey = path.substring(Config.NamedTypesDir.length()+1);
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
	
	public void ready() {
		if(!op.exist(Config.NamedTypesDir)) {
			op.createNodeOrSetData(Config.NamedTypesDir, "", IDataOperator.PERSISTENT);
		}
		op.addChildrenListener(Config.NamedTypesDir,srvChildrenListener);
	}

	private void doSrvMoTypeDelete(String skey, String data) {
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		Map<String,Set<Short>> key2Types = named2Types;
		Set<Short> change = key2Types.remove(skey);
		
		if(!change.isEmpty()) {
			this.notifyChange(IListener.REMOVE, skey, change);
		}
		
	}
	
	private void doSrvMoTypeUpdate(String skey, String data) {

		if(StringUtils.isEmpty(data)) {
			doSrvMoTypeDelete(skey,data);
			return;
		}
		
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		
		Map<String,Set<Short>> key2Types = named2Types;
		
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
		
		if(!newTs.isEmpty()) {
			Set<Short> change = new HashSet<Short>();
			change.addAll(newTs);
			this.notifyChange(IListener.ADD, skey, change);
		}
		
		if(!delTs0.isEmpty()) {
			Set<Short> change = new HashSet<Short>();
			change.addAll(delTs0);
			this.notifyChange(IListener.REMOVE, skey, change);
		}
		
	}
	
	private void doSrvMoTypeAdd(String skey, String data) {
		
		if(skey.contains(Constants.PATH_EXCAPE)) {
			skey = skey.replaceAll(Constants.PATH_EXCAPE, "/");
		}
		
		Map<String,Set<Short>> key2Types = this.named2Types;
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
		
		Set<Short> change = new HashSet<Short>();
		change.addAll(ts);
		this.notifyChange(IListener.ADD, skey, change);
	}
	
	public void addListener(String name, INamedTypeListener l) {
		if(!this.listeners.containsKey(name)) {
			this.listeners.put(name, new HashSet<INamedTypeListener>());
		}
		Set<INamedTypeListener> ls = this.listeners.get(name);
		if(!ls.contains(l)) {
			ls.add(l);
		} else {
			logger.warn(name + " listener " + l.getClass().getName() + " is exist!");
		}
	}
	
	public void removeListener(String name, INamedTypeListener l) {
		if(!this.listeners.containsKey(name)) {
			return;
		}
		Set<INamedTypeListener> ls = this.listeners.get(name);
		if(ls.contains(l)) {
			ls.remove(l);
		}
	}
	
	private void notifyChange(int type, String name, Set<Short> types) {
		if(!this.listeners.containsKey(name)) {
			return;
		}
		
		Set<INamedTypeListener> ls = this.listeners.get(name);
		if(ls.isEmpty()) {
			return;
		}
		
		for(INamedTypeListener l : ls) {
			l.namedTypeChange(type, name, types);
		}
		
	}
	
}
