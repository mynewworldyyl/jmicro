package org.jmicro.api.objectfactory;

import java.util.List;

public interface IObjectFactory {

	//<T> T createObject(Class<T> cls);
	
	<T> T get(Class<T> cls);
	<T> T getByName(String clsName);
	
	<T> List<T> getByParent(Class<T> parrentCls);
	
	void init();
	
}
