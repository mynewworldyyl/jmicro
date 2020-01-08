package org.jmicro.rcptool.main.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
@Component
public class StatisDataListenerManager {

	private final static Logger logger = LoggerFactory.getLogger(StatisDataListenerManager.class);
	
	private Map<String,Set<IStatisDataListener>> srv2Listeners = new HashMap<>();
	private Map<String,Set<IStatisDataListener>> method2Listeners = new HashMap<>();
	private Map<String,Set<IStatisDataListener>> srvInstance2Listeners = new HashMap<>();
	
	private Map<String,Set<IStatisDataListener>> srvMethod2Listeners = new HashMap<>();
	
	public void notifyData(ServiceMethod sm,Map<Integer,Double> data) {
		if(sm == null) {
			return ;
		}

		if(srv2Listeners.containsKey(sm.getKey().getServiceName())) {
			doNotify(srv2Listeners.get(sm.getKey().getServiceName()),sm,data);
		}
		
		if(method2Listeners.containsKey(sm.getKey().getMethod())) {
			doNotify(method2Listeners.get(sm.getKey().getMethod()),sm,data);
		}
		
		if(srvInstance2Listeners.containsKey(sm.getKey().getInstanceName())) {
			doNotify(srvInstance2Listeners.get(sm.getKey().getInstanceName()),sm,data);
		}
		
		String k = sm.getKey().getServiceName() + ServiceItem.KEY_SEPERATOR + sm.getKey().getMethod();
		if(srvMethod2Listeners.containsKey(k)) {
			doNotify(srvMethod2Listeners.get(k),sm,data);
		}
	
	}
	
	private void doNotify(Set<IStatisDataListener> lises,ServiceMethod sm,Map<Integer,Double> data) {
		if(lises != null && !lises.isEmpty()) {
			for(IStatisDataListener l : lises) {
				logger.info("sm:{},val:{}",sm,data);
				l.onData(sm, data);
			}
		}
	}
	
	public void registSrvMethod(String srv,String method,IStatisDataListener lis) {
		String k = srvMethodKey(srv,method);
		if(!srvMethod2Listeners.containsKey(k)) {
			srvMethod2Listeners.put(k, new HashSet<IStatisDataListener>());
		}
		
		if(!srvMethod2Listeners.get(k).contains(lis)) {
			srvMethod2Listeners.get(k).add(lis);
		}
	}
	
	public void unregistSrvMethod(String srv,String method,IStatisDataListener lis) {
		String k = srvMethodKey(srv,method);
		if(!srvMethod2Listeners.containsKey(k)) {
			return;
		}
		
		if(srvMethod2Listeners.get(k).contains(lis)) {
			srvMethod2Listeners.get(k).remove(lis);
		}
	}
	
	public String srvMethodKey(String srvName,String method) {
		return srvName + ServiceItem.KEY_SEPERATOR + method;
	}
	
}
