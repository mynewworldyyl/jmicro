package cn.jmicro.api.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.service.ServiceManager;

@Component
public class DictManager {

	private Map<String,Object> dicts = new HashMap<>();
	
	@Inject
	private ServiceManager sm;
	
	@Inject
	private ProcessInstanceManager insManager;
	
	public void ready() {
		this.mergeDict("logKey2Val", MC.LogKey2Val);
		this.mergeDict("mtKey2Val", MC.MT_Key2Val);
		Map<String,String> map = new HashMap<>();
		for(Map.Entry<Short, String> e : MC.MONITOR_VAL_2_KEY.entrySet()) {
			map.put(e.getKey().toString(), e.getValue());
		}
		this.mergeDict("mtKey2Val", map);
	}
	
	public Object getDict(String key) {
		return dicts.get(key);
	}
	
	public <T> void mergeDict(String key,Set<T> dict) {
		if(dicts.containsKey(key)) {
			Set<Object> set = (Set<Object>) dicts.get(key);
			set.addAll(dict);
		} else {
			Set<Object> set = new HashSet<>();
			set.addAll(dict);
			dicts.put(key, set);
		}
	}
	
	public <T> void mergeDict(String key,List<T> dict) {
		if(dicts.containsKey(key)) {
			List<Object> set = (List<Object>) dicts.get(key);
			set.addAll(dict);
		} else {
			List<Object> set = new ArrayList<>();
			set.addAll(dict);
			dicts.put(key, set);
		}
	}
	
	public <T> void mergeDict(String key, Map<String,T> dict) {
		if(dicts.containsKey(key)) {
			Map<String,Object> set = (Map<String,Object>) dicts.get(key);
			set.putAll(dict);
		} else {
			Map<String,Object> set = new HashMap<>();
			set.putAll(dict);
			dicts.put(key, set);
		}
	}
	
	public <T> void mergeDict(String key, T[] dict) {
		if(dicts.containsKey(key)) {
			Object[] set = (Object[]) dicts.get(key);
			Object[] newArr = new Object[dict.length + set.length];
			System.arraycopy(set, 0, newArr, 0, set.length);
			System.arraycopy(dict, 0, newArr, set.length, dict.length);
			dicts.put(key, newArr);
		} else {
			Object[] newArr = new Object[dict.length];
			System.arraycopy(dict, 0, newArr, 0, dict.length);
			dicts.put(key, newArr);
		}
	}
	
	public Set<String> serviceNames(String prefix,int loginClientId) {
		return this.sm.serviceNames(prefix,loginClientId);
	}
	
	public Set<String> serviceNamespaces(String sn,int loginClientId) {
		return this.sm.serviceNamespaces(sn,loginClientId);
	}
	
	public Set<String> serviceVersions(String sn,int loginClientId) {
		return this.sm.serviceVersions(sn,loginClientId);
	}
	
	public Set<String> serviceMethods(String sn,int loginClientId) {
		return this.sm.serviceMethods(sn,loginClientId);
	}
	
	public Set<String> serviceInstances(String sn,int loginClientId) {
		return this.sm.serviceInstances(sn,loginClientId);
	}
	
	public Set<String> resourceNames() {
		Set<String> insNames = new HashSet<>();
		this.insManager.forEach((pi)->{
			insNames.addAll(pi.getMetadatas().keySet());
		});
		return insNames;
	}
}
