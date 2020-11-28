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

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.limit.ILimitData;
import cn.jmicro.api.monitor.IServiceCounter;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
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
baseTimeUnit=Constants.TIME_SECONDS, external=false)
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
		srvManager.addListener((type,item)->{
			if(type == IListener.ADD) {
				serviceAdd(item);
			}else if(type == IListener.REMOVE) {
				serviceRemove(item);
			}else if(type == IListener.DATA_CHANGE) {
				serviceDataChange(item);
			}
		});
		
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
		
		int waitTimeout = 100;
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
				boolean  overStatus = false;
				
				while(ite.hasNext()) {
					Integer k = ite.next();
					ServiceMethodTaskQueue tq = taskQueue.get(k);
					if (tq == null) {
						ite.remove();
						taskQueue.remove(k);
						continue;
					}

					boolean cansubmit = true;
					while(cansubmit) {
						JMicroTask t = tq.pop();
						if (t != null) {
							tq.addCounter(1);
							defaultExecutor.execute(t);
							cansubmit = tq.canSubmit();
						} else {
							break;
						}
					}
					
					if(cansubmit) {
						//只要有一个队列在限流状态，则需要快速进入下一次循环
						overStatus = true;
					}
					
				}

				if(overStatus) {
					//在限流状态，需要更快地检测并发送数据
					synchronized (syncObject) {
						syncObject.wait(waitTimeout);
					}
				} else {
					//队列没数据，可以睡更长时间，等待有数据的通知
					synchronized (syncObject) {
						syncObject.wait(emptyDataWaitTimeout);
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
		//q.maxQps = (Double)sc.getStatis().get("qps");
		if(q.ss!= null) {
			q.ss.setQps((Double)sc.getStatis().get("qps"));
		}
	}

	private void serviceDataChange(ServiceItem item) {
		/*for(ServiceMethod sm : item.getMethods()) {
			String smKey = sm.getKey().toKey(false, false, false);
			
		}*/
	}

	private void serviceRemove(ServiceItem item) {
		/*for(ServiceMethod sm : item.getMethods()) {
			
		}*/
	}

	private void serviceAdd(ServiceItem item) {
		/*for(ServiceMethod sm : item.getMethods()) {
			
		}*/
	}
	
	private class ServiceMethodTaskQueue {
		private final Short TYPE = 1;
		private Queue<JMicroTask> queue = new ConcurrentLinkedQueue<>();
		
		private ServiceMethod sm;
		
		private int queueSize = MAX_QUEUE_SIZE;
		
		private IServiceCounter<Short> counter;
		
		private StatisServiceCounter ss;
		
		private double maxQps = 0;
		
		private ServiceMethodTaskQueue(ServiceMethod sm) {
			this.sm = sm;
			maxQps = sm.getMaxSpeed();
			//计数时间窗口内最大速度的请求数量
			queueSize = 6*sm.getMaxSpeed();
			if(1 == sm.getLimitType()) {
				counter = new ServiceCounter(sm.getKey().toKey(false, false, false),
						new Short[] {TYPE},6000L,100L,TimeUnit.MILLISECONDS);
			}else {
				counter = ss = new StatisServiceCounter(sm.getKey().toKey(false, false, false),
						new Short[] {TYPE});
			}
		}
		
		public void offer(JMicroTask t) {
			if(queue.size() <= queueSize) {
				queue.offer(t);
			}else {
				throw new CommonException("Queue is full with: " + MAX_QUEUE_SIZE);
			}
		}
		
		public JMicroTask pop() {
			if(!queue.isEmpty()) {
				return queue.poll();
			}
			return null;
		}
		
		public void addCounter(int val) {
			counter.add(TYPE, val);
		}
		
		public boolean canSubmit() {
			double curQps = counter.getQps(TimeUnit.SECONDS, TYPE);
			return curQps  <= maxQps;
		}
		
	}

	public void setDefaultExecutor(ExecutorService defaultExecutor) {
		this.defaultExecutor = defaultExecutor;
	}
	
}
