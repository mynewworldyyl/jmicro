package org.jmicro.example.api;

import org.jmicro.api.annotation.Service;
import org.jmicro.api.pubsub.PSData;

@Service(namespace="testsayhello",version="0.0.1", monitorEnable=1)
public interface ISayHello {
	String hello(String name);
	
	void helloTopic(PSData data);
	
	void statis(PSData data);
}
