package cn.jmicro.choreography.assignment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.sysstatis.SystemStatis;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.util.StringUtils;

@Component(value="defautAssignStrategy")
public class DefautAssignStrategy implements IAssignStrategy{

	private final String[] DEFAULT_SORT_PRIORITY = {CPU_MAX_RATE,MEM_MIN_FREE,INSTANCE_NUM};
	
	private final static Logger logger = LoggerFactory.getLogger(DefautAssignStrategy.class);
	
	@Inject
	private InstanceManager insManager;
	
	@Override
	public boolean doStrategy(List<AgentInfo> agents, Deployment dep) {
		if(agents.size() == 0) {
			return false;
		}
		
		Map<String,String> params = null;
		if(StringUtils.isNotEmpty(dep.getStrategyArgs())) {
			params = IAssignStrategy.parseArgs(dep.getStrategyArgs());
			if(params.containsKey(IAssignStrategy.AGENT_HOST)) {
				filterHost(agents,params.get(IAssignStrategy.AGENT_HOST));
			}
			
			if(params.containsKey(IAssignStrategy.AGENT_ID)) {
				filterId(agents,params.get(IAssignStrategy.AGENT_ID));
			}
			
			if(params.containsKey(IAssignStrategy.AGENT_NAME)) {
				filterName(agents,params.get(IAssignStrategy.AGENT_NAME));
			}
			
			if(params.containsKey(IAssignStrategy.CORE_NUM)) {
				filterCoreNum(agents,params.get(IAssignStrategy.CORE_NUM),dep);
			}
			
			if(params.containsKey(IAssignStrategy.MEM_MIN_FREE)) {
				filteFreeMemory(agents,params.get(IAssignStrategy.MEM_MIN_FREE),dep);
			}
			
			if(params.containsKey(IAssignStrategy.CPU_MAX_RATE)) {
				filteCpuRate(agents,params.get(IAssignStrategy.CPU_MAX_RATE),dep);
			}
		}
		
		if(agents.size() == 1) {
			return true;
		}
		
		final String[] sorts;
		
		if(params == null || !params.containsKey(SORT_PRIORITY) || 
				StringUtils.isEmpty(params.get(SORT_PRIORITY))) {
			sorts = DEFAULT_SORT_PRIORITY;
		} else {
			sorts = params.get(SORT_PRIORITY).split(",");
		}
		
		agents.sort(( o1, o2)->{
			int rst = 0;
			for(int i = 0; i < sorts.length; i++) {
				rst = sort(sorts[i],o1,o2);
				if(rst != 0) {
					return rst;
				}
			}
			return rst;
		});
		
		return true;
		
	}
	
	private int sort(String sortBy, AgentInfo o1, AgentInfo o2) {
		if(StringUtils.isEmpty(sortBy)) {
			return 0;
		}
		
		int rst = 0;
		
		SystemStatis s1 = o1.getSs();
		SystemStatis s2 = o2.getSs();
		
		if(CPU_MAX_RATE.equals(sortBy.trim())) {
			rst = s1.getCpuLoad() > s2.getCpuLoad() ? -1: (s1.getCpuLoad() == s2.getCpuLoad()?0:1);
		}else if(MEM_MIN_FREE.equals(sortBy.trim())) {
			rst = s1.getFreeMemory() > s2.getFreeMemory() ? 1: (s1.getFreeMemory() == s2.getFreeMemory() ? 0 : -1);
		}else if(CORE_NUM.equals(sortBy.trim())) {
			rst = s1.getCpuNum() > s2.getCpuNum() ? 1: (s1.getCpuNum() == s2.getCpuNum() ? 0 : -1);
		}else if(INSTANCE_NUM.equals(sortBy.trim())) {
			int z1 = this.insManager.getProcessSizeByAgentId(o1.getId());
			int z2 = this.insManager.getProcessSizeByAgentId(o2.getId());
			rst = z1 > z2 ? -1: (z1 == z2 ? 0 : 1);
		}
		
		return rst;
	}

	private void filteCpuRate(List<AgentInfo> agents, String cpuRate, Deployment dep) {
		if(StringUtils.isEmpty(cpuRate)) {
			logger.error("cpuRate val is NULL for: [" +dep.toString() +"]");
			return;
		}
		
		double cn = Double.parseDouble(cpuRate);
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(ai.getSs() == null || ai.getSs().getAvgCpuLoad()*100 > cn) {
				ite.remove();
			}
		}
	}

	private void filteFreeMemory(List<AgentInfo> agents, String minFreeMem, Deployment dep) {
		if(StringUtils.isEmpty(minFreeMem)) {
			logger.error("minFreeMem val is NULL for: [" +dep.toString() +"]");
			return;
		}
		
		long cn = 0;
		if(minFreeMem.endsWith("B")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-1);
			cn = Long.parseLong(minFreeMem);
		}else if(minFreeMem.endsWith("K")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-1);
			cn = Long.parseLong(minFreeMem)*1024;
		}else if(minFreeMem.endsWith("M")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-1);
			cn = Long.parseLong(minFreeMem)*1024*1024;
		}else if(minFreeMem.endsWith("G")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-1);
			cn = Long.parseLong(minFreeMem)*1024*1024*1024;
		}else {
			cn = Long.parseLong(minFreeMem);
		}
		
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(ai.getSs() == null || ai.getSs().getFreeMemory() < cn) {
				ite.remove();
			}
		}
		
	}

	private void filterCoreNum(List<AgentInfo> agents, String coreNum,Deployment dep) {
		if(StringUtils.isEmpty(coreNum)) {
			logger.error("coreNum val is NULL for: [" +dep.toString() +"]");
			return;
		}
		
		int cn = Integer.parseInt(coreNum);
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(ai.getSs() == null || ai.getSs().getCpuNum() < cn) {
				ite.remove();
			}
		}
		
	}

	private void filterName(List<AgentInfo> agents, String name) {
		if(agents == null || agents.isEmpty() || StringUtils.isEmpty(name)) {
			return;
		}
		
		name = name.trim();
		
		boolean matchPrefix = false;
		if(name.endsWith("*")) {
			matchPrefix = true;
			name = name.substring(0,name.length()-1);
		}
		
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(matchPrefix) {
				if(!ai.getName().startsWith(name)){
					ite.remove();
				}
			}else if(!name.equals(ai.getName())){
				ite.remove();
			}
		}
		
	}

	private void filterId(List<AgentInfo> agents, String id) {
		if(agents == null || agents.isEmpty() || StringUtils.isEmpty(id)) {
			return;
		}
		
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(!id.equals(ai.getId())) {
				ite.remove();
			}
		}
	}

	private void filterHost(List<AgentInfo> agents, String host) {
		if(agents == null || agents.isEmpty() || StringUtils.isEmpty(host)) {
			return;
		}
		
		host = host.trim();
		
		boolean matchPrefix = false;
		if(host.endsWith("*")) {
			matchPrefix = true;
			host = host.substring(0,host.length()-1);
		}
		
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(matchPrefix) {
				if(!ai.getHost().startsWith(host)){
					ite.remove();
				}
			}else if(!host.equals(ai.getHost())){
				ite.remove();
			}
		}
	}

}
