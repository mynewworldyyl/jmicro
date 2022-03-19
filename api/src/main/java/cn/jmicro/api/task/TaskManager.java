package cn.jmicro.api.task;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.utils.TimeUtils;

public class TaskManager extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

	//private boolean running = false;
	
	private int maxQueueSize = 5000;
	
	private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
	
	private long lastStartTime = 0;
	
	private Object syncObj = new Object();
	
	public TaskManager(int maxTaskSize) {
		this.maxQueueSize = maxTaskSize;
		this.start();
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				synchronized(syncObj) {
					if(tasks.isEmpty()) {
						syncObj.wait();
					}
				}
				
				Runnable r = null;
				while((r = this.tasks.poll()) != null) {
					this.lastStartTime = TimeUtils.getCurTime();
					r.run();
				}
			}catch(Throwable e) {
				logger.error("",e);
				LG.log(MC.LOG_ERROR, this.getClass(), "",e);
			}
		}
	}
	
	public boolean submitTask(Runnable t) {
		
		boolean rst = false;
		if(this.tasks.size() < maxQueueSize) {
			this.tasks.offer(t);
			rst = true;
		}
		
		synchronized(this.syncObj) {
			this.syncObj.notify();
		}
		
		return rst;
	}

	public long getLastStartTime() {
		return lastStartTime;
	}

	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

}
