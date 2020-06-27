package cn.jmicro.api.executor;

import cn.jmicro.api.annotation.SO;

@SO
public class ExecutorInfo {

	private ExecutorConfig ec;
	
	private String key;
	
	private String instanceName;
	
	//getCorePoolSize()	线程池的核心线程数量
	//getMaximumPoolSize()	线程池的最大线程数量
	
	//线程池中正在执行任务的线程数量
	private int activeCount;
	
	//线程池已完成的任务数量，该值小于等于taskCount
	private long completedTaskCount;
	
	//线程池曾经创建过的最大线程数量。通过这个数据可以知道线程池是否满过，也就是达到了maximumPoolSize
	private int largestPoolSize;
	
	//线程池当前的线程数量
	private int poolSize;
	
	//线程池已经执行的和未执行的任务总数
	private long taskCount;

	//开始数
	private int startCnt;
	
	//结束数
	private int endCnt;
	
	//当前队列任务数
	private int curQueueCnt;
	
	private boolean terminal = false;

	public ExecutorConfig getEc() {
		return ec;
	}

	public void setEc(ExecutorConfig ec) {
		this.ec = ec;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(int activeCount) {
		this.activeCount = activeCount;
	}

	public long getCompletedTaskCount() {
		return completedTaskCount;
	}

	public void setCompletedTaskCount(long completedTaskCount) {
		this.completedTaskCount = completedTaskCount;
	}

	public long getLargestPoolSize() {
		return largestPoolSize;
	}

	public void setLargestPoolSize(int largestPoolSize) {
		this.largestPoolSize = largestPoolSize;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public long getTaskCount() {
		return taskCount;
	}

	public void setTaskCount(long taskCount) {
		this.taskCount = taskCount;
	}

	public int getStartCnt() {
		return startCnt;
	}

	public void setStartCnt(int startCnt) {
		this.startCnt = startCnt;
	}

	public int getEndCnt() {
		return endCnt;
	}

	public void setEndCnt(int endCnt) {
		this.endCnt = endCnt;
	}

	public boolean isTerminal() {
		return terminal;
	}

	public void setTerminal(boolean terminal) {
		this.terminal = terminal;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public int getCurQueueCnt() {
		return curQueueCnt;
	}

	public void setCurQueueCnt(int curQueueCnt) {
		this.curQueueCnt = curQueueCnt;
	}
	
}
