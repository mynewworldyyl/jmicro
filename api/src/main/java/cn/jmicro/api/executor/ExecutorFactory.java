package cn.jmicro.api.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import cn.jmicro.api.config.Config;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.common.util.TimeUtils;

public final class ExecutorFactory {

	private ExecutorFactory() {}
	
	public static ExecutorService createExecutor(ExecutorConfig cfg) {
		if( cfg.getMsCoreSize() <= 0) {
			cfg.setMsCoreSize(1);
		}
		
		if( cfg.getMsMaxSize() <= 0) {
			cfg.setMsMaxSize(1);
		}
		
		if( cfg.getIdleTimeout() <= 0) {
			cfg.setIdleTimeout(1);
		}
		
		if( cfg.getTaskQueueSize() <= 0) {
			cfg.setTaskQueueSize(100);
		}
		
		if( cfg.getRejectedExecutionHandler() == null) {
			cfg.setRejectedExecutionHandler(new AbortPolicy());
		}
		
		if(StringUtils.isEmpty(cfg.getThreadNamePrefix())) {
			cfg.setThreadNamePrefix("Default");;
		}
		
		ThreadPoolExecutor executor = new ThreadPoolExecutor(cfg.getMsCoreSize(),cfg.getMsMaxSize(),
				cfg.getIdleTimeout(),TimeUtils.getTimeUnit(cfg.getTimeUnit()),
				new LinkedBlockingQueue<Runnable>(cfg.getTaskQueueSize()),
				new NamedThreadFactory("JMicro-"+Config.getInstanceName()+"-"+cfg.getThreadNamePrefix())
				,cfg.getRejectedExecutionHandler());
		
		return executor;
	}
}
