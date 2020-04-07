package org.jmicro.rcptool.main;

import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.annotation.Subscribe;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.rcptool.main.api.IServiceMethodStatisMonitor;
import org.jmicro.rcptool.main.api.StatisDataListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS,namespace="rcpToolsMonitor",version="0.0.1")
@Component
public class ServiceMethodStatisMonitorImpl implements IServiceMethodStatisMonitor {

	private final static Logger logger = LoggerFactory.getLogger(ServiceMethodStatisMonitorImpl.class);

	@Inject
	private StatisDataListenerManager lisManager;
	
	@SuppressWarnings("unchecked")
	@Subscribe(topic=MonitorConstant.TEST_SERVICE_METHOD_TOPIC)
	public void statisMonitor(PSData psData) {
		
		Map<Integer,Double> ps = (Map<Integer,Double>)psData.getData();
		ServiceMethod sm = psData.get(Constants.SERVICE_METHOD_KEY);
		if(ps != null && !ps.isEmpty()) {
			//logger.info("type:{},val:{}",e.getKey(),e.getValue());
			lisManager.notifyData(sm, ps);
		}
		
	}
	
}
