package org.jmicro.api.pubsub;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

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
	public static final int PUB_OK = Integer.MIN_VALUE;
	//无消息服务可用,需要启动消息服务
	public static final int PUB_SERVER_NOT_AVAILABALE = -1;
	//消息队列已经满了,客户端可以重发,或等待一会再重发
	public static final int PUB_SERVER_DISCARD = -2;
	//消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
	public static final int PUB_SERVER_BUSSUY = -3;

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
	private int maxPsItem = 1000;
	
	@Cfg(value="/PubSubManager/maxSentItems",defGlobal=false)
	private int maxSentItems = 50;
	
	@Inject
	private IDataOperator dataOp;
	
	private Queue<PSData> psItems = new ConcurrentLinkedQueue<>();
	
	private Object locker = new Object();
	
	public void init() {
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("SubmitItemHolderManager");
		executor = ExecutorFactory.createExecutor(config);
		
		new Thread(new Worker()).start();
	}
	
	public boolean isPubsubEnable() {
		return this.defaultServer != null || this.psItems.size() >= this.maxPsItem;
	}
	
	public long publish(String topic,byte flag,Object[] args) {

		if(!this.isPubsubEnable()) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(args);
		item.setContext(null);
		item.setFlag(flag);
		return publish(item);
	}
	
	
	public long publish(Map<String,Object> context, String topic, String content,byte flag) {
		if(!this.isPubsubEnable()) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		return publish(item);
		
	}
	
	public long publish(Map<String,Object> context,String topic, byte[] content,byte flag) {
		if(!this.isPubsubEnable()) {
			return PUB_SERVER_NOT_AVAILABALE;
		}
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		
		return publish(item);
	}

	public long publish(PSData item) {
		this.psItems.offer(item);
		synchronized(locker) {
			locker.notifyAll();
		}
		return PUB_OK;
	}
	
    private long doPublish(PSData item) {
		
		IInternalSubRpc s = this.defaultServer;
		
		if(openDebug) {
			logger.debug("Publish topic: {}, data: {}",item.getTopic(),item.getData());
		}
		if(item.getId() <= 0) {
			//为消息生成唯一ID
			//大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
			item.setId(idGenerator.getIntId(PSData.class));
		}
		return s.publishData(item);
	}
	
	private class Worker implements Runnable{
		
		public Worker() {
		}
		
		@Override
		public void run() {
			
			//不需要监控
			JMicroContext.get().configMonitor(0, 0);
			//发送消息RPC
			JMicroContext.get().setBoolean(Constants.FROM_PUBSUB, true);
			
			while(true) {
				try {
					synchronized(locker) {
						
						if(psItems.isEmpty()){
							locker.wait();
						}
					}
					
					int size = psItems.size();
					
						if(size == 1) {
							PSData psd = null;
							psd = psItems.poll();
							if(psd != null) {
								doPublish(psd);
							}
							
						}else if(size > 1) {
							
							Set<PSData> psds = new HashSet<>();
							
							for(int i = 0; i < size; i++ ) {
								PSData ps = psItems.poll();
								if(ps !=null) {
									psds.add(ps);
								}
							}
						
							
							if(psds.isEmpty()) {
								continue;
							}
							
							Long[] ids = idGenerator.getLongIds(PSData.class.getName(),psds.size());
							
							PSData[] pd =  new PSData[psds.size()];
							psds.toArray(pd);
							
							for(int i = 0; i <pd.length; i++ ) {
								if(pd[i] != null && pd[i].getId() <= 0) {
									//为消息生成唯一ID
									//大于0时表示客户端已经预设置值,给客户端一些选择，比如业务需要提前知道消息ID做关联记录的场景
									pd[i].setId(ids[i]);
								}
							}
							
							defaultServer.publishItems(pd);
							
						}
					
				} catch (Throwable e) {
					logger.error("",e);
				}
			}
			
		}
	}
	
}
