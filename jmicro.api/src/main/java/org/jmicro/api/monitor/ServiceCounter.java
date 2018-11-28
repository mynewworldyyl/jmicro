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
package org.jmicro.api.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.common.CommonException;
import org.jmicro.common.TimerTicker;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午12:16:36
 */
public class ServiceCounter implements IServiceCounter{

	//服务唯一标识,粒度到服务方法,=服务名+名称空间+版本+方法名+方法参数标识
	private String serviceKey;
	
	//统计数件的类型,每种类型对应一种计数器
	private ConcurrentHashMap<Integer,Counter> counters = new ConcurrentHashMap<>();
	
	public ServiceCounter(String serviceKey) {
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		this.serviceKey = serviceKey;
	}

	@Override
	public long get(Integer type) {
		Counter c = counters.get(type);
		if(c != null) {
			return c.getVal();
		} else {
			throw new CommonException("Type not support for service :"+this.serviceKey);
		}
	}
	
	public void add(Integer type,long val) {
		Counter c = counters.get(type);
		if(c != null) {
			c.add(val);
		} else {
			throw new CommonException("Type not support for service :" + this.serviceKey);
		}
	}
	
	public void increment(Integer type) {
		this.add(type, 1);
	}
	
	public void addCount(Integer type,long timeWindow,long slotSize) {
		if(this.counters.containsKey(type)) {
			throw new CommonException("Type["+type+"] exists for service ["+this.serviceKey+"]");
		}
		this.counters.put(type, new Counter(serviceKey+"-"+type,timeWindow,slotSize));
	}
	
	static class Counter {
		
		//时间窗口,单位毫秒,统计只保持时间窗口内的数据,超过时间窗口的数据自动丢弃
		//统计数据平滑向前移动
		private final long timeWindow;
		
		//每个槽位占用时间长度,单位毫秒
		private final long slotSizeInMilliseconds;
		
		private final String id;
		
		private final Slot[] slots;
		
		private int header = -1;
		
		private final int slotLen;
		
		public Counter(String id,long timeWindow,long slotSizeInMilliseconds) {
			this.id = id;
			this.timeWindow = timeWindow;
			this.slotSizeInMilliseconds = slotSizeInMilliseconds;
			if(timeWindow%slotSizeInMilliseconds != 0) {
				throw new CommonException("timeWindow%slotSizeInMilliseconds must be zero,but:" +
						timeWindow + "%" + slotSizeInMilliseconds+"="+(timeWindow % slotSizeInMilliseconds));
			}
			
			this.slotLen = (int)(timeWindow / slotSizeInMilliseconds);
			this.slots = new Slot[this.slotLen];
			
			this.header = 0;
			
			long curTime = System.currentTimeMillis();
			for(int i = 0; i < this.slotLen; i++) {
				this.slots[i] = new Slot(curTime + slotSizeInMilliseconds,0);
			}
			
			TimerTicker.getTimer(slotSizeInMilliseconds).addListener(id, () -> doTicker());
			
		}
		
		private void doTicker() {
			long curTime = System.currentTimeMillis();
			for(int i = 0; i < this.slots.length; i++) {
				if( curTime - this.slots[i].getTimeStart() > timeWindow ) {
					this.slots[i].reset();
				}
			}
		}

		public long getVal() {
			long sum = 0;
			for(Slot b : slots) {
				sum += b.getVal();
			}
			return sum;
		}
		
		public void increment() {
			currentSlot().increment();
		}
		
		public void add(long v) {
			currentSlot().add(v);
		}
		
		private Slot currentSlot() {
			long curTime = System.currentTimeMillis();
			Slot slot = slots[header];
			if(curTime < slot.getTimeStart() + slotSizeInMilliseconds) {
				//当前槽位还在时间窗口内,直接可以返回
				return slot;
			} else {
				int curHeader = header;
				//当前槽位已经过时,新建之,过去的时间可能跨过了多个槽位
				while(curTime > slot.getTimeStart() + slotSizeInMilliseconds){
					//循环转，直到进入当前时间窗口
					header = (header+1) % slotLen;
					slots[header].reset();
					if(curHeader == header ){
						//转了一圈，全部槽位都已经无效，直接从当前时间开始计算，防止长时间没使用之后的空转
						slots[header].setTimeStart(curTime + slotSizeInMilliseconds);
						break;
					} else {
						slots[header].setTimeStart(slot.getTimeStart() + slotSizeInMilliseconds);
						slot = slots[header];
					}
				}	
				return slots[header];
			}
		}
	}
	
	static class Slot {
		
		private long timeStart;
		
		private AtomicLong val;
		
		public Slot(long startTime,long v) {
			this.timeStart = startTime;
			this.val = new AtomicLong();
		}
		
		public long getVal() {
			return val.get();
		}

		public long getTimeStart() {
			return timeStart;
		}

		public void setTimeStart(long timeStart) {
			this.timeStart = timeStart;
		}

		public void reset() {
			this.val.set(0);
		}
		
		public void add(long v) {
			this.val.addAndGet(v);
		}
		
		public void increment() {
			this.add(1);
		}
		
	}
	
}





