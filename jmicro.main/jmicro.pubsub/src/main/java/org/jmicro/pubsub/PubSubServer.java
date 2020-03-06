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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
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
namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",retryCnt=0, monitorEnable=0,timeout=5000)
@Component(level=5)
public class PubSubServer implements IInternalSubRpc{
	
	private final static Logger logger = LoggerFactory.getLogger(PubSubServer.class);
	
	private static final String DISCARD_TIMER = "PubsubServerAbortPolicy";
	
	/**
	 * is enable pubsub server
	 */
	@Cfg(value="/PubSubManager/enableServer",defGlobal=false, changeListener="initPubSubServer")
	private boolean enableServer = false;
	
	@Cfg(value="/PubSubServer/openDebug")
	private boolean openDebug = false;
	
	//内存存储最大的失败消息数量，超过此值会存储到磁盘
	@Cfg(value="/PubSubServer/maxFailItemCount")
	private int maxFailItemCount = 100;
	
	//默认1千万个持久化存储数据
	@Cfg(value="/PubSubServer/maxCachePersistItem",defGlobal=true)
	private int maxCachePersistItem = 10000000;
	
	//默认1千万个内存侍发送数据，如果超过了此数，拒绝客户端继续提交
	@Cfg(value="/PubSubServer/maxMemoryItem",defGlobal=true)
	private int maxMemoryItem = 1000;
	
	//极端高峰流量时，服务器直接拒绝请求，以确保服务不挂机,此值是拒绝时长
	@Cfg(value="/PubSubServer/reOpenThreadInterval")
	private long reOpenThreadInterval = 1000;
	
	//做重发时间间隔
	@Cfg(value="/PubSubServer/doResendInterval",changeListener="resetResendTimer")
	private long doResendInterval = 1000;
	
	//当前内存消息数量
	private AtomicInteger memoryItemsCnt = new AtomicInteger();
	
	//当前缓存消息数量
	private AtomicInteger cacheItemsCnt = new AtomicInteger();
	
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
	
	private ResendManager resendManager;
	
	private ItemStorage<PSData> cacheStorage;
	
	private AtomicBoolean discard = new AtomicBoolean(false);
	
	private Map<String,List<PSData>> sendItems = new HashMap<>();
	
	private Map<String,Long> lastSendTimes = new HashMap<>();
	
	private ExecutorService executor = null;
	
