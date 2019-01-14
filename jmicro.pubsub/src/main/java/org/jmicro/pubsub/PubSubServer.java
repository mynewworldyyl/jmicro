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
package org.jmicro.pubsub;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.ISubCallback;
import org.jmicro.api.pubsub.ISubsListener;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.pubsub.SubCallbackImpl;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:21
 */
//@Component(value=Constants.DEFAULT_PUBSUB,limit2Packages="org.jmicro.api.pubsub.PubSubManager")
@Service(limit2Packages="org.jmicro.api.pubsub.PubSubManager",
namespace=Constants.DEFAULT_PUBSUB,version="0.0.1")
@Component(level=5)
public class PubSubServer implements IInternalSubRpc{
	
	private final static Logger logger = LoggerFactory.getLogger(PubSubServer.class);
	
	@Cfg(value="/PubSubServer/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private PubSubManager pubsubManager;
	
	@Inject
	private RpcClassLoader cl;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IRegistry registry;
	
	private boolean disgard = false;
	
	/**
	 *  订阅ID到回调之间映射关系
	 */
	private Map<String,ISubCallback> callbacks = new ConcurrentHashMap<>();	
	
	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String,Set<ISubCallback>> topic2Callbacks = new ConcurrentHashMap<>();
	
	private ExecutorService executor = null;
	
	private boolean running = false;
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=PubSubServer"});
		 Utils.getIns().waitForShutdown();
	}
	
	private ISubsListener subListener = new ISubsListener() {
		@Override
		public void on(byte type, String topic, UniqueServiceMethodKey smKey, 
				Map<String, String> context) {
			if(type == ISubsListener.SUB_ADD) {
				subcribe(topic,smKey,context);
			}else if(type == ISubsListener.SUB_REMOVE) {
				unsubcribe(topic,smKey,context);
			}
		}
	};
	
