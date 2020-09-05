package cn.jmicro.mng.impl;

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
	
	/*@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=100,maxPacketSize=4096)
	public int callService(String topic, Object[] args, byte flag,Map<String, Object> itemContext) {
		return psMng.publish(topic, args,flag,itemContext);
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=100,maxPacketSize=9192)
	public int publishString(String topic, String content,byte flag,Map<String, Object> itemContext) {
		return psMng.publish(topic, content, flag,itemContext);
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=100,maxPacketSize=16384)
	public int publishBytes(String topic, byte[] content,byte flag,Map<String, Object> itemContext) {
		return psMng.publish(topic, content, flag,itemContext);
	}*/

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=100,maxPacketSize=9192)
	public int publishMutilItems(PSData[] items) {
		return psMng.publish(items);
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=100,maxPacketSize=9192)
	public int publishOneItem(PSData item) {
		return psMng.publish(item);
	}

}
