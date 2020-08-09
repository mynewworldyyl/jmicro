package cn.jmicro.api.choreography;

import java.util.List;
import java.util.Set;

import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAgentProcessService {

	Set<ProcessInfo> getProcessesByDepId(String depId);
	
	Set<ProcessInfo> getAllProcesses();
	
	List<LogFileEntry> getProcessesLogFileList();
	
	LogFileEntry getItselfLogFileList();
	
	String agentId();
	
	boolean startLogMonitor(String processId, String logFile, int offsetFromLastLine);
	
	boolean stopLogMonitor(String processId, String logFile);
	
}
