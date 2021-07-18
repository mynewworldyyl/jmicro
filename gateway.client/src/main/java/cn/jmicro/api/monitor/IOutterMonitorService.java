package cn.jmicro.api.monitor;

import java.util.Set;


public interface IOutterMonitorService {

	void submit(Set<OneLogJRso> logs);
	
}
