package cn.jmicro.gateway.pubsub;

import cn.jmicro.api.pubsub.PSData;

public interface PSDataListener {

	void onMsg(PSData item);
	
	int getSubId();
	
	void setSubId(int id);
}
