package cn.jmicro.choreography.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.common.CommonException;
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

	boolean doStrategy(List<AgentInfo> agents, Deployment dep);
	
	public static Map<String,String> parseArgs(String argStr) {
		
		Map<String,String> params = new HashMap<>();
		
		String[] args = argStr.split("\\s+");
		
		for(String arg : args){
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
			}
		}
		
		return params;
	}
}
