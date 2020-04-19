package org.jmicro.mng.inter;

import org.jmicro.api.monitor.v2.MonitorInfo;
import org.jmicro.api.monitor.v2.MonitorServerStatus;

public interface IMonitorServerManager {

	 MonitorServerStatus[] status(String[] srvKeys);
	
	 boolean enable(String srvKey,Boolean enable);
	
	 MonitorInfo[]  serverList();
	 
	 void reset(String[] srvKeys);
	
}
