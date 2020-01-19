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
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.ISubCallback;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

/**
 *
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
	
	/**
	 * is enable pubsub server
	 */
	@Cfg(value="/PubSubManager/enableServer",defGlobal=false, changeListener="initPubSubServer")
	private boolean enableServer = false;
	
	@Cfg(value="/PubSubServer/openDebug")
	private boolean openDebug = false;
	
	@Cfg(value="/PubSubServer/maxFailItemCount")
	private int maxFailItemCount = 100;
	
	@Cfg(value="/PubSubServer/maxCachePersistItem",defGlobal=true)
	//默认1千万
	private int maxCachePersistItem = 10000000;
	
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
	private JedisPool cache;
	
	@Inject
	private IDataOperator dataOp;
	
	private SubcriberManager subManager;
	
	private ItemStorage storage;
	
	private AtomicBoolean discard = new AtomicBoolean(false);
	
	private Map<Long,TimerTicker> resendTimers = new ConcurrentHashMap<>();
	
	private Queue<SendItem> resendItems = null;
	
	//private Queue<PSData> sendItems = null;
	
	private ExecutorService executor = null;
	
	private boolean running = false;
	
	//private Map<String,Set<String>> topic2Method = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {});
		 Utils.getIns().waitForShutdown();
	}
	
	public void init() {

		if(!isEnableServer()) {
			logger.warn("/PubSubManager/isEnableServer must be true for pubsub server");
			return;
		}
		
		this.storage = new ItemStorage(of);
		subManager = new SubcriberManager(of,this.openDebug);
		
		if(this.maxFailItemCount <=0) {
			logger.warn("Invalid maxFailItemCount: {}, set to default:{}",this.maxFailItemCount,10000);
			this.maxFailItemCount = 10000;
		}
		
		if(reOpenThreadInterval <= 0) {
			logger.warn("Invalid reOpenThreadInterval: {}, set to default:{}",this.reOpenThreadInterval,1000);
			this.reOpenThreadInterval = 1000;
		}
		
		this.resendItems = new ConcurrentLinkedQueue<>(new HashSet<>(maxFailItemCount));
		
		//this.sendItems = new ConcurrentLinkedQueue<>(new HashSet<>(maxFailItemCount));
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(5000);
		config.setThreadNamePrefix("PublishExecurot");
		config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = ExecutorFactory.createExecutor(config);
		
		initPubSubServer();
		resetResendTimer();
	}
	
	private void initPubSubServer() {
		if(!isEnableServer()) {
			//不启用pubsub Server功能，此运行实例是一个
			logger.info("Pubsub server is disable by config [/PubSubManager/enableServer]");
			return;
		}
		Set<String> children = this.dataOp.getChildren(Config.PubSubDir,true);
		for(String t : children) {
			Set<String>  subs = this.dataOp.getChildren(Config.PubSubDir+"/"+t,true);
			for(String sub : subs) {
				this.dataOp.deleteNode(Config.PubSubDir+"/"+t+"/"+sub);
			}
		}
		
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
	
	public boolean subcribe(String topic,ServiceMethod srvMethod,Map<String, String> context) {
		return this.subManager.subcribe(topic, srvMethod, context);
	}
	
	public boolean unsubcribe(String topic,ServiceMethod srvMethod,Map<String, String> context) {
		return this.subManager.unsubcribe(topic, srvMethod, context);
	}
	
	public long publishString(String topic,String content) {
		if(discard.get()) {
			//logger.warn("Disgard One:{}",topic);
			//由发送者处理失败消息
			return PubSubManager.PUB_SERVER_DISCARD;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		return this.publishData(item);
	}
	
	public long publishData(PSData item) {
		if(discard.get()) {
			//logger.warn("Disgard One:{}",item.getTopic());
			//由发送者处理失败消息
			return PubSubManager.PUB_SERVER_DISCARD;
		}
		try {
			/*if(this.sendItems.size() > this.maxFailItemCount) {
				//本服务发送队列已经达上限，由发送者负责重发
				return PubSubManager.PUB_SERVER_DISCARD;
			}*/
			//sendItems.offer(item);
			
			executor.submit(()->{
				//在这里失败的消息，服务负责失败重发，最大限度保证消息能送达目标结点
				if(isQueue(item.getFlag())) {
					doPublishQuque(item,0,0);
				} else {
					doPublishSubsub(item,0,0);
				}
			});
			
			return item.getId();
		} catch (RejectedExecutionException e) {
			//线程对列满了,停指定时间,待消息队列有空位再重新提交
			logger.error("",e);
			//由发送者处理失败消息
			SendItem si = new SendItem(SendItem.TYPY_RESEND,null,item,0);
			return queueItem(si);
		}
	}
	
	private long queueItem(SendItem item) {
		if(item.retryCnt < 2 && resendItems.size() < maxFailItemCount) {
			//内存缓存
			resendItems.offer(item);
		} else {
			long l = storage.len(item.item.getTopic());
			if(l < this.maxCachePersistItem) {
				//做持久化
				storage.push(item);
			} else {
				//没办法，服务器吃不消了，直接丢弃
				SF.doBussinessLog(MonitorConstant.LOG_ERROR,PubSubServer.class,null, 
						"缓存消息量已经达上限："+JsonUtils.getIns().toJson(item));
				discardOneTime();
				return PubSubManager.PUB_SERVER_DISCARD;
			}
		}
		return item.item.getId();
	}

	private void doPublishSubsub(PSData item,int retryCnt, long time) {

		String topic = item.getTopic();
		/*if(openDebug) {
			logger.debug("Got topic: {}",item.getTopic());
		}*/
		//如果在对主题分发过程中有订阅进来,最多就少接收或多收消息
		//基于pubsub的应该注意此特性,如果应用接受不了,就不应该使得pubsub,
		//或者确保在消息发送前注册或取消订阅器，
		Set<ISubCallback> q = this.subManager.getCallback(topic);
		if(q == null || q.isEmpty()) {
			if(openDebug) {
				//消息被丢弃
				logger.debug("No subscriber for: {}",item.getTopic());
				SF.doBussinessLog(MonitorConstant.LOG_ERROR,PubSubServer.class,null, 
						"订阅消息当前无消费者,进入持久存储并重发msgID："+JsonUtils.getIns().toJson(item));
			}
			SendItem si = new SendItem(SendItem.TYPY_RESEND,null,item,retryCnt);
			si.time = time;
			queueItem(si);
		} else {
			boolean flag = false;
			for(ISubCallback cb : q) {
				/*if(openDebug) {
					logger.debug("Publish topic: {}, cb {}",item.getTopic(),cb.info());
				}*/
				try {
					cb.onMessage(item);
					//最少一个消费者成功消费消息
					flag = true;
				} catch (Throwable e) {
					flag = false;
					logger.error("doPublish,Fail Item Size["+resendItems.size()+"]",e);
					String info = "";
					try {
						info = cb.info();
					} catch (Throwable e1) {
					}
					SF.doBussinessLog(MonitorConstant.LOG_ERROR,PubSubServer.class,e, info+"\n"+JsonUtils.getIns().toJson(item));
				}
			}
			if(!flag) {
				//一个都没有成功
				//需要重发消息
				queueItem(new SendItem(SendItem.TYPY_RESEND,null,item,retryCnt));
			}
		}
	}

	private void doPublishQuque(PSData item,int retryCnt,long time) {

		String topic = item.getTopic();
		/*if(openDebug) {
			logger.debug("Got topic: {}",item.getTopic());
		}*/
		//synchronized(topic) {
		//没必要做同步
		//如果在对主题分发过程中有订阅进来,最多就少接收或多收消息
		//基于pubsub的应该注意此特性,如果应用接受不了,就不应该使得pubsub,
		//或者确保在消息发送前注册或取消订阅器，
		Set<ISubCallback> q = this.subManager.getCallback(topic);
		if(q == null || q.isEmpty()) {
			if(openDebug) {
				//消息进入重发队列
				logger.debug("No subscriber for: {}",item.getTopic());
			}
			//等待消费者上线
			SendItem si = new SendItem(SendItem.TYPY_RESEND,null,item,retryCnt++);
			si.time = time;
			queueItem(si);
		} else {
			for(ISubCallback cb : q) {
				/*if(openDebug) {
					logger.debug("Publish topic: {}, cb {}",item.getTopic(),cb.info());
				}*/
				try {
					cb.onMessage(item);
					//队列类消息,只要一个并且只有一个消费者成功消费
					return;
				} catch (Throwable e) {
					logger.error("doPublishQuque",e);
					String info = "";
					try {
						info = cb.info();
					} catch (Throwable e1) {
					}
					SF.doBussinessLog(MonitorConstant.LOG_ERROR,PubSubServer.class,e, info+"\n"+JsonUtils.getIns().toJson(item));
				}
			}
			
			//需要重发消息
			SendItem si = new SendItem(SendItem.TYPY_RESEND,null,item,retryCnt++);
			si.time = time;
			queueItem(si);
			
			/*if(resendItems.size() > maxFailItemCount) {
				//失败队列满,丢弃消息,确保服务能自动恢复
				SF.doBussinessLog(MonitorConstant.LOG_ERROR,PubSubServer.class,null, "队列消息发送失败,进入持久存储重发msgID："+item.getId());
				persist(item);
			} else {
				resendItems.offer(new SendItem(SendItem.TYPY_RESEND,null,item,retryCnt++));
			}*/
		
		}
	}

	/**
	 * 极端高峰流量时，服务器直接拒绝请求，以确保服务不挂机
	 */
	private void discardOneTime() {
		logger.warn("Thread pool exceed and stop {} Milliseconds",this.reOpenThreadInterval);
		discard.set(true);
    	TimerTicker.getDefault(this.reOpenThreadInterval).addListener(DISCARD_TIMER,(key,att)->{
    		discard.set(false);
    		logger.warn("Thread pool reopen after {} Milliseconds!",this.reOpenThreadInterval);
    		TimerTicker.getDefault(this.reOpenThreadInterval).removeListener(DISCARD_TIMER,false);
    	},null);
	}
	
	private boolean isQueue(byte flag) {
		return Message.is(flag, PSData.FLAG_QUEUE);
	}

	private void doResend() {
		
		if(resendItems.isEmpty() ) {
			/*if(openDebug) {
				logger.debug("doResend check empty");
			}*/
			
			Set<String> keys = this.subManager.topics();
			for(String k : keys) {
				long l = 0;
				if((l = storage.len(k)) > 0) {
					if(l > 100) {
						l = 100;
					}
					for(;l > 0;) {
						resendItems.addAll(storage.pops(k, l));
					}
					
				}
			}
		}
		
		if( resendItems.isEmpty() ) {
			return;
		}
		
		if(openDebug) {
			logger.debug("doResend submit ones, send size:{}",resendItems.size());
		}
		executor.submit(()->{
			for(SendItem si = resendItems.poll(); si != null; si = resendItems.poll()) {
				if(isQueue(si.item.getFlag())) {
					doPublishQuque(si.item,si.retryCnt,si.time);
				} else {
					doPublishSubsub(si.item,si.retryCnt,si.time);
				}
			}
		});
	}
	
 final class SendItem {
		transient public static final int TYPY_RESEND = 1;
		
		transient public ISubCallback cb;
		public PSData item;
		public int retryCnt=0;
		
		public long time = 0;
		
		public SendItem(int type,ISubCallback cb,PSData item,int retryCnt) {
			this.cb = cb;
			this.item = item;
			this.retryCnt = retryCnt;
			time = System.currentTimeMillis();
		}
	}

	public boolean isEnableServer() {
		return this.enableServer;
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
