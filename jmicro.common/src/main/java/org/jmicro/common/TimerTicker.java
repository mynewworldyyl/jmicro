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
package org.jmicro.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午11:00:28
 */
public class TimerTicker {

	private static final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	public static TimerTicker getTimer(Long ticker) {
		if(timers.containsKey(ticker)) {
			return timers.get(ticker);
		}else {
			timers.put(ticker, new TimerTicker(ticker));
			return timers.get(ticker);
		}
	}
	
	//private long ticker;
	private Timer timer;
	
	private Map<String,TickerAction> listeners = new HashMap<>();
	
	public TimerTicker(long ticker) {
		//this.ticker = ticker;
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				notifyAction();
			}
		}, 0, ticker);
	}
	
	private void notifyAction() {
		listeners.forEach((key,act)->{
			act.on();
		});
	}
	
	public void addListener(String key,TickerAction act) {
		if(this.listeners.containsKey(key) && act != this.listeners.get(key)) {
			throw new CommonException("listener with key[" + key+"] have been exists");
		}
		this.listeners.put(key, act);
	}
	
	public void removeListener(String key) {
		listeners.remove(key);
	}

	public static interface TickerAction{
		void on();
	}
	

}
