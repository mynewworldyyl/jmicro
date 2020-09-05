package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.IAgentProcessService;
import cn.jmicro.api.choreography.genclient.IAgentProcessService$JMAsyncClient;
import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.service.IServiceAsyncResponse;
import cn.jmicro.common.Constants;
import cn.jmicro.mng.api.IAgentLogService;

@Component(level=20001)
@Service(namespace="mng", version="0.0.1", external=true, debugMode=0, showFront=false)
public class AgentLogServiceImpl implements IAgentLogService {

	private final Logger logger = LoggerFactory.getLogger(AgentLogServiceImpl.class);
	
	@Reference(namespace="*", version="*", type="ins", changeListener="changeListener")
	private Set<IAgentProcessService$JMAsyncClient> agentServices = new HashSet<>();
	
	private Map<String,IAgentProcessService$JMAsyncClient> id2Aps = new HashMap<>();
	
	public void ready() {
		if(!agentServices.isEmpty()) {
			for(IAgentProcessService po : agentServices) {
				changeListener((AbstractClientServiceProxyHolder)po,IServiceListener.ADD);
			}
		}
	}
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<LogFileEntry>> getAllLogFileEntry() {
		Resp<List<LogFileEntry>> resp = new Resp<>();
		resp.setCode(0);
		
		if(agentServices.isEmpty()) {
			resp.setCode(1);
			resp.setMsg("No data");
			return resp;
		}
		
		JMicroContext cxt = JMicroContext.get();
		
		IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
		if(cxt.isAsync() && cb == null) {
			logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
		}
		
		if(cxt.isAsync() && cb != null) {
			
			List<LogFileEntry> list = new ArrayList<>();
			resp.setData(list);
			
			AtomicInteger ai = new AtomicInteger(agentServices.size());
			
			for(IAgentProcessService$JMAsyncClient aps : agentServices) {
				aps.getProcessesLogFileListJMAsync()
				.then((rl,fail,actx) -> {
					ai.decrementAndGet();
					if(fail == null) {
						if(rl != null) {
							list.addAll((List<LogFileEntry>)rl);
						}
					} else {
						logger.error(fail.toString());
					}
					if(ai.get() == 0) {
						cb.result(resp);
					}
				});
			}
			return null;
		} else {
			List<LogFileEntry> list = new ArrayList<>();
			resp.setData(list);
			for(IAgentProcessService$JMAsyncClient aps : agentServices) {
				List<LogFileEntry> logs = aps.getProcessesLogFileList();
				list.addAll(logs);
			}
			return resp;
		}
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Boolean> startLogMonitor(String processId,String logFile, String agentId, 
			int offsetFromLastLine) {
		
		Resp<Boolean> resp = new Resp<Boolean>();
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			return resp;
		}
		
		JMicroContext cxt = JMicroContext.get();
		
		IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
		if(cxt.isAsync() && cb == null) {
			logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
		}
		
		if(cxt.isAsync() && cb != null) {
			IAgentProcessService$JMAsyncClient aps = this.id2Aps.get(agentId);
			aps.startLogMonitorJMAsync(processId, logFile, offsetFromLastLine)
			.then((rst,fail,actx) -> {
				if(fail == null) {
					resp.setData(rst);
				} else {
					logger.error(fail.toString());
					resp.setData(false);
					resp.setMsg(fail.toString());
				}
				cb.result(resp);
			});
			return null;
		} else {
			IAgentProcessService aps = this.id2Aps.get(agentId);
			boolean rst = aps.startLogMonitor(processId, logFile, offsetFromLastLine);
			resp.setData(rst);
			return resp;
		}
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<Boolean> stopLogMonitor(String processId,String logFile, String agentId) {
		
		Resp<Boolean> resp = new Resp<Boolean>();
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			return resp;
		}
		
		JMicroContext cxt = JMicroContext.get();
		
		IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
		if(cxt.isAsync() && cb == null) {
			logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
		}
		
		if(cxt.isAsync() && cb != null) {
			IAgentProcessService$JMAsyncClient aps = this.id2Aps.get(agentId);
			aps.stopLogMonitorJMAsync(processId,logFile)
			.then((rst,fail,actx) -> {
				if(fail == null) {
					resp.setData(rst);
				} else {
					logger.error(fail.toString());
					resp.setData(false);
					resp.setMsg(fail.toString());
				}
				cb.result(resp);
			});
			return null;
		} else {
			IAgentProcessService aps = this.id2Aps.get(agentId);
			boolean rst = aps.stopLogMonitor(processId,logFile);
			resp.setData(rst);
			return resp;
		}
		
	}
	
	public void changeListener(AbstractClientServiceProxyHolder po,int opType) {
		IAgentProcessService$JMAsyncClient aps = (IAgentProcessService$JMAsyncClient)po;
		if(IServiceListener.ADD == opType) {
			id2Aps.put(po.getHolder().getItem().getKey().getInstanceName(), aps);
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
