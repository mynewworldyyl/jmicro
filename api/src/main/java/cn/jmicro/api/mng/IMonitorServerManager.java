package cn.jmicro.api.mng;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMonitorServerManager {

	IPromise<MonitorServerStatus[]> status(String[] srvKeys);
	
	IPromise<Boolean> enable(String srvKey,Boolean enable);
	
	IPromise<MonitorInfo[]>  serverList();
	 
	IPromise<Void> reset(String[] srvKeys);
	
}
