package cn.jmicro.monitor.statis.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.api.monitor.StatisIndex;
import cn.jmicro.api.monitor.StatisItem;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.monitor.statis.config.StatisConfigManager.IStatisConfigListener;

@Component
public class StatisManager {

	private final Logger logger = LoggerFactory.getLogger(StatisManager.class);
	
	private final Map<String,ServiceCounter> services =  new ConcurrentHashMap<>();
	
	private final Map<String,ServiceCounter> accounts =  new ConcurrentHashMap<>();
	
	private final Map<String,ServiceCounter> instances =  new ConcurrentHashMap<>();
	
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
	private StatisConfigManager mscm;
	
	private String logDir;
	
	@Inject
	private MongoDatabase mongoDb;
	
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
			
			 ServiceCounter sc = null;
			if(!Utils.isEmpty(si.getKey())) {
				 //RPC上下文
				 si.setSmKey(UniqueServiceMethodKey.fromKey(si.getKey()));
				 key = si.getSmKey().toKey(true, true, true);
				 String an = si.getActName();
				 if(an == null) {
					 an = "";
				 }
				 
				 key = key + UniqueServiceKey.SEP + an;
				 sc = getSc(services, key, windowSize, slotInterval, tu);
				 if(sc != null) {
					 doStatis(sc,si);
				 }
			}
			
