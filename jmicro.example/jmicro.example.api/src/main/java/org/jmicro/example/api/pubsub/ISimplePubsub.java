package org.jmicro.example.api.pubsub;

import java.util.Map;

import org.jmicro.api.pubsub.PSData;

public interface ISimplePubsub {

	void helloTopic(PSData data);
	
	void statis(PSData data);
	
	void testTopic(PSData data);
	
	void helloTopicWithArrayArgs(PSData[] data);
	
	void notifyMessageStatu(int statusCode,long msgId,Map<String,Object> cxt);
	
}
