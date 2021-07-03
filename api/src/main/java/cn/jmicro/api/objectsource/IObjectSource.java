package cn.jmicro.api.objectsource;

import java.util.Set;

public interface IObjectSource {

	<T> T get(Class type);
	
	<T> T  getByName(String name);
	
	<T> Set<T> getByParent(Class type);
	
}
