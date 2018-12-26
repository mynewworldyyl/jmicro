package org.jmicro.api.pubsub;

import java.util.Map;

public interface ITopicListener {

	public static final byte TOPIC_ADD = 1;
	public static final byte TOPIC_REMOVE = 2;
	
	void on(byte type,String path,Map<String,String> context);
	
}
