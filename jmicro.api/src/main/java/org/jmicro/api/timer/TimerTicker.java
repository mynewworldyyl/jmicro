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
package org.jmicro.api.timer;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午11:00:28
 */
public class TimerTicker {
	private static final Logger logger = LoggerFactory.getLogger(TimerTicker.class);
	private static final Map<Long,TimerTicker> defaultTimers = new ConcurrentHashMap<>();
	
	private boolean openDebug = false;
	
	public static TimerTicker getDefault(Long ticker) {
		return getTimer(defaultTimers,ticker);
	}
	
	public static TimerTicker getTimer(Map<Long,TimerTicker> timers,Long ticker) {
		if(timers.containsKey(ticker)) {
			return timers.get(ticker);
		} else {
			timers.put(ticker, new TimerTicker(ticker));
			return timers.get(ticker);
		}
	}
	
	private long ticker;
	private Timer timer;
	
	private Map<String,ITickerAction> listeners = new ConcurrentHashMap<>();
	private Map<String,Object> attachements = new ConcurrentHashMap<>();
	private Queue<String> removeKeys = new ConcurrentLinkedQueue<>();
	
	long lastRunTime = 0;
	
	public TimerTicker(long ticker) {
		this.ticker = ticker;
		if(ticker < 99) {
			//logger.warn("Ticker:" + ticker);
			throw new CommonException("Ticker have to big to: " + 99+", but got: "+ticker);
		}
		
		timer = new Timer("JMicro-Timer-"+ticker, true);
		
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				
				try {
					notifyAction(); 
					if(!removeKeys.isEmpty()) {
						for(;!removeKeys.isEmpty();) {
							String k = removeKeys.poll();
							listeners.remove(k);
							attachements.remove(k);
							if(openDebug) {
								logger.info("Remote Action: "+ k);
							}
						}
					}
				} catch (Throwable e) {
					logger.error("JMicro TimerTicker.scheduleAtFixedRate",e);
				}
			}
		}, 0, ticker);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void notifyAction() {
		/*listeners.forEach((key,act)->{
			if(System.currentTimeMillis() - lastRunTime < 100) {
				logger.warn("Timer interval small to 100ms from last run: " + ticker+", key: "+key);
			}
			act.act(key,attachements.get(key));
		});*/
		
		for(Iterator<Map.Entry<String, ITickerAction>> ite = listeners.entrySet().iterator(); ite.hasNext();) {
			Entry<String, ITickerAction> kv = ite.next();
			kv.getValue().act(kv.getKey(), attachements.get(kv.getKey()));
			if(System.currentTimeMillis() - lastRunTime < 10) {
				logger.warn("Timer interval small to 100ms from last run: " + ticker+", key: "+kv.getKey());
			}
		}
		
		lastRunTime = System.currentTimeMillis();
	}
	
	public void addListener(String key,ITickerAction act,Object attachement) {
		addListener(key,act,attachement,false);
	}
	
	public void addListener(String key,ITickerAction act,Object attachement,boolean replace) {
		if(this.listeners.containsKey(key) && act != this.listeners.get(key)) {
			if(!replace) {
				throw new CommonException("listener with key[" + key+"] have been exists");
			} else {
				logger.warn("Replace Listener: " + key);
			}
		}
		if(attachement != null) {
			attachements.put(key, attachement);
		}
		this.listeners.put(key, act);
	}
	
	public void removeListener(String key,boolean atonce) {
		if(atonce) {
			listeners.remove(key);
			attachements.remove(key);
		} else {
			removeKeys.offer(key);
		}
	}
	
	public boolean container(String key) {
		return listeners.containsKey(key);
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.println("finalize: "+timer.toString());
		super.finalize();
	}

	public TimerTicker setOpenDebug(boolean openDebug) {
		this.openDebug = openDebug;
		return this;
	}

	
	
}
