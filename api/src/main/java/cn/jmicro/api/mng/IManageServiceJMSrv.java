package cn.jmicro.api.mng;

import java.util.Set;

import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IManageServiceJMSrv {

	Set<ServiceItemJRso> getServices(boolean all);
	
	boolean updateMethod(ServiceMethodJRso method);
	
	boolean updateItem(ServiceItemJRso item);
	
}
