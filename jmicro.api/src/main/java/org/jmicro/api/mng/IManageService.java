package org.jmicro.api.mng;

import java.util.Set;

import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;

public interface IManageService {

	Set<ServiceItem> getServices();
	
	boolean updateMethod(ServiceMethod method);
	
	boolean updateItem(ServiceItem item);
	
}
