package cn.jmicro.api.choreography;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.Utils;

@AsyncClientProxy
public interface IAgentProcessService {

	Set<ProcessInfo> getProcessesByDepId(String depId);
	
	Set<ProcessInfo> getAllProcesses();
	
	List<LogFileEntry> getProcessesLogFileList();
	
	LogFileEntry getItselfLogFileList();
	
	String agentId();
	
	boolean startLogMonitor(Integer processId, String logFile, int offsetFromLastLine);
	
	boolean stopLogMonitor(Integer processId, String logFile);
	
	public static Set<String> parseJvmArgs(String argStr) {
		if(Utils.isEmpty(argStr)) {
			return Collections.EMPTY_SET;
		}
		Set<String> set = new HashSet<>();
		String[] args = argStr.split("\\s+");
		if(args != null && args.length > 0) {
			for(String a : args) {
				set.add(a);
			}
		}
		return set;
	}
}
