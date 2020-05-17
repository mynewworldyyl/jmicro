package cn.jmicro.mng.api;

import cn.jmicro.api.monitor.v2.MonitorInfo;
import cn.jmicro.api.monitor.v2.MonitorServerStatus;

public interface IMonitorServerManager {

	 MonitorServerStatus[] status(String[] srvKeys);
	
	 boolean enable(String srvKey,Boolean enable);
	
	 MonitorInfo[]  serverList();
	 
	 void reset(String[] srvKeys);
	
}
