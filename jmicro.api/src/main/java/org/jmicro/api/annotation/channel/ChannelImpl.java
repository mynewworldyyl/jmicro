package org.jmicro.api.annotation.channel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Channel;
import org.jmicro.api.exception.CommonException;

@Channel("defaultChannel")
public class ChannelImpl<T> implements IChannel<T> {

	private volatile List<IChannelListener<T>> listeners = new ArrayList<IChannelListener<T>>();
	
	private volatile Map<String,Object> names = new ConcurrentHashMap<String,Object>();
	
	private volatile LinkedList<T> messages = null;
	
	private int cacheSize = 0; 
	
	public synchronized boolean push(T msg) {
		if( this.listeners.isEmpty()) {
			if(this.cacheSize > 0) {
				if(messages.size() >= this.cacheSize){
					return false;
				}
			}
		} else {
			this.triggerListener(msg);
		}
		return false;
	}

	private void triggerListener(T msg) {
		Iterator<IChannelListener<T>> ite = this.listeners.iterator();
		for(;ite.hasNext();) {
			ite.next().onMessage(msg);
		}
	}

	public synchronized void addListener(IChannelListener<T> listener) {
		if(this.isExist(listener.name())) {
			throw new CommonException("channelListenerNameRepeat","Channel listner "+listener.name()+" exist");
		}
		this.listeners.add(listener);
		names.put(listener.name(), "");
		
		if(messages != null && !this.messages.isEmpty()){
			try {
				while(true) {
					T msg =  messages.pop();
					this.triggerListener(msg);
				}
			} catch (NoSuchElementException e) {	
			}
		}
	}
	
	private boolean isExist(String name) {
		return names.containsKey(name);
	}

}
