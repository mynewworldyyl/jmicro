package cn.jmicro.api.monitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.monitor.MonitorStatisConfigManager.IStatisConfigListener;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@Component
public class StatisManager {

	private final Logger logger = LoggerFactory.getLogger(StatisManager.class);
	
	private final Map<String,ServiceCounter> counters =  new ConcurrentHashMap<>();
	
	private final Map<String,Long> timeoutList = new ConcurrentHashMap<>();
	
	@Cfg("/StatisManager/enable")
	private boolean enable = false;
	
	@Cfg(value="/StatisManager/openDebug")
	private boolean openDebug = true;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MonitorStatisConfigManager mscm;
	
	private String logDir;
	
	/**
	 * 服务，名称空间，版本，方法，实例，IP，账号
	 * 
	 * 非RPC环境下：
	 * 统计某个实例的指标数据
	 * 统计某个IP的指标数据
	 * 统计某个账号的指标数据
	 * 
	 * 
	 * RPC环境下：
	 * 统计服务全部名称空间和版本的特定方法的指标数据
	 * 统计服务的特定名称空间下的全部版本的特定方法的指标数据
	 * 统计服务的特定版本的全部名称空间的特定方法的指标数据
	 * 统计服务的特定名称空间和版本的特定方法的指标数据
	 * 
	 * 统计特定账号对某个服务方法调用的指标数据
	 * 统计特定实例的某个服务方法被调用的指标数据
	 * 
	 * @param items
	 */
	public void onItems(MRpcStatisItem[] items) {

		for(MRpcStatisItem si : items) {
			if(openDebug) {
				log(si);
			}
			
			//5分钟
			long windowSize = 3000;
			long slotInterval = 1;
			TimeUnit tu = TimeUnit.SECONDS;
			String key = null;
			
			if(si.getKey() == null) {
				//非RPC上下文
				doStatis(si, si.getInstanceName(), windowSize, slotInterval, tu);
				
			} else {
				//RPC上下文
				
			}
			
			/*if(StringUtils.isNotEmpty(si.getRemoteHost()) && StringUtils.isNotEmpty(si.getRemotePort())) {
				 key = si.getRemoteHost()+":"+si.getRemotePort();
				 doStatis(si,key,windowSize,slotSize,tu);
			}*/
			
			ServiceMethod sm = si.getSm();
			
			if(sm != null) {
				
				 key = sm.getKey().getUsk().toKey(true, true, true);
				
				 doStatis(si,key,windowSize,slotInterval,tu);
				
				 //服务数据统计
				 key = sm.getKey().getUsk().toSnv();
				 doStatis(si,key,windowSize,slotInterval,tu);
				 
				 windowSize = sm.getTimeWindow();
				 slotInterval = sm.getSlotInterval();
				 tu = TimeUtils.getTimeUnit(sm.getBaseTimeUnit());
				 
				 //服务方法数据统计
				 key = sm.getKey().toSnvm();
				 doStatis(si,key,windowSize,slotInterval,tu);
				 
				 //服务实例方法
				 key = sm.getKey().toKey(true,true,true);
				 doStatis(si,key,windowSize,slotInterval,tu);
				 
				 if(!Utils.isEmpty(si.getActName())) {
					 doStatis(si,si.getActName(),windowSize,slotInterval,tu);
				 }
				 
			}
		}
		
	
	}
	
