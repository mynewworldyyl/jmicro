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
import java.util.concurrent.atomic.AtomicBoolean;

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
	
	private static final String DISCARD_TIMER = "PubsubServerAbortPolicy";
	
	private static final String RESEND_TIMER = "PubsubServerResendTimer";
	
	@Cfg(value="/PubSubServer/openDebug")
	private boolean openDebug = false;
	
	@Cfg(value="/PubSubServer/maxFailItemCount")
	private int maxFailItemCount = 10000;
	
	@Cfg(value="/PubSubServer/reOpenThreadInterval")
	private long reOpenThreadInterval = 1000;
	
	@Cfg(value="/PubSubServer/doResendInterval",changeListener="resetResendTimer")
	private long doResendInterval = 1000;
	
	private long doResendInterval0 = 1000;
	
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
	
	private AtomicBoolean discard = new AtomicBoolean(false);
	
	/**
	 *  订阅ID到回调之间映射关系
	 */
	private Map<String,ISubCallback> callbacks = new ConcurrentHashMap<>();	
	
	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String,Set<ISubCallback>> topic2Callbacks = new ConcurrentHashMap<>();
	
	private Map<Long,TimerTicker> resendTimers = new ConcurrentHashMap<>();
	
	private Queue<SendItem> psitems = null;
	
	private ExecutorService executor = null;
	
	private boolean running = false;
	
	private Object loadingLock = new Object();
	private Queue<SubcribeItem> waitingLoadClazz = new ConcurrentLinkedQueue<>();
	private ClassLoadingWorker clWorker = null;
	
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

		if(!pubsubManager.isEnableServer()) {
			logger.warn("/PubSubManager/isEnableServer must be true for pubsub server");
			return;
		}
		
		if(this.maxFailItemCount <=0) {
			logger.warn("Invalid maxFailItemCount: {}, set to default:{}",this.maxFailItemCount,10000);
			this.maxFailItemCount = 10000;
		}
		
		if(reOpenThreadInterval <= 0) {
			logger.warn("Invalid reOpenThreadInterval: {}, set to default:{}",this.reOpenThreadInterval,1000);
			this.reOpenThreadInterval = 1000;
		}
		
		this.psitems = new ConcurrentLinkedQueue<>(new HashSet<>(maxFailItemCount));
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(1000);
		config.setThreadNamePrefix("PublishExecurot");
		config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = ExecutorFactory.createExecutor(config);
		
		pubsubManager.addSubsListener(subListener);
		
		clWorker = new ClassLoadingWorker();
		clWorker.start();
		
		resetResendTimer();
	}
	
	public void resetResendTimer() {
		
		logger.info("Reset timer with doResendInterval0:{},doResendInterval:{}",doResendInterval0,doResendInterval);
		TimerTicker.getTimer(this.resendTimers, doResendInterval0).removeListener(RESEND_TIMER,true);
		
		TimerTicker.getTimer(this.resendTimers, doResendInterval)
		.addListener(RESEND_TIMER, (key,att)->{
			try {
				doResend();
			} catch (Throwable e) {
				logger.error("Submit doResend fail: ",e);
			}
		}, null);
		
		doResendInterval0 = doResendInterval;
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
		if(discard.get()) {
			//logger.warn("Disgard One:{}",topic);
			//由发送者处理失败消息
			return false;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		return this.publishData(item);
	}
	
	public boolean publishData(PSData item) {
		if(discard.get()) {
			//logger.warn("Disgard One:{}",item.getTopic());
			//由发送者处理失败消息
			return false;
		}
		try {
			executor.submit(()->{
				//在这里失败的消息，服务负责失败重发，最大限度保证消息能送达目标结点
				doPublish(item);
			});
			return true;
		} catch (RejectedExecutionException e) {
			//线程对列满了，停指定时间，待消息队列有空位再重新提交
			discardOneTime();
			logger.error("",e);
			//由发送者处理失败消息
			return false;
		}
	}
	
	private void doFailItem(PSData item,int qsize) {
		logger.error("Discard Item:{},queue size:{}, maxFailItemCount:{}",qsize,maxFailItemCount);
	}

	private void discardOneTime() {
		logger.warn("Thread pool exceed and stop {} Milliseconds",this.reOpenThreadInterval);
		discard.set(true);
    	TimerTicker.getDefault(this.reOpenThreadInterval).addListener(DISCARD_TIMER,(key,att)->{
    		discard.set(false);
    		logger.warn("Thread pool reopen after {} Milliseconds!",this.reOpenThreadInterval);
    		TimerTicker.getDefault(this.reOpenThreadInterval).removeListener(DISCARD_TIMER,false);
    	},null);
	}
	
	private void doPublish(PSData item) {
		String topic = item.getTopic();
		/*if(openDebug) {
			logger.debug("Got topic: {}",item.getTopic());
		}*/
		//synchronized(topic) {
		//没必要做同步
		//如果在对主题分发过程中有订阅进来，最多就少接收或多收消息
		//基于pubsub的应该注意此特性，如果应用接受不了，就不应该使得pubsub,
		//或者确保在消息发送前注册或删除订阅器，
		Set<ISubCallback> q = topic2Callbacks.get(topic);
		if(q == null || q.isEmpty()) {
			if(openDebug) {
				logger.debug("No subscriber for: {}",item.getTopic());
			}
		} else {
			for(ISubCallback cb : q) {
				/*if(openDebug) {
					logger.debug("Publish topic: {}, cb {}",item.getTopic(),cb.info());
				}*/
				try {
					cb.onMessage(item);
				} catch (Throwable e) {
					logger.error("doPublish,Fail Item Size["+psitems.size()+"]",e);
					//需要重发消息
					if(psitems.size() > maxFailItemCount) {
						//失败队列满，丢弃消息，确保服务能自动恢复
						doFailItem(item,psitems.size());
						return;
					}
					psitems.offer(new SendItem(SendItem.TYPY_RESEND,cb,item));
				}
			}
		}
		//}
	}
	
	private void doResend() {
		
		if(psitems.isEmpty()) {
			/*if(openDebug) {
				logger.debug("doResend check empty");
			}*/
			return;
		}
		if(openDebug) {
			logger.debug("doResend submit ones, send size:{}",psitems.size());
		}
		executor.submit(()->{
			Set<SendItem> failPsDatas = new HashSet<>();
			int cnt = 10;
			for(SendItem si = psitems.poll(); si != null; si = psitems.poll()) {
				try {
					si.cb.onMessage(si.item);
				} catch (Throwable e1) {
					si.retryCnt++;
					if(si.retryCnt > 3) {
						//doFailItem(si.item,psitems.size());
						logger.error("Retry exceed 3 Item:{},retryCnt:{}",si.item,si.retryCnt);
					} else {
						failPsDatas.add(si);
					}
				}
				cnt--;
				if(cnt <= 0) {
					break;
				}
			}
			if(!failPsDatas.isEmpty()) {
				psitems.addAll(failPsDatas);
				failPsDatas.clear();
			}
		});
		
	}
	
	private final class SendItem {
		public static final int TYPY_RESEND = 1;
		
		public int type;
		public ISubCallback cb;
		public PSData item;
		public int retryCnt=0;
		public Long retryInterval = 1000L;
		
		public SendItem(int type,ISubCallback cb,PSData item) {
			this.type = type;
			this.cb = cb;
			this.item = item;
			retryCnt = 0;
		}
	}
	
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
		
		private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
		
		//存在发送失败需要重新发的项目
		//只对doPublish中失败的消息负责
		//publishString及publishData返回false的消息，由客户端处理
		public ClassLoadingWorker() {
			super("JMicro-"+Config.getInstanceName()+"-ClassLoadingWorker");
		}
		
		public void submit(Runnable r) {
			tasks.offer(r);
			synchronized(loadingLock) {
				loadingLock.notify();;
			}
		}
		
		public void run() {
			Set<SubcribeItem> failItems = new HashSet<>();
			//Set<SendItem> failPsDatas = new HashSet<>();
			this.setContextClassLoader(cl);
			while(true) {
				try {
					if(tasks.isEmpty() && waitingLoadClazz.isEmpty()) {
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
					
					/*for(SendItem psd = psitems.poll(); psd != null; psd = psitems.poll() ) {
						try {
							psd.cb.onMessage(psd.item);
						} catch (Throwable e1) {
							psd.retryCnt++;
							if(psd.retryCnt > 3) {
								logger.error("Fail Item:{},retryCnt:{}",psd.item,psd.retryCnt);
							} else {
								failPsDatas.add(psd);
							}
						}
					}*/
					
					for(Runnable psd = tasks.poll(); psd != null; psd = tasks.poll() ) {
						psd.run();
					}
					
				}catch(Throwable e) {
					logger.error("",e);
				} finally {
					boolean needSleep = false;
					if(!failItems.isEmpty()) {
						waitingLoadClazz.addAll(failItems);
						needSleep = true;
						failItems.clear();
					}
					
					/*if(!failPsDatas.isEmpty()) {
						psitems.addAll(failPsDatas);
						needSleep = true;
						failPsDatas.clear();
					}*/
					
					if(needSleep) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							logger.error("",e);
						}
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
		
		//topic = topic.intern();
		//synchronized(topic) {
			ISubCallback cb = callbacks.remove(k);
			Set<ISubCallback> q = topic2Callbacks.get(topic);
			if(q != null) {
				q.remove(cb);
			}
			return cb != null;
		//}
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
		
		//sui.topic = sui.topic.intern();
		//synchronized(sui.topic) {
			if(!topic2Callbacks.containsKey(sui.topic)) {
				topic2Callbacks.put(sui.topic, new HashSet<ISubCallback>());
			}
			topic2Callbacks.get(sui.topic).add(cb);
		//}
		
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
