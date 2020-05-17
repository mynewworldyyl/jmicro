package cn.jmicro.api.monitor.v2;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.v1.MonitorConstant;

@Service(timeout=5000,retryCnt=0,debugMode=0,
monitorEnable=0,logLevel=MonitorConstant.LOG_ERROR)
public interface IMonitorAdapter {

	MonitorInfo info();
	
	void enableMonitor(boolean enable);
	
	MonitorServerStatus status();
	
	void reset();
	
}
