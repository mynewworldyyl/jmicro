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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:59
 */
@Component(lazy=false,level=20000)
public class SubmitItemHolderManager implements IMonitorDataSubmiter{
    
	private final static Logger logger = LoggerFactory.getLogger(SubmitItemHolderManager.class);
	
	private static final int NON = 0;
	
	private static final int INITED = 1;
	
	private static final int WORKING = 2;
	
	private Queue<SubmitItem> caches = new ConcurrentLinkedQueue<>();
	
	@Cfg(value="/SubmitItemHolderManager/enable",required=false)
	private boolean enable = true;
	
	@Cfg(value="/SubmitItemHolderManager/openDebug",required=false)
	private boolean openDebug = true;
	
	private AtomicInteger index = new AtomicInteger();
	
	@Cfg(value="/SubmitItemHolderManager/threadSize",required=false,changeListener="reset")
	private int threadSize = 1;
	
	@Cfg(value="/SubmitItemHolderManager/maxCacheItems",required=false)
	private int maxCacheItems = 1000;
	
	private int status = NON;
	
	//@Inject(required=false, remote=true)
	//配置方式参数Reference注解说明
	@Reference(required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber> submiters = new HashSet<>();
	
	private Map<Integer,Set<IMonitorDataSubscriber>> type2Subscribers = new HashMap<>();
	
	private Worker[] workers = null;
	
	private Boolean subscriberChange = true;
	
	public void subscriberChange() {
		subscriberChange = true;
	}
	
	/**
	 * 工作线哦数量程改变
	 */
	public synchronized void reset(){
		if(status == NON){
			return;
		}
		
		if(threadSize == workers.length) {
			return;
		}
		
		Worker[] ws = new Worker[this.threadSize];
		
		if(threadSize < workers.length) {
			//减少工作线程
			for(int j = 0; j < threadSize; j++){
				ws[j] = workers[j];
				workers[j] = null;
			}
			for(int j = threadSize; j < workers.length; j++){
				workers[j].pause(true);
				synchronized(workers[j]) {
					workers[j].notify();
				}
				workers[j] = null;
			}
		} else {
			//增加工作线程
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
	
	/**
	 * 由日志类型映射到日志订阅者
	 */
	private void updateSubmiterList(){
		if(!submiters.isEmpty()){
			for(IMonitorDataSubscriber m : this.submiters){
				Integer[] types = m.intrest();
				for(Integer t : types) {
					if(!type2Subscribers.containsKey(t)){
						type2Subscribers.put(t, new HashSet<IMonitorDataSubscriber>());
					}
					type2Subscribers.get(t).add(m);
				}
			}
		}
	}
	
	/**
	 * 初始化，只是创建工作线程对象，并没有启动
	 */
	public synchronized void init(){
		//updateSubmiterList();
		if(NON == status){
			status = INITED;
			workers = new Worker[threadSize];
			for(int i = 0; i < threadSize; i++){
				workers[i] = new Worker();
			}
		}
	}
	
	/**
	 * 启动线程
	 * @param f
	 */
	public synchronized void startWork(String f) {
		if(NON == status){
			return;
		}
		//updateSubmiterList();
		if(INITED == status) {
			if(!submiters.isEmpty()){
				status = WORKING;
				for(int i = 0; i < threadSize; i++){
					if(workers[i].isPause()) {
						 workers[i].start();
					}
				}
			}
		}
	}
	
	private class Worker extends Thread{
		private boolean pause = true;
		private Queue<SubmitItem> its = new ConcurrentLinkedQueue<>();
		@Override
		public void run() {
			//自身所有代码不加入日志统计，否则会进入死循环
			//如果有需要，可以选择其他方式，如slf4j等
			pause = false;
			JMicroContext.get().configMonitor(0, 0);
			for(;!pause;){
				try {
					if(its.isEmpty()){
						synchronized(this){
							try {
								this.wait();
							} catch (InterruptedException e) {
								logger.error("",e);
							}
						}
					}
					checkUpdate();
					//JMicroContext.get().configMonitor(0, 0);
					for(SubmitItem si = its.poll();si != null;si = its.poll()){
						Set<IMonitorDataSubscriber> ss = type2Subscribers.get(si.getType());
						if(ss == null || ss.isEmpty()) {
							continue;
						}
						exception(si);
						for(IMonitorDataSubscriber m : ss){
							if(openDebug){
								logger.debug("Submit {} to {}",si,m);
							}
							m.onSubmit(si);
						}
						cache(si);
					}
				} catch (Throwable e) {
					pause = true;
					logger.error("",e);
				}
			}	
		}
		
		public void addItem(SubmitItem si){
			its.add(si);
			synchronized(this){
				this.notifyAll();
			}			
		}
		
		public int size(){
			return its.size();
		}
		
		public boolean isPause(){return this.pause;}
		public void pause(boolean s){this.pause=s;}
	}
	
	private int size() {	
		int size  =0;
		for(int i = 0; i < threadSize; i++){
			size += workers[i].size();
		}
		return size;
	}
	
	private void checkUpdate() {
		if(subscriberChange) {
			synchronized(subscriberChange) {
				if(subscriberChange) {
					// 避免多线程进入问题
					subscriberChange = false;
					this.updateSubmiterList();
				}
			}
		}
	}
	
	private boolean needSubmit() {
		if(!enable || size() > this.maxCacheItems || submiters.isEmpty()){
			return false;
		}
		return true;
	}
	
	private SubmitItem getItem() {
		SubmitItem si = caches.poll();
		if(si == null){
			 si = new SubmitItem();
		}
		
		return si;
	}
	
	private void setHeader(SubmitItem si) {
		
		si.setInstanceName(Config.getInstanceName());
		si.setTime(System.currentTimeMillis());
		
		si.setRemoteHost(JMicroContext.get().getString(JMicroContext.REMOTE_HOST,""));
		si.setRemotePort(JMicroContext.get().getString(JMicroContext.REMOTE_PORT,""));
		si.setLocalPort(JMicroContext.get().getString(JMicroContext.LOCAL_PORT,""));
		si.setLocalHost(Config.getHost());
		
		si.setNamespace(JMicroContext.get().getString(JMicroContext.CLIENT_NAMESPACE,null));
		si.setServiceName(JMicroContext.get().getString(JMicroContext.CLIENT_SERVICE,null));
		si.setVersion(JMicroContext.get().getString(JMicroContext.CLIENT_VERSION,null));
		si.setMethod(JMicroContext.get().getString(JMicroContext.CLIENT_METHOD,null));
		
		if(Config.isClientOnly()) {
			si.setSide(Constants.SIDE_COMSUMER);
		} else if(Config.isServerOnly()) {
			si.setSide(Constants.SIDE_PROVIDER);
		} else {
			si.setSide(Constants.SIDE_ANY);
		}
	}
	
	private void cache(SubmitItem si){
		if(si == null){
			return;
		}
		
		if(caches.size() >= this.maxCacheItems){
			return;
		}
		
		//free memory
		si.reset();
		caches.offer(si);
	}

	@Override
	public void submit(SubmitItem item) {
		
		if(item == null) {
			return;
		}
		
		if(!needSubmit()) {
			return;
		}
		
		setHeader(item);
		
		int idx = index.getAndIncrement()%this.workers.length;
		Worker w = this.workers[idx];
		if(w == null || w.isPause()) {
			w = this.workers[idx] = new Worker();
		}
		w.addItem(item);
		if(w.isPause()) {
			w.start();
		}
	}
	
    public void submit(int type,IRequest req, IResponse resp,String... others){
		
		if(!needSubmit()) {
			return;
		}
		
		SubmitItem si = this.getItem();
		si.setReq(req);
		si.setResp(resp);
		si.setOthers(others);
		si.setType(type);
		
		si.setTime(System.currentTimeMillis());
		
		this.submit(si);
	}

	@Override
	public void submit(int type, IReq req, IResp resp, Throwable exp, String... others) {
		if(!needSubmit()) {
			return;
		}
		
		SubmitItem si = this.getItem();
		
		si.setType(type);
		si.setReq(req);
		si.setResp(resp);
		si.setEx(exp);
		si.setOthers(others);
		
		submit(si);
	}

	@Override
	public void submit(int type, IReq req, Throwable exp, String... others) {
		if(!needSubmit()) {
			return;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setReq(req);
		si.setEx(exp);
		si.setOthers(others);
		submit(si);
	}

	@Override
	public void submit(int type, IResp resp, Throwable exp, String... others) {
		if(!needSubmit()) {
			return;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setResp(resp);
		si.setEx(exp);
		si.setOthers(others);
		submit(si);
	}

	@Override
	public void submit(int type, Message msg, Throwable exp, String... others) {
		if(!needSubmit()) {
			return;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setMsg(msg);
		si.setEx(exp);
		si.setOthers(others);
		submit(si);
	}

	@Override
	public void submit(int type, Throwable exp, String... others) {
		if(!needSubmit()) {
			return;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setEx(exp);
		si.setOthers(others);
		submit(si);
	}

	@Override
	public void submit(int type, String... others) {
		if(!needSubmit()) {
			return;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setOthers(others);
		submit(si);
	}
	
	private void exception(SubmitItem si) {
		if(si.getEx() == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = null;
		try {
			 ps = new PrintStream(baos,true,Constants.CHARSET);
			 si.getEx().printStackTrace(ps);
			 sb.append(new String(baos.toByteArray(),Constants.CHARSET));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}finally {
			if(ps != null) {
				ps.close();
			}
		}
		si.setExp(sb.toString());
	}
}
