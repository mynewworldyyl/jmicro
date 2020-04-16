package org.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.v2.IMonitorServer;
import org.jmicro.api.monitor.v2.MonitorManager;
import org.jmicro.api.monitor.v2.MonitorServerStatus;
import org.jmicro.mng.inter.IMonitorServerManager;

@Component
@Service(namespace="mng",version="0.0.1")
public class MonitorServerManagerImpl implements IMonitorServerManager{
	
	@Reference(namespace="monitorServer",type="ins")//每个服务实例一个代理对象
	private List<IMonitorServer> monitorServers = new ArrayList<>();
	
	@Inject
	private MonitorManager monitorManager;
	
	@Override
	public MonitorServerStatus[] status(boolean needTotal) {
		if(this.monitorServers.isEmpty()) {
			return null;
		}
		
		MonitorServerStatus[] status = new MonitorServerStatus[this.monitorServers.size()+1];
		
		MonitorServerStatus totalStatus = null;
		Map<String,Double> sds = null;
		if(needTotal) {
			totalStatus = new MonitorServerStatus();
			sds = new HashMap<>();
			totalStatus.setStatisData(sds);
			totalStatus.setInstanceName("Total");
			totalStatus.setSubsriberSize(monitorManager.getMkey2Types().size());
			totalStatus.getSubsriber2Types().putAll(monitorManager.getMkey2Types());
			status[0] = totalStatus;
		}
		
		
		for(int i = 0; i < this.monitorServers.size(); i++) {
			IMonitorServer server = this.monitorServers.get(i);
			MonitorServerStatus s = server.status();
			if(s != null) {
				status[i+1] = s;
				if(needTotal) {
					
					totalStatus.setSendCacheSize(s.getSendCacheSize()+totalStatus.getSendCacheSize());
					
					for(Short t : MonitorServerStatus.TYPES) {
						String totakKey = "total_"+t;
						String qpsKey = "qps_"+t;
						String curKey = "cur_"+t;
						
						double totalVal = s.getStatisData().get(totakKey) + sds.get(totakKey);
						Double curVal = s.getStatisData().get(qpsKey) + sds.get(qpsKey) ;
						double qps = s.getStatisData().get(curKey) + sds.get(curKey);
						
						sds.put(totakKey, totalVal);
						sds.put(qpsKey, qps);
						sds.put(curKey, curVal);
					}
				}
			}
		}
		
		return status;
	}

	
}
