package cn.jmicro.mng.impl;

import java.util.Map;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.pubsub.IPubSubClientService;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;

@Component
@Service(namespace="mng", version="0.0.1", external=true, debugMode=0, showFront=false)
public class PubSubClientServiceImpl implements IPubSubClientService {

	@Inject
	private PubSubManager psMng;
	
	@Override
	@SMethod(needLogin=true,perType=true)
	public int callService(String topic, Object[] args) {
		return psMng.publish(topic, PSData.FLAG_PUBSUB, args);
	}

	@Override
	@SMethod(needLogin=true,perType=true)
	public int publishString(Map<String, Object> itemContext, String topic, String content) {
		return psMng.publish(itemContext, topic, content, PSData.FLAG_PUBSUB);
	}

	@Override
	@SMethod(needLogin=true,perType=true)
	public int publishBytes(Map<String, Object> itemContext, String topic, byte[] content) {
		return psMng.publish(itemContext, topic, content, PSData.FLAG_PUBSUB);
	}

	@Override
	@SMethod(needLogin=true,perType=true)
	public int publishMutilItems(PSData[] items) {
		return psMng.publish(items);
	}

	@Override
	@SMethod(needLogin=true,perType=true)
	public int publishOneItem(PSData item) {
		return psMng.publish(item);
	}

}
