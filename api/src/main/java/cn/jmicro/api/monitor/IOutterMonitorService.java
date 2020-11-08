package cn.jmicro.api.monitor;

import java.util.Set;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IOutterMonitorService {

	void submit(Set<OneLog> logs);
	
}
