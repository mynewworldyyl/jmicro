package org.jmicro.mng.impl;

import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.mng.IManageService;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceManager;

@Component
@Service(namespace="manageService", version="0.0.1")
public class ManageServiceImpl implements IManageService {

	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Override
	public Set<ServiceItem> getServices() {
		 Set<ServiceItem> items = srvManager.getAllItems();
		 return items;
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
		    		sm.setSlotSize(method.getSlotSize());
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
