package cn.jmicro.choreography.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

public interface IAssignStrategy {
	
	public static final String AGENT_ID = "agentId";
	
	public static final String AGENT_HOST = "agentHost";
	
	public static final String AGENT_NAME = "agentName";
	
	//最小内存
	public static final String MEM_MIN_FREE = "minFreeMemory";
	
	//最大CPU使用率
	public static final String CPU_MAX_RATE = "maxCPURate";
	
	//最小内核数量
	public static final String CORE_NUM = "coreNum";
	
	public static final String INSTANCE_NUM = "instanceNum";
	
	public static final String SORT_PRIORITY = "sortPriority";

	boolean doStrategy(List<AgentInfo> agents, Deployment dep);
	
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
