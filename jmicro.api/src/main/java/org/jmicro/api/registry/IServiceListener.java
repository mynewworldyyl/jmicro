package org.jmicro.api.registry;

public interface IServiceListener {

	public static final int SERVICE_ADD = 1;
	public static final int SERVICE_REMOVE = 2;
	
	public static final int SERVICE_DATA_CHANGE = 3;
	
	void serviceChanged(int type,ServiceItem item);
	
	//void serviceRemove(ServiceItem item);
}
