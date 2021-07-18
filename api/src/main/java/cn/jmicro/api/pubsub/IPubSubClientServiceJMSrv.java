package cn.jmicro.api.pubsub;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPubSubClientServiceJMSrv {

	
/*	int callService(String topic, Object[] args,byte flag, Map<String,Object> itemContext);
	
	int publishString(String topic, String content,byte flag, Map<String,Object> itemContext);
	
	int publishBytes(String topic, byte[] content,byte flag, Map<String,Object> itemContext);*/
	
	int publishMutilItems(PSDataJRso[] items);
	
	int publishOneItem(PSDataJRso item);
}
