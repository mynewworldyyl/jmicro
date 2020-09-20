package cn.jmicro.pubsub;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.internal.pubsub.IInternalSubRpc;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.pubsub.IPubSubClientService;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.security.ActInfo;

@Component
@Service(namespace="mng", version="0.0.1", external=true, debugMode=0, showFront=false)
public class PubSubClientServiceImpl implements IPubSubClientService {

	@Inject
	private PubSubManager psMng;
	
	@Inject(required=false)
	private IInternalSubRpc psServer;
	
	@Inject
	private ProfileManager pm;
	
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
	@SMethod(perType=true,needLogin=true,maxSpeed=50,maxPacketSize=81920)
	public int publishMutilItems(PSData[] items) {
		if(items.length == 0 || items.length > 10) {
			return PSData.INVALID_ITEM_COUNT;
		}

		ActInfo ai = JMicroContext.get().getAccount();
		if(pm.getVal(ai.getClientId(),  PubSubManager.PROFILE_PUBSUB, "needPersist",false, Boolean.class)) {
			psMng.persist2Db(ai.getClientId(),items);
		}
		return psServer.publishItems(items[0].getTopic(),items);
	
		//return PSData.PUB_TOPIC_INVALID;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=50,maxPacketSize=8192)
	public int publishOneItem(PSData item) {
		ActInfo ai = JMicroContext.get().getAccount();
		if(item.isPersist()) {
			if(pm.getVal(item.getSrcClientId(), PubSubManager.PROFILE_PUBSUB, "needPersist",false, Boolean.class)) {
				psMng.persit2Db(ai.getClientId(),item);
			}
		}
		return psServer.publishItem(item);
	}

}
