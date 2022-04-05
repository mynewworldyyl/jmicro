package cn.jmicro.api.objectsource;

import java.util.Set;

public interface IObjectSource {
	
	public static final ThreadLocal<IObjectSource> sysOS = new ThreadLocal<>();
	
	public static IObjectSource getObjectSource() {
		return sysOS.get();
	}
	
	public static void setObjectSource(IObjectSource s) {
		sysOS.set(s);
	}

	<T> T get(Class type);
	
	<T> T  getByName(String name);
	
	<T> Set<T> getByParent(Class type);
	
}
