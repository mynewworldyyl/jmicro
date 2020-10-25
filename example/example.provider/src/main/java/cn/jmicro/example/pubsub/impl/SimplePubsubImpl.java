package cn.jmicro.example.pubsub.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.annotation.Subscribe;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.example.api.pubsub.ISimplePubsub;

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
	
	@Subscribe(topic=MC.TEST_SERVICE_METHOD_TOPIC)
	public void statis(PSData data) {
		
		Map<Short,Double> ps = (Map<Short,Double>)data.getData();
		
		logger.info("总请求:{}, 总响应:{}, TO:{}, QPS:{}"
				,ps.get(MC.MT_REQ_START)
				,ps.get(MC.STATIS_TOTAL_RESP)
				,ps.get(MC.MT_REQ_TIMEOUT)
				//,ps.get(MonitorConstant.req)
				,ps.get(MC.MT_REQ_START));
		
		//System.out.println("Topic: "+data.getTopic()+", data: "+ data.getData().toString());
	}
	
	public void notifyMessageStatu(int statusCode,long msgId,Map<String,Object> cxt) {
		System.out.println("Message StatusCode: "+statusCode+", msgId: "+ msgId+",cxt:" + JsonUtils.getIns().toJson(cxt));
	}
	
}
