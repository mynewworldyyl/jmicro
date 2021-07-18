package cn.jmicro.api.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class ExecutorFactory {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorFactory.class);
	
	private Map<String,JmicroThreadPoolExecutor> executors = new HashMap<>();
	
    private static final String GROUP = Constants.EXECUTOR_POOL;
    
    private final Set<ExecutorConfigJRso> waitingRegist = new HashSet<>();
    
    private final Map<String,ExecutorMonitorServer> emses = new HashMap<>();
    
    @Cfg(value="/ExecutorFactory/registExecutorInfoService", changeListener="registExecutorInfoStatusChange")
    private boolean registExecutorInfoService = false;
    
    @Inject
    private ServiceLoader sl;
    
    @Inject
    private IObjectFactory of;
    
	public ExecutorFactory() {}
	
	public void ready() {
		if(!Config.isClientOnly()) {
			TimerTicker.doInBaseTicker(10, "ExecutorInfoChecker", null, this::doCheck);
		}
	}
	
	private void doCheck(String key,Object cxt) {
		if(!waitingRegist.isEmpty() && sl.hasServer()) {
			Iterator<ExecutorConfigJRso> ecs = this.waitingRegist.iterator();
			while(ecs.hasNext()) {
				ExecutorConfigJRso cfg = ecs.next();
				ecs.remove();
				this.createExecutorService(cfg);
			}
		}
		
		for(ExecutorMonitorServer je: emses.values()) {
			je.check();
		}
	}

	public ExecutorService createExecutor(ExecutorConfigJRso cfg) {
		
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
			 if(sl.hasServer()) {
				//createExecutorService(cfg);
			}else {
				waitingRegist.add(cfg);
			}
		}
		
		return executor;
	}
	
	private void createExecutorService(ExecutorConfigJRso cfg) {
		
		JmicroThreadPoolExecutor executor = this.executors.get(cfg.getThreadNamePrefix());
		
		String ns = Config.getInstanceName() + "." + GROUP+"_" + cfg.getThreadNamePrefix();
		
		ServiceItemJRso si = sl.createSrvItem(IExecutorInfoJMSrv.class, ns,"0.0.1", ExecutorMonitorServer.class.getName(),Config.getClientId());
		executor.getEi().setKey(si.getKey().fullStringKey());
		
		ExecutorMonitorServer ems = new ExecutorMonitorServer(cfg,executor.getEi());
		ems.setE(executor);
		emses.put(cfg.getThreadNamePrefix(),ems);
		 
		of.regist(ns, ems);
		
	}
	
	public void registExecutorInfoStatusChange() {
		if(registExecutorInfoService) {
			for(ExecutorMonitorServer je: emses.values()) {
				sl.unregistService(je.si);
			}
		} else {
			for(ExecutorMonitorServer je: emses.values()) {
				sl.registService(je.si,je);
			}
		}
	}

	public class ExecutorMonitorServer  implements IExecutorInfoJMSrv {
		
		 private final Logger ilog = LoggerFactory.getLogger(ExecutorMonitorServer.class);
		 
		 private ServiceItemJRso si;
		 
		 private ExecutorInfoJRso ei;
		 private JmicroThreadPoolExecutor e;
		 private ExecutorConfigJRso cfg = null;
		 private long warnSize = Long.MAX_VALUE;
		 
		 public ExecutorMonitorServer(ExecutorConfigJRso cfg,ExecutorInfoJRso ei) {
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
		public ExecutorInfoJRso getInfo() {
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
			if(this.cfg.getTaskQueueSize() > 2 && e.getQueue().size() > this.warnSize) {
				setInfo();
				LG.log(MC.LOG_WARN, JmicroThreadPoolExecutor.class,this.cfg.getThreadNamePrefix()+": "
						+ JsonUtils.getIns().toJson(this.ei));
				MT.rpcEvent(MC.EP_TASK_WARNING);
			}
		}

		public void setE(JmicroThreadPoolExecutor e) {
			this.e = e;
		}
	}
	
	public static class JmicroThreadPoolExecutor extends ThreadPoolExecutor{
		
		 private ExecutorInfoJRso ei;
		 
		 public JmicroThreadPoolExecutor(ExecutorConfigJRso cfg) {
			 super(cfg.getMsCoreSize(),cfg.getMsMaxSize(),
						cfg.getIdleTimeout(),TimeUtils.getTimeUnit(cfg.getTimeUnit()),
						new ArrayBlockingQueue<Runnable>(cfg.getTaskQueueSize()),
						new NamedThreadFactory("JMicro-"+Config.getInstanceName()+"-"+cfg.getThreadNamePrefix())
						,cfg.getRejectedExecutionHandler());
			this.ei = new ExecutorInfoJRso();
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
			LG.log(MC.LOG_WARN, JmicroThreadPoolExecutor.class,
					JsonUtils.getIns().toJson(ei));
			MT.rpcEvent(MC.EP_TERMINAL);
			
			super.terminated();
		}

		public ExecutorInfoJRso getEi() {
			return ei;
		}
		
	}
}
