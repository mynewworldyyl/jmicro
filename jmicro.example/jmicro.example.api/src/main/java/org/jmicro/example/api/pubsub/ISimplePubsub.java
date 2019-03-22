package org.jmicro.example.api.pubsub;

import org.jmicro.api.pubsub.PSData;

public interface ISimplePubsub {

	void helloTopic(PSData data);
	
	void statis(PSData data);
	
	void testTopic(PSData data);
	
}
