package cn.jmicro.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.limit.ILimitData;
import cn.jmicro.api.monitor.IServiceCounter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.server.ServerMessageReceiver.JMicroTask;
import cn.jmicro.server.limit.StatisServiceCounter;

/**
   *     实现高并发任务的削峰填谷
 * 
 * @author yeyulei
 */
@Component
@Service(namespace="serviceMethodTaskQueue", version="0.0.1", monitorEnable=0, maxSpeed=0,debugMode=0,
baseTimeUnit=Constants.TIME_SECONDS, external=false, showFront=false)
public class ServiceMethodTaskQueueManager implements ILimitData{

	private static final Logger logger = LoggerFactory.getLogger(ServiceMethodTaskQueueManager.class);

	private int MAX_QUEUE_SIZE = 100;
	
	//即将被执行的任务队列
	private Map<Integer,ServiceMethodTaskQueue> taskQueue = new ConcurrentHashMap<>();
	
	private Set<Integer> tempKeys = new HashSet<>();
	
	private Object syncObject = new Object();
	
	private ExecutorService defaultExecutor = null;
	
	@Inject
	private ServiceManager srvManager;
	
	public void ready() {
		/*
		 srvManager.addListener((type,item)->{
			if(type == IListener.ADD) {
				serviceAdd(item);
			}else if(type == IListener.REMOVE) {
				serviceRemove(item);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(item);
			}
		});
		*/
		new Thread(this::run).start();
	}
	
	public void sumbit(JMicroTask t,ServiceMethod sm) {
		ServiceMethodTaskQueue tq = taskQueue.get(t.getMsg().getSmKeyCode());
		if(tq == null) {
			synchronized(tempKeys) {
				tq = taskQueue.get(t.getMsg().getSmKeyCode());
				if(tq == null) {
					tq =  new ServiceMethodTaskQueue(sm);
					taskQueue.put(t.getMsg().getSmKeyCode(),tq);
					tempKeys.add(t.getMsg().getSmKeyCode());
				}
			}
		}
		
		//使用最新的方法配置
		tq.sm = sm;
		tq.offer(t);
		
		synchronized(syncObject) {
			syncObject.notify();
		}
		
		//defaultExecutor.execute(t);
	}

	public void run() {

		Set<Integer> keys = new HashSet<>();
		
		int waitTimeout = 10;
		int emptyDataWaitTimeout = 1000;
		
		while(true) {
			try {
				
				if(!tempKeys.isEmpty()) {
					synchronized(tempKeys) {
						keys.addAll(tempKeys);
						tempKeys.clear();
					}
				}
				
				Iterator<Integer> ite = keys.iterator();
				//如果一个循环没有数据发送，则说明队列数据为空，可以睡眠更长时间，节约CPU
				boolean  emptyData = true;
				
				while(ite.hasNext()) {
					Integer k = ite.next();
					ServiceMethodTaskQueue tq = taskQueue.get(k);
					if(tq == null) {
						ite.remove();
						taskQueue.remove(k);
						continue;
					}

					if(tq.canSubmit()) {
						JMicroTask[] ts = tq.pop();
						if(ts != null) {
							//logger.info(tq.sm.getKey().getMethod()+" curTime:" + TimeUtils.getCurTime()+",Size: " + ts.length);
							for(JMicroTask t: ts) {
								defaultExecutor.execute(t);
							}
							if(tq.sm.getLimitType() == Constants.LIMIT_TYPE_SS) {
								MT.rpcEvent(tq.sm,MC.MT_SERVER_LIMIT_MESSAGE_POP, ts.length);
							} else {
								tq.addCounter( ts.length);
							}
						}
					}
					
					if(emptyData) {
						emptyData = tq.isEmpty();
					}
					
				}

				if(emptyData) {
					//队列没数据，可以睡更长时间，等待有数据的通知
					synchronized (syncObject) {
						syncObject.wait(emptyDataWaitTimeout);
					}
				} else {
					//在限流状态，需要更快地检测并发送数据
					synchronized (syncObject) {
						syncObject.wait(waitTimeout);
					}
				}
			} catch (Throwable e) {
				logger.error("", e);
			} finally {

			}
		}
		
	}
	
	@Override
	public void onData(StatisData sc) {
		Integer smCode = HashUtils.FNVHash1(sc.getKey());
		ServiceMethodTaskQueue q = taskQueue.get(smCode);
		if(q!= null && q.ss!= null) {
			if(sc.containIndex(StatisData.AVG_QPS)) {
				q.ss.setQps((Double)sc.getStatis().get(StatisData.AVG_QPS));
				logger.info(sc.getKey() + " avgQps: " + (Double)sc.getStatis().get(StatisData.AVG_QPS));
			} else {
				q.ss.setQps((Double)sc.getStatis().get(StatisData.QPS));
				logger.info(sc.getKey() + " qps: " + sc.getStatis().get(StatisData.QPS));
			}
		}
	}

