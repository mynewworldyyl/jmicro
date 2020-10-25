package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAgentLogService {

	Resp<List<LogFileEntry>> getAllLogFileEntry();
	
	Resp<Boolean> startLogMonitor(Integer processId,String logPath,String agentId, int offsetFromLastLine);
	
	Resp<Boolean> stopLogMonitor(Integer processId,String logPath,String agentId);
}
