package cn.jmicro.gateway.pubsub;

import cn.jmicro.api.pubsub.PSDataJRso;

public interface PSDataListener {

	void onMsg(PSDataJRso item);
	
	int getSubId();
	
	void setSubId(int id);
}
