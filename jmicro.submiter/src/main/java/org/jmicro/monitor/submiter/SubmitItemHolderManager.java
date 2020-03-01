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
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.IServiceListener;
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
	
	private Queue<SubmitItem> caches = new ConcurrentLinkedQueue<>();
	
	@Cfg(value="/SubmitItemHolderManager/enable",required=false,defGlobal=false)
	private boolean enable = true;
	
	@Cfg(value="/SubmitItemHolderManager/waitSubscriberReady",required=false,defGlobal=false)
	private boolean waitSubscriberReady = false;
	
	@Cfg(value="/SubmitItemHolderManager/openDebug",required=true,defGlobal=false)
	private boolean openDebug = false;
	
	@Cfg(value="/SubmitItemHolderManager/threadSize",required=false,changeListener="reset")
	private int threadSize = 2;
	
	@Cfg(value="/SubmitItemHolderManager/maxCacheItems",required=false)
	private int maxCacheItems = 5000;
	
	@Cfg(value="/SubmitItemHolderManager/doAsyncSubmit",required=false)
	private boolean doAsyncSubmit = true;
	
	private ExecutorService executor = null;
	
	//@Inject(required=false, remote=true)
	//配置方式参数Reference注解说明
	//同步提交
	@Reference(required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber> submiters = new HashSet<>();
	
	private Set<IMonitorDataSubscriber> deleteMonitors = new HashSet<>();
	private Set<IMonitorDataSubscriber> addMonitors = new HashSet<>();
	
	private Map<IMonitorDataSubscriber,Short[]> sub2Types = new ConcurrentHashMap<>();
	private Map<IMonitorDataSubscriber,Long> lastSubmitTime = new ConcurrentHashMap<>();
	
	private Map<Short,Set<IMonitorDataSubscriber>> type2Subscribers = new ConcurrentHashMap<>();
	
	private Object cacheItemsLock = new Object();
	private Map<IMonitorDataSubscriber,Set<SubmitItem>> cacheItems = new ConcurrentHashMap<>();
	
	private boolean ready = false;
	
	private AsyncConfig ac = new AsyncConfig(true,"onSubmit", AsyncConfig.ASYNC_DIRECT);
	
	//测试统计模式使用
	@Cfg(value="/SubmitItemHolderManager/clientStatis",defGlobal=false)
	private boolean clientStatis=false;
	private ServiceCounter counter = null;

	public void subscriberChange(AbstractClientServiceProxy po,int opType) {
		logger.info("subscriberChange");
		if(opType == IServiceListener.SERVICE_ADD) {
			synchronized(addMonitors) {
				this.addMonitors.add((IMonitorDataSubscriber)po);
			}
		}else if(opType == IServiceListener.SERVICE_REMOVE) {
			synchronized(deleteMonitors) {
				this.deleteMonitors.add((IMonitorDataSubscriber)po);
			}
		}
		
		synchronized(cacheItemsLock) {
			cacheItemsLock.notifyAll();
		}
	}
	
	/**
	 * 工作线程数量程改变
	 */
	public synchronized void reset(){
	
	}
	
	/**
	 * 初始化，只是创建工作线程对象，并没有启动
	 */
	public synchronized void init0(){
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("SubmitItemHolderManager");
		executor = ExecutorFactory.createExecutor(config);
		
		if(clientStatis) {
			counter = new ServiceCounter("SubmitItemHolderManager",
					AbstractMonitorDataSubscriber.YTPES,10,2,TimeUnit.SECONDS);
			TimerTicker.getDefault(2000L).addListener("SubmitItemHolderManager", (key,att)->{
				System.out.println("======================================================");
				Double dreq = counter.getTotal(MonitorConstant.REQ_START);
				Double toreq = counter.getTotal(MonitorConstant.REQ_TIMEOUT);
				Double dresp = counter.getTotal(MonitorConstant.CLIENT_GET_RESPONSE_ERROR,MonitorConstant.CLIENT_GET_SERVER_ERROR);
				Double qps = counter.getQps(TimeUnit.SECONDS,MonitorConstant.REQ_START);
				if(dreq > -1 && dresp > -1) {
					logger.info("总请求:{}, 总响应:{}, 超时:{}, QPS:{}",dreq,dresp,toreq,qps);
				}
			}, null);
		}
		
		Thread checkThread = new Thread(this::doCheck,"JMicro-"+Config.getInstanceName()+"-SubmitItemHolderManager");
		checkThread.setDaemon(true);
		checkThread.start();
		
		if(waitSubscriberReady && !ready) {
			synchronized(cacheItemsLock) {
				while(!ready) {
					logger.warn("Wait subscriber ready");
					try {
						cacheItemsLock.wait(1000);
					} catch (InterruptedException e) {
						logger.error("",e);
					}
				}
			}
		}
	}
	
	private void addOneMonitor(IMonitorDataSubscriber m) {
		Short[] types = m.intrest();
		sub2Types.put(m, types);
		
		//用于提交数据
		lastSubmitTime.put(m, 0L);
		cacheItems.put(m, new HashSet<>());
		
		for(Short t : types) {
			if(null == type2Subscribers.get(t)) {
				type2Subscribers.put(t, new HashSet<IMonitorDataSubscriber>());
			}
			type2Subscribers.get(t).add(m);
		}
	}
	
	private void deleteOneMonitor(IMonitorDataSubscriber m) {
		
		Short[] types = sub2Types.get(m);
		sub2Types.remove(m);
		
		//用于提交数据
		lastSubmitTime.remove(m);
		
		Set<SubmitItem> items = cacheItems.remove(m);
		if(items != null && !items.isEmpty()) {
			logger.warn("Items not null when delete IMonitorDataSubscriber");
		}
		
		for(Short t : types) {
			if(type2Subscribers.containsKey(t)) {
				Set<IMonitorDataSubscriber> subs = type2Subscribers.get(t);
				subs.remove(m);
			}
		}
	}
	
	private void doCheck() {
		try {
			//服务启动时执行一次
			if(!submiters.isEmpty()) {
				for(IMonitorDataSubscriber m : this.submiters){
					addOneMonitor(m);
				}
			}
			
			ready = true;
			
			while(true) {
				
				if(!addMonitors.isEmpty()) {
					synchronized(addMonitors) {
						for(IMonitorDataSubscriber m : this.addMonitors){
							addOneMonitor(m);
						}
						addMonitors.clear();
					}
				}
				
				if(!deleteMonitors.isEmpty()) {
					synchronized(deleteMonitors) {
						for(IMonitorDataSubscriber m : this.deleteMonitors){
							deleteOneMonitor(m);
						}
						deleteMonitors.clear();
					}
				}
				
				boolean f = false;
				if(!this.submiters.isEmpty()) {
					for(Map.Entry<IMonitorDataSubscriber, Long> sub : lastSubmitTime.entrySet()) {
						Set<SubmitItem> items = cacheItems.get(sub.getKey());
						//f=true,有数据要提交
						f = !items.isEmpty();
						if(f) {
							//有数据要提交
							break;
						}
					}
				}
				
				if(!f) {
					//f=false,无数据要提交
					if(openDebug)
						logger.info("wait");
					synchronized(cacheItemsLock) {
						cacheItemsLock.wait(3000);
					}
				}
				
				if(openDebug)
					logger.info("send");
				
				long curTime = System.currentTimeMillis();
				for(Map.Entry<IMonitorDataSubscriber, Long> sub : lastSubmitTime.entrySet()) {
					Set<SubmitItem> items = cacheItems.get(sub.getKey());
					//离上一次提交超过300毫秒或者待提交数超过500
					int batchSize = 50;
					if(items != null && !items.isEmpty() 
							&& (((curTime - sub.getValue()) > 300) ||  items.size() >= batchSize)) {
						synchronized(items) {
							Set<SubmitItem> temp = new HashSet<>();
							if(items.size() < batchSize) {
								temp.addAll(items);
								items.clear();
							}else {
								Iterator<SubmitItem> ite = items.iterator();
								for(int c = batchSize; c> 0; c--) {
									temp.add(ite.next());
									ite.remove();
								}
							}
							//提交一批数据
							this.executor.submit(new Worker(sub.getKey(),temp));
							//记录本次提交时间
							lastSubmitTime.put(sub.getKey(), curTime);
						}
					}
				}
			}
		} catch (Throwable e) {
			//永不结束线程
			logger.error("",e.getMessage());
		}
	}

	private class Worker implements Runnable{
		
		public Worker(IMonitorDataSubscriber sub,Set<SubmitItem> its) {
			this.sub = sub;
			this.its = its;
		}
		
		private IMonitorDataSubscriber sub;
		private Set<SubmitItem> its = null;
		
		@Override
		public void run() {
			try {
				if(its.isEmpty()){
					return;
				}
				//自身所有代码不加入日志统计，否则会进入死循环
				//如果有需要，可以选择其他方式，如slf4j等
				JMicroContext.get().configMonitor(0, 0);
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
				
				if(doAsyncSubmit) {
					JMicroContext.get().setParam(Constants.ASYNC_CONFIG, ac);
				}
				
				//开始RPC提交日志
				SubmitItem[] is = new SubmitItem[its.size()];
				its.toArray(is);
				
				sub.onSubmit(is);
				
				if(doAsyncSubmit) {
					JMicroContext.get().removeParam(Constants.ASYNC_CONFIG);
				}
				
				if(clientStatis) {
					for(SubmitItem it : its) {
						if(it.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
							//这里的代码仅用于测试
							if(!counter.increment(it.getType())) {
								//logger.debug("No Counter: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(si.getType()));
							} else {
								//logger.debug("Counter: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(si.getType()));
							}
						}
					}
				}
			
			} catch (Throwable e) {
				logger.error("",e);
			}
		}
	}
	
	public boolean canSubmit(short type) {
		
		if(!enable){
			if(openDebug)
				logger.debug("enable: {},type: {}",enable,MonitorConstant.MONITOR_VAL_2_KEY.get(type));
			return false;
		}
		
		if(submiters.isEmpty()) {
			if(openDebug)
				logger.debug("submiters.isEmpty(),type: {}",MonitorConstant.MONITOR_VAL_2_KEY.get(type));
			return false;
		}
		
		for(Short[] types : sub2Types.values()) {
			for(int t : types) {
				if(t == type) {
					return true;
				}
			}
		}
		if(openDebug)
			logger.debug("No subscriber for type {}",type);
		return false;
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
		} else {
			si.setSm(JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null));
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
			if(openDebug)
				logger.info("Got NULL item");
			return false;
		}
		
		if(!canSubmit(item.getType())) {
			if(openDebug)
				logger.info("Type [{}] not support",item.getType());
			return false;
		}
		
		setHeader(item);
		
		Set<IMonitorDataSubscriber> subs = this.type2Subscribers.get(item.getType());
		for(IMonitorDataSubscriber sub : subs) {
			Set<SubmitItem> items = this.cacheItems.get(sub);
			if(items.size() > this.maxCacheItems) {
				logger.warn("Exceed: {}",item);
			} else {
				synchronized(items) {
					if(openDebug)
						logger.info("add: {}",item);
					items.add(item);
				}
				synchronized(cacheItemsLock) {
					if(openDebug)
						logger.info("wakup cacheItemsLock: {}",item);
					cacheItemsLock.notify();
				}
			}
		}
		
		return true;
	}
	
}
