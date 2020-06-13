package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.List;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorClient;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.IMonitorServerManager;

@Component
@Service(namespace="mng",version="0.0.1",debugMode=0,
monitorEnable=0,logLevel=MC.LOG_ERROR,retryCnt=0)
public class MonitorServerManagerImpl implements IMonitorServerManager{
	
	@Reference(namespace="*",version="*",type="ins")//每个服务实例一个代理对象
	private List<IMonitorAdapter> monitorServers = new ArrayList<>();
	
	//private Map<String,MonitorInfo> minfos = new HashMap<>();
	
	//private Short[] types = null; 
	
	@Inject
	private MonitorClient monitorManager;
	
	public void ready() {
	/*	typeLabels = new String[MonitorServerStatus.TYPES.length];
		for(int i = 0; i < MonitorServerStatus.TYPES.length; i++) {
			typeLabels[i] = MonitorConstant.MONITOR_VAL_2_KEY.get(MonitorServerStatus.TYPES[i]);
		}*/
	}
	
	@Override
	public MonitorServerStatus[] status(String[] srvKeys) {
		if(this.monitorServers.isEmpty() || srvKeys == null || srvKeys.length == 0) {
			return null;
		}
		
		MonitorServerStatus[] status = new MonitorServerStatus[srvKeys.length+1];
		double[] qpsArr =  null;
		double[] curArr = null;
		double[] totalArr = null;
		int typeLen = 0;
		
		for(int i = 0; i < srvKeys.length ; i++) {
			IMonitorAdapter server = this.getServerByKey(srvKeys[i]);
			if(server == null) {
				continue;
			}
			MonitorServerStatus s = server.status();
			if(s == null) {
				continue;
			}
			
			if(qpsArr == null) {
				
				//MonitorInfo in = this.minfos.get(s.getGroup());
				
				typeLen = s.getCur().length;
				qpsArr =  new double[typeLen];
				curArr = new double[typeLen];
				totalArr = new double[typeLen];
				
				MonitorServerStatus totalStatus = new MonitorServerStatus();
				status[0] = totalStatus;
				
				totalStatus.setCur(curArr);
				totalStatus.setQps(qpsArr);
				totalStatus.setTotal(totalArr);
			}
			
			status[i+1] = s;
			for(int j = 0; j < typeLen; j++) {
				totalArr[j] = totalArr[j] +  s.getTotal()[j];
				curArr[j] = curArr[j] + s.getCur()[j];
				qpsArr[j] = qpsArr[j] + s.getQps()[j];
			}
		}
		
		return status;
	}

	@Override
	public boolean enable(String srvKey,Boolean enable) {
		IMonitorAdapter s = this.getServerByKey(srvKey);
		if(s != null) {
			s.enableMonitor(enable);
			return true;
		}
		return false;
	}

	@Override
	public void reset(String[] srvKeys) {
		if(this.monitorServers.isEmpty() || srvKeys == null || srvKeys.length == 0) {
			return;
		}
		
		for(int i = 0; i < srvKeys.length ; i++) {
			IMonitorAdapter server = this.getServerByKey(srvKeys[i]);
			if(server != null) {
				
			}
		}
		
	}

	@Override
	public MonitorInfo[] serverList() {
		MonitorInfo[] infos = new MonitorInfo[this.monitorServers.size()];
		
		for(int i = 0; i < this.monitorServers.size(); i++) {
			
			IMonitorAdapter s = this.monitorServers.get(i);
			
			AbstractClientServiceProxy proxy = (AbstractClientServiceProxy)((Object)s);
			if(!proxy.isUsable()) {
				continue;
			}
			
			ServiceItem si = proxy.getItem();
			String srvKey = si.getKey().toKey(true, true, true);
			
			MonitorInfo in = s.info();
			if(in != null) {
				in.setSrvKey(srvKey);
			}
			
			/*if(!minfos.containsKey(in.getGroup())) {
				minfos.put(in.getGroup(), in);
			}*/

			//this.minfos.put(srvKey, in);
			infos[i] = in;
		}
		return infos;
	}
	
	private IMonitorAdapter getServerByKey(String key) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxy s = (AbstractClientServiceProxy)((Object)this.monitorServers.get(i));
			ServiceItem si = s.getItem();
			if(si == null) {
				continue;
			}
			if(si.getKey().toKey(true, true, true).equals(key)) {
				return this.monitorServers.get(i);
			}
		}
		return null;
	}
	
}
