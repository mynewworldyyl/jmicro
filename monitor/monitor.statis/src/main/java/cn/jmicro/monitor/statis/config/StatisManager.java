package cn.jmicro.monitor.statis.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.api.monitor.StatisIndex;
import cn.jmicro.api.monitor.StatisItem;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;

@Component
public class StatisManager {

	private static final long COUNTER_TIMEOUT = 5*60*1000;
	
	private static final Logger logger = LoggerFactory.getLogger(StatisManager.class);
	
	private final Map<String,ServiceCounter> services =  new ConcurrentHashMap<>();
	
	private final Map<String,ServiceCounter> accounts =  new ConcurrentHashMap<>();
	
	private final Map<String,ServiceCounter> instances =  new ConcurrentHashMap<>();
	
	private final Map<String,Long> timeoutList = new ConcurrentHashMap<>();
	
	@Cfg("/StatisManager/enable")
	private boolean enable = false;
	
	@Cfg(value="/StatisManager/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private StatisConfigManager mscm;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private PubSubManager pubsubMng;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ServiceInvokeManager invokeMng;
	
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
	public void onItems(JMStatisItem[] items) {

		for(JMStatisItem si : items) {
			
			if(openDebug) {
				log(si);
			}
			
			//3分钟
			long windowSize = 1*60*1000;
			long slotInterval = 500; //每间隔500毫秒转一次
			TimeUnit tu = TimeUnit.MILLISECONDS;
			String key = null;
			
			ServiceCounter sc = null;
			if(si.isRpc()) {
				 //RPC上下文
				 si.setSmKey(UniqueServiceMethodKey.fromKey(si.getKey()));
				 key = si.getSmKey().toKey(true, true, true);
				 key = key + UniqueServiceKey.SEP + si.getClientId();
				 sc = getSc(services, key, windowSize, slotInterval, tu);
				 if(sc != null) {
					 doStatis(sc,si);
				 }
			} else {
				//非RPC数据
				Set<StatisConfig> ins2Configs = mscm.getInstanceConfigs(si.getInstanceName());
				if(ins2Configs != null && !ins2Configs.isEmpty()) {
				  sc = getSc(instances,si.getInstanceName()+"##"+si.getClientId(),windowSize,slotInterval,tu);
				  doStatis(sc,si);
				}
			}
		}
	}
	
	private void doStatis(ServiceCounter sc,JMStatisItem si) {
		if(sc != null) {
			for(Short type : si.getTypeStatis().keySet()) {
				List<StatisItem> items = si.getTypeStatis().get(type);
				for(StatisItem oi : items) {
					sc.add(oi.getType(), oi.getVal(),si.getSubmitTime()- oi.getTime());
				}
			}
		}
	}
	
	private ServiceCounter getSc(Map<String,ServiceCounter> counters, String key,long windowSize,
			long slotInterval,TimeUnit tu) {
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
	
	/*private IStatisConfigListener lis = (evt,sc)->{
		if(evt == IListener.ADD) {
			statisConfigAdd(sc);
		}else if(evt == IListener.DATA_CHANGE) {
			statisConfigDataChange(sc);
		}else if(evt == IListener.REMOVE) {
			statisConfigRemove(sc);
		}
	};*/


	
	protected void log(JMStatisItem si) {
		for(Short type : si.getTypeStatis().keySet()) {
			List<StatisItem> items = si.getTypeStatis().get(type);
			for(StatisItem oi : items) {
				StringBuffer sb = new StringBuffer();
				sb.append("GOT: " + MC.MONITOR_VAL_2_KEY.get(oi.getType()));
				if(si.getSmKey() != null) {
					sb.append(", SM: ").append(si.getSmKey().getMethod());
				}
				sb.append(", actName: ").append(si.getClientId());
				logger.debug(sb.toString()); 
			}
		}
	}
	
	public void ready() {
		//mscm.addStatisConfigListener(lis);
		//每秒钟一个检测
		TimerTicker.doInBaseTicker(1, "statisManager", null, this::act0);
		
		/*
		 TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
				sm.getBaseTimeUnit())).removeListener(key,false);
		*/
	}
	
	private void act0(String key,Object att) {

		//Set<StatisConfig> instanceConfigs = this.mscm.getConfigByType(StatisConfig.BY_TYPE_INSTANCE);
		
		checkTimeoutCounter(instances);
		checkTimeoutCounter(services);
		
		if(!instances.isEmpty()) {
			configMatchCount(instances,StatisConfig.BY_TYPE_INSTANCE);
		}
		
		if(!services.isEmpty()) {
			configMatchCount(services,StatisConfig.BY_TYPE_SERVICE_METHOD,
					StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD,StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD);
		}
	}
	
	private void checkTimeoutCounter(Map<String,ServiceCounter> counters) {
		synchronized(counters) {
			Set<String> keys = new HashSet<>();
			keys.addAll(counters.keySet());
			long curTime = TimeUtils.getCurTime();
			for(String k : keys) {
				ServiceCounter sc = counters.get(k);
				if(sc != null &&  curTime - sc.getLastActiveTime() > COUNTER_TIMEOUT) {
					logger.warn("Remove timeout counter: {}",k);
					counters.remove(k);
				}
			}
		}
	}

	private void configMatchCount(Map<String,ServiceCounter> counters, int ...types) {
		Set<String> methodKeys  = new HashSet<>();
		synchronized(counters) {
			methodKeys.addAll(counters.keySet());
		}
		
		Set<StatisConfig> insConfigs = this.mscm.getConfigByType(types);
		for(StatisConfig sc : insConfigs) {
			Iterator<String> methodKeyIte = methodKeys.iterator();
			while(methodKeyIte.hasNext()) {
				String insName = methodKeyIte.next();
				if(sc.getPattern().matcher(insName).find()) {
					//记录配置上次活跃时间
					sc.setLastActiveTime(TimeUtils.getCurTime());
					ServiceCounter cter = counters.get(insName);
					for(StatisIndex si : sc.getStatisIndexs()) {
						statisOneIndex(si,cter);
					}
				}
			}
		}
		
		long curTime = TimeUtils.getCurTime();
		for(StatisConfig sc : insConfigs) {
			if(sc.getCounterTimeout() <= 0 || ((curTime - sc.getLastActiveTime()) < sc.getCounterTimeout())) {
				finalStatisData(sc);
			}
			for(StatisIndex idx : sc.getStatisIndexs()) {
				idx.setCurDens(0);
				idx.setCurNums(0);
			}
		}
	}
	
	private void statisOneIndex(StatisIndex idx,ServiceCounter counter) {

		if(idx.getType() == StatisConfig.PREFIX_CUR) {
			long v = counter.getByTypes(idx.getNums());
			if(v > 0) {
				idx.setCurNums(idx.getCurNums() + v);
			}
		}else if(idx.getType() == StatisConfig.PREFIX_CUR_PERCENT) {
			long v = counter.getByTypes(idx.getNums());
			if(v > 0) {
				idx.setCurNums(idx.getCurNums() + v);
			}
			
			v = counter.getByTypes(idx.getDens());
			if(v > 0) {
				idx.setCurDens(idx.getCurDens() + v);
			}
			
		}else if(idx.getType() == StatisConfig.PREFIX_QPS) {
			int v = (int)(counter.getQps(TimeUnit.SECONDS, idx.getNums())*1000);
			if(v > 0) {
				idx.setCurNums(idx.getCurNums() + v);
			}
		}else if(idx.getType() == StatisConfig.PREFIX_TOTAL) {
			long v = counter.getTotal(idx.getNums());
			if(v > 0) {
				idx.setCurNums(idx.getCurNums() + v);
			}
		}else if(idx.getType() == StatisConfig.PREFIX_TOTAL_PERCENT) {
			long v = counter.getTotal(idx.getNums());
			if(v > 0) {
				idx.setCurNums(idx.getCurNums() + v);
			}
			
			v = counter.getTotal(idx.getDens());
			if(v > 0) {
				idx.setCurDens(idx.getCurDens() + v);
			}
		} else {
			logger.error("Not support index type for statis config: " + JsonUtils.getIns().toJson(idx));
		}
	
	}

	private void finalStatisData(StatisConfig sc) {

		if(StatisConfig.TO_TYPE_SERVICE_METHOD == sc.getToType() && 
				TimeUtils.getCurTime() - sc.getLastNotifyTime() < sc.getMinNotifyTime()) {
			return;
		}
		
		Map<String,Object> indexes = new HashMap<>();
		
		for(StatisIndex idx : sc.getStatisIndexs()) {
			if(idx.getType() == StatisConfig.PREFIX_CUR || idx.getType() == StatisConfig.PREFIX_TOTAL) {
				indexes.put(idx.getName(), idx.getCurNums());
			}else if(idx.getType() == StatisConfig.PREFIX_QPS) {
				indexes.put(idx.getName(), idx.getCurNums()/1000.0);
			}else if(idx.getType() == StatisConfig.PREFIX_TOTAL_PERCENT 
					|| idx.getType() == StatisConfig.PREFIX_CUR_PERCENT) {
				long n = idx.getCurNums();
				long d = idx.getCurDens();
				if(d != 0) {
					indexes.put(idx.getName(), ((n*1.0D)/d)*100);
				} else {
					indexes.put(idx.getName(), -1D);
				}
			} else {
				logger.error("Not support index type for statis config: " + sc.getId());
			}
		}
		
		if(sc.getExp() != null && !ExpUtils.compute(sc.getExp(), indexes, Boolean.class)) {
			if(StatisConfig.TO_TYPE_SERVICE_METHOD != sc.getToType() 
					|| TimeUtils.getCurTime() - sc.getLastNotifyTime() < 10000) {
				return;
			}
		}
		
		sc.changeExpIndex();

		StatisData sd = new StatisData();
		sd.setStatis(indexes);
		sd.setInputTime(TimeUtils.getCurTime());
		sd.setCid(sc.getId());
		sd.setKey(sc.getByKey());
		sd.setType(sc.getToType());
		sd.setActName(sc.getActName());
		sd.setClientId(sc.getCreatedBy());//由谁创建的配置，产生的数据就是谁的
		
		switch(sc.getToType()) {
		case StatisConfig.TO_TYPE_SERVICE_METHOD:
			sc.setLastNotifyTime(TimeUtils.getCurTime());
			//PromiseUtils.callService(sc.getSrv(), sc.getToMt(), null, sd)
			this.invokeMng.call(sc.getToSn(), sc.getToNs(), sc.getToVer(),
					sc.getToMt(), IPromise.class, new Class[]{StatisData.class}, new Object[] {sd})
			.fail((code,msg,cxt)->{
				logger.error("Notify fail: " + sc.getToSn() +"##"+sc.getToNs() +"##" + sc.getToVer()+"##"+ sc.getToMt());
			});
			break;
		case StatisConfig.TO_TYPE_CONSOLE:
			logger.info(JsonUtils.getIns().toJson(sd));
			break;
		case StatisConfig.TO_TYPE_MESSAGE:
			PSData pd = new PSData();
			pd.setData(sd);
			pd.setTopic(sc.getToParams());
			pd.setPersist(false);
			pd.setId(idGenerator.getIntId(PSData.class));
			pd.setSrcClientId(sc.getCreatedBy());//由谁创建配置，数据就由谁可见
			this.pubsubMng.publish(pd);
			break;
		case StatisConfig.TO_TYPE_DB:
			MongoCollection<Document> coll = mongoDb.getCollection(sc.getToParams());
			coll.insertOne(Document.parse(JsonUtils.getIns().toJson(sd)));
			break;
		case StatisConfig.TO_TYPE_MONITOR_LOG:
			//LG.logWithNonRpcContext(MC.LOG_INFO, sc.getToParams(), JsonUtils.getIns().toJson(sd),null,MC.MT_DEFAULT,true);
			saveLog(sd,sc);//直接保存到日志库
			break;
		case StatisConfig.TO_TYPE_FILE:
			try {
				sc.getBw().write(JsonUtils.getIns().toJson(sd)+"\n");
			} catch (IOException e) {
				logger.error("",JsonUtils.getIns().toJson(sd));
			}
			break;
		}
	}
	
	private void saveLog(StatisData sd,StatisConfig sc) {
		long curTime = TimeUtils.getCurTime();
		JMLogItem ji = new JMLogItem();
		ji.setActClientId(sc.getCreatedBy());
		ji.setActName(sd.getActName());
		ji.setConfigId(sc.getId()+"");
		ji.setCreateTime(curTime);
		ji.setInputTime(curTime);
		ji.setInstanceName(sc.getByns());
		ji.setLogLevel(MC.LOG_INFO);
		ji.setSysClientId(sc.getCreatedBy());
		ji.addOneItem(MC.LOG_INFO, "StatisConfig", JsonUtils.getIns().toJson(sd),TimeUtils.getCurTime());
		ji.setTag("StatisConfig");
		MongoCollection<Document> coll = mongoDb.getCollection(sc.getToParams());
		coll.insertOne(Document.parse(JsonUtils.getIns().toJson(ji)));
	}
	
	
	@SuppressWarnings("unused")
	private boolean computeByExpression(StatisConfig cfg,Map<String,Object> cxt) {
		
		//Map<String,Object> cxt = new HashMap<>();
		
		Exp exp = cfg.getExp();
		//cxt.put(JMicroContext.LOCAL_HOST, Config.getExportSocketHost());
		//cxt.put(JMicroContext.LOCAL_PORT, Config.getInstanceName());
		//cxt.put(Constants.LOCAL_INSTANCE_NAME, Config.getInstanceName());
		
		return ExpUtils.compute(exp, cxt, Boolean.class);
		
	}
	
}
