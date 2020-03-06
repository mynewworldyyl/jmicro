package org.jmicro.api.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:50
 */
@Component(value="pubSubManager")
public class PubSubManager {
	
	//生产者成功将消息放入消息队列,但并不意味着消息被消费者成功消费
	public static final int PUB_OK = 0;
	//无消息服务可用,需要启动消息服务
	public static final int PUB_SERVER_NOT_AVAILABALE = -1;
	//消息队列已经满了,客户端可以重发,或等待一会再重发
	public static final int PUB_SERVER_DISCARD = -2;
	//消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
	public static final int PUB_SERVER_BUSSUY = -3;
	
	public static final int PUB_TOPIC_NOT_VALID= -4;

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private ComponentIdServer idGenerator;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",required=false)
	private IInternalSubRpc defaultServer;
	
	private ExecutorService executor = null;
	
	/**
	 * is enable pubsub feature
	 */
	@Cfg(value="/PubSubManager/enable",defGlobal=false)
	private boolean enable = true;
	
	@Cfg(value="/PubSubManager/openDebug",defGlobal=false)
	private boolean openDebug = true;
	
	@Cfg(value="/PubSubManager/maxPsItem",defGlobal=false)
	private int maxPsItem = 10000;
	
	@Cfg(value="/PubSubManager/maxSentItems",defGlobal=false)
	private int maxSentItems = 50;
	
	@Inject
	private IDataOperator dataOp;
	
	private Map<String,List<PSData>> topicSubmitItems = new HashMap<>();
	
	private Map<String,Long> topicLastSubmitTime = new HashMap<>();
	
	private Object locker = new Object();
	
	private AtomicLong curItemCount = new AtomicLong(0);
	
	private Boolean isRunning = false;
	
	public void init() {

		logger.info("Init object :" +this.hashCode());
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("SubmitItemHolderManager");
		executor = ExecutorFactory.createExecutor(config);
		
	}
	
	public boolean isPubsubEnable(int itemNum) {
		return this.defaultServer != null || this.curItemCount.get()+itemNum <= this.maxPsItem;
	}
	
	public int publish(String topic,byte flag,Object[] args) {

		if(!this.isPubsubEnable(1)) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(args);
		item.setContext(null);
		item.setFlag(flag);
		return publish(item);
	}
	
	
	public int publish(Map<String,Object> context, String topic, String content,byte flag) {
		if(!this.isPubsubEnable(1)) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		return publish(item);
		
	}
	
	public int publish(Map<String,Object> context,String topic, byte[] content,byte flag) {
		if(!this.isPubsubEnable(1)) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		
		return publish(item);
	}
	
	public int publish(PSData[] items) {
		
		if(!this.isPubsubEnable(1)) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		 
		 if(!this.isRunning) {
			 synchronized(isRunning) {
				 if(!isRunning) {
					 this.isRunning = true;
					 new Thread(this::doWork).start();
				 }
			 }
		 }
		 
		curItemCount.addAndGet(items.length);
		 
		for(PSData d :items) {
			List<PSData> is = topicSubmitItems.get(d.getTopic());
			if(is == null) {
				synchronized(topicSubmitItems) {
					topicSubmitItems.put(d.getTopic(), is=new ArrayList<>());
					topicLastSubmitTime.put(d.getTopic(), System.currentTimeMillis());
				}
			}
			synchronized(is) {
				is.add(d);
			}
		}
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		return PUB_OK;
	}

