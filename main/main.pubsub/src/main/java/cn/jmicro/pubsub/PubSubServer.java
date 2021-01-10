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
package cn.jmicro.pubsub;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.basket.BasketFactory;
import cn.jmicro.api.basket.IBasket;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.internal.pubsub.IInternalSubRpc;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import redis.clients.jedis.JedisPool;

/**
 *
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:21
 */
@Service(limit2Packages="org.jmicro.api.pubsub.PubSubManager",
namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",retryCnt=0, monitorEnable=0,timeout=5000)
@Component(level=5)
public class PubSubServer implements IInternalSubRpc{
	
	private final static Logger logger = LoggerFactory.getLogger(PubSubServer.class);
	
	//private static final String DISCARD_TIMER = "PubsubServerAbortPolicy";
	
	/**
	 * is enable pubsub server
	 */
	//@Cfg(value="/PubSubManager/enableServer",defGlobal=false, changeListener="initPubSubServer")
	//private boolean enableServer = false;
	
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
	
	private int batchSize = 100;
	private int sendInterval = 300;
	
	//当前内存消息数量
	//private AtomicInteger memoryItemsCnt = new AtomicInteger();
	
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
	
	@Inject
	private PubsubMessageStatis sta;
	
	private SubcriberManager subManager;
	
	private ResendManager resendManager;
	
	private ItemStorage<PSData> cacheStorage;
	
	private BasketFactory<PSData> basketFactory = null;
	
	private Map<String,List<PSData>> sendCache = new HashMap<>();
	
	private Map<String,Long> lastSendTimes = new HashMap<>();
	
	private ExecutorService executor = null;
	
	private Object syncLocker = new Object();
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}
	
	public void init() {

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
		config.setMsCoreSize(10);
		config.setMsMaxSize(100);
		config.setTaskQueueSize(10000);
		config.setThreadNamePrefix("PublishExecurot");
		config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		
		basketFactory = new BasketFactory<PSData>(2000,20);
		
		/*Set<String> children = this.dataOp.getChildren(Config.PubSubDir,true);
		for(String t : children) {
			Set<String>  subs = this.dataOp.getChildren(Config.PubSubDir+"/"+t,true);
			for(String sub : subs) {
				this.dataOp.deleteNode(Config.PubSubDir+"/"+t+"/"+sub);
			}
		}*/
		
	}
	
	public void ready() {
		
		Thread checkThread = new Thread(this::doCheck,"JMicro-"+Config.getInstanceName()+"-PubSubServer");
		checkThread.setDaemon(true);
		checkThread.start();
		
	}
	
	@Override
	public boolean hasTopic(String topic) {
		return this.subManager.isValidTopic(topic);
	}

	/**
	 * asyncable=false，此方法不能是异步方法，否则会构成异步死循环
	 */
	@Override
	@SMethod(timeout=5000,retryCnt=0,asyncable=false,debugMode=0)
	public int publishItem(PSData item) {
		return publishItems(item.getTopic(),new PSData[]{item});
	}
	
	/**
	 * asyncable=false，此方法不能是异步方法，否则会构成异步死循环
	 */
	@SMethod(timeout=5000,retryCnt=0,asyncable=false,debugMode=0)
	public int publishString(String topic,String content) {
		if(!this.subManager.isValidTopic(topic)) {
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), " PUB_TOPIC_INVALID for: " + topic);
			}
			return PubSubManager.PUB_TOPIC_INVALID;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		return publishItems(topic,new PSData[]{item});
	}
	
	private void foreachItem(PSData[] items,Consumer<PSData> c) {
		for(PSData pd: items) {
			if(pd != null) {
				c.accept(pd);
			}
		}
	}
	
	/**
	 * 同一主题的多个消息。
	 * asyncable=false，此方法不能是异步方法，否则会构成异步死循环
	 */
	@Override
	@SMethod(timeout=5000,retryCnt=0,asyncable=false,debugMode=0)
	public int publishItems(String topic,PSData[] items) {
		
		/*if(openDebug) {
			logger.info("GOT: " + topic);
		}*/
		
		/*boolean me = this.statusMonitorAdapter.monitoralbe;
		ServiceCounter sc = this.statusMonitorAdapter.getServiceCounter();*/
		long curTime = TimeUtils.getCurTime();
		if(items != null && items.length > 0) {
			/*if(me) {
				sc.add(MC.Ms_ReceiveItemCnt,items.length);
			}*/
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_ReceiveItemCnt, 1);
			});
		}
		
		if(!this.subManager.isValidTopic(topic)) {
			/*if(me) {
				sc.add(MC.Ms_TopicInvalid, items.length);
			}*/
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_TopicInvalid, 1);
			});
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), " PUB_TOPIC_INVALID for: " + topic);
			}
			return PubSubManager.PUB_TOPIC_INVALID;
		}
		
		if(items == null || StringUtils.isEmpty(topic) || items.length == 0) {
			//无效消息
			/*if(me) {
				sc.add(MC.Ms_ServerDisgard, 1);
			}*/
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_ServerDisgard, 1);
			});
			
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), " PUB_SERVER_DISCARD null items for: " + topic);
			}
			
			return PubSubManager.PUB_SERVER_DISCARD;
		}
		
		long size = basketFactory.size() + items.length;
		if(size > this.maxMemoryItem && (items.length + cacheItemsCnt.get()) > this.maxCachePersistItem) {
			//无效消息
			/*if(me) {
				sc.add(MC.Ms_ServerBusy,1);
			}*/
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_ServerBusy, 1);
			});
			
			if(LG.isLoggable(MC.LOG_WARN)) {
				LG.log(MC.LOG_WARN, this.getClass(), " PUB_SERVER_BUSUY : " + topic +"send len: " + 
						items.length+" max "+this.maxCachePersistItem);
			}
			
			return PubSubManager.PUB_SERVER_BUSUY;
		}
		
		if(!lastSendTimes.containsKey(topic)) {
			lastSendTimes.put(topic, curTime);
		}
		
		/*if(me) {
			sc.add(MC.Ms_SubmitCnt, items.length);
		}*/
		
		foreachItem(items,(pd)->{
			this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_SubmitCnt, 1);
		});
		
		//IBasket<PSData> b = this.basketFactory.borrowWriteBasket(true);
		
		if(size < this.maxMemoryItem) {
			int pos = 0;
			while(pos < items.length) {
				IBasket<PSData>  b = basketFactory.borrowWriteBasket(true);
				if(b != null) {
					int re = b.remainding();
					int len = re;
					if(items.length - pos < re) {
						len = items.length - pos;
					}	
					if(b.write(items,pos,len)) {
						/*if(openDebug) {
							logger.info("basket isEmpty: {}",b.isEmpty());
						}*/
						boolean rst = basketFactory.returnWriteBasket(b, true);
						if(rst) {
							pos += len;
							continue;
						}else {
							/*if(me) {
								sc.add(MC.Ms_FailReturnWriteBasket, 1);
								sc.add(MC.Ms_FailItemCount, items.length);
							}*/
							foreachItem(items,(pd)->{
								this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailReturnWriteBasket, 1);
								this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
							});
							String errMsg = "Fail to return basket fail size: "+ (items.length - pos);
							LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
							logger.error(errMsg);
							break;
						}
					} else {
						basketFactory.returnWriteBasket(b, true);
						String errMsg = "Fail write basket size: "+ (items.length - pos);
						LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
						logger.error(errMsg);
						/*if(me) {
							//sc.add(MonitorConstant.Ms_Fail2BorrowBasket, 1);
							sc.add(MC.Ms_FailItemCount, items.length - pos);
						}*/
						foreachItem(items,(pd)->{
							this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
						});
						break;
					}	
				} else {
					/*if(me) {
						sc.add(MC.Ms_FailReturnWriteBasket, 1);
						sc.add(MC.Ms_FailItemCount, items.length);
					}*/
					foreachItem(items,(pd)->{
						this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailReturnWriteBasket, 1);
						this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
					});
					String errMsg = "Fail size: "+ (items.length - pos);
					LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
					logger.error(errMsg);
					break;
				}
			}
		} else {
			this.cacheStorage.push(topic,items,0,items.length);
			cacheItemsCnt.addAndGet(items.length);
			/*if(me) {
				sc.add(MC.Ms_Pub2Cache, items.length);
			}*/
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_Pub2Cache, 1);
			});
			
			String errMsg = "push to cache :"+items.length+",total:"+cacheItemsCnt.get();
			LG.log(MC.LOG_WARN, this.getClass(), errMsg);
			logger.warn(errMsg);
		}
		
		synchronized(syncLocker) {
			syncLocker.notifyAll();
		}
		
		if(JMicroContext.get().isDebug()) {
			JMicroContext.get().appendCurUseTime("pubsub server finishTime",true);
		}
		
		/*if(openDebug) {
			logger.info("Resp OK: " + topic);
		}*/
		
		return PubSubManager.PUB_OK;
	}
	
	private int writeBasket(PSData[] items,long curTime) {
		
		/*boolean me = this.statusMonitorAdapter.monitoralbe;
		ServiceCounter sc = this.statusMonitorAdapter.getServiceCounter();*/
		
		int pos = 0;
		while(pos < items.length) {
			IBasket<PSData>  b = basketFactory.borrowWriteBasket(true);
			if(b != null) {
				int re = b.remainding();
				int len = re;
				if(items.length - pos < re) {
					len = items.length - pos;
				}
				if(b.write(items,pos,len)) {
					boolean rst = basketFactory.returnWriteBasket(b, true);
					if(rst) {
						pos += len;
						continue;
					}else {
						/*if(me) {
							sc.add(MC.Ms_FailReturnWriteBasket, 1);
							sc.add(MC.Ms_FailItemCount, items.length - pos);
						}*/
						
						foreachItem(items,(pd)->{
							this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailReturnWriteBasket, 1);
							this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
						});
						String errMsg = "Fail to return basket fail size: "+ (items.length - pos);
						LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
						logger.error(errMsg);
						break;
					}
					
				} else {
					basketFactory.returnWriteBasket(b, true);
					String errMsg = "Fail write basket size: "+ (items.length - pos);
					LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
					logger.error(errMsg);
					/*if(me) {
						//sc.add(MonitorConstant.Ms_Fail2BorrowBasket, 1);
						sc.add(MC.Ms_FailItemCount, items.length - pos);
					}*/
					foreachItem(items,(pd)->{
						this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
					});
					break;
				}	
			} else {
				/*if(me) {
					sc.add(MC.Ms_FailReturnWriteBasket, 1);
					sc.add(MC.Ms_FailItemCount, items.length);
				}*/
				
				foreachItem(items,(pd)->{
					this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailReturnWriteBasket, 1);
					this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_FailItemCount, 1);
				});
				String errMsg = "Fail size: "+ (items.length - pos);
				LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
				logger.error(errMsg);
				break;
			}
		}
		return pos;
	}
	
	private void doCheck() {

		//long lastLoopTime = System.currentTimeMillis();
		while (true) {
			try {

				/*boolean me = this.statusMonitorAdapter.monitoralbe;
				ServiceCounter sc = this.statusMonitorAdapter.getServiceCounter();*/
				
				/*if(me) {
					sc.add(MC.Ms_CheckLoopCnt, 1);
				}*/
				
				long len = basketFactory.size();
				long curTime = TimeUtils.getCurTime();
				if (len < batchSize) {
					// 优先发送内存中的消息，如果内存中无消息，则发送缓存中的消息
					for (Map.Entry<String, Long> e : lastSendTimes.entrySet()) {
						if (curTime - e.getValue() > sendInterval && this.cacheStorage.len(e.getKey()) > 0) {
							List<PSData> items = this.cacheStorage.pops(e.getKey(), batchSize);
							if(items == null || items.size() == 0) {
								continue;
							}
							
							PSData[] arr = new PSData[items.size()];
							items.toArray(arr);
							
							int size = writeBasket(arr,curTime);
							
							if(size < arr.length) {
								this.cacheStorage.push(e.getKey(), arr,size,arr.length-size);
								cacheItemsCnt.addAndGet(-size);
							}
							
							if(LG.isLoggable(MC.LOG_DEBUG)) {
								String errMsg = "begin get items from cache topic:"+e.getKey();
								LG.log(MC.LOG_DEBUG, this.getClass(), errMsg);
							}
							
							if(openDebug) {
								logger.info("begin get items from cache topic:{}",e.getKey());
							}
						}
					}
				}

				len = basketFactory.size();
				
				if (len == 0) {
					boolean dor = false;
					for(String topic : sendCache.keySet()) {
						List<PSData> ll = sendCache.get(topic);
						if(!ll.isEmpty() && (ll.size() > batchSize || curTime - lastSendTimes.get(topic) > sendInterval)) {
							dor = true;
							break;
						}
					}
					
					if(dor) {
						doSend(curTime);
					} else {
						synchronized (syncLocker) {
							syncLocker.wait(1000);
						}
						// 没有待发送消息，进入下一轮循环
						/*if (openDebug) {
							logger.info("No data to submit:");
						}*/
					}
					continue;
				}
				
				IBasket<PSData> rb = null;
				Iterator<IBasket<PSData>> readIte = this.basketFactory.iterator(true);
				while ((rb = readIte.next()) != null) {
					int rm = rb.remainding();
					if(rm <= 0) {
						continue;
					}
					PSData[] psd = new PSData[rm];
					if(!rb.readAll(psd)) {
						this.basketFactory.returnReadSlot(rb, false);
						
						if(LG.isLoggable(MC.LOG_DEBUG)) {
							String errMsg = "Fail to get element from basket remaiding:"+rb.remainding();
							LG.log(MC.LOG_DEBUG, this.getClass(), errMsg);
						}
						
						if(openDebug) {
							logger.info("Fail to get element from basket remaiding:{}",rb.remainding());
						}
						//消息数量及距上次发送时间间隔都不到，直接路过
						continue;
					} else {
						this.basketFactory.returnReadSlot(rb, true);
					}
					
					String topic = psd[0].getTopic();
					
					List<PSData> ll = this.sendCache.get(topic);
					if(ll == null) {
						this.sendCache.put(topic, (ll = new ArrayList<PSData>()));
					}
					ll.addAll(Arrays.asList(psd));
				}
				
				if(!sendCache.isEmpty()) {
					doSend(curTime);
				}

			} catch (Throwable e) {
				// 永不结束线程
				logger.error("doCheck异常", e);
				LG.log(MC.LOG_ERROR, this.getClass(), "",e);
				//SF.doBussinessLog(MonitorConstant.LOG_ERROR, PubSubServer.class, e, "doCheck异常");
			}
		}
		
	}
	
	private void doSend(long curTime/*,ServiceCounter sc*/) {
		
		int sendSize = 0;
		
		//boolean me = this.statusMonitorAdapter.monitoralbe;
		
		for(String topic : sendCache.keySet()) {
			
			long lastSendTime = lastSendTimes.get(topic);
			
			List<PSData> ll = sendCache.get(topic);
			
			if(ll.isEmpty() || ll.size() < batchSize && curTime - lastSendTime < sendInterval) {
				if(openDebug) {
					//logger.info("size :{},interval:{}",ll.size(),System.currentTimeMillis() - lastSendTime);
				}//消息数量及距上次发送时间间隔都不到，直接路过
				continue;
			}

			//更新发送时间
			lastSendTimes.put(topic, curTime);
			
			int size = ll.size();
			if (size > batchSize) {
				// 每批次最多能同时发送batchSize个消息
				size = batchSize;
			}

			PSData[] items = new PSData[size];

			int i = 0;
			for (Iterator<PSData> ite = ll.iterator(); ite.hasNext() && i < size; i++) {
				items[i] = ite.next();
				ite.remove();
			}
			
			sendSize += items.length;
			
			/*if(openDebug) {
				logger.info("Submit {} size: {} " ,topic,sendSize);
			}*/
			
			this.executor.submit(new Worker(items, topic));
			
			/*if(me) {
				sc.add(MC.Ms_SubmitTaskCnt, 1);
			}*/
			
			foreachItem(items,(pd)->{
				this.sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_SubmitTaskCnt, 1);
			});
			
		}
		
		/*if(me) {
			sc.add(MC.Ms_CheckerSubmitItemCnt, sendSize);
		}*/
	}
	

	private class Worker implements Runnable{
		
		private PSData[] items = null;
		
		private Set<ISubscriberCallback> subscribers = null;
		
		private String topic = null;
		
		public Worker(PSData[] items,String topic) {
			this.items = items;
			this.topic = topic;
			this.subscribers =  subManager.getCallback(topic);
		}
		
		@Override
		public void run() {
			try {
				long curTime = TimeUtils.getCurTime();
				
			/*	boolean me = statusMonitorAdapter.monitoralbe;
				ServiceCounter sc = statusMonitorAdapter.getServiceCounter();*/
				
				//logger.info("Dispatch {} size: {} " ,topic,items.length);
				
				if(subscribers == null || subscribers.isEmpty()) {
					//没有对应主题的监听器，直接进入重发队列，此时回调cb==null
					SendItem si = new SendItem(SendItem.TYPY_RESEND, null, items, 0);
					resendManager.queueItem(si);
					
					/*if(me) {
						sc.add(MC.Ms_DoResendWithCbNullCnt, items.length);
					}*/
					
					foreachItem(items,(pd)->{
						sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_DoResendWithCbNullCnt, 1);
					});
					
					String errMsg = "Push to resend component topic:"+topic;
					LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
					logger.error(errMsg);
				} else {
					for (ISubscriberCallback cb : subscribers) {
						try {
							cb.onMessage(items)
							.then((psds,fail,ctx)->{
								if(psds != null && psds.length > 0) {
									if(fail != null) {
										logger.error(fail.toString());
									}

									SendItem si = new SendItem(SendItem.TYPY_RESEND, cb, psds, 0);
									resendManager.queueItem(si);
									/*if(me) {
										sc.add(MC.Ms_DoResendCnt, psds.length);
									}*/
									foreachItem(items,(pd)->{
										sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_DoResendCnt, 1);
									});
									String errMsg = "Push to resend component:"+cb.getSm().getKey().toKey(true, true, true);
									LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
									logger.error(errMsg);
								}
							});
							/*if(me) {
								sc.add(MC.Ms_TaskSuccessItemCnt, items.length);
							}*/
							foreachItem(items,(pd)->{
								sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_TaskSuccessItemCnt, 1);
							});
						} catch (Throwable e) {
							// 进入重发队列
							SendItem si = new SendItem(SendItem.TYPY_RESEND, cb, items, 0);
							resendManager.queueItem(si);
							/*if(me) {
								sc.add(MC.Ms_DoResendCnt, items.length);
							}*/
							foreachItem(items,(pd)->{
								sta.getSc(pd.getTopic(),pd.getSrcClientId()).add(MC.Ms_DoResendCnt, 1);
							});
							String errMsg = "Worker get exception";
							LG.log(MC.LOG_ERROR, this.getClass(), errMsg,e);
							logger.error(errMsg, e);
							//SF.doBussinessLog(MonitorConstant.LOG_ERROR, PubSubServer.class, e, "Subscribe mybe down: "+cb.getSm().getKey().toKey(true, true, true));
						}
					}
				}
			} finally {
				 Thread.currentThread().setContextClassLoader(null);
				/*if(openDebug) {
					//logger.info("Do decrement memoryItemsCnt cur:"+memoryItemsCnt.get()+", before:"+size);
				}*/
			}
		}
	}

	 private class PubsubServerAbortPolicy implements RejectedExecutionHandler {

	        public PubsubServerAbortPolicy() { }
	
	        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
	        	String errMsg = "JMcro Pubsub Server Task " + r.toString() +" rejected from " +e.toString();
	        	LG.log(MC.LOG_ERROR, getClass(), errMsg);
	            throw new RejectedExecutionException(errMsg);
	        }
	    }
	 
}
