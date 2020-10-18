package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.monitor.LogWarningConfig;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogWarningConfig {

	Resp<List<LogWarningConfig>> query();
	
	Resp<Boolean> update(LogWarningConfig cfg);
	
	Resp<Boolean> delete(String id);
	
	Resp<LogWarningConfig> add(LogWarningConfig cfg);
}
