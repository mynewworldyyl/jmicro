package cn.jmicro.choreography.assignment;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.choreography.api.Deployment;
import cn.jmicro.choreography.api.IAssignStrategy;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.util.StringUtils;

@Component(value="defautAssignStrategy")
public class DefautAssignStrategy implements IAssignStrategy{

	private final static Logger logger = LoggerFactory.getLogger(DefautAssignStrategy.class);
	
	@Inject
	private InstanceManager insManager;
	
	private Comparator<AgentInfo> comparator = ( o1, o2)->{
		int o1Size = insManager.getProcessSizeByAgentId(o1.getId());
		int o2Size = insManager.getProcessSizeByAgentId(o2.getId());
		return o1Size > o2Size ? 1: o1Size == o2Size ? 0 : -1;
	};
	
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
		
		agents.sort(comparator);
		return true;
		
	}
	
	private void filteCpuRate(List<AgentInfo> agents, String cpuRate, Deployment dep) {
		if(StringUtils.isEmpty(cpuRate)) {
			logger.error("cpuRate val is NULL for: [" +dep.toString() +"]");
			return;
		}
		
		double cn = Double.parseDouble(cpuRate)*100;
		Iterator<AgentInfo> ite = agents.iterator();
		while(ite.hasNext()) {
			AgentInfo ai = ite.next();
			if(ai.getSs() == null || ai.getSs().getAvgCpuLoad() > cn) {
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
		}else if(minFreeMem.endsWith("KB")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-2);
			cn = Long.parseLong(minFreeMem)*1024;
		}else if(minFreeMem.endsWith("MB")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-2);
			cn = Long.parseLong(minFreeMem)*1024*1024;
		}else if(minFreeMem.endsWith("GB")) {
			minFreeMem = minFreeMem.substring(0,minFreeMem.length()-2);
			cn = Long.parseLong(minFreeMem)*1024*1024*1024;
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
