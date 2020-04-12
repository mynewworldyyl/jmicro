package org.jmicro.example.pubsub.impl;

import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.annotation.Subscribe;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.example.api.pubsub.ISimplePubsub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class SimplePubsubImpl implements ISimplePubsub {

	private final static Logger logger = LoggerFactory.getLogger(SimplePubsubImpl.class);

	@Subscribe(topic="/jmicro/test/topic01"+Constants.TOPIC_SEPERATOR+"/jmicro/test/topic02")
	public void helloTopicWithArrayArgs(PSData[] data) {
		System.out.println("helloTopicWithArrayArgs: "+data[0].getTopic()+", size: "+ data.length );
	}
	
	//@Subscribe(topic="/jmicro/test/topic01")
	public void helloTopic(PSData data) {
		System.out.println("helloTopic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	@Subscribe(topic="/jmicro/test/topic02")
	public void testTopic(PSData data) {
		System.out.println("testTopic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	@Subscribe(topic=MonitorConstant.TEST_SERVICE_METHOD_TOPIC)
	public void statis(PSData data) {
		
		Map<Short,Double> ps = (Map<Short,Double>)data.getData();
		
		logger.info("总请求:{}, 总响应:{}, TO:{}, QPS:{}"
				,ps.get(MonitorConstant.REQ_START)
				,ps.get(MonitorConstant.STATIS_TOTAL_RESP)
				,ps.get(MonitorConstant.REQ_TIMEOUT)
				//,ps.get(MonitorConstant.req)
				,ps.get(MonitorConstant.REQ_START)
				);
		
		//System.out.println("Topic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	public void notifyMessageStatu(int statusCode,long msgId,Map<String,Object> cxt) {
		System.out.println("Message StatusCode: "+statusCode+", msgId: "+ msgId+",cxt:" + JsonUtils.getIns().toJson(cxt));
	}
	
}
