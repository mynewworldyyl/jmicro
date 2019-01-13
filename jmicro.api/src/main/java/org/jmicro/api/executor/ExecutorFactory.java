package org.jmicro.api.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.jmicro.api.config.Config;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;

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
		
		if(StringUtils.isEmpty(cfg.getThreadNamePrefix())) {
			cfg.setThreadNamePrefix("Default");;
		}
		
		ThreadPoolExecutor executor = new ThreadPoolExecutor(cfg.getMsCoreSize(),cfg.getMsMaxSize(),
				cfg.getIdleTimeout(),TimeUtils.getTimeUnit(cfg.getTimeUnit()),
				new LinkedBlockingQueue<Runnable>(cfg.getTaskQueueSize()),
				new NamedThreadFactory("JMicro-"+Config.getInstanceName()+"-"+cfg.getThreadNamePrefix()));
		
		return executor;
	}
}
