package cn.jmicro.pubsub;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorClientStatusAdapter;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;

@Component(level=3)
public class PubsubMessageStatis {
	
	public static final String KEY_SPERATOR = "##";
	
	public static final String PUBSUB_BASE_DATA = "t_pubsub_base_data";
	
	public static final String PUBSUB_TOTAL = "t_pubsub_total";
	
	public static final String PUBSUB_QPS_DATA = "t_pubsub_qps_data";
	
	//public static final String PUBSUB_TOTAL_DATA = "t_pubsub_total_data";
	
	public static final Short[] TYPES  = {
			MC.Ms_ReceiveItemCnt,MC.Ms_SubmitCnt,MC.Ms_CheckLoopCnt,
			MC.Ms_CheckerSubmitItemCnt,MC.Ms_SubmitTaskCnt,MC.Ms_TaskSuccessItemCnt,
			MC.Ms_Pub2Cache,MC.Ms_DoResendWithCbNullCnt,MC.Ms_DoResendCnt,
			MC.Ms_TopicInvalid,MC.Ms_ServerDisgard,MC.Ms_ServerBusy,
			MC.Ms_Fail2BorrowBasket,MC.Ms_FailReturnWriteBasket,
			MC.Ms_TaskFailItemCnt,
	};
	
	public static final String[] typeLabels = new String[TYPES.length]; 
	
	private Map<String,ServiceCounter> counters = new HashMap<>();
	
	private Object syncLock = new Object();
	
	private MonitorClientStatusAdapter statusMonitorAdapter;
	
	private long scTimeout = 3*60*1000;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private IObjectStorage os;
	
	public void ready() {
		for(int i = 0; i < TYPES.length; i++) {
			typeLabels[i] = MC.MONITOR_VAL_2_KEY.get(TYPES[i]);
		}
		
		String group = "PubsubServer";
		statusMonitorAdapter = new MonitorClientStatusAdapter(PubsubMessageStatis.TYPES,
				PubsubMessageStatis.typeLabels,Config.getInstanceName()+"_PubsubServerStatuCheck",group);

		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, Config.getNamespace()+"."+group, 
				"0.0.1", IMonitorAdapter.class.getName(),Config.getClientId());
		of.regist("MonitorManagerStatuCheckAdapter", statusMonitorAdapter);
		sl.registService(si,statusMonitorAdapter);
		
		TimerTicker.doInBaseTicker(5, "PubsubMessageStatis-Checker", null, (key,att)->{
			doChecker();
		});
	}
	
	private void doChecker() {
		if(counters.isEmpty()) {
			return;
		}
		
		Set<String> temp = new HashSet<>();
		synchronized(syncLock) {
			temp.addAll(this.counters.keySet());
		}
		
		clearTimeout(temp);
		
		if(temp.isEmpty()) {
			//当前批次已全部超时清除
			return;
		}
		
		saveData(temp);
		
	}

	private void saveData(Set<String> temp) {
		
		List<Document> baseData = new ArrayList<Document>();
		List<Document> qpsData = new ArrayList<Document>();
		
		long curTime = TimeUtils.getCurTime();
		Calendar cd = Calendar.getInstance();
		cd.set(Calendar.HOUR_OF_DAY, 0);
		cd.set(Calendar.MINUTE, 0);
		cd.set(Calendar.SECOND, 0);
		cd.set(Calendar.MILLISECOND, 0);
		
		//System.out.println(DateUtils.formatDate(cd.getTime(),DateUtils.PATTERN_YYYY_MM_DD_HHMMSSZZZ));
		
		for(String key : temp) {
			
			ServiceCounter sc = this.counters.get(key);
			if(sc == null) {
				continue;
			}
			
			String[] arr = key.split(KEY_SPERATOR);
			int id = Integer.parseInt(arr[0]);
			String topic = arr[1];
			
			Document bd = new Document("clientId",id)
					.append(IObjectStorage.CREATED_TIME, curTime)
					.append("topic", topic);
			
			Document qd = new Document("clientId",id)
					.append(IObjectStorage.CREATED_TIME, curTime)
					.append("topic", topic);
			
			Document tdFilter = new Document("clientId",id)
					.append("topic", topic)
					.append(IObjectStorage.CREATED_TIME, cd.getTime().getTime());
			
			Document td = new Document(IObjectStorage.UPDATED_TIME, curTime);
			
			for(int i = 0; i < TYPES.length; i++) {
				bd.put(typeLabels[i], sc.get(TYPES[i]));
				qd.put(typeLabels[i], sc.getQps(TimeUnit.SECONDS, TYPES[i]));
				td.put(typeLabels[i], sc.getAndResetTotal(TYPES[i]));
			}
			
			baseData.add(bd);
			qpsData.add(qd);
			//totalData.add(td);
			
			os.update(PUBSUB_TOTAL, tdFilter, new Document("$inc",td),Document.class);
			
		}
		
		os.save(PUBSUB_BASE_DATA, baseData,Document.class, true,true);
		os.save(PUBSUB_QPS_DATA, qpsData,Document.class, true,true);
		
	}

	//清除超时计数器，防止内存溢出
	private void clearTimeout(Set<String> temp) {
		long curTime = TimeUtils.getCurTime();
		Set<String> keys = new HashSet<>();
		for(String key : temp) {
			ServiceCounter sc = this.counters.get(key);
			if(sc != null && curTime - sc.getLastActiveTime() > scTimeout) {
				keys.add(key);
			}
		}
		
		if(!keys.isEmpty()) {
			synchronized(syncLock) {
				for(String key : keys) {
					counters.remove(key);
				}
			}
		}
		
		temp.removeAll(keys);
	}

	public ServiceCounter getSc(String topic,Integer clientId) {
		String key =  clientId+ KEY_SPERATOR + topic;
		if(counters.containsKey(key)) {
			return counters.get(key);
		}
		
		if(!counters.containsKey(key)) {
			synchronized(syncLock) {
				if(!counters.containsKey(key)) {
					ServiceCounter sc = new ServiceCounter(key, TYPES, 60L, 1L, TimeUnit.SECONDS);
					counters.put(key, sc);
				}
			}
		}
		
		return this.counters.get(key);
	}
	
}
