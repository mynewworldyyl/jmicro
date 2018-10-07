/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.api.monitor;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:59
 */
@Component(lazy=false,level=1)
public class SubmitItemHolderManager {
    
	private final static Logger logger = LoggerFactory.getLogger(SubmitItemHolderManager.class);
	
	@Cfg(value="/monitorServerIoSession",required=false,changeListener="startWork")
	private boolean monitorIoSession1=true;
	
	@Cfg(value="/isDefaultOpenMonitor",required=false,changeListener="startWork")
	private boolean isDefaultOpenMonitor=false;
	
	@Cfg(value="/monitorMaxCacheItems",required=false,changeListener="startWork")
	private int maxCacheItems = 10000;
	
	private Queue<SubmitItem> caches = new ConcurrentLinkedQueue<>();
	
	private boolean enable = true;
	
	private AtomicInteger index = new AtomicInteger();
	
	private int threadSize = 1;
	
	//@Inject(required=false, remote=true)
	@Reference(version="0.0.0",required=false)
	private Set<IMonitorSubmitWorker> submiters = new HashSet<>();
	
	private Worker[] workers = null;
	
	public void init(){
		workers = new Worker[threadSize];
		for(int i = 0; i < threadSize; i++){
			workers[i] = new Worker();
		}
	}
	//@JMethod("init")
	/*public*/ void startWork() {
		if(!submiters.isEmpty()){
			for(int i = 0; i < threadSize; i++){
				new Thread(workers[i]).start();
			}
		}
	}
	
	private class Worker implements Runnable{
		private Queue<SubmitItem> its = new ConcurrentLinkedQueue<>();
		@Override
		public void run() {
			JMicroContext.get().configMonitor(0, 0);
			for(;;){
				try {
					if(its.isEmpty()){
						synchronized(this){
							try {
								this.wait();
							} catch (InterruptedException e) {
							}
						}
					}
					JMicroContext.get().configMonitor(0, 0);
					SubmitItem si = its.poll();
					for(IMonitorSubmitWorker m : submiters){
						m.submit(si);
					}
					cache(si);
				} catch (Throwable e) {
					logger.error("",e);
				}
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
		
		si.setFinish(false);
		
		if(req != null){
			si.setReqId(req.getRequestId());
			if(req.getSession() != null){
				si.setSessionId(req.getSession().getSessionId());
			}
			si.setNamespace(req.getNamespace());
			si.setVersion(req.getVersion());
			si.setReqArgs(req.getArgs());
			si.setMethod(req.getMethod());
			si.setMsgId(req.getMsgId());
		}
		
		for(Object o: args){
			si.setOthers(si.getOthers()+o.toString());
		}
		
		if(resp != null){
			si.setRespId(resp.getId());
			si.setResult(resp.getResult());
		}
		
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
		si.setFinish(true);
		si.setType(-1);
		si.setReqId(-1);
		si.setSessionId(-1);
		si.setNamespace("");
		si.setVersion("");
		si.setReqArgs("");
		si.setMethod("");
		si.setMsgId(-1);
		si.setOthers("");
		si.setRespId(-1L);
		si.setResult("");
		
		caches.offer(si);
	}
	
}
