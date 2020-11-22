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
package cn.jmicro.api.timer;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午11:00:28
 */
public class TimerTicker {
	private static final Logger logger = LoggerFactory.getLogger(TimerTicker.class);
	private static final Map<Long,TimerTicker> defaultTimers = new ConcurrentHashMap<>();
	
	private boolean openDebug = false;
	
	public static final long BASE_TIME_TICK = 1000;
	
	public static TimerTicker getDefault(Long ticker) {
		return getTimer(defaultTimers,ticker);
	}
	
	public static TimerTicker getBaseTimer() {
		if(defaultTimers.containsKey(BASE_TIME_TICK)) {
			return defaultTimers.get(BASE_TIME_TICK);
		} else {
			defaultTimers.put(BASE_TIME_TICK, new TimerTicker(BASE_TIME_TICK));
			return defaultTimers.get(BASE_TIME_TICK);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void doInBaseTicker(int fact, String key, Object attachement, ITickerAction act) {
		if(fact <= 0) {
			throw new CommonException("Invalid fact: " + fact);
		}
		final int[] checkCnt = new int[1];
		checkCnt[0] = 0;
		TimerTicker.getBaseTimer().addListener(key, attachement, (key0,att0)->{
			checkCnt[0]++;
			if( (checkCnt[0] % fact) == 0) {
				checkCnt[0] = 0;
				act.act(key0, att0);
			}
		});
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
	long lastCostTime = 0;
	
	public TimerTicker(long ticker) {
		this.ticker = ticker;
		if(ticker < 99) {
			//logger.warn("Ticker:" + ticker);
			throw new CommonException("Ticker have to big to: " + 99+", but got: "+ticker);
		}
		
		logger.debug("Ticker: " + ticker);
		
		timer = new Timer("JMicro-Timer-"+ticker, true);
		
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					long beginTime = TimeUtils.getCurTime();
				
					long interval = beginTime - lastRunTime; //允许10毫秒误差
					
					if(interval < ticker) {
						//logger.debug("Timer ["+interval+"] small ticker ["+ticker+"] from last run!");
						return;
					}
					
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
					
					lastRunTime = TimeUtils.getCurTime();
					lastCostTime = lastRunTime - beginTime;
					if(lastCostTime > ticker) {
						logger.debug("Action cost time ["+lastCostTime+"] more than ["+ticker+"]!");
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
		}
		
		
	}
	
	@SuppressWarnings("rawtypes")
	public void addListener(String key,Object attachement,ITickerAction act) {
		addListener(key,attachement,false,act);
	}
	
	@SuppressWarnings("rawtypes")
	public void addListener(String key,Object attachement,boolean replace,ITickerAction act) {
		if(this.listeners.containsKey(key)) {
			if(act != this.listeners.get(key) && !replace) {
				throw new CommonException("listener with key[" + key+"] have been exists");
			}
			logger.warn("Replace Listener: " + key);
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
