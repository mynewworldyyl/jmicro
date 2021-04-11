package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.cache.lock.ILocker;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.api.mng.IStatisMonitor;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

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
@Service(version="0.0.1",external=true,showFront=false,logLevel=MC.LOG_NO)
public class StatisMonitorImpl implements IStatisMonitor {

	private final static Logger logger = LoggerFactory.getLogger(StatisMonitorImpl.class);
	
	private static final String STATIS_MONITOR_DIR = Config.getRaftBasePath("") + "/statisMonitorKeys";
	
	private static final String RES_LOCK = "statisCounterRegLock";
	
	private static final String[] DATA_TYPE = new String[] {MC.PREFIX_QPS,MC.PREFIX_TOTAL_PERCENT,MC.PREFIX_TOTAL};
	
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
	
	@Reference(namespace="*", version="0.0.1",required=false)
	private IMonitorDataSubscriber dataServer;
	
	private String prefix = "statis.index.";
	
	private Short[] types = null;
	
	private String[] labels = null;
	
	@JMethod("ready")
	public void ready() {
		types = new Short[MC.MONITOR_VAL_2_KEY.size()];
		labels = new String[MC.MONITOR_VAL_2_KEY.size()];
		int i = 0;
		for(Map.Entry<Short, String> e:MC.MONITOR_VAL_2_KEY.entrySet() ) {
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
	@SMethod(perType=true,retryCnt=0,needLogin=true,maxSpeed=5,maxPacketSize=512)
	public boolean startStatis(String mkey, Integer t) {
		
		AbstractClientServiceProxyHolder ds = (AbstractClientServiceProxyHolder)((Object)dataServer);
		
		if(dataServer == null || !ds.getHolder().isUsable()) {
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
					op.createNodeOrSetData(regPath, "1", true);
					TimerTicker timer = TimerTicker.getTimer(timers,time);
					if(!timer.container(rkey)) {
						timer.addListener(rkey,mkey,tickerAct);
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
	@SMethod(perType=true,retryCnt=0,needLogin=true,maxSpeed=5,maxPacketSize=512)
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
		result.put("indexes", MC.MONITOR_VAL_2_KEY);
		return result;
	}


}
