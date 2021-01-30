package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.api.executor.genclient.IExecutorInfo$JMAsyncClient;
import cn.jmicro.api.mng.IThreadPoolMonitor;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(version="0.0.1",debugMode=1,
monitorEnable=0,logLevel=MC.LOG_ERROR,retryCnt=0,external=true,timeout=3000,showFront=false)
public class ThreadPoolMonitorImpl implements IThreadPoolMonitor {

	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitorImpl.class);
	
	@Reference(namespace="*",version="*",type="ins")
	private List<IExecutorInfo$JMAsyncClient> monitorServers = new ArrayList<>();
	
	@Override
	@SMethod(perType=true,timeout=60000,retryCnt=0,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<List<ExecutorInfo>> serverList() {
		Resp<List<ExecutorInfo>> resp = new Resp<>();
		resp.setCode(Resp.CODE_SUCCESS);
		if(monitorServers.isEmpty()) {
			resp.setData(null);
		}
		
		CountDownLatch cd = new CountDownLatch(this.monitorServers.size());
		
		List<ExecutorInfo> l = new ArrayList<>();
		resp.setData(l);
		for(IExecutorInfo$JMAsyncClient iei : this.monitorServers) {
			if(PermissionManager.checkAccountClientPermission(iei.clientId())) {
				iei.getInfoJMAsync().then((ei,fail,ctx)->{
					cd.countDown();
					if(ei != null) {
						l.add(ei);
					} else {
						logger.error(fail.toString());
					}
				});
			}else {
				cd.countDown();
			}
		}
		
		try {
			cd.await();
		} catch (InterruptedException e) {
		}
		return resp;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public Resp<List<ExecutorInfo>> getInfo(String key,String type) {
		Set<IExecutorInfo$JMAsyncClient> iei = getServerByKey(key,type);
		
		Resp<List<ExecutorInfo>> resp = new Resp<>();
		resp.setCode(Resp.CODE_SUCCESS);
		
		if(iei != null && !iei.isEmpty()) {
			CountDownLatch cd = new CountDownLatch(iei.size());
			List<ExecutorInfo> l = new ArrayList<>();
			resp.setData(l);
			for(IExecutorInfo$JMAsyncClient ei : iei) {
				if(PermissionManager.checkAccountClientPermission(ei.clientId())) {
					ei.getInfoJMAsync()
					.success((info,cxt)->{
						//ExecutorInfo info = ei.getInfo();
						if(info != null) {
							l.add(info);
						}
						cd.countDown();
					})
					.fail((code,err,cxt)->{
						logger.error(err);
						cd.countDown();
					});
				}else {
					cd.countDown();
				}
			}
			
			try {
				cd.await();
			} catch (InterruptedException e) {
			}
			
		}
		
		return resp;
	}

	
	private Set<IExecutorInfo$JMAsyncClient> getServerByKey(String key,String type) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		boolean byIns = false;
		if("ins".equals(type)) {
			byIns = true;
		}
		
		Set<IExecutorInfo$JMAsyncClient> eis = new HashSet<>();
		
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxyHolder s = (AbstractClientServiceProxyHolder)((Object)this.monitorServers.get(i));
			ServiceItem si = s.getHolder().getItem();
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