	public void init() {

		if(!pubsubManager.isEnable()) {
			logger.warn("/PubSubManager/enable must be true for pubsub server");
			return;
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(1000);
		config.setThreadNamePrefix("PublishExecurot");
		config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = ExecutorFactory.createExecutor(config);
		
		pubsubManager.addSubsListener(subListener);
		
		clWorker.start();
	}
	
	public void start() {
		if(running) {
			return;
		}
		running = true;
		logger.debug("start pubsub server: {}", PubSubServer.class.getName());
	}
	
	public void stop() {
		
	}
	
	public boolean subcribe(String topic,UniqueServiceMethodKey key,Map<String, String> context) {
		String k = key.toKey(false, false, false);
		if(callbacks.containsKey(k)) {
			logger.warn("{} have been in the callback list",k);
			return true;
		}
		this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_SUB,topic,key,context));
		synchronized(loadingLock) {
			loadingLock.notify();
		}
		return true;
	}
	
	public boolean unsubcribe(String topic,UniqueServiceMethodKey key,Map<String, String> context) {
		String k = key.toKey(false, false, false);
		if(!callbacks.containsKey(k)) {
			return true;
		}
		
		this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_REMOVE,topic,key,context));
		synchronized(loadingLock) {
			loadingLock.notify();
		}
		return true;
	}
	
	public boolean publishString(String topic,String content) {
		if(this.disgard) {
			//logger.warn("Disgard One:{}",topic);
			return false;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		return this.publishData(item);
	}
	
	public boolean publishData(PSData item) {
		if(this.disgard) {
			//logger.warn("Disgard One:{}",item.getTopic());
			return false;
		}
		try {
			executor.submit(()->{
				doPublish(item);
			});
		} catch (RejectedExecutionException e) {
			disgardOneTime();
			logger.error("",e);
		}
		return true;
	}
	
	private void disgardOneTime() {
		logger.warn("Thread pool exceed and disgard one second!");
    	disgard = true;
    	TimerTicker.getDefault(3000L).addListener("PubsubServerAbortPolicy",(key,att)->{
    		disgard = false;
    		logger.warn("Thread pool reopen after one second!");
    		TimerTicker.getDefault(3000L).removeListener("PubsubServerAbortPolicy");
    	},null);
    	
	}
	
	private void doPublish(PSData item) {
		String topic = item.getTopic().intern();
		if(openDebug) {
			logger.debug("Got topic: {}",item.getTopic());
		}
		synchronized(topic) {
			Set<ISubCallback> q = topic2Callbacks.get(topic);
			if(q == null || q.isEmpty()) {
				if(openDebug) {
					logger.debug("No subscriber for: {}",item.getTopic());
				}
			}else {
				for(ISubCallback cb : q) {
					if(openDebug) {
						logger.debug("Publish topic: {}, cb {}",item.getTopic(),cb.info());
					}
					cb.onMessage(item);
				}
			}
			
		}
	}
	
	private Object loadingLock = new Object();
	private Queue<SubcribeItem> waitingLoadClazz = new ConcurrentLinkedQueue<>();
	
	private ClassLoadingWorker clWorker = new ClassLoadingWorker();
	
	private final class SubcribeItem {
		public static final int TYPE_SUB = 1;
		public static final int TYPE_REMOVE = 2;
		
		public int type;
		public String topic;
		public UniqueServiceMethodKey key;
		public Map<String, String> context;
		
		public SubcribeItem(int type,String topic,UniqueServiceMethodKey key,Map<String, String> context) {
			this.type = type;
			this.topic = topic;
			this.key = key;
			this.context = context;
		}
	}
	
	private final class ClassLoadingWorker extends Thread {
		
		public ClassLoadingWorker() {
			super("JMicro-"+Config.getInstanceName()+"-ClassLoadingWorker");
		}
		
		public void run() {
			Set<SubcribeItem> failItems = new HashSet<>();
			this.setContextClassLoader(cl);
			while(true) {
				try {
					if(waitingLoadClazz.isEmpty()) {
						synchronized(loadingLock) {
							loadingLock.wait();
						}
					}
					
					for(SubcribeItem si = waitingLoadClazz.poll(); si != null; si = waitingLoadClazz.poll() ) {
						try {
							switch(si.type) {
							case SubcribeItem.TYPE_SUB:
								if(!doSubscribe(si)) {
									failItems.add(si);
								}
								break;
							case SubcribeItem.TYPE_REMOVE:
								if(!doUnsubcribe(si.topic,si.key,si.context)) {
									failItems.add(si);
								}
								break;
							}
						} catch (Throwable e) {
							failItems.add(si);
							throw e;
						}
					}
				}catch(Throwable e) {
					logger.error("",e);
				} finally {
					if(!failItems.isEmpty()) {
						waitingLoadClazz.addAll(failItems);
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						failItems.clear();
					}
				}
			}
		}
	}
	
	private boolean doUnsubcribe(String topic,UniqueServiceMethodKey key,Map<String, String> context) {
		String k = key.toKey(false, false, false);
		if(openDebug) {
			logger.debug("Unsubscribe CB:{} topic: {}",k,topic);
		}
		
		topic = topic.intern();
		
		synchronized(topic) {
			ISubCallback cb = callbacks.remove(k);
			Set<ISubCallback> q = topic2Callbacks.get(topic);
			if(q != null) {
				q.remove(cb);
			}
			return cb != null;
		}
	}
	
	private boolean doSubscribe(SubcribeItem sui) {
		
		String k = sui.key.toKey(false, false, false);
		
		if(callbacks.containsKey(k)) {
			logger.warn("{} have been in the callback list",k);
			return true;
		}
		
		Set<ServiceItem> sis = registry.getServices(sui.key.getServiceName(), sui.key.getNamespace(), sui.key.getVersion());
		
		if(sis == null || sis.isEmpty()) {
			logger.warn("Service Item not found {}",k);
			return false;
		}
		
		ServiceItem si = sis.iterator().next();
		
		Object srv = null;
		try {
			PubSubServer.class.getClassLoader().loadClass(sui.key.getUsk().getServiceName());
			srv = of.getRemoteServie(si,null);
		} catch (ClassNotFoundException e) {
			try {
				Class<?> cls = this.cl.loadClass(sui.key.getUsk().getServiceName());
				if(cls != null) {
					srv = of.getRemoteServie(si,this.cl);
				}
			} catch (ClassNotFoundException e1) {
				logger.warn("Service {} not found.{}",k,e1);
				return false;
			}
		}
		
		if(srv == null) {
			logger.warn("Servive [" + k + "] not found");
			return false;
		}
		
		SubCallbackImpl cb = new SubCallbackImpl(sui.key,srv);
		
		callbacks.put(k, cb);
		
		sui.topic = sui.topic.intern();
		
		synchronized(sui.topic) {
			if(!topic2Callbacks.containsKey(sui.topic)) {
				topic2Callbacks.put(sui.topic, new HashSet<ISubCallback>());
			}
			topic2Callbacks.get(sui.topic).add(cb);
		}
		
		if(openDebug) {
			logger.debug("Subcribe:{},topic:",k,sui.topic);
		}
		
		return true;
	}

	
	 private class PubsubServerAbortPolicy implements RejectedExecutionHandler {

	        public PubsubServerAbortPolicy() { }
	
	        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
	            throw new RejectedExecutionException("JMcro Pubsub Server Task " + r.toString() +
	                                                 " rejected from " +
	                                                 e.toString());
	        }
	    }
	 
}
