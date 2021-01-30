package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.codegenerator.AsyncClientProxy;

@Service(timeout=5000,retryCnt=0,debugMode=0,monitorEnable=0,logLevel=MC.LOG_ERROR,showFront=false)
@AsyncClientProxy
public interface IMonitorAdapter {

	MonitorInfo info();
	
	void enableMonitor(boolean enable);
	
	MonitorServerStatus status();
	
	void reset();
	
}
