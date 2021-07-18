package cn.jmicro.api.monitor;

import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IOutterMonitorServiceJMSrv {

	void submit(Set<OneLogJRso> logs);
	
}
