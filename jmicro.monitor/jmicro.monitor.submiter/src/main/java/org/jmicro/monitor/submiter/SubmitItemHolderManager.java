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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.timer.TimerTicker;
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
	
	@Cfg(value="/SubmitItemHolderManager/enable",required=false,defGlobal=false)
	private boolean enable = true;
	
	@Cfg(value="/SubmitItemHolderManager/openDebug",required=true,defGlobal=false)
	private boolean openDebug = false;
	
	private AtomicInteger index = new AtomicInteger();
	
	@Cfg(value="/SubmitItemHolderManager/threadSize",required=false,changeListener="reset")
	private int threadSize = 2;
	
	@Cfg(value="/SubmitItemHolderManager/maxCacheItems",required=false)
	private int maxCacheItems = 1000;
	
	private int status = NON;
	
	private ExecutorService executor = ExecutorFactory.createExecutor(new ExecutorConfig());
	
	//@Inject(required=false, remote=true)
	//配置方式参数Reference注解说明
	@Reference(required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber> submiters = new HashSet<>();
	
	private Map<Integer,Set<IMonitorDataSubscriber>> type2Subscribers = new HashMap<>();
	
	private Worker[] workers = null;
	
	private Boolean subscriberChange = true;
	
	//测试统计模式使用
	@Cfg(value="/SubmitItemHolderManager/clientStatis",defGlobal=false)
	private boolean clientStatis=false;
	private ServiceCounter counter = null;

	public void subscriberChange() {
		subscriberChange = true;
	}
	
	/**
	 * 工作线程数量程改变
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
	private static final AtomicBoolean avoidLoop = new AtomicBoolean(false);
	private void updateSubmiterList(){

		if(!avoidLoop.compareAndSet(false, true)) {
			return;
		}
		
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
		
		avoidLoop.compareAndSet(true,false);
		
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

		if(clientStatis) {
			counter = new ServiceCounter("SubmitItemHolderManager",
					AbstractMonitorDataSubscriber.YTPES,10,2,TimeUnit.SECONDS);
			TimerTicker.getDefault(2000L).addListener("SubmitItemHolderManager", (key,att)->{
				System.out.println("======================================================");
				Double dreq = counter.getTotal(MonitorConstant.CLIENT_REQ_BEGIN);
				Double toreq = counter.getTotal(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
				Double dresp = counter.getTotal(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,MonitorConstant.CLIENT_REQ_OK,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				Double qps = counter.getAvg(TimeUnit.SECONDS,MonitorConstant.CLIENT_REQ_OK);
				if(dreq > -1 && dresp > -1) {
					logger.debug("总请求:{}, 总响应:{}, 超时:{}, QPS:{}",dreq,dresp,toreq,qps);
				}
			}, null);
		}
		
		/*if(this.enable) {
			TimerTicker.getDefault(5000L).addListener("subscriberChangeUpdater", (key,att)->{
				if(subscriberChange) {
					synchronized(subscriberChange) {
						if(subscriberChange) {
							// 避免多线程进入问题
							subscriberChange = false;
							this.updateSubmiterList();
						}
					}
				}
			}, null);
		}*/
	
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
			JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
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
					//checkUpdate();
					//JMicroContext.get().configMonitor(0, 0);
					for(SubmitItem si = its.poll();si != null;si = its.poll()){
						Set<IMonitorDataSubscriber> ss = type2Subscribers.get(si.getType());
						if(ss == null || ss.isEmpty()) {
							continue;
						}
						exception(si);
						for(IMonitorDataSubscriber m : ss){
							if(openDebug){
								//logger.debug("Submit {} to {}",si,m);
							}
							m.onSubmit(si);
						}
						if(clientStatis && si.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
							//这里的代码仅用于测试
							if(!counter.increment(si.getType())) {
								//logger.debug("No Counter: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(si.getType()));
							} else {
								//logger.debug("Counter: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(si.getType()));
							}
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
	
	public boolean canSubmit(Integer type) {
		if(!enable){
			if(openDebug){
				logger.debug("enable: {},type: {}",enable,MonitorConstant.MONITOR_VAL_2_KEY.get(type));
			}
			return false;
		}
		
		if(!type2Subscribers.containsKey(type)) {
			if(subscriberChange)  {
				this.updateSubmiterList();
			}else {
				return false;
			}
			
			/*if(!type2Subscribers.containsKey(type)) {
				if(openDebug){
					if(MonitorConstant.MONITOR_VAL_2_KEY.get(type) != null) {
						logger.debug("no submit for type: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(type));
					} else {
						logger.debug("no submit for type: {}",Integer.toHexString(type));
					}
				}
				return false;
			}*/
			
			if(!type2Subscribers.containsKey(type)) {
				return false;
			}
		}
		
		if(size() > this.maxCacheItems) {
			if(openDebug){
				//logger.debug("size()({}) > this.maxCacheItems:({}),type: {}",size(),this.maxCacheItems,MonitorConstant.MONITOR_VAL_2_KEY.get(type));
			}
			return false;
		}
		
		if(submiters.isEmpty()) {
			if(openDebug){
				//logger.debug("submiters.isEmpty(),type: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(type));
			}
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
		
		if(si.getSm() == null) {
			si.setSm(JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY,null));
		}
		
		if(si.getSm() != null) {
			ServiceMethod sm = si.getSm();
			si.setNamespace(sm.getKey().getNamespace());
			si.setServiceName(sm.getKey().getServiceName());
			si.setVersion(sm.getKey().getVersion());
			si.setMethod(JMicroContext.get().getString(JMicroContext.CLIENT_METHOD,null));
		} else {
			si.setNamespace(JMicroContext.get().getString(JMicroContext.CLIENT_NAMESPACE,null));
			si.setServiceName(JMicroContext.get().getString(JMicroContext.CLIENT_SERVICE,null));
			si.setVersion(JMicroContext.get().getString(JMicroContext.CLIENT_VERSION,null));
			si.setMethod(JMicroContext.get().getString(JMicroContext.CLIENT_METHOD,null));
			si.setSm(JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null));
		}
		
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
	public boolean submit(SubmitItem item) {
		
		if(item == null) {
			if(openDebug){
				logger.debug("Got NULL item");
			}
			return false;
		}
		
		if(!canSubmit(item.getType())) {
			return false;
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
		return true;
	}
	
	public void submit(int type,IRequest req, IResponse resp,String... others){
		
		if(!canSubmit(type)) {
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
	public boolean submit(int type, IReq req, IResp resp, Throwable exp, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		
		SubmitItem si = this.getItem();
		
		si.setType(type);
		si.setReq(req);
		si.setResp(resp);
		si.setEx(exp);
		si.setOthers(others);
		
		return submit(si);
	}

	@Override
	public boolean submit(int type, IReq req, Throwable exp, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setReq(req);
		si.setEx(exp);
		si.setOthers(others);
		return submit(si);
	}

	@Override
	public boolean submit(int type, IResp resp, Throwable exp, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setResp(resp);
		si.setEx(exp);
		si.setOthers(others);
		return submit(si);
	}

	@Override
	public boolean submit(int type, Message msg, Throwable exp, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setMsg(msg);
		si.setEx(exp);
		si.setOthers(others);
		return submit(si);
	}

	@Override
	public boolean submit(int type, Throwable exp, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setEx(exp);
		si.setOthers(others);
		return submit(si);
	}

	@Override
	public boolean submit(int type, String... others) {
		if(!canSubmit(type)) {
			return false;
		}
		SubmitItem si = this.getItem();
		si.setType(type);
		si.setOthers(others);
		return submit(si);
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
