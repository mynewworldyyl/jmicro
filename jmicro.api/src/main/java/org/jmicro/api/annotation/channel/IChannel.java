package org.jmicro.api.annotation.channel;

public interface IChannel<T> {

	boolean push(T msg);
	
	void addListener(IChannelListener<T> listener);
		
}
