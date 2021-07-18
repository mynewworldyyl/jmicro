package cn.jmicro.api.monitor;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogWarningJMSrv {

	void warn(JMLogItemJRso log);
	
	//LogWarningConfig getConfig();
}
