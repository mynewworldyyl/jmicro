package org.jmicro.api.monitor;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

@Component(lazy=true)
public class SubmitItemHolderManager {
    
	@Cfg(value="/monitorServerIoSession",required=false,changeListener="init")
	private boolean monitorIoSession1=true;
	
	@Cfg(value="/monitorClientIoSession",required=false,changeListener="init")
	private boolean monitorIoSession2=true;
	
	@Cfg(value="/monitorClientEnable",required=false,changeListener="init")
	private boolean monitorClientEnable = true;
	
	@Cfg(value="/monitorServerEnable",required=false,changeListener="init")
	private boolean monitorServerEnable=true;
	
	@Cfg(value="/monitorMaxCacheItems",required=false,changeListener="init")
	private int maxCacheItems = 10000;
	
	private Queue<SubmitItem> caches = new ConcurrentLinkedQueue<>();
	
	private boolean enable = true;
	
	private AtomicInteger index = new AtomicInteger();
	
	private int threadSize = 1;
	
	@Inject(required=false)
	private Set<IMonitorSubmitWorker> submiters = new HashSet<>();
	
	private Worker[] workers = null;
	
	private class Worker implements Runnable{
		private Queue<SubmitItem> its = new ConcurrentLinkedQueue<>();
		@Override
		public void run() {
			for(;;){
				if(its.isEmpty()){
					synchronized(this){
						try {
							this.wait();
						} catch (InterruptedException e) {
						}
					}
				}
				SubmitItem si = its.poll();
				for(IMonitorSubmitWorker m : submiters){
					m.submit(si);
				}
				cache(si);
			}	
		}
		
		public void addItem(SubmitItem si){
			its.add(si);
			synchronized(this){
				this.notify();
			}			
		}
		
		public int size(){
			return its.size();
		}
	}
	
	@JMethod("init")
	public void init() {
		this.enable =(monitorIoSession1 || monitorIoSession2
				|| monitorClientEnable|| monitorServerEnable) && !submiters.isEmpty();
		if(this.enable){
			workers = new Worker[threadSize];
			for(int i = 0; i < threadSize; i++){
				workers[i] = new Worker();
				new Thread(workers[i]).start();
			}
		}
	}
	
	private int size() {	
		int size  =0;
		for(int i = 0; i < threadSize; i++){
			size += workers[i].size();
		}
		return size;
	}

	public void submit(int type,IRequest req, IResponse resp,Object... args){
		if(!enable || size() > this.maxCacheItems){
			return;
		}
		
		SubmitItem si = caches.poll();
		if(si == null){
			 si = new SubmitItem();
		}
		
		si.setArgs(args);
		si.setFinish(false);
		si.setReq(req);
		si.setResp(resp);
		si.setType(type);
		si.setTime(System.currentTimeMillis());
		
		this.workers[index.getAndIncrement()%this.workers.length].addItem(si);
	}
	
	private void cache(SubmitItem si){
		if(si == null){
			return;
		}
		
		if(caches.size() >= this.maxCacheItems){
			return;
		}
		
		//free memory
		si.setArgs(null);
		si.setFinish(true);
		si.setReq(null);
		si.setResp(null);
		si.setType(-1);
		caches.offer(si);
	}
	
}
