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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午12:16:36
 */
public class ServiceCounter implements IServiceCounter{

	private static final int DEFAULT_SLOT_SIZE = 10;
	
	//服务唯一标识,粒度到服务方法,=服务名+名称空间+版本+方法名+方法参数标识
	private String serviceKey;
	
	private Set<Integer> supportTypes = new HashSet<>();
	
	//统计数件的类型,每种类型对应一种计数器
	private ConcurrentHashMap<Integer,Counter> counters = new ConcurrentHashMap<>();
	
	public ServiceCounter(String serviceKey) {
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		this.serviceKey = serviceKey;
	}
	
	public ServiceCounter(String serviceKey,Integer[] types,long timeWindow,long slotSizeInMilliseconds,TimeUnit unit) {
		this(serviceKey);
		supportTypes.addAll(Arrays.asList(types));
		for(Integer type : types) {
			addCounter(type,timeWindow,slotSizeInMilliseconds,unit);
		}
	}

	@Override
	public long get(Integer type) {
		Counter c = getCounter(type,false);
		if(c != null) {
			return  c.getVal();
		}
		//返回一个无效值，使用方应该判断此值是否有效才能使用
		return -1;
	}

	@Override
	public boolean add(Integer type, long val) {
		Counter c = getCounter(type,false);
		if(c != null) {
			c.add(val);
			return true;
		}
		//失败
		return false;
	}

	@Override
	public Double getTotal(Integer... types) {
		double sum = 0;
		for(Integer type : types) {
			Counter c = getCounter(type,false);
			if(c == null) {
				//返回一个无效值，使用方应该判断此值是否有效才能使用
				return -1D;
			}
			sum += getCounter(type,true).getTotal();
		}
		return sum;
	}
	
	public double getAvg(int type,TimeUnit unit) {
		Counter c = getCounter(type,false);
		if(c != null) {
			return c.getAvg(unit);
		}
		return -1;
	}

	@Override
	public boolean increment(Integer type) {
		Counter c = getCounter(type,false);
		if(c != null) {
			c.add(1);
			return true;
		}
		return false;
	}
	
	//***********************************************************//

	public double getAvgWithEx(int type,TimeUnit unit) {
		return getCounter(type,true).getAvg(unit);
	}
	
	@Override
	public long getWithEx(Integer type) {
		return getCounter(type,true).getVal();
	}
	
	public Double getTotalWithEx(Integer... types) {
		double sum = 0;
		for(Integer type : types) {
			sum += getCounter(type,true).getTotal();
		}
		return sum;
	}
	
	private Counter getCounter(int type,boolean withEx) {
		Counter c = counters.get(type);
		if(c == null && withEx) {
			throw new CommonException("Type not support for service :"
					+ this.serviceKey + ",type="+Integer.toHexString(type));
		}
		return c;
	}
	
	/**
	 * 增加指定值
	 */
	public void addWithEx(Integer type,long val) {
		getCounter(type,true).add(val);
	}
	
	/**
	 * 自增1
	 */
	public void incrementWithEx(Integer type) {
		this.addWithEx(type, 1);
	}
	
	@Override
	public boolean addCounter(Integer type, long timeWindow, long slotSize, String unit) {
		return this.addCounter(type, timeWindow, slotSize, TimeUtils.getTimeUnit(unit));
	}

	public boolean addCounter(Integer type,long timeWindow,long slotSizeInMilliseconds,TimeUnit unit) {
		if(this.counters.containsKey(type)) {
			throw new CommonException("Type["+type+"] exists for service ["+this.serviceKey+"]");
		}
		
		timeWindow = TimeUtils.getMilliseconds(timeWindow, unit);
		slotSizeInMilliseconds = TimeUtils.getMilliseconds(slotSizeInMilliseconds, unit);
		
		if(timeWindow % slotSizeInMilliseconds != 0) {
			throw new CommonException("timeWindow%slotSizeInMilliseconds must be zero,but:" +
					timeWindow + "%" + slotSizeInMilliseconds+"="+(timeWindow % slotSizeInMilliseconds));
		}
		String key = serviceKey+"-"+type;
		
		Counter cnt = new Counter(timeWindow,slotSizeInMilliseconds);
		TimerTicker.getDefault(slotSizeInMilliseconds).addListener(key, cnt,null);
		this.counters.put(type, cnt);
		supportTypes.add(type);
		return true;
	}
	
	static class Counter implements ITickerAction{
		
		//时间窗口,单位毫秒,统计只保持时间窗口内的数据,超过时间窗口的数据自动丢弃
		//统计数据平滑向前移动,单位毫秒
		private final long timeWindow;
		
		//每个槽位占用时间长度,单位毫秒
		private final long slotSizeInMilliseconds;
		
		private final int slotLen;
		
		//private final String id;
		
		private final Slot[] slots;
		
		private int header = -1;
		
		private final ReentrantLock locker = new ReentrantLock();
		
