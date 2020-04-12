package org.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.cache.lock.ILocker;
import org.jmicro.api.cache.lock.ILockerManager;
import org.jmicro.api.config.Config;
import org.jmicro.api.i18n.I18NManager;
import org.jmicro.api.mng.IStatisMonitor;
import org.jmicro.api.mng.ReportData;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v2.IMonitorDataSubscriber;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *   负责启一个定时器，从ServiceReqMonitor取统计数据并publish出去，从而使订阅者定时获得
 *   统计数据。
 *   配合org.jmicro.gateway.MessageServiceImpl订阅指定主题的数据，客户端即可拿到数据。
 * 
 * 
 * @author Yulei Ye
 * @date 2020年3月27日
 */
@Component(level=1001)
@Service(namespace="mng",version="0.0.1")
public class StatisMonitorImpl implements IStatisMonitor {

	private final static Logger logger = LoggerFactory.getLogger(StatisMonitorImpl.class);
	
	private static final String STATIS_MONITOR_DIR = Config.BASE_DIR + "/statisMonitorKeys";
	
	private static final String RES_LOCK = "statisCounterRegLock";
	private static final String[] DATA_TYPE = new String[] {MonitorConstant.PREFIX_QPS,
			MonitorConstant.PREFIX_PERCENT,MonitorConstant.PREFIX_TOTAL};
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private PubSubManager psManager;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private ILockerManager lockManager;
	
	@Inject
	private I18NManager i18nManager;
	
	@Reference(namespace="rpcStatisMonitor", version="0.0.1",required=false)
	private IMonitorDataSubscriber dataServer;
	
	private String prefix = "statis.index.";
	
	private Short[] types = null;
	
	private String[] labels = null;
	
	@JMethod("ready")
	public void ready() {
		types = new Short[MonitorConstant.MONITOR_VAL_2_KEY.size()];
		labels = new String[MonitorConstant.MONITOR_VAL_2_KEY.size()];
		int i = 0;
		for(Map.Entry<Short, String> e:MonitorConstant.MONITOR_VAL_2_KEY.entrySet() ) {
			labels[i] = i18nManager.value("en", prefix + e.getKey());
			types[i] = e.getKey();
			i++;
		}
	}
	
	private ITickerAction tickerAct = new ITickerAction() {
		public void act(String key,Object attachement) {
			
			String mkey = (String)attachement;
			if(!srvManager.containTopic(key)) {
				String t = key.substring(key.lastIndexOf("##")+2);
				stopStatis(mkey,Integer.parseInt(t));
				return;
			}
			
			ReportData rd = dataServer.getData(mkey, types, DATA_TYPE);
			//rd.setTypes(types);
			//rd.setLabels(labels);
			//System.out.println("QPS type: "+MonitorConstant.STATIS_QPS+"="+v);
			
			PSData psData = new PSData();
			psData.setData(rd);
			psData.setTopic(key);
			psData.put(Constants.SERVICE_METHOD_KEY, key);
			
			//将统计数据分发出去
			long f = psManager.publish(psData);
			if(f != PubSubManager.PUB_OK) {
				logger.warn("Fail to publish topic: {},result:{}",psData.getTopic(),f);
			}
			
		}
	};
	
	
	
	@Override
	@SMethod(timeout=60000,retryCnt=0)
	public boolean startStatis(String mkey, Integer t) {
		
		AbstractClientServiceProxy ds = (AbstractClientServiceProxy)((Object)dataServer);
		
		if(dataServer == null || !ds.isUsable()) {
			logger.error("Monitor server is not available, do you start it!");
			return false;
		}
		
		//t单位是秒，后端统一转为毫秒，也许将来能提供更灵活的使用方式
		long time = TimeUtils.getMilliseconds(t,Constants.TIME_SECONDS);
		String rkey = mkey + "##" + time ;
		String regPath =  STATIS_MONITOR_DIR + "/" + rkey;
		
		ILocker lock = null;
		
		try {
			lock = lockManager.getLocker(RES_LOCK);
			if(lock.tryLock(1000)) {
				if(op.exist(regPath) ) {
					//cnt 表示当前订阅此服务统计数据的客户端数量，订阅数值加1
					int cnt = Integer.parseInt(op.getData(regPath))+1;
					op.setData(regPath, cnt+"");
				} else {
					//如果此服务挂机，结点将消失，重新启动后，客户端需要重新订阅
					op.createNode(regPath, "1", true);
					TimerTicker timer = TimerTicker.getTimer(timers,time);
					if(!timer.container(rkey)) {
						timer.addListener(rkey,tickerAct,mkey);
					}
				}
			} else {
				return false;
			}
		}finally {
			if(lock != null) {
				lock.unLock();
			}
		}
		return true;
	}

	@Override
	@SMethod(timeout=60000,retryCnt=0)
	public boolean stopStatis(String mkey,Integer t) {
		if(dataServer == null) {
			logger.error("Monitor server is not available, do you start it!");
			return false;
		}
		long time = TimeUtils.getMilliseconds(t,Constants.TIME_SECONDS);
		String rkey = mkey + "##" + time ;
		String regPath =  STATIS_MONITOR_DIR + "/" + rkey;
		
		ILocker lock = null;
		
		try {
			lock = lockManager.getLocker(RES_LOCK);
			if(lock.tryLock(1000)) {
				if(op.exist(regPath) ) {
					Integer cnt = Integer.parseInt(op.getData(regPath));
					if(cnt == 1) {
						//最后一个订阅者，停止定时器
						op.deleteNode(regPath);
						TimerTicker timer = TimerTicker.getTimer(timers,time);
						if(timer.container(rkey)) {
							timer.removeListener(rkey, true);
						}
					} else {
						//不是最后一个订阅者，订阅计数减1
						op.setData(regPath, (--cnt)+"");
					}
				}
			}else {
				return false;
			}
		}finally {
			if(lock != null) {
				lock.unLock();
			}
		}
		return true;
	}

	@Override
	public Map<String,Object> index2Label() {
		Map<String,Object> result = new HashMap<>();
		result.put("types", this.types);
		result.put("labels", this.labels);
		result.put("indexes", MonitorConstant.MONITOR_VAL_2_KEY);
		return result;
	}


}
