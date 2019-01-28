package org.jmicro.api.pubsub;

import java.util.Map;

import org.jmicro.api.registry.UniqueServiceMethodKey;

public interface ISubsListener {
	
	public static final byte SUB_ADD = 3;
	public static final byte SUB_REMOVE = 4;
	
	void on(byte type,String topic,UniqueServiceMethodKey smKey,Map<String,String> context);
	
}
