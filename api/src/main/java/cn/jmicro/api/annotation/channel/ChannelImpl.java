/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.api.annotation.channel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import cn.jmicro.common.CommonException;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:59:36
 */
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
			throw new CommonException(0,"Channel listner "+listener.name()+" exist");
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
