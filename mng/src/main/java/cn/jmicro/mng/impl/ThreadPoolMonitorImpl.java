package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.executor.ExecutorInfoJRso;
import cn.jmicro.api.executor.genclient.IExecutorInfoJMSrv$JMAsyncClient;
import cn.jmicro.api.mng.IThreadPoolMonitorJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.Namespace;

@Component
@Service(version="0.0.1",debugMode=1,
monitorEnable=0,logLevel=MC.LOG_NO,namespace=Namespace.NS,retryCnt=0,external=true,timeout=3000,showFront=false)
public class ThreadPoolMonitorImpl implements IThreadPoolMonitorJMSrv {

	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitorImpl.class);
	
	@Reference(namespace="*",version="*",type="ins")
	private List<IExecutorInfoJMSrv$JMAsyncClient> monitorServers = new ArrayList<>();
	
	@Override
	@SMethod(perType=true,timeout=60000,retryCnt=0,needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<List<ExecutorInfoJRso>> serverList() {
		RespJRso<List<ExecutorInfoJRso>> resp = new RespJRso<>();
		resp.setCode(RespJRso.CODE_SUCCESS);
		if(monitorServers.isEmpty()) {
			resp.setData(null);
		}
		
		CountDownLatch cd = new CountDownLatch(this.monitorServers.size());
		
		List<ExecutorInfoJRso> l = new ArrayList<>();
		resp.setData(l);
		for(IExecutorInfoJMSrv$JMAsyncClient iei : this.monitorServers) {
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
	public RespJRso<List<ExecutorInfoJRso>> getInfo(String key,String type) {
		Set<IExecutorInfoJMSrv$JMAsyncClient> iei = getServerByKey(key,type);
		
		RespJRso<List<ExecutorInfoJRso>> resp = new RespJRso<>();
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		if(iei != null && !iei.isEmpty()) {
			CountDownLatch cd = new CountDownLatch(iei.size());
			List<ExecutorInfoJRso> l = new ArrayList<>();
			resp.setData(l);
			for(IExecutorInfoJMSrv$JMAsyncClient ei : iei) {
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

	
	private Set<IExecutorInfoJMSrv$JMAsyncClient> getServerByKey(String key,String type) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		boolean byIns = false;
		if("ins".equals(type)) {
			byIns = true;
		}
		
		Set<IExecutorInfoJMSrv$JMAsyncClient> eis = new HashSet<>();
		
		for(int i = 0; i < this.monitorServers.size(); i++) {
			AbstractClientServiceProxyHolder s = (AbstractClientServiceProxyHolder)((Object)this.monitorServers.get(i));
			ServiceItemJRso si = s.getHolder().getItem();
			if(si == null) {
				continue;
			}
			if(byIns) {
				if(si.getKey().getInstanceName().equals(key)) {
					eis.add(this.monitorServers.get(i)) ;
				}
			} else {
				if(si.getKey().fullStringKey().equals(key)) {
					eis.add(this.monitorServers.get(i));
					break;
				}
			}
		}
		return eis;
	}
	
}
