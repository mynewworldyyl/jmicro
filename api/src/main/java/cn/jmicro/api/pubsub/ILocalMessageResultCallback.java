package cn.jmicro.api.pubsub;

import java.util.Map;

public interface ILocalMessageResultCallback {

	void callback(int resultCode,long msgId,Map<String,Object> context);
}
