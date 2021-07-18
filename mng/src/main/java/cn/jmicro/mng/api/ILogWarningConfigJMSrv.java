package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.monitor.LogWarningConfigJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogWarningConfigJMSrv {

	RespJRso<List<LogWarningConfigJRso>> query();
	
	RespJRso<Boolean> update(LogWarningConfigJRso cfg);
	
	RespJRso<Boolean> delete(String id);
	
	RespJRso<LogWarningConfigJRso> add(LogWarningConfigJRso cfg);
}
