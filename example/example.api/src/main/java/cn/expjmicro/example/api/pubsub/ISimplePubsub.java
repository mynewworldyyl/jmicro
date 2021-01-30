package cn.expjmicro.example.api.pubsub;

import java.util.Map;

import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISimplePubsub {

	void helloTopic(PSData data);
	
	void statis(PSData data);
	
	void testTopic(PSData data);
	
	void helloTopicWithArrayArgs(PSData[] data);
	
	void notifyMessageStatu(int statusCode,long msgId,Map<String,Object> cxt);
	
}