			Set<Integer> ins2Configs = this.mscm.getInstanceConfigs(si.getInstanceName());
			if(ins2Configs != null && !ins2Configs.isEmpty()) {
				  sc = getSc(this.instances,si.getInstanceName(),windowSize,slotInterval,tu);
				  doStatis(sc,si);
			}
			 
		}
	}
	
	private void doStatis( ServiceCounter sc,MRpcStatisItem si) {
		if(sc != null) {
			for(StatisItem oi : si.getTypeStatis().values()) {
				sc.add(oi.getType(), oi.getVal());
			}
		}
	}
	
	private ServiceCounter getSc(Map<String,ServiceCounter> counters, String key,long windowSize,long slotInterval,TimeUnit tu) {
		timeoutList.put(key, TimeUtils.getCurTime());
		ServiceCounter counter = counters.get(key);
		if(counter != null) {
			return counter;
		}
		
		//取常量池中的字符串实例做同步锁,保证基于服务方法标识这一级别的同步
		//key = key.intern();
		synchronized(counters) {
			counter = counters.get(key);
			if(counter == null) {
				counter = new ServiceCounter(key, null,windowSize,slotInterval,tu);
				counters.put(key, counter);
			}
		}
		
		return counter;
	
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

		if(StatisConfig.TO_TYPE_SERVICE_METHOD == lw.getToType()) {
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
		} else if (StatisConfig.TO_TYPE_DB == lw.getToType()) {
			if(Utils.isEmpty(lw.getToParams())) {
				lw.setToParams(StatisConfig.DEFAULT_DB);
			}
		} else if (StatisConfig.TO_TYPE_FILE == lw.getToType()) {

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
		
		/*for(StatisIndex si : lw.getStatisIndexs()) {
			String numExpStr = "";
			
		}*/
	}

	private void statisConfigRemove(StatisConfig lw) {
		if(StatisConfig.TO_TYPE_FILE == lw.getToType()) {
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
			if(si.getSmKey() != null) {
				sb.append(", SM: ").append(si.getSmKey().getMethod());
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
		//每秒钟一个检测
		TimerTicker.doInBaseTicker(1, "statisManager", null, this::act0);
		
		/*
		 TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
				sm.getBaseTimeUnit())).removeListener(key,false);
		*/
	}
	
	private void act0(String key,Object att) {
		/*if(!accounts.isEmpty()) {
			Iterator<String> accs = this.accounts.keySet().iterator();
			while(accs.hasNext()) {
				String ac = accs.next();
				Set<Integer> acconts = mscm.getAccountConfigs(ac);
				if(acconts != null && !acconts.isEmpty()) {
					for(Integer cid : acconts) {
						StatisConfig sc = mscm.getConfigs(cid);
						ServiceCounter counter = this.accounts.get(ac);
						statisData0(sc,counter,ac,ac);
					}
				}
			}
		}*/
		
		if(!instances.isEmpty()) {
			Set<String> set  = new HashSet<>();
			synchronized(instances) {
				set.addAll(this.instances.keySet());
			}
			Iterator<String> accs = set.iterator();
			while(accs.hasNext()) {
				String ac = accs.next();
				Set<Integer> acconts = mscm.getInstanceConfigs(ac);
				if(acconts != null && !acconts.isEmpty()) {
					for(Integer cid : acconts) {
						StatisConfig sc = mscm.getConfigs(cid);
						ServiceCounter counter = this.instances.get(ac);
						statisData0(sc,counter,ac,null);
					}
				}
			}
		}
		
		if(!services.isEmpty()) {
			Set<String> set  = new HashSet<>();
			synchronized(services) {
				set.addAll(this.services.keySet());
			}
			Iterator<String> smKeys = set.iterator();
			
			while(smKeys.hasNext()) {
				String smKey = smKeys.next();
				Set<Integer> srvsConfigs = mscm.getSrvConfigs(smKey);
				if(srvsConfigs != null && !srvsConfigs.isEmpty()) {
					String actName = smKey.substring(smKey.lastIndexOf(UniqueServiceKey.SEP)+ UniqueServiceKey.SEP.length());
					for(Integer cid : srvsConfigs) {
						StatisConfig sc = mscm.getConfigs(cid);
						ServiceCounter counter = services.get(smKey);
						statisData0(sc,counter,smKey,actName);
					}
				}
			}
		}
	}
	
	private void statisData0(StatisConfig sc, ServiceCounter counter, String key,String actName) {
		if(sc.getByType() != StatisConfig.BY_TYPE_INSTANCE) {
			logger.error("StatisConfig type error: " + JsonUtils.getIns().toJson(sc));
			return;
		}
		
		Map<String,Object> indexes = new HashMap<>();
		
		for(StatisIndex idx : sc.getStatisIndexs()) {
			if(idx.getType() == StatisConfig.PREFIX_CUR) {
				indexes.put(idx.getName(), counter.getByTypes(idx.getNums()));
			}else if(idx.getType() == StatisConfig.PREFIX_CUR_PERCENT) {
				long n = counter.getByTypes(idx.getNums());
				long d = counter.getByTypes(idx.getDens());
				if(d != 0) {
					indexes.put(idx.getName(), ((n*1.0D)/d)*100);
				}else {
					indexes.put(idx.getName(), -1D);
				}
			}else if(idx.getType() == StatisConfig.PREFIX_QPS) {
				indexes.put(idx.getName(), counter.getQps(TimeUnit.SECONDS,idx.getNums()));
			}else if(idx.getType() == StatisConfig.PREFIX_TOTAL) {
				indexes.put(idx.getName(), counter.getTotal(idx.getNums()));
			}else if(idx.getType() == StatisConfig.PREFIX_TOTAL_PERCENT) {
				long n = counter.getTotal(idx.getNums());
				long d = counter.getTotal(idx.getDens());
				if(d != 0) {
					indexes.put(idx.getName(), ((n*1.0D)/d)*100);
				}else {
					indexes.put(idx.getName(), -1D);
				}
			} else {
				logger.error("Not support index type for statis config: " + sc.getId());
			}
		}
		
		StatisData sd = new StatisData();
		sd.setCid(sc.getId());
		sd.setStatis(indexes);
		sd.setInputTime(TimeUtils.getCurTime());
		sd.setKey(key);
		sd.setType(sc.getToType());
		sd.setActName(actName);
		
		if(sc.getToType() == StatisConfig.TO_TYPE_SERVICE_METHOD) {
			PromiseUtils.callService(sc.getSrv(), sc.getToMt(), null, sd)
			.fail((code,msg,cxt)->{
				logger.error("Notify fail: " + sc.getToSn() +"##"+sc.getToNs() +"##" + sc.getToVer()+"##"+ sc.getToMt());
			});
		}else if(sc.getToType() == StatisConfig.TO_TYPE_CONSOLE) {
			logger.info(JsonUtils.getIns().toJson(sd));
		}else if(sc.getToType() == StatisConfig.TO_TYPE_DB) {
			MongoCollection<Document> coll = mongoDb.getCollection(sc.getToParams());
			coll.insertOne(Document.parse(JsonUtils.getIns().toJson(sd)));
		}else if(sc.getToType() == StatisConfig.TO_TYPE_FILE) {
			try {
				sc.getBw().write(JsonUtils.getIns().toJson(sd)+"\n");
			} catch (IOException e) {
				logger.error("",JsonUtils.getIns().toJson(sd));
			}
		}
	
	}
	
}
