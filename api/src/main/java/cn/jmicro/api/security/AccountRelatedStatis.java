package cn.jmicro.api.security;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;

@Component
public class AccountRelatedStatis {

	private static final Short[] TYPES = new Short[] {MC.MT_REQ_START};
	
	private static final long DataTimeout = 60*3*1000;
	
	private Map<String,ServiceCounter> limiterData = new ConcurrentHashMap<>();
	
	public void ready(){
		TimerTicker.doInBaseTicker(60, "DefaultSpeedLimiter-Checker", null,
		(key,att)->{
			doCheck();
		});
	}
	
	private void doCheck() {
		
		Map<String,ServiceCounter> lds = new HashMap<>();
		lds.putAll(this.limiterData);
		long curTime = TimeUtils.getCurTime();
		
		for(Map.Entry<String,ServiceCounter> e : lds.entrySet()) {
			if(curTime - e.getValue().getLastActiveTime() > DataTimeout) {
				limiterData.remove(e.getKey());
			}
		}
		
	}
	
	public ServiceCounter getCounter(String key) {
		ServiceCounter sc =  null;
		
		if(!limiterData.containsKey(key)){
			key = key.intern();
			synchronized(key) {
				if(!limiterData.containsKey(key)){
					sc =  new ServiceCounter(key, TYPES,10,1,TimeUnit.SECONDS);
					this.limiterData.put(key, sc);
				} else {
					sc = limiterData.get(key);
				}
			}
		} else {
			sc = limiterData.get(key);
		}
		
		return sc;
	}
}
