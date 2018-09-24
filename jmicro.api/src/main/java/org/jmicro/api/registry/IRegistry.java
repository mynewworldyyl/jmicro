package org.jmicro.api.registry;

import java.util.Set;

public interface IRegistry {

	void regist(String url);
	
	void unregist(String url);
	
	Set<String> getServices();
}
