package org.jmicro.api.monitor.v2;

public interface IMonitorAdapter {

	MonitorInfo info();
	
	void enableMonitor(boolean enable);
	
	MonitorServerStatus status();
	
	void reset();
	
}
