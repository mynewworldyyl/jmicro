package cn.jmicro.api.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.common.util.TimeUtils;

@Component
public class ExecutorFactory {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorFactory.class);
	
	private Map<String,JmicroThreadPoolExecutor> executors = new HashMap<>();
	
    private static final String GROUP = Constants.EXECUTOR_POOL;
    
    private final Set<ExecutorConfig> waitingRegist = new HashSet<>();
    
    private final Map<String,ExecutorMonitorServer> emses = new HashMap<>();
    
    @Inject
    private ServiceLoader sl;
    
    @Inject
    private IObjectFactory of;
    
	public ExecutorFactory() {}
	
	public void ready() {
		if(!Config.isClientOnly()) {
			new Thread(this::doCheck).start();
		}
	}
	
	private void doCheck() {
		while(true) {
			try {
				
				if(!waitingRegist.isEmpty() && sl.hashServer()) {
					Iterator<ExecutorConfig> ecs = this.waitingRegist.iterator();
					while(ecs.hasNext()) {
						ExecutorConfig cfg = ecs.next();
						ecs.remove();
						this.createExecutorService(cfg);
					}
				}
				
				for(ExecutorMonitorServer je: emses.values()) {
					je.check();
				}
				
				Thread.sleep(5000);
				
			} catch (Throwable e) {
				
			}
		}	
	}

	public ExecutorService createExecutor(ExecutorConfig cfg) {
		
		if(StringUtils.isEmpty(cfg.getThreadNamePrefix())) {
			throw new CommonException("ThreadNamePrefix cannot be null");
		}
		
		if(this.executors.containsKey(cfg.getThreadNamePrefix())) {
			logger.info("Return exist thread pool: " + cfg.getThreadNamePrefix());
			return this.executors.get(cfg.getThreadNamePrefix());
		}
		
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
			cfg.setThreadNamePrefix("Default");
		}
		
		JmicroThreadPoolExecutor executor = new JmicroThreadPoolExecutor(cfg);
		executors.put(cfg.getThreadNamePrefix(), executor);
		
		if(!Config.isClientOnly()) {
			 if(sl.hashServer()) {
				createExecutorService(cfg);
			}else {
				waitingRegist.add(cfg);
			}
		}
		
		return executor;
	}
	
	private void createExecutorService(ExecutorConfig cfg) {
		
		JmicroThreadPoolExecutor executor = this.executors.get(cfg.getThreadNamePrefix());
		
		String ns = Config.getInstanceName() + "." + GROUP+"_" + cfg.getThreadNamePrefix();
		ServiceItem si = sl.createSrvItem(IExecutorInfo.class, ns,"0.0.1", ExecutorMonitorServer.class.getName());
		
		ExecutorMonitorServer ems = new ExecutorMonitorServer(cfg,executor.getEi());
		ems.setE(executor);
		executor.getEi().setKey(si.getKey().toKey(true, true, true));
		emses.put(cfg.getThreadNamePrefix(),ems);
		 
		of.regist(ns, ems);
		sl.registService(si,ems);
		
	}

	public class ExecutorMonitorServer  implements IExecutorInfo {
		
		 private final Logger ilog = LoggerFactory.getLogger(ExecutorMonitorServer.class);
		 
		 private ExecutorInfo ei;
		 private JmicroThreadPoolExecutor e;
		 private ExecutorConfig cfg = null;
		 private long warnSize = Long.MAX_VALUE;
		 
		 public ExecutorMonitorServer(ExecutorConfig cfg,ExecutorInfo ei) {
			this.cfg = cfg;
			this.ei = ei;
			this.ei.setTerminal(false);
			this.ei.setInstanceName(Config.getInstanceName());
			//this.ei.setKey("JMicro-" + Config.getInstanceName()+"-" + this.cfg.getThreadNamePrefix());
			this.ei.setEc(this.cfg);
			
			ilog.info("Create thread pool: " + this.ei.getKey());
			//queue size got 80% of the max queue size will sent warning message to monitor server
			warnSize = (long)(this.cfg.getTaskQueueSize()*0.8);
		 }
		 
		@Override
		public ExecutorInfo getInfo() {
			setInfo();
			return this.ei;
		}
		 
		private void setInfo() {
			this.ei.setActiveCount(e.getActiveCount());
			this.ei.setCompletedTaskCount(e.getCompletedTaskCount());
			this.ei.setLargestPoolSize(e.getLargestPoolSize());
			this.ei.setPoolSize(e.getPoolSize());
			this.ei.setTaskCount(e.getTaskCount());
			this.ei.setCurQueueCnt(e.getQueue().size());
		}

		public void check() {
			//if pool size less than 2, you should know what you doing
			if(this.cfg.getTaskQueueSize() > 2 && e.getPoolSize() > this.warnSize) {
				setInfo();
				SF.eventLog(MC.EP_TASK_WARNING, MC.LOG_WARN, JmicroThreadPoolExecutor.class,
						JsonUtils.getIns().toJson(this.ei));
			}
		}

		public void setE(JmicroThreadPoolExecutor e) {
			this.e = e;
		}
	}
	
	public static class JmicroThreadPoolExecutor extends ThreadPoolExecutor{
		
		 private ExecutorInfo ei;
		 
		 public JmicroThreadPoolExecutor(ExecutorConfig cfg) {
			 super(cfg.getMsCoreSize(),cfg.getMsMaxSize(),
						cfg.getIdleTimeout(),TimeUtils.getTimeUnit(cfg.getTimeUnit()),
						new ArrayBlockingQueue<Runnable>(cfg.getTaskQueueSize()),
						new NamedThreadFactory("JMicro-"+Config.getInstanceName()+"-"+cfg.getThreadNamePrefix())
						,cfg.getRejectedExecutionHandler());
			this.ei = new ExecutorInfo();
		 }
		 
		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			this.ei.addStartCnt();
			super.beforeExecute(t, r);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			this.ei.addEndCnt();
			super.afterExecute(r, t);
		}

		@Override
		protected void terminated() {
			this.ei.setTerminal(true);
			SF.eventLog(MC.EP_TERMINAL, MC.LOG_WARN, JmicroThreadPoolExecutor.class,
					JsonUtils.getIns().toJson(ei));
			super.terminated();
		}

		public ExecutorInfo getEi() {
			return ei;
		}
		
	}
}
