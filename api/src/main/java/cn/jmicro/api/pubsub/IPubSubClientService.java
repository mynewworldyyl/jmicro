package cn.jmicro.api.pubsub;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPubSubClientService {

	
/*	int callService(String topic, Object[] args,byte flag, Map<String,Object> itemContext);
	
	int publishString(String topic, String content,byte flag, Map<String,Object> itemContext);
	
	int publishBytes(String topic, byte[] content,byte flag, Map<String,Object> itemContext);*/
	
	int publishMutilItems(PSData[] items);
	
	int publishOneItem(PSData item);
}
