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

import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.common.util.JsonUtils;

class ResendManager {

	private static final String RESEND_TIMER = "PubsubServerResendTimer";
	
	private final static Logger logger = LoggerFactory.getLogger(ResendManager.class);
	
	private ItemStorage<SendItem> resendStorage;
	
	private ItemStorage<SendItem> failStorage;
	
	private boolean openDebug = false;
	
	private IObjectFactory of;
	
	private Map<Long,TimerTicker> resendTimers = new ConcurrentHashMap<>();
	
	private Map<String,List<SendItem>> sendItems = new HashMap<>();
	
	private int maxFailItemCount = 100000;
	
	private long doResendInterval = 1000;
	
	private SubcriberManager subManager;
	
	private ExecutorService executor = null;
	
	
	ResendManager(IObjectFactory of,boolean openDebug,int maxFailItemCount,long doResendInterval) {
		this.openDebug = openDebug;
		this.of = of;
		this.resendStorage = new ItemStorage<SendItem>(of,"/pubsubResend/");
		this.failStorage = new ItemStorage<SendItem>(of,"/failItem/");
		this.maxFailItemCount = maxFailItemCount;
		this.doResendInterval = doResendInterval;
		
		if(this.maxFailItemCount <=0) {
			logger.warn("Invalid maxFailItemCount: {}, set to default:{}",this.maxFailItemCount,10000);
			this.maxFailItemCount = 10000;
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(5000);
		config.setThreadNamePrefix("ResendManager");
		//config.setRejectedExecutionHandler(new PubsubServerAbortPolicy());
		executor = ExecutorFactory.createExecutor(config);
		
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
					
					List<SendItem> ll = resendStorage.pops(k, l);
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
		
		for(Map.Entry<String,List<SendItem>> e : sendItems.entrySet()) {
			
			if(e.getValue().isEmpty()) {
				continue;
			}
			
			List<SendItem> ll = e.getValue();
			
			int size = ll.size();
			if(size > batchSize) {
				//每批次最多能同时发送batchSize个消息
				size = batchSize;
			}
			
			synchronized(ll) {
				int i = 0;
				for(Iterator<SendItem> ite = ll.iterator(); ite.hasNext() && i < batchSize; i++) {
					SendItem si = ite.next();
					this.executor.submit(new Worker(si));
					ite.remove();
				}
			}
			
			
		}
		
	}
	
	private class Worker implements Runnable{
		
		private SendItem item = null;
		
		private Set<ISubCallback> callbacks = null;
		
		private ISubCallback callback = null;
		
		public Worker(SendItem item) {
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
						PSData[] psds = callback.onMessage(item.items);
						 if(psds != null && psds.length > 0) {
							 item.items = psds;
							 queueItem(item);
						 }
					}else {
						if(this.callbacks.isEmpty()) {
							 queueItem(item);
						} else {
							for(ISubCallback c : this.callbacks) {
								PSData[] psds = c.onMessage(item.items);
								 if(psds != null && psds.length > 0) {
									 SendItem si = new SendItem(SendItem.TYPY_RESEND, c, psds, item.retryCnt);
									 queueItem(si);
								 }
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
	
	void queueItem(SendItem item) {
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
				SF.doBussinessLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR,PubSubServer.class,null, 
						"缓存消息量已经达上限："+JsonUtils.getIns().toJson(item));
			}
		}
	}

	public void setSubManager(SubcriberManager subManager) {
		this.subManager = subManager;
	}

	
}
