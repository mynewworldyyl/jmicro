package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.IAgentProcessService;
import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.mng.api.IAgentLogService;

@Component(level=20001)
@Service(namespace="mng", version="0.0.1", external=true, debugMode=0, showFront=false)
public class AgentLogServiceImpl implements IAgentLogService {

	private final Logger logger = LoggerFactory.getLogger(AgentLogServiceImpl.class);
	
	@Reference(namespace="*", version="*", type="ins", changeListener="changeListener")
	private Set<IAgentProcessService> agentServices = new HashSet<>();
	
	private Map<String,IAgentProcessService> id2Aps = new HashMap<>();
	
	public void ready() {
		if(!agentServices.isEmpty()) {
			for(IAgentProcessService po : agentServices) {
				changeListener((AbstractClientServiceProxyHolder)po,IServiceListener.ADD);
			}
		}
	}
	
	@Override
	public Resp<List<LogFileEntry>> getAllLogFileEntry() {
		Resp<List<LogFileEntry>> resp = new Resp<>();
		resp.setCode(0);
		
		if(agentServices.isEmpty()) {
			resp.setCode(1);
			resp.setMsg("No data");
			return resp;
		}
		
		List<LogFileEntry> list = new ArrayList<>();
		for(IAgentProcessService aps : agentServices) {
			List<LogFileEntry> logs = aps.getProcessesLogFileList();
			list.addAll(logs);
		}
		resp.setData(list);
		return resp;
	}

	@Override
	public Resp<Boolean> startLogMonitor(String processId,String logFile, String agentId, 
			int offsetFromLastLine) {
		Resp<Boolean> resp = new Resp<Boolean>();
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			return resp;
		}
		
		IAgentProcessService aps = this.id2Aps.get(agentId);
		boolean rst = aps.startLogMonitor(processId, logFile, offsetFromLastLine);
		resp.setData(rst);
		
		return resp;
	}

	@Override
	public Resp<Boolean> stopLogMonitor(String processId,String logFile, String agentId) {
		
		Resp<Boolean> resp = new Resp<Boolean>();
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			return resp;
		}
		
		IAgentProcessService aps = this.id2Aps.get(agentId);
		boolean rst = aps.stopLogMonitor(processId,logFile);
		resp.setData(rst);
		
		return resp;
	}
	
	public void changeListener(AbstractClientServiceProxyHolder po,int opType) {
		IAgentProcessService aps = (IAgentProcessService)po;
		if(IServiceListener.ADD == opType) {
			id2Aps.put(aps.agentId(), aps);
		} else {
			String key = null;
			for(String k : id2Aps.keySet()) {
				if(po == id2Aps.get(k)) {
					key = k;
					break;
				}
			}
			
			if(key != null) {
				id2Aps.remove(key);
			}
		}
	}
	
}
