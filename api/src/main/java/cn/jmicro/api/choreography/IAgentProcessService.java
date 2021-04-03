package cn.jmicro.api.choreography;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@AsyncClientProxy
public interface IAgentProcessService {

	Set<ProcessInfo> getProcessesByDepId(String depId);
	
	Set<ProcessInfo> getAllProcesses();
	
	List<LogFileEntry> getProcessesLogFileList();
	
	LogFileEntry getItselfLogFileList();
	
	String agentId();
	
	boolean startLogMonitor(Integer processId, String logFile, int offsetFromLastLine);
	
	boolean stopLogMonitor(Integer processId, String logFile);
	
	public static Map<String,String> parseProgramArgs(String argStr) {
		
		Map<String,String> params = new HashMap<>();
		
		if(Utils.isEmpty(argStr)) {
			return params;
		}
		
		String[] args = argStr.split("\\s+");
		
		for(String arg : args){
			if(Utils.isEmpty(arg)) {
				continue;
			}
			if(arg.startsWith("-D")){
				String ar = arg.substring(2);
				if(StringUtils.isEmpty(ar)){
					throw new CommonException("Invalid arg: "+ arg);
				}
				ar = ar.trim();
				if(ar.indexOf("=") > 0){
					String[] ars = ar.split("=");
					params.put(ars[0].trim(), ars[1].trim());
				} else {
					params.put(ar, null);
				}
			} else {
				throw new CommonException("Invalid program arg: " + arg);
			}
		}
		
		return params;
	}
	
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
