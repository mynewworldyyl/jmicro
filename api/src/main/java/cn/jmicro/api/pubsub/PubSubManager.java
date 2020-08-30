package cn.jmicro.api.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.pubsub.genclient.IInternalSubRpc$JMAsyncClient;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

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
	public static final int PUB_SERVER_BUSUY = -3;
	
	public static final int PUB_TOPIC_INVALID= -4;

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ServiceInvokeManager siManager;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",required=false)
	private IInternalSubRpc$JMAsyncClient defaultServer;
	
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
	
	@Inject
	private ExecutorFactory ef;
	
	private Map<String,List<PSData>> topicSubmitItems = new HashMap<>();
	
	private Map<String,Long> topicLastSubmitTime = new HashMap<>();
	
	private Object locker = new Object();
	
	private AtomicLong curItemCount = new AtomicLong(0);
	
	private Object runLocker = new Object();
	private Boolean isRunning = false;
	
	public void ready() {
		logger.info("Init object :" +this.hashCode());
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("PubSubManager");
		executor = ef.createExecutor(config);
	}
	
	public boolean hasTopic(String topic) {
		return defaultServer.hasTopic(topic);
	}
	
	
	
	public boolean isPubsubEnable(int itemNum) {
		if(itemNum == 0) {
			return this.defaultServer != null;
		}else {
			return this.defaultServer != null && this.curItemCount.get() + itemNum <= this.maxPsItem 
					&& ProxyObject.isUsableRemoteProxy(this.defaultServer);
		}
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
			 startChecker();
		 }
		 
		curItemCount.addAndGet(items.length);
		 
		ActInfo ai = JMicroContext.get().getAccount();
		synchronized (topicSubmitItems) {
			for (PSData d : items) {
				if(ai != null) {
					d.setSrcClientId(ai.getClientId());
				}
				List<PSData> is = topicSubmitItems.get(d.getTopic());
				if (is == null) {
					topicSubmitItems.put(d.getTopic(), is = new ArrayList<>());
					topicLastSubmitTime.put(d.getTopic(), System.currentTimeMillis());
				}
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
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		if(!this.isRunning) {
			 startChecker();
		}
		
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai != null) {
			item.setSrcClientId(ai.getClientId());
		}
		
		curItemCount.incrementAndGet();
		
		List<PSData> items = topicSubmitItems.get(item.getTopic());
		
		synchronized (topicSubmitItems) {
			if (items == null) {
				topicSubmitItems.put(item.getTopic(), items = new ArrayList<>());
				topicLastSubmitTime.put(item.getTopic(), System.currentTimeMillis());
			}
			items.add(item);
		}
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		return PUB_OK;
	}
	
	private void startChecker() {
		 synchronized(this.runLocker) {
			 if(this.isRunning) {
				 return;
			 }
			 this.isRunning = true;
			 Thread t = new Thread(this::doWork);
			 t.setName(Config.getInstanceName()+"_PubSubManager_Checker");
			 t.start();
		 }
	}
	
    private void doWork() {
		logger.info("START submit worker");
    	int interval = 500;
    	int batchSize = 50;
    	//long lastLoopTime = System.currentTimeMillis();
		while(isRunning) {
			try {
				
				/*if(System.currentTimeMillis() - lastLoopTime < 100) {
					System.out.println("PubSubManager.doWork On loop");
				}
				lastLoopTime = System.currentTimeMillis();
				*/
				
				while(curItemCount.get() == 0) {
					//没有数据，等待
					synchronized (locker) {
						locker.wait(interval);
					}
				}
				
				long curTime = System.currentTimeMillis();
				
				Map<String,List<PSData>> ms = new HashMap<>();
				
				int cnt = 0;
				
				synchronized(topicSubmitItems) {
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
			//JMicroContext.get().configMonitor(0, 0);
			//发送消息RPC
			JMicroContext.get().setBoolean(Constants.FROM_PUBSUB, true);
				
			for (Map.Entry<String, List<PSData>> e : ms.entrySet()) {
				try {
					List<PSData> l = e.getValue();
					if (l == null || l.isEmpty()) {
						continue;
					}

					int size = l.size();
					//int result = 0;

					if (size == 1) {
						PSData psd = l.get(0);
						if (psd.getId() <= 0) {
							// 为消息生成唯一ID
							// 大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
							psd.setId(idGenerator.getIntId(PSData.class));
						}
						defaultServer.publishItemJMAsync(null,psd).then(new AsyncCallback(l));
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
						
						defaultServer.publishItemsJMAsync(null,e.getKey(), pd).then(new AsyncCallback(l));
					}
					
				} catch (Throwable ex) {
					logger.error("", ex);
				}

			}
		}
	}
	
	private class AsyncCallback implements IAsyncCallback<Integer> {

		private List<PSData> list;
		
		private AsyncCallback(List<PSData> l) {
			this.list = l;
		}
		
		public void onResult(Integer result, AsyncFailResult fail,Map<String,Object> context) {

			curItemCount.addAndGet(-list.size());
			
			logger.info("Got result: {}",result);
			
			if (PUB_SERVER_BUSUY == result) {
				logger.warn("Got bussy result and sleep one seconds");
				for(PSData d : list) {
					if(d.getFailCnt() < 3) {
						//重发3次
						d.setFailCnt(d.getFailCnt()+1);
						if((result = publish(d)) != PUB_OK) {
							if(d.getCallback() != null && Message.is(d.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
								//消息通知
								//siManager.call(d.getCallback(), new Object[] {PUB_SERVER_BUSUY, d.getId(), d.getContext()});
								siManager.callAsync(d.getCallback(), new Object[] {PUB_SERVER_BUSUY, d.getId(), d.getContext()})
								.then((rst,f,cxt)->{
									if(f != null) {
										logger.error(f.toString());
									}
								});
							} else {
								logger.error("Pubsub Server is busuy so disgard msg:" + JsonUtils.getIns().toJson(d));
							}
						}
					} else {
						if(d.getLocalCallback() != null) {
							d.getLocalCallback().call(result, d);
						}else if(d.getCallback() != null && Message.is(d.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
							//消息通知
							//siManager.call(d.getCallback(), new Object[] {PUB_SERVER_BUSUY, d.getId(), d.getContext()});
							siManager.callAsync(d.getCallback(), new Object[] {PUB_SERVER_BUSUY, d.getId(), d.getContext()})
							.then((rst,f,cxt)->{
								if(f != null) {
									logger.error(f.toString());
								}
							});
						} else {
							logger.error("Pubsub Server is busuy and retry failure :" + JsonUtils.getIns().toJson(d));
						}
					}
				}
			} else if (PubSubManager.PUB_SERVER_NOT_AVAILABALE == result
					|| PubSubManager.PUB_SERVER_DISCARD == result
					|| PubSubManager.PUB_TOPIC_INVALID == result) {
				for(PSData d : list) {
					if(d.getLocalCallback() != null) {
						d.getLocalCallback().call(result, d);
					}else if(d.getCallback() != null && Message.is(d.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
						//消息通知
						//siManager.call(d.getCallback(), new Object[] {result, d.getId(), d.getContext()});
						siManager.callAsync(d.getCallback(), new Object[] {result, d.getId(), d.getContext()})
						.then((rst,f,cxt)->{
							if(f != null) {
								logger.error(f.toString());
							}
						});
						
					} else {
						logger.error("Publish message failure with code:"+result +" ,topic:"+d.getTopic() +" , message: " + JsonUtils.getIns().toJson(d));
					}
				}
			}
		}
	}
	
}
