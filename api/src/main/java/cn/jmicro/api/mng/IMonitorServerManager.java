package cn.jmicro.api.mng;

import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMonitorServerManager {

	 MonitorServerStatus[] status(String[] srvKeys);
	
	 boolean enable(String srvKey,Boolean enable);
	
	 MonitorInfo[]  serverList();
	 
	 void reset(String[] srvKeys);
	
}