		private AtomicLong total = new  AtomicLong(0);
		
		/**
		 * 
		 * @param timeWindow 统计时间总长值，单位是毫秒
		 * @param slotSizeInMilliseconds 每个槽位所占时间长度，最大值等于timeWindow,
		 *     并且是必须满足timeWindow=N*slotSizeInMilliseconds, N是非负整数，N就是槽位个数
		 *     
		 */
		public Counter(long timeWindow,long slotSizeInMilliseconds) {
			//this.id = id;
			this.timeWindow = timeWindow;
			this.slotSizeInMilliseconds = slotSizeInMilliseconds;
			
			//计算槽位个数
			this.slotLen = (int)(timeWindow / slotSizeInMilliseconds);
			this.slots = new Slot[this.slotLen];
			
			this.header = 0;
			
			long curTime = System.currentTimeMillis();
			for(int i = 0; i < this.slotLen; i++) {
				this.slots[i] = new Slot(curTime + slotSizeInMilliseconds*i,0);
			}
		}
		
		@Override
		public void act(String key,Object attachement) {
			/*
			 * 不用加锁，因为时间判断上不可能与当前使用的槽位重复
			 */
			long curTime = System.currentTimeMillis();
			for(int i = 0; i < this.slots.length; i++) {
				if(curTime - this.slots[i].getTimeStart() > timeWindow ) {
					//槽位时间值已经超过时间窗口，重置之
					this.slots[i].reset();
				}
			}
		}
		
		/**
		 * 当前窗口同平均值，总值除时间窗口
		 * @return
		 */
		public double getAvg(TimeUnit timeUnit) {
			long sum = getVal();
			long time = this.timeWindow;
			
			if(timeUnit == TimeUnit.SECONDS) {
				time = TimeUnit.MILLISECONDS.toSeconds(this.timeWindow);
			}else if(timeUnit == TimeUnit.MINUTES) {
				time = TimeUnit.MILLISECONDS.toMinutes(this.timeWindow);
			}else if(timeUnit == TimeUnit.HOURS) {
				time = TimeUnit.MILLISECONDS.toHours(this.timeWindow);
			}else if(timeUnit == TimeUnit.DAYS) {
				time = TimeUnit.MILLISECONDS.toDays(this.timeWindow);
			}else if(timeUnit == TimeUnit.MICROSECONDS) {
				time = TimeUnit.MILLISECONDS.toMicros(this.timeWindow);
			}else if(timeUnit == TimeUnit.NANOSECONDS) {
				time = TimeUnit.MILLISECONDS.toNanos(this.timeWindow);
			}
			
			return ((double)sum)/time;
		}

		/**
		 * 总值，直接计算全部槽位值的总和，如果有过期槽位，上面已经设置为0，所以没有影响
		 * @return
		 */
		public long getVal() {
			long sum = 0;
			for(Slot b : slots) {
				sum += b.getVal();
			}
			return sum;
		}
		
		/**
		 * 递增1
		 */
		public void increment() {
			total.addAndGet(1);
			currentSlot().increment();
		}
		
		/**
		 * 增加指定值
		 * @param v
		 */
		public void add(long v) {
			total.addAndGet(v);
			currentSlot().add(v);
		}
		
		private Slot currentSlot() {
			long curTime = System.currentTimeMillis();
			Slot slot = slots[header];
			if(curTime < slot.getTimeStart() + slotSizeInMilliseconds) {
				//当前槽位还在时间窗口内,直接可以返回
				return slot;
			} else {
				boolean isLock = false;
				try {
					if(isLock = locker.tryLock(5, TimeUnit.MILLISECONDS)) {
						slot = slots[header];
						if(curTime < slot.getTimeStart() + slotSizeInMilliseconds) {
							//线程等待锁时,已经有另外一个线程在重置当前旋转木马
							return slot;
						}	
						int curHeader = header;
						//当前槽位已经过时,新建之,过去的时间可能跨过了多个槽位
						while(curTime > slot.getTimeStart() + slotSizeInMilliseconds){
							//循环转,直到进入当前时间窗口
							header = (header+1) % slotLen;
							slots[header].reset();
							if(curHeader == header ){
								//转了一圈,全部槽位都已经无效,直接从当前时间开始计算,防止长时间没使用之后的空转
								slots[header].setTimeStart(curTime + slotSizeInMilliseconds);
								break;
							} else {
								slots[header].setTimeStart(slot.getTimeStart() + slotSizeInMilliseconds);
								slot = slots[header];
							}
						}	
						return slots[header];
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}finally {
					if(isLock) {
						locker.unlock();
					}
				}
				//没锁成功,返回之前的槽位,此时算在计算误差范围内
				return slot;
			}
		}

		public long getTotal() {
			return total.get();
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
			this.val.getAndAdd(v);
		}
		
		public void increment() {
			this.add(1);
		}
		
	}
	
}





