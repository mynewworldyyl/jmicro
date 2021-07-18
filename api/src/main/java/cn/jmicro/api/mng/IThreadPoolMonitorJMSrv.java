package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.executor.ExecutorInfoJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IThreadPoolMonitorJMSrv {

	RespJRso<List<ExecutorInfoJRso>> serverList();
	
	RespJRso<List<ExecutorInfoJRso>> getInfo(String key,String type);
	
}
