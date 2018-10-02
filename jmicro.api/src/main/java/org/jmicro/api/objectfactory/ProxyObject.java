package org.jmicro.api.objectfactory;

public interface ProxyObject {

	public static Object getTarget(Object proxy){
		if(proxy instanceof ProxyObject){
			return ((ProxyObject)proxy).getTarget();
		}
		return proxy;
	}
	
	public static Class<?> getTargetCls(Class<?> proxyCls){
		if(ProxyObject.class.isAssignableFrom(proxyCls)){
			proxyCls = proxyCls.getSuperclass();
		}
		return proxyCls;
	}
	
	public Object getTarget();
}
