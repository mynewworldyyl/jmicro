package org.jmicro.limit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.limitspeed.Limiter;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;

@Component(lazy=false)
public class DefaultSpeedLimiter implements Limiter{
	
	@Inject
	private IRegistry registry;
	
	//in seconds
	@Cfg("/limitKeepTimeLong")
	private int keepTimeLong;
	
	@JMethod("init")
	public void init(){
		startWorker();
	}
	
	/**
	 * output from header and input from tail
	 */
	private Map<String,ConcurrentLinkedDeque<LimitData>> limiterData = new ConcurrentHashMap<>();
	
	@Override
	public int apply(IRequest req) {
		
		//not support method override
		String key = this.serviceKey(req);
		if(!limiterData.containsKey(key)){
			this.limiterData.put(key, new ConcurrentLinkedDeque<LimitData>());
		}
		
		ConcurrentLinkedDeque<LimitData> ld = this.limiterData.get(key);
		ServiceItem si = null;
		if(ld.isEmpty()){
			si = getServiceItem(req);
		}else {
			si = ld.peek().getSi();
		}
		
		if(si == null){
			// service not found and let the laster handler to decide how to response
			return 1;
		}
		
		LimitData d = new LimitData();
		d.setSi(si);
		ld.add(d);
		
		//return the time to be wait
		int result = compute(ld,si,req);
		if(result == 0){
			return 0;
		}
		
		doWait(result,d);
		return 0;
	}
	
	private void doWait(int result,LimitData d) {
		synchronized(d){
			try {
				d.wait(result);
			} catch (InterruptedException e) {
			}
		}
	}

	private int compute(ConcurrentLinkedDeque<LimitData> ld,ServiceItem si,IRequest req ) {
		ServiceMethod sm = null;
		for(ServiceMethod mi : si.getMethods()){
			if(sm.getMethodName().equals(req.getMethod())){
				sm =mi;
			}
		}
		
		int maxSpeed = sm.getMaxSpeed();
		if(maxSpeed == 0){
			//not limit
			return 0;
		}
		
		if(maxSpeed < 0){
			//decide by Service
			maxSpeed = si.getMaxSpeed();
		}
		
		if(maxSpeed <= 0){
			//not limit
			return 0;
		}
		
		//maxSpeed > 0 limit speed
		LimitData last = ld.getLast();
		LimitData first = ld.getFirst();
		long sp = (first.getReqTime()-last.getReqTime())/ld.size();
		
		if(sp < maxSpeed){
			//not got the max speed
			return 0;
		}
		
		// simple wait 500 ms
		return 500;
	}
	

	private String serviceKey(IRequest req){
		String key = req.getServiceName()+req.getMethod();
		if(req.getArgs() == null || req.getArgs().length == 0){
			return key;
		}
		for(Object o: req.getArgs()){
			key = key + o.getClass().getName();
		}
		return key;
	}

	private ServiceItem getServiceItem(IRequest req) {
		Set<ServiceItem> sis = registry.getServices(req.getServiceName());
		if(sis == null || sis.isEmpty()){
			return null;
		}
		return sis.iterator().next();
	}
	
	
	private void startWorker(){
		new Thread(()->{
			for(;;){
				try {
					if(limiterData.isEmpty()){
						Thread.sleep(2000);
						continue;
					}
					long timeLong = this.keepTimeLong*1000;
					for(ConcurrentLinkedDeque<LimitData> ld: this.limiterData.values()){
						LimitData last = ld.getLast();
						LimitData first = ld.getFirst();
						if((first.getReqTime() - last.getReqTime()) < timeLong) {
							continue;
						}
						doRemove(ld,timeLong);
					}
				} catch (Throwable e) {
				}
			}
		}).start();
	}

	private void doRemove(ConcurrentLinkedDeque<LimitData> ld,long timeLong) {
		
		LimitData last = ld.getLast();
		for(;;){
			LimitData first = ld.getFirst();
			if((first.getReqTime() - last.getReqTime()) < timeLong) {
				return;
			}
			ld.pollFirst();
		}
		
		
	}

}
