package cn.jmicro.mng.impl;

import java.util.Set;
import java.util.TreeSet;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.IManageServiceJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.mng.Namespace;

@Component
@Service(version="0.0.1",namespace=Namespace.NS,external=true,timeout=10000,debugMode=1,showFront=false,logLevel=MC.LOG_NO)
public class ManageServiceImpl implements IManageServiceJMSrv {

	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=256)
	public Set<ServiceItemJRso> getServices(boolean all) {
		 Set<UniqueServiceKeyJRso> items = srvManager.getAllItems();
		 if(items == null || items.isEmpty()) {
			 return null;
		 }
		 
		 Set<ServiceItemJRso> sis = new TreeSet<>();
		 
		 if(all && PermissionManager.isCurAdmin(Config.getClientId())) {
			 for(UniqueServiceKeyJRso key : items) {
				 ServiceItemJRso sij = this.srvManager.getServiceByKey(key.fullStringKey());
				 if(sij == null) continue;
				 sis.add(sij);
			 }
			 return sis;
		 }

		 for(UniqueServiceKeyJRso si : items) {
			 if(PermissionManager.isOwner(si.getCreatedBy())) {
				 ServiceItemJRso sij = this.srvManager.getServiceByKey(si.fullStringKey());
				 if(sij != null && (all || sij.isShowFront())) {
					 sis.add(sij);
				 }
			 }
		 }
	 
		 return sis;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public boolean updateItem(ServiceItemJRso item) {
	    ServiceItemJRso si = srvManager.getServiceByKey(item.getKey().fullStringKey());
	    
	    if(si != null) {
	    	 if(!PermissionManager.isOwner(si.getActId())) {
				return false;
			 }
	    	
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
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=1014)
	public boolean updateMethod(ServiceMethodJRso method) {
		
		 ServiceItemJRso si = srvManager.getServiceByServiceMethod(method);
		    if(si != null) {
		    	 if(!PermissionManager.isOwner(si.getActId())) {
						return false;
				  }
		    	 
		    	ServiceMethodJRso sm = si.getMethod(method.getKey().getMethod(), method.getKey().getParamsStr());
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
		    		sm.setDownSsl(method.isDownSsl());
		    		sm.setUpSsl(method.isUpSsl());
		    		sm.setNeedLogin(method.isNeedLogin());
		    		sm.setPerType(method.isPerType());
		    		sm.setEncType(method.getEncType());
		    		sm.setMaxPacketSize(method.getMaxPacketSize());
		    		sm.setFeeType(method.getFeeType());
		    		sm.setLimitType(method.getLimitType());
		    		
			    	reg.update(si);
			    	return true;
		    	}
		    }
			return false;
	}

}