	private void doStatis(MRpcStatisItem si, String key,long windowSize,long slotInterval,TimeUnit tu) {
		ServiceCounter counter = counters.get(key);
		timeoutList.put(key, TimeUtils.getCurTime());
		
		if(counter == null) {
			//取常量池中的字符串实例做同步锁,保证基于服务方法标识这一级别的同步
			key = key.intern();
			synchronized(key) {
				counter = counters.get(key);
				if(counter == null) {
					counter = new ServiceCounter(key, MC.MT_TYPES_ARR,windowSize,slotInterval,tu);
					counters.put(key, counter);
				}
			}
		}
		
		for(StatisItem oi : si.getTypeStatis().values()) {
			/* if(openDebug) {
					logger.debug("GOT: " + MonitorConstant.MONITOR_VAL_2_KEY.get(oi.getType()) + " , KEY: " 
			 + key + " , Num: " + oi.getNum() + " , Val: " + oi.getVal());
			 }*/
			 if(MC.MT_CLIENT_IOSESSION_READ == oi.getType()
				|| MC.MT_SERVER_JRPC_GET_REQUEST == oi.getType()) {
				 counter.add(oi.getType(), oi.getCnt());
			 } else {
				 counter.add(oi.getType(), oi.getSum());
			 }
		}
		
	}
	
	private IStatisConfigListener lis = (evt,sc)->{
		if(evt == IListener.ADD) {
			statisConfigAdd(sc);
		}/*else if(evt == IListener.DATA_CHANGE) {
			statisConfigDataChange(sc);
		}*/else if(evt == IListener.REMOVE) {
			statisConfigRemove(sc);
		}
	};

	private void statisConfigAdd(StatisConfig lw) {

		if(StatisConfig.TO_TYPE_SERVICE_METHOD.equals(lw.getToType())) {
			if(!reg.isExists(lw.getToSn(), lw.getToNs(), lw.getToVer())) {
				//服务还不在在，可能后续上线，这里只发一个警告
				String msg2 = "Now config service ["+lw.getToSn() +"##"+lw.getToNs()+"##"+ lw.getToVer()+"] not found for id: " + lw.getId();
				logger.warn(msg2);
				LG.logWithNonRpcContext(MC.LOG_WARN, StatisManager.class, msg2);
			}
			
			Object srv = of.getRemoteServie(lw.getToSn(),lw.getToNs(),lw.getToVer(),null);
			if(srv == null) {
				String msg2 = "Fail to create service proxy ["+lw.getToSn() +"##"+lw.getToNs()+"##"+ lw.getToVer()+"] not found for id: " + lw.getId();
				logger.warn(msg2);
				LG.logWithNonRpcContext(MC.LOG_WARN, StatisManager.class, msg2);
			}
			
			lw.setSrv(srv);
		} else if (StatisConfig.TO_TYPE_DB.equals(lw.getToType())) {
			if(Utils.isEmpty(lw.getToParams())) {
				lw.setToParams(StatisConfig.DEFAULT_DB);
			}
		} else if (StatisConfig.TO_TYPE_FILE.equals(lw.getToType())) {

			File logFile = new File(this.logDir + lw.getId() + "_" + lw.getToParams());
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					String msg ="Create log file fail";
					logger.error(msg, e);
					LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class, msg, e);
				}
			}

			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
				lw.setBw(bw);
			} catch (FileNotFoundException e) {
				String msg ="Create writer fail";
				logger.error(msg, e);
				LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class, msg, e);
			}
		}
	}

	private void statisConfigRemove(StatisConfig lw) {
		if(StatisConfig.TO_TYPE_FILE.equals(lw.getToType())) {
			if(lw.getBw() != null) {
				try {
					lw.getBw().close();
				} catch (IOException e) {
					LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class, "Close buffer error for: " + lw.getId(), e);
				}
			}
		}
	}
	
	protected void log(MRpcStatisItem si) {
		for(StatisItem oi : si.getTypeStatis().values()) {
			StringBuffer sb = new StringBuffer();
			sb.append("GOT: " + MC.MONITOR_VAL_2_KEY.get(oi.getType()));
			if(si.getSm() != null) {
				sb.append(", SM: ").append(si.getSm().getKey().getMethod());
			}
			sb.append(", actName: ").append(si.getActName());
			logger.debug(sb.toString()); 
		}
	}
	
	public void ready() {
		logDir = System.getProperty("user.dir")+"/logs/most/";
		File d = new File(logDir);
		if(!d.exists()) {
			d.mkdirs();
		}
		mscm.addStatisConfigListener(lis);
	}
}
