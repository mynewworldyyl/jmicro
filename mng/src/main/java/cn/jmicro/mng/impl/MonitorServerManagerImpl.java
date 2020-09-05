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
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.mng.IMonitorServerManager;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorClient;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.monitor.genclient.IMonitorAdapter$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.service.IServiceAsyncResponse;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace="mng",version="0.0.1",debugMode=1,timeout=10000,
monitorEnable=0, logLevel=MC.LOG_ERROR, retryCnt=0, external=true,showFront=false)
public class MonitorServerManagerImpl implements IMonitorServerManager{
	
	private final static Logger logger = LoggerFactory.getLogger(MonitorServerManagerImpl.class);
	
	@Reference(namespace="*",version="*",type="ins")//每个服务实例一个代理对象
	private List<IMonitorAdapter$JMAsyncClient> monitorServers = new ArrayList<>();
	
	//private Map<String,MonitorInfo> minfos = new HashMap<>();
	//private Short[] types = null; 
	
	@Inject
	private MonitorClient monitorManager;
	
	public void ready() {
	   /*typeLabels = new String[MonitorServerStatus.TYPES.length];
		for(int i = 0; i < MonitorServerStatus.TYPES.length; i++) {
			typeLabels[i] = MonitorConstant.MONITOR_VAL_2_KEY.get(MonitorServerStatus.TYPES[i]);
		}*/
	}
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
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
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public Boolean enable(String srvKey,Boolean enable) {
		
		JMicroContext cxt = JMicroContext.get();
		
		IMonitorAdapter$JMAsyncClient s = this.getServerByKey(srvKey);
		if(s == null) {
			return false;
		}
		
		if(!PermissionManager.checkAccountClientPermission(s.clientId())) {
			logger.warn("Permission reject for " + cxt.getAccount().getActName() + " to enable " +srvKey);
			return false;
		}
		
		IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
		if(cxt.isAsync() && cb == null) {
			logger.error(Constants.CONTEXT_SERVICE_RESPONSE + " is null in async context");
		}
		
		if(cxt.isAsync() && cb != null) {
			s.enableMonitorJMAsync(enable)
			.then((rst,fail,actx) -> {
				if(fail == null) {
					cb.result(true);
				} else {
					logger.error(fail.toString());
					cb.result(false);
				}
			});
			return null;
		} else {
			s.enableMonitor(enable);
			return true;
		}
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
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
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=4096)
	public MonitorInfo[] serverList() {
		
		JMicroContext cxt = JMicroContext.get();
		
		if(cxt.isAsync()) {
			
			IServiceAsyncResponse cb = cxt.getParam(Constants.CONTEXT_SERVICE_RESPONSE,null);
			Set<MonitorInfo> set = new HashSet<>();
			
			Set<IMonitorAdapter$JMAsyncClient> servers = new HashSet<>();
			for(int i = 0; i < this.monitorServers.size(); i++) {
				IMonitorAdapter$JMAsyncClient s = this.monitorServers.get(i);
				if(s.isReady() && PermissionManager.checkAccountClientPermission(s.clientId())) {
					servers.add(s);
				}
			}
			
			if(servers.isEmpty()) {
				return new MonitorInfo[0];
			}
			
			AtomicInteger ai = new AtomicInteger(servers.size());
			
			for(IMonitorAdapter$JMAsyncClient s : servers) {
				
				s.infoJMAsync(s.getItem().getKey().toKey(true, true, true)).then((in,fail,ctx0) -> {
					if(fail != null) {
						logger.error(fail.toString());
					}
					
					if (in != null) {
						
						in.setSrvKey((String)ctx0);
						set.add(in);
					}
					
					int cnt = ai.decrementAndGet();
					if(cnt == 0 ) {
						MonitorInfo[] infos = null;
						if(set.size() > 0) {
							infos = new MonitorInfo[set.size()];
							set.toArray(infos);
						}
						cb.result(infos);
					}
				});
			}
			
			return null;
			
		} else {
			Set<MonitorInfo> set = new HashSet<>();
			for(int i = 0; i < this.monitorServers.size(); i++) {
				IMonitorAdapter$JMAsyncClient s = this.monitorServers.get(i);
				if(!s.isReady()) {
					continue;
				}
				if(PermissionManager.checkAccountClientPermission(s.clientId())) {
					MonitorInfo in = s.info();
					if(in != null) {
						AbstractClientServiceProxyHolder proxy = (AbstractClientServiceProxyHolder)((Object)s);
						ServiceItem si = proxy.getHolder().getItem();
						String srvKey = si.getKey().toKey(true, true, true);
						in.setSrvKey(srvKey);
						set.add(in);
					}
				}
			}
			
			MonitorInfo[] infos = new MonitorInfo[set.size()];
			set.toArray(infos);
			
			return infos;
		}
		
	}
	
	private IMonitorAdapter$JMAsyncClient getServerByKey(String key) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxyHolder s = (AbstractClientServiceProxyHolder)((Object)this.monitorServers.get(i));
			ServiceItem si = s.getHolder().getItem();
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
