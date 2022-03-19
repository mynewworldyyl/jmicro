package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.mng.IMonitorServerManagerJMSrv;
import cn.jmicro.api.monitor.IMonitorAdapterJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorInfoJRso;
import cn.jmicro.api.monitor.MonitorServerStatusJRso;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.monitor.genclient.IMonitorAdapterJMSrv$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(version="0.0.1",debugMode=1,timeout=10000,logLevel=MC.LOG_NO,
monitorEnable=0,retryCnt=0, external=true,showFront=false)
public class MonitorServerManagerImpl implements IMonitorServerManagerJMSrv{
	
	private final static Logger logger = LoggerFactory.getLogger(MonitorServerManagerImpl.class);
	
	@Reference(namespace="*",version="*",type="ins")//每个服务实例一个代理对象
	private List<IMonitorAdapterJMSrv$JMAsyncClient> monitorServers = new ArrayList<>();
	
	//private Map<String,MonitorInfo> minfos = new HashMap<>();
	//private Short[] types = null; 
	
	@Inject
	private StatisMonitorClient monitorManager;
	
	public void jready() {
	   /*typeLabels = new String[MonitorServerStatus.TYPES.length];
		for(int i = 0; i < MonitorServerStatus.TYPES.length; i++) {
			typeLabels[i] = MonitorConstant.MONITOR_VAL_2_KEY.get(MonitorServerStatus.TYPES[i]);
		}*/
	}
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public IPromise<MonitorServerStatusJRso[]> status(String[] srvKeys) {
		
		Promise<MonitorServerStatusJRso[]> p = new Promise<>();
		
		if(this.monitorServers.isEmpty() || srvKeys == null || srvKeys.length == 0) {
			p.done();
			return p;
		}
		
		MonitorServerStatusJRso[] status = new MonitorServerStatusJRso[srvKeys.length+1];
		double[] qpsArr =  null;
		double[] curArr = null;
		double[] totalArr = null;
		int typeLen = 0;
		
		for(int i = 0; i < srvKeys.length ; i++) {
			IMonitorAdapterJMSrv server = this.getServerByKey(srvKeys[i]);
			if(server == null) {
				continue;
			}
			MonitorServerStatusJRso s = server.status();
			if(s == null) {
				continue;
			}
			
			if(qpsArr == null) {
				
				//MonitorInfo in = this.minfos.get(s.getGroup());
				
				typeLen = s.getCur().length;
				qpsArr =  new double[typeLen];
				curArr = new double[typeLen];
				totalArr = new double[typeLen];
				
				MonitorServerStatusJRso totalStatus = new MonitorServerStatusJRso();
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
		
		p.setResult(status);
		p.done();
		return p;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public IPromise<Boolean> enable(String srvKey,Boolean enable) {
		
		JMicroContext cxt = JMicroContext.get();
		
		Promise<Boolean> p = new Promise<>(false);
		
		IMonitorAdapterJMSrv$JMAsyncClient s = this.getServerByKey(srvKey);
		if(s == null) {
			p.done();
			return p;
		}
		
		if(!PermissionManager.checkAccountClientPermission(s.clientId())) {
			logger.warn("Permission reject for " + cxt.getAccount().getActName() + " to enable " +srvKey);
			p.done();
			return p;
		}

		s.enableMonitorJMAsync(enable)
		.then((rst,fail,actx) -> {
			if(fail == null) {
				p.setResult(true);
			} else {
				logger.error(fail.toString());
			}
			p.done();
		});
		return p;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public IPromise<Void> reset(String[] srvKeys) {
		
		Promise<Void> p = new Promise<>();
		
		if(this.monitorServers.isEmpty() || srvKeys == null || srvKeys.length == 0) {
			p.done();
			return p;
		}
		
		for(int i = 0; i < srvKeys.length ; i++) {
			IMonitorAdapterJMSrv server = this.getServerByKey(srvKeys[i]);
			if(server != null) {
				
			}
		}
		return p;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public IPromise<MonitorInfoJRso[]> serverList() {
		
		Promise<MonitorInfoJRso[]> p = new Promise<>();
		
		JMicroContext cxt = JMicroContext.get();
		
		Set<MonitorInfoJRso> set = new HashSet<>();
		
		Set<IMonitorAdapterJMSrv$JMAsyncClient> servers = new HashSet<>();
		for(int i = 0; i < this.monitorServers.size(); i++) {
			IMonitorAdapterJMSrv$JMAsyncClient s = this.monitorServers.get(i);
			if(s.isReady() && PermissionManager.checkAccountClientPermission(s.clientId())) {
				servers.add(s);
			}
		}
		
		if(servers.isEmpty()) {
			p.done();
			return p;
		}
		
		p.setCounter(servers.size());
		
		for(IMonitorAdapterJMSrv$JMAsyncClient s : servers) {
			
			s.infoJMAsync(s.getItem().getKey().fullStringKey()).then((in,fail,ctx0) -> {
				if(fail != null) {
					logger.error(fail.toString());
				}
				
				if (in != null) {
					in.setSrvKey((String)ctx0);
					set.add(in);
				}
				
				if(p.decCounter(1,false)) {
					MonitorInfoJRso[] infos = null;
					if(set.size() > 0) {
						infos = new MonitorInfoJRso[set.size()];
						set.toArray(infos);
					}
					p.setResult(infos);
					p.done();
				}
			});
		}
		
		return p;
	}
	
	private IMonitorAdapterJMSrv$JMAsyncClient getServerByKey(String key) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxyHolder s = (AbstractClientServiceProxyHolder)((Object)this.monitorServers.get(i));
			ServiceItemJRso si = s.getHolder().getItem();
			if(si == null) {
				continue;
			}
			if(si.getKey().fullStringKey().equals(key)) {
				return this.monitorServers.get(i);
			}
		}
		return null;
	}
	
}
