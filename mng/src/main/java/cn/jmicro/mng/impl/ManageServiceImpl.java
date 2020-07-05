package cn.jmicro.mng.impl;

import java.util.Set;
import java.util.TreeSet;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.mng.IManageService;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.service.ServiceManager;

@Component
@Service(namespace="mng", version="0.0.1",external=true,timeout=10000,debugMode=1)
public class ManageServiceImpl implements IManageService {

	@Cfg(value="/notLonginClientId",defGlobal=true)
	private int notLonginClientId = 10;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Override
	public Set<ServiceItem> getServices() {
		 Set<ServiceItem> items = srvManager.getAllItems();
		 if(items == null || items.isEmpty()) {
			 return null;
		 }
		 
		 Set<ServiceItem> sis = new TreeSet<>();
		 ActInfo ai = JMicroContext.get().getAccount();
		 if(ai != null) {
			 for(ServiceItem si : items) {
				 if(si.getClientId() >= ai.getClientId()) {
					 sis.add(si);
				 }
			 }
		 } else {
			 for(ServiceItem si : items) {
				 if(si.getClientId() >= this.notLonginClientId) {
					 sis.add(si);
				 }
			 }
		 }
		 return sis;
	}

	@Override
	public boolean updateItem(ServiceItem item) {
	    ServiceItem si = srvManager.getServiceByKey(item.getKey().toKey(true, true, true));
	    
	    if(si != null) {
	    	si.setAvgResponseTime(item.getAvgResponseTime());
	    	si.setBaseTimeUnit(item.getBaseTimeUnit());
	    	si.setCheckInterval(item.getCheckInterval());
	    	si.setDebugMode(item.getDebugMode());
	    	si.setDegrade(item.getDebugMode());
	    	si.setMaxSpeed(item.getMaxSpeed());
	    	si.setLogLevel(item.getLogLevel());
	    	si.setMonitorEnable(item.getMonitorEnable());
	    	si.setRetryCnt(item.getRetryCnt());
	    	si.setRetryInterval(item.getRetryInterval());
	    	si.setSlotSize(item.getSlotSize());
	    	si.setTimeout(item.getTimeout());
	    	si.setTimeUnit(item.getTimeUnit());
	    	si.setTimeWindow(item.getTimeWindow());
	    	reg.update(si);
	    	return true;
	    }
		return false;
	}
	
	@Override
	public boolean updateMethod(ServiceMethod method) {
		
		 ServiceItem si = srvManager.getServiceByServiceMethod(method);
		    if(si != null) {
		    	ServiceMethod sm = si.getMethod(method.getKey().getMethod(), method.getKey().getParamsStr());
		    	if(sm != null) {
		    		sm.setAvgResponseTime(method.getAvgResponseTime());
		    		sm.setBaseTimeUnit(method.getBaseTimeUnit());
		    		sm.setCheckInterval(method.getCheckInterval());
		    		sm.setDebugMode(method.getDebugMode());
		    		sm.setDegrade(method.getDebugMode());
		    		sm.setMaxSpeed(method.getMaxSpeed());
		    		sm.setLogLevel(method.getLogLevel());
		    		sm.setMonitorEnable(method.getMonitorEnable());
		    		sm.setRetryCnt(method.getRetryCnt());
		    		sm.setRetryInterval(method.getRetryInterval());
		    		sm.setSlotInterval(method.getSlotInterval());
		    		sm.setTimeout(method.getTimeout());
		    		sm.setBaseTimeUnit(method.getBaseTimeUnit());
		    		sm.setTimeWindow(method.getTimeWindow());
		    		sm.setDumpDownStream(method.isDumpDownStream());
		    		sm.setDumpUpStream(method.isDumpUpStream());
		    		sm.setBreakingRule(method.getBreakingRule());
		    		sm.setAsyncable(method.isAsyncable());
		    		sm.setBreaking(method.isBreaking());
		    		sm.setFailResponse(method.getFailResponse());
		    		sm.setMaxFailBeforeDegrade(method.getMaxFailBeforeDegrade());
		    		sm.setTestingArgs(method.getTestingArgs());
		    		sm.setTopic(method.getTopic());
		    		
			    	reg.update(si);
			    	return true;
		    	}
		    	
		    }
			return false;
	}

}
