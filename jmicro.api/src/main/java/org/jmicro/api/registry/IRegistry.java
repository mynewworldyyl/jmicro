package org.jmicro.api.registry;

import java.util.Set;

import org.jmicro.api.Init;

public interface IRegistry extends Init{

	void regist(ServiceItem url);
	
	void unregist(ServiceItem url);
	
	Set<ServiceItem> getServices(String serviceName);
	
	boolean isExist(String serviceName);
}
