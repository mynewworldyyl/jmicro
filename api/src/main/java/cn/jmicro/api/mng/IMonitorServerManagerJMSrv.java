package cn.jmicro.api.mng;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MonitorInfoJRso;
import cn.jmicro.api.monitor.MonitorServerStatusJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMonitorServerManagerJMSrv {

	IPromise<MonitorServerStatusJRso[]> status(String[] srvKeys);
	
	IPromise<Boolean> enable(String srvKey,Boolean enable);
	
	IPromise<MonitorInfoJRso[]>  serverList();
	 
	IPromise<Void> reset(String[] srvKeys);
	
}
