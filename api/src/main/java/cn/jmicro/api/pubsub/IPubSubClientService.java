package cn.jmicro.api.pubsub;

import java.util.Map;

import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPubSubClientService {

	int callService(String topic, Object[] args);
	
	int publishString(Map<String,Object> itemContext, String topic, String content);
	
	int publishBytes(Map<String,Object> itemContext, String topic, byte[] content);
	
	int publishMutilItems(PSData[] items);
	
	int publishOneItem(PSData item);
}
