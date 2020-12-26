package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAgentLogService {

	IPromise<Resp<List<LogFileEntry>>> getAllLogFileEntry();
	
	IPromise<Resp<Boolean>> startLogMonitor(Integer processId,String logPath,String agentId, int offsetFromLastLine);
	
	IPromise<Resp<Boolean>> stopLogMonitor(Integer processId,String logPath,String agentId);
}
