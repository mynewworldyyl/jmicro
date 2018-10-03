package org.jmicro.api.objectfactory;

public interface IPostInitListener {

	public void preInit(Object obj);
	
	public void afterInit(Object obj);
}
