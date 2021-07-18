package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MonitorInfoJRso;
import cn.jmicro.api.monitor.MonitorServerStatusJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@Service(timeout=5000,retryCnt=0,debugMode=0,monitorEnable=0,logLevel=MC.LOG_ERROR,showFront=false)
@AsyncClientProxy
public interface IMonitorAdapterJMSrv {

	MonitorInfoJRso info();
	
	void enableMonitor(boolean enable);
	
	MonitorServerStatusJRso status();
	
	void reset();
	
}
