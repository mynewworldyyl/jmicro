package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.api.executor.IExecutorInfo;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.IThreadPoolMonitor;

@Component
@Service(namespace="mng",version="0.0.1",debugMode=0,
monitorEnable=0,logLevel=MC.LOG_ERROR,retryCnt=0,external=true,timeout=10000)
public class ThreadPoolMonitorImpl implements IThreadPoolMonitor {

	@Reference(namespace="*",version="*",type="ins")
	private List<IExecutorInfo> monitorServers = new ArrayList<>();
	
	@Override
	public Resp<List<ExecutorInfo>> serverList() {
		Resp<List<ExecutorInfo>> resp = new Resp<>();
		resp.setCode(Resp.CODE_SUCCESS);
		if(monitorServers.isEmpty()) {
			resp.setData(null);
		}
		
		List<ExecutorInfo> l = new ArrayList<>();
		resp.setData(l);
		for(IExecutorInfo iei : this.monitorServers) {
			ExecutorInfo ei = iei.getInfo();
			if(ei != null) {
				l.add(ei);
			}
		}
		return resp;
	}

	@Override
	public Resp<List<ExecutorInfo>> getInfo(String key,String type) {
		Set<IExecutorInfo> iei = getServerByKey(key,type);
		
		Resp<List<ExecutorInfo>> resp = new Resp<>();
		resp.setCode(Resp.CODE_SUCCESS);
		
		if(iei != null) {
			List<ExecutorInfo> l = new ArrayList<>();
			resp.setData(l);
			for(IExecutorInfo ei : iei) {
				ExecutorInfo info = ei.getInfo();
				if(info != null) {
					l.add(info);
				}
			}
		}
		return resp;
	}

	
	private Set<IExecutorInfo> getServerByKey(String key,String type) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		boolean byIns = false;
		if("ins".equals(type)) {
			byIns = true;
		}
		
		Set<IExecutorInfo> eis = new HashSet<>();
		
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxy s = (AbstractClientServiceProxy)((Object)this.monitorServers.get(i));
			ServiceItem si = s.getItem();
			if(si == null) {
				continue;
			}
			if(byIns) {
				if(si.getKey().getInstanceName().equals(key)) {
					eis.add(this.monitorServers.get(i)) ;
				}
			} else {
				if(si.getKey().toKey(true, true, true).equals(key)) {
					eis.add(this.monitorServers.get(i));
					break;
				}
			}
		}
		return eis;
	}
	
}
