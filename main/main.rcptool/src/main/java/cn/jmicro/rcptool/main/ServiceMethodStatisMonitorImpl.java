package cn.jmicro.rcptool.main;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.annotation.Subscribe;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;
import cn.jmicro.rcptool.main.api.IServiceMethodStatisMonitor;
import cn.jmicro.rcptool.main.api.StatisDataListenerManager;

@Service(maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS,namespace="rcpToolsMonitor",version="0.0.1")
@Component
public class ServiceMethodStatisMonitorImpl implements IServiceMethodStatisMonitor {

	private final static Logger logger = LoggerFactory.getLogger(ServiceMethodStatisMonitorImpl.class);

	@Inject
	private StatisDataListenerManager lisManager;
	
	@SuppressWarnings("unchecked")
	@Subscribe(topic=MC.TEST_SERVICE_METHOD_TOPIC)
	public void statisMonitor(PSData psData) {
		
		Map<Integer,Double> ps = (Map<Integer,Double>)psData.getData();
		ServiceMethod sm = psData.get(Constants.SERVICE_METHOD_KEY);
		if(ps != null && !ps.isEmpty()) {
			//logger.info("type:{},val:{}",e.getKey(),e.getValue());
			lisManager.notifyData(sm, ps);
		}
		
	}
	
}