	private Object syncLocker = new Object();
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {});
		 Utils.getIns().waitForShutdown();
	}
	
	@Override
	public int publishItem(PSData item) {
		return publishItems(item.getTopic(),new PSData[]{item});
	}
	
	public int publishString(String topic,String content) {
		if(!this.subManager.isValidTopic(topic)) {
			return PubSubManager.PUB_TOPIC_NOT_VALID;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		return publishItems(topic,new PSData[]{item});
	}
	
	/**
	 * 同一主题的多个消息
	 */
	@Override
	public int publishItems(String topic,PSData[] items) {
		if(!this.subManager.isValidTopic(topic)) {
			return PubSubManager.PUB_TOPIC_NOT_VALID;
		}
		
		if(items == null || StringUtils.isEmpty(topic)) {
			//无效消息
			return PubSubManager.PUB_SERVER_DISCARD;
		}
		
		long size = this.memoryItemsCnt.get() + items.length;
		if(size > this.maxMemoryItem && (items.length + cacheItemsCnt.get()) > this.maxCachePersistItem) {
			return PubSubManager.PUB_SERVER_BUSUY;
		}
		
		if(size < this.maxMemoryItem) {
			List<PSData> l = this.sendItems.get(topic);
			if(l == null) {
				synchronized(syncLocker) {
					 l = this.sendItems.get(topic);
					 if(l == null) {
						 this.sendItems.put(topic, l = new ArrayList<PSData>());
						 lastSendTimes.put(topic, System.currentTimeMillis());
					 }
				}
			}
			
			synchronized(l) {
				if(openDebug) {
					//logger.info("Client publish topic:{}",topic);
				}
				l.addAll(Arrays.asList(items));
				memoryItemsCnt.addAndGet(items.length);
			}
		} else {
			this.cacheStorage.push(topic,items);
			cacheItemsCnt.addAndGet(items.length);
			if(openDebug) {
				logger.info("push to cache :{},total:{}",items.length,cacheItemsCnt.get());
			}
		}
		
		synchronized(syncLocker) {
			syncLocker.notifyAll();
		}
		
		return PubSubManager.PUB_OK;
	}
	
	public void init() {

		if(!isEnableServer()) {
			logger.warn("/PubSubManager/isEnableServer must be true for pubsub server");
			return;
		}
		
		subManager = new SubcriberManager(of,this.openDebug);
		
		this.resendManager = new ResendManager(of,this.openDebug,maxFailItemCount,doResendInterval);
		resendManager.setSubManager(this.subManager);
		
		this.cacheStorage = new ItemStorage<PSData>(of,"/pubsubCache/");
		
		if(reOpenThreadInterval <= 0) {
			logger.warn("Invalid reOpenThreadInterval: {}, set to default:{}",this.reOpenThreadInterval,1000);
			this.reOpenThreadInterval = 1000;
		}
		//this.sendItems = new ConcurrentLinkedQueue<>(new HashSet<>(maxFailItemCount));
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(5);
		config.setTaskQueueSize(1000);
		config.setThreadNamePrefix("PublishExecurot");
		config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = ExecutorFactory.createExecutor(config);
		
		Set<String> children = this.dataOp.getChildren(Config.PubSubDir,true);
		for(String t : children) {
			Set<String>  subs = this.dataOp.getChildren(Config.PubSubDir+"/"+t,true);
			for(String sub : subs) {
				this.dataOp.deleteNode(Config.PubSubDir+"/"+t+"/"+sub);
			}
		}
		
		Thread checkThread = new Thread(this::doCheck,"JMicro-"+Config.getInstanceName()+"-PubSubServer");
		checkThread.setDaemon(true);
		checkThread.start();
		
	}
	
	private void doCheck() {

		int batchSize = 100;
		int sendInterval = 500;
		while (true) {
			try {

				if (memoryItemsCnt.get() == 0 && cacheItemsCnt.get() == 0) {
					synchronized (syncLocker) {
						syncLocker.wait(1000);
					}
				}

				long curTime = System.currentTimeMillis();
				if (memoryItemsCnt.get() < batchSize) {
					// 优先发送内存中的消息，如果内存中无消息，则发送缓存中的消息
					for (Map.Entry<String, Long> e : lastSendTimes.entrySet()) {
						if (curTime - e.getValue() > sendInterval && this.cacheStorage.len(e.getKey()) > 0) {
							List<PSData> items = this.cacheStorage.pops(e.getKey(), batchSize);
							cacheItemsCnt.addAndGet(-items.size());

							List<PSData> is = sendItems.get(e.getKey());
							if (is == null) {
								synchronized (syncLocker) {
									is = sendItems.get(e.getKey());
									if (is == null) {
										sendItems.put(e.getKey(), is = new ArrayList<>());
									}
								}
							}
							
							synchronized (is) {
								if(openDebug) {
									logger.info("end get items from cache topic:{}",e.getKey());
								}
								is.addAll(items);
							}
							
							memoryItemsCnt.addAndGet(items.size());
							
							if(openDebug) {
								logger.info("begin get items from cache topic:{}",e.getKey());
							}
						}
					}
				}

				if (memoryItemsCnt.get() == 0) {
					// 没有待发送消息，进入下一轮循环
					if (openDebug) {
						//logger.info("No data to submit:");
					}
					continue;
				}
				
				int sendSize = 0;

				for (Map.Entry<String, List<PSData>> e : sendItems.entrySet()) {

					if (e.getValue().isEmpty()) {
						//无消息要发送
						continue;
					}
					
					long lastSendTime = lastSendTimes.get(e.getKey());
					if(e.getValue().size() < batchSize && System.currentTimeMillis() - lastSendTime < sendInterval) {
						//消息数量及距上次发送时间间隔都不到，直接路过
						continue;
					}

					//更新发送时间
					lastSendTimes.put(e.getKey(), System.currentTimeMillis());
					
					List<PSData> ll = e.getValue();

					int size = ll.size();
					if (size > batchSize) {
						// 每批次最多能同时发送batchSize个消息
						size = batchSize;
					}

					PSData[] items = new PSData[size];
					synchronized (ll) {
						int i = 0;
						for (Iterator<PSData> ite = ll.iterator(); ite.hasNext() && i < size; i++) {
							items[i] = ite.next();
							ite.remove();
						}
					}
					sendSize += items.length;
					this.executor.submit(new Worker(items, e.getKey()));
				}
				
				if(sendSize == 0 && memoryItemsCnt.get()/batchSize > 5) {
					String msg = "Pubsub server got in exception statu: memory size: "+memoryItemsCnt.get()+
							",cacheItemsCnt size:"+cacheItemsCnt.get()+", but no message to send";
					logger.error(msg);
					//SF.doBussinessLog(MonitorConstant.LOG_ERROR, PubSubServer.class, null, msg);
				}

			} catch (Throwable e) {
				// 永不结束线程
				logger.error("doCheck异常", e);
				//SF.doBussinessLog(MonitorConstant.LOG_ERROR, PubSubServer.class, e, "doCheck异常");
			}
		}
		
	}

	private class Worker implements Runnable{
		
		private PSData[] items = null;
		
		private Set<ISubCallback> callbacks = null;
		
		private String topic = null;
		
		public Worker(PSData[] items,String topic) {
			this.items = items;
			this.topic = topic;
			this.callbacks =  subManager.getCallback(topic);
		}
		
		@Override
		public void run() {
			try {
				
				/*if (openDebug) {
					logger.info("submit topic:{},callbacks size:{}", topic, callbacks == null ? 0 : callbacks.size());
				}*/
				if(callbacks == null || callbacks.isEmpty()) {
					//没有对应主题的监听器，直接进入重发队列，此时回调cb==null
					SendItem si = new SendItem(SendItem.TYPY_RESEND, null, items, 0);
					resendManager.queueItem(si);
					logger.error("Push to resend component topic:"+topic);
				} else {
					for (ISubCallback cb : callbacks) {
						PSData[] psds = null;
						try {
							psds = cb.onMessage(items);
						} catch (Throwable e) {
							// 进入重发队列
							if (psds != null && psds.length > 0) {
								SendItem si = new SendItem(SendItem.TYPY_RESEND, cb, psds, 0);
								resendManager.queueItem(si);
								logger.error("Push to resend component:"+cb.getSm().getKey().toKey(true, true, true));
							}
							logger.error("Worker get exception", e);
							//SF.doBussinessLog(MonitorConstant.LOG_ERROR, PubSubServer.class, e, "Subscribe mybe down: "+cb.getSm().getKey().toKey(true, true, true));
						}
					}
				}
			} finally {
				// 全部失败消息进入重发组件，在此也算成功，减去此批消息数量
				int size = memoryItemsCnt.getAndAdd(-items.length);
				if(openDebug) {
					//logger.info("Do decrement memoryItemsCnt cur:"+memoryItemsCnt.get()+", before:"+size);
				}
			}
		}
	}

	private boolean isEnableServer() {
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
