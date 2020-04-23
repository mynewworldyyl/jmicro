package org.jmicro.api.monitor.v2;

import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.v1.MonitorConstant;

@Service(timeout=5000,retryCnt=0,debugMode=0,
monitorEnable=0,logLevel=MonitorConstant.LOG_ERROR)
public interface IMonitorAdapter {

	MonitorInfo info();
	
	void enableMonitor(boolean enable);
	
	MonitorServerStatus status();
	
	void reset();
	
}
