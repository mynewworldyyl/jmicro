package cn.jmicro.api.mng;

import java.util.Set;

import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IManageService {

	Set<ServiceItem> getServices();
	
	boolean updateMethod(ServiceMethod method);
	
	boolean updateItem(ServiceItem item);
	
}
