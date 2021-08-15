package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.IAgentProcessServiceJMSrv;
import cn.jmicro.api.choreography.genclient.IAgentProcessServiceJMSrv$JMAsyncClient;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.mng.LogFileEntryJRso;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.mng.api.IAgentLogServiceJMSrv;

@Component(level=20001)
@Service(version="0.0.1", external=true, debugMode=0, showFront=false,logLevel=MC.LOG_NO)
public class AgentLogServiceImpl implements IAgentLogServiceJMSrv {

	private final Logger logger = LoggerFactory.getLogger(AgentLogServiceImpl.class);
	
	@Reference(namespace="*", version="*", type="ins", changeListener="changeListener")
	private Set<IAgentProcessServiceJMSrv$JMAsyncClient> agentServices = new HashSet<>();
	
	private Map<String,IAgentProcessServiceJMSrv$JMAsyncClient> id2Aps = new HashMap<>();
	
	public void jready() {
		if(!agentServices.isEmpty()) {
			for(IAgentProcessServiceJMSrv po : agentServices) {
				changeListener((AbstractClientServiceProxyHolder)po,IServiceListener.ADD);
			}
		}
	}
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public IPromise<RespJRso<List<LogFileEntryJRso>>> getAllLogFileEntry() {
		RespJRso<List<LogFileEntryJRso>> resp = new RespJRso<>();
		resp.setCode(0);
		
		PromiseImpl<RespJRso<List<LogFileEntryJRso>>> p = new PromiseImpl<>();
		p.setResult(resp);
		
		if(agentServices.isEmpty()) {
			resp.setCode(1);
			resp.setMsg("No data");
			p.done();
			return p;
		}
		
		List<LogFileEntryJRso> list = new ArrayList<>();
		resp.setData(list);
		
		p.setCounter(agentServices.size());
		
		for(IAgentProcessServiceJMSrv$JMAsyncClient aps : agentServices) {
			aps.getProcessesLogFileListJMAsync()
			.then((rl,fail,actx) -> {
				if(fail == null) {
					if(rl != null) {
						list.addAll((List<LogFileEntryJRso>)rl);
					}
				} else {
					logger.error(fail.toString());
				}
				p.decCounter(1,true);
			});
		}
		return p;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=512)
	public IPromise<RespJRso<Boolean>> startLogMonitor(Integer processId,String logFile, String agentId, 
			int offsetFromLastLine) {
		
		RespJRso<Boolean> resp = new RespJRso<Boolean>();
		
		PromiseImpl<RespJRso<Boolean>> p = new PromiseImpl<>();
		p.setResult(resp);
		
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			p.done();
			return p;
		}

		IAgentProcessServiceJMSrv$JMAsyncClient aps = this.id2Aps.get(agentId);
		aps.startLogMonitorJMAsync(processId, logFile, offsetFromLastLine)
		.then((rst,fail,actx) -> {
			if(fail == null) {
				resp.setData(rst);
			} else {
				logger.error(fail.toString());
				resp.setData(false);
				resp.setMsg(fail.toString());
			}
			p.done();
		});
		
		return p;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=512)
	public IPromise<RespJRso<Boolean>> stopLogMonitor(Integer processId,String logFile, String agentId) {
		
		RespJRso<Boolean> resp = new RespJRso<Boolean>();
		
		PromiseImpl<RespJRso<Boolean>> p = new PromiseImpl<>();
		p.setResult(resp);
		
		if(!id2Aps.containsKey(agentId)) {
			resp.setData(false);
			resp.setCode(1);
			resp.setMsg("Agent: " + agentId + " not found!");
			p.done();
			return p;
		}

		IAgentProcessServiceJMSrv$JMAsyncClient aps = this.id2Aps.get(agentId);
		aps.stopLogMonitorJMAsync(processId,logFile)
		.then((rst,fail,actx) -> {
			if(fail == null) {
				resp.setData(rst);
			} else {
				logger.error(fail.toString());
				resp.setData(false);
				resp.setMsg(fail.toString());
			}
			p.done();
		});
		return p;
	}
	
	public void changeListener(AbstractClientServiceProxyHolder po,int opType) {
		IAgentProcessServiceJMSrv$JMAsyncClient aps = (IAgentProcessServiceJMSrv$JMAsyncClient)po;
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