	private class ServiceMethodTaskQueue {
		private final Short TYPE = 1;
		private Queue<JMicroTask> queue = new ConcurrentLinkedQueue<>();
		
		private ServiceMethod sm;
		
		private int queueSize = MAX_QUEUE_SIZE;
		
		private IServiceCounter<Short> counter;
		
		private StatisServiceCounter ss;
		
		private double maxQps = 0;
		
		private int t = 0;
		
		private int batchSize = 0;
		
		private long lastPopTime = TimeUtils.getCurTime();
		
		private ServiceMethodTaskQueue(ServiceMethod sm) {
			this.sm = sm;
			this.maxQps = sm.getMaxSpeed();
			computePeriodAndBatchSize();
			//计数时间窗口内最大速度的请求数量
			if(1 == sm.getLimitType()) {
				this.counter = new ServiceCounter(sm.getKey().toKey(false, false, false),
						new Short[] {TYPE},6000L,100L,TimeUnit.MILLISECONDS);
			} else {
				this.counter = this.ss = new StatisServiceCounter(sm.getKey().toKey(false, false, false),
						new Short[] {TYPE});
			}
		}
		
		//以100毫秒为一个周期
		private void computePeriodAndBatchSize() {
			this.t = 100;
			if(this.sm.getMaxSpeed() <= 10) {
				//每100毫秒处理一个
				this.batchSize = 1;
			}else {
				//每100毫秒处理最大速度的10份之一
				this.batchSize = (int)(sm.getMaxSpeed() * 0.1);
			}
			
			//队列大小为1分种最大QPS处理消息数量
			if(this.sm.getTimeout() > 0) {
				this.queueSize = (int)TimeUtils.getTime(this.sm.getTimeout(),
						TimeUtils.getTimeUnit(sm.getBaseTimeUnit()),TimeUnit.SECONDS)
						* sm.getMaxSpeed();
			} else {
				this.queueSize = 60 * sm.getMaxSpeed();
			}
			
		}

		public void offer(JMicroTask task) {
			
			if((this.queue.size()/this.maxQps)*1000 > sm.getTimeout()) {
				MT.rpcEvent(this.sm,MC.MT_SERVER_LIMIT_MESSAGE_REJECT, 1);
				throw new CommonException(MC.MT_SERVER_LIMIT_MESSAGE_REJECT,"Rpc will timeout in queue cost: " + ((this.queue.size()/this.maxQps)*1000 )+", timeout: "+sm.getTimeout());
			}
			
			if(this.queue.size() >= this.queueSize) {
				MT.rpcEvent(this.sm,MC.MT_SERVER_LIMIT_MESSAGE_REJECT, 1);
				throw new CommonException(MC.MT_SERVER_LIMIT_MESSAGE_REJECT,"Queue is full with max size:" + this.queueSize+" CurSize: "+this.queue.size());
			}
			
			this.queue.offer(task);
			MT.rpcEvent(this.sm,MC.MT_SERVER_LIMIT_MESSAGE_PUSH, 1);
		}
		
		public JMicroTask[] pop() {
			long curTime =  TimeUtils.getCurTime();
			long interval = curTime - this.lastPopTime;
			if(!this.queue.isEmpty() && interval >= t) {
				
				int size = this.queue.size();
				if(size > this.batchSize) {
					size = this.batchSize;
				}
				
				JMicroTask[] bts = new JMicroTask[size];
				for(int i = 0; i < size; i++) {
					bts[i] = queue.poll();
				}
				
				this.lastPopTime = TimeUtils.getCurTime();
				//logger.info("SM: {}, Interval:{}, Size:{},Time:{}",sm.getKey().getMethod(),interval,bts.length,curTime);
				
				return bts;
			}
			return null;
		}
		
		public void addCounter(int val) {
			this.counter.add(TYPE, val);
		}
		
		public boolean canSubmit() {
			double curQps = this.counter.getQps(TimeUnit.SECONDS, TYPE);
			return curQps  <= this.maxQps;
		}
		
		public boolean isEmpty() {
			return this.queue.isEmpty();
		}
		
	}

	public void setDefaultExecutor(ExecutorService defaultExecutor) {
		this.defaultExecutor = defaultExecutor;
	}
	
}