	public int publish(PSData item) {
		
		if(!this.isPubsubEnable(1)) {
			 this.isRunning = true;
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		if(!this.isRunning) {
			 synchronized(isRunning) {
				 if(!isRunning) {
					 this.isRunning = true;
					 new Thread(this::doWork).start();
				 }
			 }
		 }
		
		curItemCount.incrementAndGet();
		
		List<PSData> items = topicSubmitItems.get(item.getTopic());
		if(items == null) {
			synchronized(topicSubmitItems) {
				topicSubmitItems.put(item.getTopic(), items=new ArrayList<>());
				topicLastSubmitTime.put(item.getTopic(), System.currentTimeMillis());
			}
		}
		
		synchronized(items) {
			items.add(item);
		}
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		return PUB_OK;
	}
	
    private void doWork() {
		logger.info("START submit worker");
    	int interval = 900;
    	int batchSize = 5;
		while(isRunning) {
			try {
				
				while(curItemCount.get() == 0) {
					//没有数据，等待
					synchronized (locker) {
						locker.wait(interval);
					}
				}
				
				long curTime = System.currentTimeMillis();
				
				Map<String,List<PSData>> ms = new HashMap<>();
				
				int cnt = 0;
				
				for(Map.Entry<String, List<PSData>> e : topicSubmitItems.entrySet()) {
					if(e.getValue().isEmpty()) {
						continue;
					}
					
					int subCnt = e.getValue().size();
					
					if(subCnt < batchSize) {
						Long lastTime = topicLastSubmitTime.get(e.getKey());
						if(curTime - lastTime < interval) {
							//需要提交的数据量小于50且距上次提交时间小于100毫秒，暂时不提交此批数据
							continue;
						}
					}
					
					topicLastSubmitTime.put(e.getKey(), System.currentTimeMillis());
					
					List<PSData> sl = ms.get(e.getKey());
					if(!ms.containsKey(e.getKey())) {
						ms.put(e.getKey(), sl = new ArrayList<>());
					}
					
					List<PSData> l = e.getValue();
					synchronized(l) {
						
						if(subCnt > batchSize) {
							//每个主题第次最大提交50个数量
							subCnt = batchSize;
						}
						
						for(Iterator<PSData> ite = l.iterator(); subCnt > 0 && ite.hasNext(); subCnt--) {
							PSData psd = ite.next();
							if (psd != null) {
								cnt++;
								sl.add(psd);
								ite.remove();
							}
						}
					}
				}
				
				if(cnt > 0) {
					executor.submit(new Worker(ms));
				}
				
			} catch (Throwable e) {
				logger.error("",e);
			}
		}
    }
	
	private class Worker implements Runnable{
		
		private Map<String,List<PSData>> ms = null;
		
		public Worker(Map<String,List<PSData>> ms) {
			this.ms = ms;
		}
		
		@Override
		public void run() {
			
			//不需要监控
			JMicroContext.get().configMonitor(0, 0);
			//发送消息RPC
			JMicroContext.get().setBoolean(Constants.FROM_PUBSUB, true);
				
			for (Map.Entry<String, List<PSData>> e : ms.entrySet()) {
				try {
					List<PSData> l = e.getValue();
					if (l == null || l.isEmpty()) {
						continue;
					}

					int size = l.size();
					int result = 0;

					if (size == 1) {
						PSData psd = l.get(0);
						if (psd.getId() <= 0) {
							// 为消息生成唯一ID
							// 大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
							psd.setId(idGenerator.getIntId(PSData.class));
						}
						result = defaultServer.publishItem(psd);
					} else if (size > 1) {
						Long[] ids = idGenerator.getLongIds(PSData.class.getName(), l.size());

						PSData[] pd = new PSData[l.size()];
						l.toArray(pd);

						for (int i = 0; i < pd.length; i++) {
							if (pd[i] != null && pd[i].getId() <= 0) {
								// 为消息生成唯一ID
								// 大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
								pd[i].setId(ids[i]);
							}
						}
						result = defaultServer.publishItems(e.getKey(), pd);
					}
					
					curItemCount.addAndGet(-size);

					if (PubSubManager.PUB_SERVER_BUSSUY == result) {
						logger.warn("Got bussy result and sleep one seconds");
						for(PSData d : l) {
							if(d.getFailCnt() < 3) {
								//重发3次
								d.setFailCnt(d.getFailCnt()+1);
								Thread.sleep(1000);
								if((result = publish(d)) != 0) {
									if(d.getLocalCallback() != null) {
										d.getLocalCallback().callback(PubSubManager.PUB_SERVER_BUSSUY, d.getId(), d.getContext());
									}
								}
							} else {
								if(d.getLocalCallback() != null) {
									d.getLocalCallback().callback(PubSubManager.PUB_SERVER_BUSSUY, d.getId(), d.getContext());
								}
							}
						}
					} else if (PubSubManager.PUB_SERVER_NOT_AVAILABALE == result
							|| PubSubManager.PUB_SERVER_DISCARD == result
							|| PubSubManager.PUB_TOPIC_NOT_VALID == result) {
						for(PSData d : l) {
							if(d.getLocalCallback() != null) {
								d.getLocalCallback().callback(result, d.getId(), d.getContext());
							}
						}
					}
				
				} catch (Throwable ex) {
					logger.error("", ex);
				}

			}
		}
	}
	
}
