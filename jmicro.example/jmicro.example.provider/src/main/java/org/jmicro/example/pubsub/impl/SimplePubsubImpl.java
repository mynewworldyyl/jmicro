package org.jmicro.example.pubsub.impl;

import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.annotation.Subscribe;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.common.Constants;
import org.jmicro.example.api.pubsub.ISimplePubsub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class SimplePubsubImpl implements ISimplePubsub {

	private final static Logger logger = LoggerFactory.getLogger(SimplePubsubImpl.class);

	@Subscribe(topic="/jmicro/test/topic01")
	public void helloTopic(PSData data) {
		System.out.println("helloTopic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	@Subscribe(topic="/jmicro/test/topic02")
	public void testTopic(PSData data) {
		System.out.println("testTopic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	@Subscribe(topic=MonitorConstant.TEST_SERVICE_METHOD_TOPIC)
	public void statis(PSData data) {
		
		Map<Integer,Double> ps = (Map<Integer,Double>)data.getData();
		
		logger.info("总请求:{}, 总响应:{}, TO:{}, TOF:{}, QPS:{}"
				,ps.get(MonitorConstant.REQ_START)
				,ps.get(MonitorConstant.STATIS_TOTAL_RESP)
				,ps.get(MonitorConstant.REQ_TIMEOUT)
				//,ps.get(MonitorConstant.req)
				,ps.get(MonitorConstant.STATIS_QPS)
				);
		
		//System.out.println("Topic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
}
