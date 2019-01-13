package org.jmicro.api.executor;

public class ExecutorConfig {

    private int msCoreSize = 2;
	
	private int msMaxSize = 10;
	
	private int idleTimeout = 60;
	
	private int taskQueueSize = 100;
	
	private String timeUnit = "S";
	
	private String threadNamePrefix = "JmicroExecutor";

	public int getMsCoreSize() {
		return msCoreSize;
	}

	public void setMsCoreSize(int msCoreSize) {
		this.msCoreSize = msCoreSize;
	}

	public int getMsMaxSize() {
		return msMaxSize;
	}

	public void setMsMaxSize(int msMaxSize) {
		this.msMaxSize = msMaxSize;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public int getTaskQueueSize() {
		return taskQueueSize;
	}

	public void setTaskQueueSize(int taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public String getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit;
	}
	
	
}
