package org.jmicro.api.mng;

import java.util.Set;

import org.jmicro.api.registry.ServiceItem;

public interface IManagerService {

	Set<ServiceItem> getAllItems();
	
	boolean deleteItem(String path);
	
	
	boolean updateItem(ServiceItem item);
	
	
	
	
}
