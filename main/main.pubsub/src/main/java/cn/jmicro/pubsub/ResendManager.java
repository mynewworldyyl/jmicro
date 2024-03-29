package cn.jmicro.pubsub;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfigJRso;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.common.util.JsonUtils;

class ResendManager {

	private static final String RESEND_TIMER = "PubsubServerResendTimer";
	
	private final static Logger logger = LoggerFactory.getLogger(ResendManager.class);
	
	private ItemStorage<SendItemJRso> resendStorage;
	
	private ItemStorage<SendItemJRso> failStorage;
	
	private boolean openDebug = false;
	
	private IObjectFactory of;
	
	private Map<Long,TimerTicker> resendTimers = new ConcurrentHashMap<>();
	
	private Map<String,List<SendItemJRso>> sendItems = new HashMap<>();
	
	private int maxFailItemCount = 100000;
	
	private long doResendInterval = 1000;
	
	private SubscriberManager subManager;
	
	private ExecutorService executor = null;
	
	ResendManager(IObjectFactory of,boolean openDebug,int maxFailItemCount,long doResendInterval) {
		this.openDebug = openDebug;
		this.of = of;
		this.resendStorage = new ItemStorage<SendItemJRso>(of,"/"+Config.getClientId()+"/pubsubResend/");
		this.failStorage = new ItemStorage<SendItemJRso>(of,"/"+Config.getClientId()+"/failItem/");
		this.maxFailItemCount = maxFailItemCount;
		this.doResendInterval = doResendInterval;
		
		if(this.maxFailItemCount <=0) {
			logger.warn("Invalid maxFailItemCount: {}, set to default:{}",this.maxFailItemCount,10000);
			this.maxFailItemCount = 10000;
		}
		
		ExecutorConfigJRso config = new ExecutorConfigJRso();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(5000);
		config.setThreadNamePrefix("ResendManager");
		//config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		
		resetResendTimer();
	}
	
	private void resetResendTimer() {
		
		logger.info("Reset timer with doResendInterval0:{},doResendInterval:{}",doResendInterval,doResendInterval);
		TimerTicker.getTimer(this.resendTimers, doResendInterval).removeListener(RESEND_TIMER,true);
		
		TimerTicker.getTimer(this.resendTimers, doResendInterval).setOpenDebug(openDebug)
		.addListener(RESEND_TIMER, null, (key,att)->{
			try {
				doResend();
			} catch (Throwable e) {
				logger.error("Submit doResend fail: ",e);
			}
		});
		
	}
	
	private void doResend() {
		
		int batchSize = 10;
		int total = 500;
		int curCnt = 0;
		if(sendItems.isEmpty() ) {
			Set<String> keys = this.subManager.topics();
			for(String k : keys) {
				long l = 0;
				if((l = resendStorage.len(k)) > 0) {
					if(l > batchSize) {
						l = batchSize;
					}
					
					List<SendItemJRso> ll = resendStorage.pops(k, l);
					curCnt += ll.size(); 
					sendItems.put(k,ll);
					
					if(curCnt > total) {
						//最多total批重发数据
						break;
					}
				}
			}
		}
		
		if( sendItems.isEmpty() ) {
			return;
		}
		
		/*if(openDebug) {
			logger.debug("doResend submit ones, send size:{}",sendItems.size());
		}*/
		
		for(Map.Entry<String,List<SendItemJRso>> e : sendItems.entrySet()) {
			
			if(e.getValue().isEmpty()) {
				continue;
			}
			
			List<SendItemJRso> ll = e.getValue();
			
			int size = ll.size();
			if(size > batchSize) {
				//每批次最多能同时发送batchSize个消息
				size = batchSize;
			}
			
			synchronized(ll) {
				int i = 0;
				for(Iterator<SendItemJRso> ite = ll.iterator(); ite.hasNext() && i < batchSize; i++) {
					SendItemJRso si = ite.next();
					this.executor.submit(new Worker(si));
					ite.remove();
				}
			}
		}
	}
	
	private class Worker implements Runnable{
		
		private SendItemJRso item = null;
		
		private Set<ISubscriberCallback> callbacks = null;
		
		private ISubscriberCallback callback = null;
		
		public Worker(SendItemJRso item) {
			this.item = item;
			if(item.sm == null) {
				callbacks = subManager.getCallback(item.topic);
			} else {
				callback = subManager.getCallback(item.sm);
			}
		}
		
		@Override
		public void run() {
			try {
				if(callback == null && callbacks == null ) {
					queueItem(item);
				} else {
					if(callback != null) {
						 callback.onMessage(item.items)
						 .then((psds,fail,ctx)->{
							 if(psds != null && psds.length > 0) {
								 item.items = psds;
								 queueItem(item);
							 } else if(fail != null) {
								 logger.error(fail.toString());
							 }
						 });
					} else {
						if(this.callbacks.isEmpty()) {
							 queueItem(item);
						} else {
							for(ISubscriberCallback c : this.callbacks) {
								 c.onMessage(item.items)
								 .then((psds,fail,ctx)->{
									 if(psds != null && psds.length > 0) {
										 SendItemJRso si = new SendItemJRso(SendItemJRso.TYPY_RESEND, c, psds, item.retryCnt);
										 queueItem(si);
									 } else if(fail != null) {
										 logger.error(fail.toString());
									 }
								 });
							}
						}
					}
				}
			} catch (Throwable e) {
				logger.error("",e);
				queueItem(item);
			}
		}
	}
	
	void queueItem(SendItemJRso item) {
		if(item.retryCnt > 2 ) {
			//内存缓存,发送3次失败，
			failStorage.push(item.topic, item);
			logger.error("Fail item:"+JsonUtils.getIns().toJson(item));
			if(item.cb != null) {
				
			}
		} else {
			long l = resendStorage.len(item.topic);
			if(l < this.maxFailItemCount) {
				//做持久化
				resendStorage.push(item.topic,item);
			} else {
				failStorage.push(item.topic, item);
				logger.error("缓存消息量已经达上限："+JsonUtils.getIns().toJson(item));
				//没办法，服务器吃不消了，直接丢弃
				LG.log(MC.LOG_ERROR,PubSubServer.class, 
						"缓存消息量已经达上限："+JsonUtils.getIns().toJson(item));
			}
		}
	}

	public void setSubManager(SubscriberManager subManager) {
		this.subManager = subManager;
	}

	
}
