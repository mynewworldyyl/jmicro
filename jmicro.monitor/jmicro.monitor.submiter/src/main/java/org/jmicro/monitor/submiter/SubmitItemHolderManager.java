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
package org.jmicro.monitor.submiter;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.SubmitItem;
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
public class SubmitItemHolderManager implements IMonitorDataSubmiter{
    
	private final static Logger logger = LoggerFactory.getLogger(SubmitItemHolderManager.class);
	
	private static final int NON = 0;
	
	private static final int INITED = 1;
	
	private static final int WORKING = 2;
	
	private Queue<SubmitItem> caches = new ConcurrentLinkedQueue<>();
	
	@Cfg(value="/SubmitItemHolderManager/enable",required=false)
	private boolean enable = true;
	
	private AtomicInteger index = new AtomicInteger();
	
	@Cfg(value="/SubmitItemHolderManager/threadSize",required=false,changeListener="reset")
	private int threadSize = 1;
	
	@Cfg(value="/SubmitItemHolderManager/maxCacheItems",required=false)
	private int maxCacheItems = 1000;
	
	private int status = NON;
	
	//@Inject(required=false, remote=true)
	//配置方式参数Reference注解说明
	@Reference(version="0.0.0",required=false,changeListener="startWork")
	private Set<IMonitorDataSubscriber> submiters = new HashSet<>();
	
	private Worker[] workers = null;
	
	public void reset(){
		if(status == NON){
			return;
		}
		if(threadSize == workers.length) {
			return;
		}
		
		Worker[] ws = new Worker[this.threadSize];
		
		if(threadSize < workers.length) {
			for(int j = 0; j < threadSize; j++){
				ws[j] = workers[j];
			}
			for(int j = threadSize; j < workers.length; j++){
				workers[j].pause(true);
			}
			
		} else {
			for(int j = 0; j < workers.length; j++){
				ws[j] = workers[j];
			}
			
			for(int j = workers.length; j < threadSize; j++){
				ws[j] = new Worker();
				if(status == WORKING){
					ws[j].start();
				}
			}
		}
		this.workers = ws;
	}
	
	public synchronized void init(){
		if(NON == status){
			status = INITED;
			workers = new Worker[threadSize];
			for(int i = 0; i < threadSize; i++){
				workers[i] = new Worker();
			}
		}
	}
	
	public synchronized void startWork() {
		if(NON == status){
			init();
		}
		if(INITED == status) {
			if(!submiters.isEmpty()){
				status = WORKING;
				for(int i = 0; i < threadSize; i++){
					 workers[i].start();
				}
			}
		}
	}
	
	private class Worker extends Thread{
		private boolean stop = false;
		private Queue<SubmitItem> its = new ConcurrentLinkedQueue<>();
		@Override
		public void run() {
			//自身所有代码不加入日志统计，否则会进入死循环
			//如果有需要，可以选择其他方式，如slf4j等
			JMicroContext.get().configMonitor(0, 0);
			for(;!stop;){
				try {
					if(its.isEmpty()){
						synchronized(this){
							try {
								this.wait();
							} catch (InterruptedException e) {
							}
						}
					}
					
					//JMicroContext.get().configMonitor(0, 0);
					for(SubmitItem si = its.poll();si != null;si = its.poll()){
						for(IMonitorDataSubscriber m : submiters){
							m.submit(si);
						}
						cache(si);
					}
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
		
		public boolean isPause(){return this.stop;}
		public void pause(boolean s){this.stop=s;}
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
				si.setSessionId(req.getSession().getId());
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
