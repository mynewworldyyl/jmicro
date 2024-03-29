package cn.jmicro.api.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfigJRso;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.pubsub.IInternalSubRpcJMSrv;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:50
 */
@Component(value="pubSubManager")
public class PubSubManager {
	
	public static final String PROFILE_PUBSUB = "pubsub";
	
	public static final String TABLE_PUBSUB_ITEMS = "t_pubsub_items";
	
	//生产者成功将消息放入消息队列,但并不意味着消息被消费者成功消费
	public static final int PUB_OK = PSDataJRso.PUB_OK;
	//无消息服务可用,需要启动消息服务
	public static final int PUB_SERVER_NOT_AVAILABALE = PSDataJRso.PUB_SERVER_NOT_AVAILABALE;
	//消息队列已经满了,客户端可以重发,或等待一会再重发
	public static final int PUB_SERVER_DISCARD = PSDataJRso.PUB_SERVER_DISCARD;
	//消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
	public static final int PUB_SERVER_BUSUY = PSDataJRso.PUB_SERVER_BUSUY;
	
	public static final int PUB_TOPIC_INVALID= PSDataJRso.PUB_TOPIC_INVALID;

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ServiceInvokeManager siManager;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace="*",version="0.0.1",required=false)
	private IInternalSubRpcJMSrv defaultServer;
	
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
	
	@Inject
	private ProfileManager pm;
	
	@Inject(required=false)
	private IObjectStorage objStorage;
	
	private Map<String,List<PSDataJRso>> topicSubmitItems = new HashMap<>();
	
	private Map<String,Long> topicLastSubmitTime = new HashMap<>();
	
	private Object locker = new Object();
	
	private AtomicLong curItemCount = new AtomicLong(0);
	
	private Object runLocker = new Object();
	private Boolean isRunning = false;
	
	public void jready() {
		//logger.info("Init object :" +this.hashCode());
		ExecutorConfigJRso config = new ExecutorConfigJRso();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("PubSubManager");
		executor = ef.createExecutor(config);
	}
	
	public boolean hasTopic(String topic) {
		return defaultServer.hasTopic(topic).getResult().getData();
	}
	
	public boolean isPubsubEnable(int itemNum) {
		if(itemNum == 0) {
			return this.defaultServer != null;
		}else {
			 return this.defaultServer != null && this.curItemCount.get() + itemNum <= this.maxPsItem 
					&& ProxyObject.isUsableRemoteProxy(this.defaultServer);
		}
	}
	
	private void doLog(int itemNum,String msg) {
		logger.info(msg);
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			StringBuffer sb = new StringBuffer(msg + " Pubsub disable by: ");
			if(this.defaultServer == null) {
				sb.append("Pubsub server is disable!");
			}else if(this.curItemCount.get() + itemNum > this.maxPsItem ) {
				sb.append("cur item ["+this.curItemCount.get()+"] send count ["+ itemNum + "] is too max with " + this.maxPsItem);
			}else {
				sb.append("pubsub server not ready now!");
			}
			LG.log(MC.LOG_DEBUG, this.getClass(), sb.toString());
		}
	}
	
	public int publish(String topic, Object[] args, byte flag, Map<String, Object> itemContext) {

		if(!this.isPubsubEnable(1)) {
			doLog(1,"return code: " + PUB_SERVER_NOT_AVAILABALE+" for topic: " + topic);
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSDataJRso item = new PSDataJRso();
		item.setTopic(topic);
		item.setData(args);
		item.setContext(itemContext);
		item.setFlag(flag);
		return publish(item);
	}
	
	
	public int publish(String topic, String content,byte flag,Map<String,Object> context) {
		if(!this.isPubsubEnable(1)) {
			doLog(1,"return code: " + PUB_SERVER_NOT_AVAILABALE+" for topic: " + topic);
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSDataJRso item = new PSDataJRso();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		return publish(item);
	}
	
	public int publish(String topic, byte[] content,byte flag,Map<String,Object> context) {
		if(!this.isPubsubEnable(1)) {
			doLog(1,"return code: " + PUB_SERVER_NOT_AVAILABALE+" for topic: " + topic);
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSDataJRso item = new PSDataJRso();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		
		return publish(item);
	}
	
	public int publish(PSDataJRso[] items) {
		
		if(items == null || items.length == 0) {
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), "send null items");
			}
			return PSDataJRso.PUB_ITEM_IS_NULL;
		}
		
		 if(!this.isPubsubEnable(1)) {
			doLog(1,"return code: " + PUB_SERVER_NOT_AVAILABALE);
			return PUB_SERVER_NOT_AVAILABALE;
		 }
		 
		 if(!this.isRunning) {
			 startChecker();
		 }
		 
		curItemCount.addAndGet(items.length);
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		ActInfoJRso sai = JMicroContext.get().getSysAccount();
		for(PSDataJRso item : items) {
			if(item.getFr() == 0) {
				//设置消息来源账号ID
				if(ai != null) {
					item.setFr(ai.getId());
					item.setSrcClientId(ai.getClientId());
				}else {
					 if(sai != null) {
						 item.setFr(sai.getId());
						 item.setSrcClientId(sai.getClientId());
					 } else {
						 item.setFr(Config.getClientId());
						 item.setSrcClientId(Config.getClientId());
					 }
				}
			}
		}
		
		if(ai != null && pm.getVal(ai.getClientId(), PROFILE_PUBSUB, "needPersist",false, Boolean.class)) {
			persist2Db(ai.getClientId(),items);
		}
		
		synchronized (topicSubmitItems) {
			for (PSDataJRso d : items) {
				List<PSDataJRso> is = topicSubmitItems.get(d.getTopic());
				if (is == null) {
					topicSubmitItems.put(d.getTopic(), is = new ArrayList<>());
					topicLastSubmitTime.put(d.getTopic(), TimeUtils.getCurTime());
				}
				is.add(d);
			}
		}
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		return PUB_OK;
	}

	public int publish(PSDataJRso item) {
		
		if(item == null) {
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(),"return PUB_ITEM_IS_NULL=" + PSDataJRso.PUB_ITEM_IS_NULL);
			}
			return PSDataJRso.PUB_ITEM_IS_NULL;
		}
		
		if(StringUtils.isEmpty(item.getTopic())) {
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(),"return PUB_TOPIC_IS_NULL=" + PSDataJRso.PUB_TOPIC_IS_NULL);
			}
			return PSDataJRso.PUB_TOPIC_IS_NULL;
		}
		
		if(!this.isPubsubEnable(1)) {
			doLog(1,"return code: " + PUB_SERVER_NOT_AVAILABALE);
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		if(!this.isRunning) {
			 startChecker();
		}
		
		if(item.getFr() == 0) {
			//设置消息来源账号ID
			ActInfoJRso ai = JMicroContext.get().getAccount();
			if(ai != null) {
				item.setFr(ai.getId());
				item.setSrcClientId(ai.getClientId());
			} else {
				 ai = JMicroContext.get().getSysAccount();
				 if(ai != null) {
					 item.setFr(ai.getId());
					 item.setSrcClientId(ai.getClientId());
				 } else {
					 item.setFr(Config.getClientId());
					 item.setSrcClientId(Config.getClientId());
				 }
			}
		}
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(ai != null && item.isPersist()) {
			if(pm.getVal(item.getSrcClientId(), PROFILE_PUBSUB, "needPersist",false, Boolean.class)) {
				persit2Db(ai.getClientId(),item);
			}
		}
		
		curItemCount.incrementAndGet();
		
		List<PSDataJRso> items = topicSubmitItems.get(item.getTopic());
		
		synchronized (topicSubmitItems) {
			if (items == null) {
				topicSubmitItems.put(item.getTopic(), items = new ArrayList<>());
				topicLastSubmitTime.put(item.getTopic(), TimeUtils.getCurTime());
			}
			items.add(item);
		}
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		return PUB_OK;
	}
	
	public void persit2Db(int clientId,PSDataJRso item) {
		if(!item.isPersist()) {
			return;
		}
		
		if(item.getId() <= 0) {
			item.setId(idGenerator.getIntId(PSDataJRso.class));
		}
		item.setSrcClientId(clientId);
		item.setPersist(true);
		
		if(objStorage != null) {
			objStorage.save(TABLE_PUBSUB_ITEMS, item,PSDataJRso.class,true);
		}
	}
	
	public void persist2Db(int clientId, PSDataJRso[] items) {
		
		Set<PSDataJRso> set = null;
		
		for(PSDataJRso d : items) {
			if(!d.isPersist()) {
				continue;
			}
			
			if(d.getId() <= 0) {
				d.setId(idGenerator.getIntId(PSDataJRso.class));
			}
			d.setSrcClientId(clientId);
			d.setPersist(true);
			
			if(objStorage != null) {
				if(set == null) {
					set = new HashSet<>();
				}
				set.add(d);
			}
		}
		
		if(objStorage != null && !set.isEmpty()) {
			PSDataJRso[] pds = new PSDataJRso[set.size()];
			set.toArray(pds);
			objStorage.save(TABLE_PUBSUB_ITEMS, pds,PSDataJRso.class,true);
		}
	}

	private void startChecker() {
		 synchronized(this.runLocker) {
			 if(this.isRunning) {
				 return;
			 }
			 
			 if(LG.isLoggable(MC.LOG_INFO)) {
				LG.log(MC.LOG_INFO, this.getClass(),"Rerun checker thread: "+Config.getInstanceName()+"_PubSubManager_Checker");
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
				
				long curTime = TimeUtils.getCurTime();
				
				Map<String,List<PSDataJRso>> ms = new HashMap<>();
				
				int cnt = 0;
				
				synchronized(topicSubmitItems) {
					for(Map.Entry<String, List<PSDataJRso>> e : topicSubmitItems.entrySet()) {
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
						
						topicLastSubmitTime.put(e.getKey(), TimeUtils.getCurTime());
						
						List<PSDataJRso> sl = ms.get(e.getKey());
						if(!ms.containsKey(e.getKey())) {
							ms.put(e.getKey(), sl = new ArrayList<>());
						}
						
						List<PSDataJRso> l = e.getValue();
						
						if(subCnt > batchSize) {
							//每个主题第次最大提交50个数量
							subCnt = batchSize;
						}
						
						for(Iterator<PSDataJRso> ite = l.iterator(); subCnt > 0 && ite.hasNext(); subCnt--) {
							PSDataJRso psd = ite.next();
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
		
		private Map<String,List<PSDataJRso>> ms = null;
		
		public Worker(Map<String,List<PSDataJRso>> ms) {
			this.ms = ms;
		}
		
		@Override
		public void run() {
			
			//不需要监控
			//JMicroContext.get().configMonitor(0, 0);
			//发送消息RPC
			JMicroContext.get().setBoolean(Constants.FROM_PUBSUB, true);
				
			for (Map.Entry<String, List<PSDataJRso>> e : ms.entrySet()) {
				try {
					final List<PSDataJRso> l = e.getValue();
					if (l == null || l.isEmpty()) {
						continue;
					}

					int size = l.size();
					//int result = 0;

					if (size == 1) {
						PSDataJRso psd = l.get(0);
						if (psd.getId() <= 0) {
							// 为消息生成唯一ID
							// 大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
							psd.setId(idGenerator.getIntId(PSDataJRso.class));
						}
						defaultServer.publishItem(psd)
						.then((rst,fail,cxt)->{
							onResult(l, rst.getData(), fail);
						});
					} else if (size > 1) {
						Long[] ids = idGenerator.getLongIds(PSDataJRso.class.getName(), l.size());

						PSDataJRso[] pd = new PSDataJRso[l.size()];
						l.toArray(pd);

						for (int i = 0; i < pd.length; i++) {
							if (pd[i] != null && pd[i].getId() <= 0) {
								// 为消息生成唯一ID
								// 大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
								pd[i].setId(ids[i]);
							}
						}
						defaultServer.publishItems(e.getKey(), pd)
						.then((rst,fail,cxt)->{
							onResult(l, rst.getData(), fail);
						});
					}
					
				} catch (Throwable ex) {
					logger.error("", ex);
				}

			}
		}
	}
	
	public void onResult(List<PSDataJRso> list,Integer result, AsyncFailResult fail) {

		curItemCount.addAndGet(-list.size());
		
		//logger.info("Got result: {}",result);
		
		if (PUB_SERVER_BUSUY == result) {
			logger.warn("Got bussy result and sleep one seconds");
			for(PSDataJRso d : list) {
				if(d.getFailCnt() < 3) {
					//重发3次
					d.setFailCnt(d.getFailCnt()+1);
					if((result = publish(d)) != PUB_OK) {
						doCallback(d,result);
						if(objStorage != null ) {
							objStorage.updateOrSaveById(TABLE_PUBSUB_ITEMS,d,PSDataJRso.class,IObjectStorage._ID,true);
						}
					}
				} else {
					
					if(objStorage != null ) {
						objStorage.updateOrSaveById(TABLE_PUBSUB_ITEMS,d,PSDataJRso.class,IObjectStorage.ID,true);
					}
					
					if(d.getLocalCallback() != null) {
						d.getLocalCallback().call(result, d);
					}else if(StringUtils.isNotEmpty(d.getCallback())) {
						//消息通知
						doCallback(d,result);
					} else {
						logger.error("Pubsub Server is busuy and retry failure :" + JsonUtils.getIns().toJson(d));
					}
				}
			}
		} else if (PubSubManager.PUB_SERVER_NOT_AVAILABALE == result
				|| PubSubManager.PUB_SERVER_DISCARD == result
				|| PubSubManager.PUB_TOPIC_INVALID == result) {
			for(PSDataJRso d : list) {
				
				if(objStorage != null ) {
					objStorage.updateOrSaveById(TABLE_PUBSUB_ITEMS,d,PSDataJRso.class,IObjectStorage._ID,true);
				}
				
				if(d.getLocalCallback() != null) {
					//本地回调
					d.getLocalCallback().call(result, d);
				}else if(StringUtils.isNotEmpty(d.getCallback())) {
					doCallback(d,result);
				} else {
					logger.error("Publish message failure with code:"+result +" ,topic:"+d.getTopic() +" , message: " + JsonUtils.getIns().toJson(d));
				}
			}
		}
	}
	
	public void doCallback(PSDataJRso d,int cbRst) {

		if(StringUtils.isNotEmpty(d.getCallback())) {
			if(Message.is(d.getFlag(), PSDataJRso.FLAG_CALLBACK_METHOD)) {
				siManager.call(d.getCallback(), new Object[] {cbRst, d.getId(), d.getContext()})
				.then((rst,f,cxt)->{
					if(f != null) {
						logger.error(f.toString());
					}
				});
			} else if(Message.is(d.getFlag(), PSDataJRso.FLAG_CALLBACK_TOPIC)) {
				if(cbRst != PUB_SERVER_BUSUY &&  cbRst != PUB_SERVER_NOT_AVAILABALE && cbRst != PUB_SERVER_DISCARD) {
					this.publish(d.getCallback(), new Object[] {cbRst, d.getId()}, PSDataJRso.FLAG_DEFALUT,  d.getContext());
				}else {
					logger.error("Pubsub Server is disable now:" + JsonUtils.getIns().toJson(d));
				}
			}
		} else {
			logger.error("Pubsub Server is disable now:" + JsonUtils.getIns().toJson(d));
		}
		
	}
	
}
