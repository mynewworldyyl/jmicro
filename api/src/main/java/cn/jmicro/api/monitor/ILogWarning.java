package cn.jmicro.api.monitor;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogWarning {

	void warn(MRpcLogItem log);
	
	//LogWarningConfig getConfig();
}
