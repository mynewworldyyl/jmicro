package org.jmicro.api.annotation.channel;

public interface IChannelListener<T> {

	String name();
	
	void onMessage(T msg);
}
