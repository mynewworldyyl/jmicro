package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.LogFileEntryJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAgentLogServiceJMSrv {

	IPromise<RespJRso<List<LogFileEntryJRso>>> getAllLogFileEntry();
	
	IPromise<RespJRso<Boolean>> startLogMonitor(Integer processId,String logPath,String agentId, int offsetFromLastLine);
	
	IPromise<RespJRso<Boolean>> stopLogMonitor(Integer processId,String logPath,String agentId);
}
