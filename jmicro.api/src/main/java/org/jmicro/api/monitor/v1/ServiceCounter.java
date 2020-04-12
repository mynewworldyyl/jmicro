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
package org.jmicro.api.monitor.v1;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午12:16:36
 */
public class ServiceCounter implements IServiceCounter<Short>{

	private static final Logger logger = LoggerFactory.getLogger(ServiceCounter.class);
	
	private static final int DEFAULT_SLOT_SIZE = 10;
	
	private boolean openDebug = false;
	
	//服务唯一标识,粒度到服务方法,=服务名+名称空间+版本+方法名+方法参数标识
	private String serviceKey;
	
	private long slotSize;
	
	private long slotSizeInMilliseconds;
	
	private long timeWindowInMilliseconds;
	
	private Set<Short> supportTypes = new HashSet<>();
	
	private long timeWindow;
	
	private TimeUnit unit;
	
	//统计事件的类型,每种类型对应一种计数器
	private ConcurrentHashMap<Short,Counter> counters = new ConcurrentHashMap<>();
	
	
	/*public ServiceCounter(String serviceKey,long timeWindow,TimeUnit unit) {
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		this.serviceKey = serviceKey;
		this.timeWindow = timeWindow;
		this.unit = unit;
	}*/
	
	public ServiceCounter(String serviceKey,Short[] types,long timeWindow,long slotSize,TimeUnit unit) {
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		if(timeWindow <= 0) {
			throw new CommonException("Invalid timeWindow: " + timeWindow);
		}
		if(slotSize <= 0) {
			throw new CommonException("Invalid slotSize: " + slotSize);
		}
		
		this.serviceKey = serviceKey;
		this.unit = unit;
		this.slotSize = slotSize;
		this.timeWindow = timeWindow;
		
		timeWindowInMilliseconds = TimeUtils.getTime(timeWindow, unit,TimeUnit.MILLISECONDS);
		
		if(timeWindowInMilliseconds <= 0) {
			throw new CommonException("Invalid timeWindow to MILLISECONDS : " + timeWindow);
		}
		
		slotSizeInMilliseconds = timeWindowInMilliseconds / this.slotSize;
		
		if(slotSizeInMilliseconds == 0 || timeWindowInMilliseconds % slotSizeInMilliseconds != 0) {
			throw new CommonException("timeWindow%slotSizeInMilliseconds must be zero,but:" +
					timeWindow + "%" + slotSizeInMilliseconds+"="+(timeWindow % slotSizeInMilliseconds));
		}
		
		if(types != null && types.length > 0) {
			supportTypes.addAll(Arrays.asList(types));
			for(Short type : types) {
				addCounter(type);
			}
		}
	}
	
	public void destroy() {
		
	}

	@Override
	public long get(Short type) {
		Counter c = getCounter(type,false);
		if(c != null) {
			return  c.getVal();
		}
		//返回一个无效值，使用方应该判断此值是否有效才能使用
		return -1;
	}

	@Override
	public boolean add(Short type, long val) {
		Counter c = getCounter(type,false);
		if(c != null) {
			c.add(val);
			return true;
		}
		//失败
		return false;
	}

	@Override
	public Long getTotal(Short... types) {
		long sum = 0;
		for(Short type : types) {
			Counter c = getCounter(type,false);
			if(c == null) {
				//返回一个无效值，使用方应该判断此值是否有效才能使用
				return -1L;
			}
			sum += getCounter(type,true).getTotal();
		}
		return sum;
	}
	/**
	 * 
	 * @param tounit
	 * @param types
	 * @return
	 */
	public double getQps(TimeUnit tounit,Short... types) {
		if(types.length == 1) {
			Counter c = getCounter(types[0],false);
			if(c != null) {
				return c.getQps(tounit);
			}
		} else {
			double sum = getValueWithEx(types);
			long time = TimeUtils.getTime(this.timeWindow, this.unit, tounit);
			return ((double)sum)/time;
		}
		
		return -1;
	}

	@Override
	public boolean increment(Short type) {
		Counter c = getCounter(type,false);
		if(c != null) {
			c.add(1);
			return true;
		}
		return false;
	}
	
	//***********************************************************//

	public double getQpsWithEx(Short type,TimeUnit unit) {
		return getCounter(type,true).getQps(unit);
	}
	
	@Override
	public long getWithEx(Short type) {
		return getCounter(type,true).getVal();
	}
	
	/**
	 * 全部类型的当前值的和
	 */
	public Double getValueWithEx(Short... types) {
		double sum = 0;
		for(Short type : types) {
			sum += getCounter(type,true).getVal();
		}
		return sum;
	}
	
	private Counter getCounter(Short type,boolean withEx) {
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
	public void addWithEx(Short type,long val) {
		getCounter(type,true).add(val);
	}
	
	/**
	 * 自增1
	 */
	public void incrementWithEx(Short type) {
		this.addWithEx(type, 1);
	}
	
	/*@Override
	public boolean addCounter(Integer type, long timeWindow, long slotSize, String unit) {
		return this.addCounter(type, timeWindow, slotSize, TimeUtils.getTimeUnit(unit));
	}*/

	public boolean addCounter(Short type) {
		if(this.counters.containsKey(type)) {
			throw new CommonException("Type["+type+"] exists for service ["+this.serviceKey+"]");
		}

		String key = serviceKey+"-"+type;
		
		Counter cnt = new Counter(type,timeWindowInMilliseconds,slotSizeInMilliseconds);
		TimerTicker.getDefault(slotSizeInMilliseconds).addListener(key, cnt,null,true);
		this.counters.put(type, cnt);
		supportTypes.add(type);
		return true;
	}
	
	
	
	@Override
	public void getAll(Map<Short, Double> values) {
		if(values == null) {
			throw new NullPointerException("value is NULL");
		}
		
		for(Map.Entry<Short, Counter> c : this.counters.entrySet()) {
			values.put(c.getKey(), new Double(c.getValue().getVal()));
		}
		
	}

	/**
	 * 指定类型所占总请求数的百分比
	 * @param counter
	 * @param type
	 * @return
	 */
	public static double takePercent(ServiceCounter counter,Short type) {
		Long totalReq = counter.get(MonitorConstant.REQ_START);
		Long typeCount = counter.get(type);
		if(totalReq != 0) {
			return (typeCount*1.0/totalReq)*100;
		}else {
			return -1;
		}
	}
	
	public static double getData(ServiceCounter counter,Short type) {

		if(counter == null) {
			return 0D;
		}
		
		Double result = 0D;
		switch(type) {
		case MonitorConstant.STATIS_TOTAL_FAIL_PERCENT:
			Long totalReq = counter.get(MonitorConstant.REQ_START);
			if(totalReq != 0) {
				double totalFail = counter.getValueWithEx(MonitorConstant.CLIENT_SERVICE_ERROR,MonitorConstant.REQ_TIMEOUT);
				result = (totalFail/totalReq)*100;
				//logger.debug("totalReq:{},totalFail:{},Percent:{}",totalReq,totalFail,result);
			}
			break;
		case MonitorConstant.REQ_START:
			result = 1.0 * counter.getTotal(MonitorConstant.REQ_START);		
			break;
		case MonitorConstant.STATIS_TOTAL_RESP:
			result = new Double(counter.getTotal(MonitorConstant.REQ_END));
			break;
		case MonitorConstant.STATIS_TOTAL_SUCCESS:
			result =  1.0 * counter.getTotal(MonitorConstant.REQ_SUCCESS);
			break;
		case MonitorConstant.STATIS_TOTAL_FAIL:
			result = 1.0 * counter.getTotal(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR)+
			counter.getTotal(MonitorConstant.CLIENT_SERVICE_ERROR)+
			counter.getTotal(MonitorConstant.REQ_TOTAL_TIMEOUT_FAIL)+
			counter.getTotal(MonitorConstant.REQ_ERROR);
			break;
		case MonitorConstant.STATIS_TOTAL_SUCCESS_PERCENT:
			totalReq = counter.get(MonitorConstant.REQ_START);
			if(totalReq != 0) {
				result =  1.0 * counter.get(MonitorConstant.REQ_SUCCESS)/*+
						counter.get(MonitorConstant.CLIENT_REQ_OK)*/;
						result = (result*1.0/totalReq)*100;
			}
			break;
		case MonitorConstant.REQ_TOTAL_TIMEOUT_FAIL:
			result = 1.0 * counter.get(MonitorConstant.REQ_TOTAL_TIMEOUT_FAIL);
			break;
		case MonitorConstant.STATIS_TOTAL_TIMEOUT_PERCENT:
			totalReq = counter.get(MonitorConstant.REQ_START);
			if(totalReq != 0) {
				result = 1.0 * counter.get(MonitorConstant.REQ_TOTAL_TIMEOUT_FAIL);
				result = (result/totalReq)*100;
			}
			break;
		case MonitorConstant.REQ_START+1:
			result = counter.getQps(TimeUnit.SECONDS,MonitorConstant.REQ_START);
			break;
		default:
			result = counter.getValueWithEx(type);
			break;
		}
		return result;
	
	}
	
	/**
	 *                                                             Tx
	 *                   s0v             s1v               s2v     |       s3v 
	 * |                  |                |                |      |         |
	 * s0----------------s1---------------s2---------------s3---------------------->time zoone
	 * 
	 * s0,s1,s2,s3表示3个槽位，s0v,s1v,s2v,s3v表示有效时间最大值，Tx表示任意一个时间点。
	 * 
	 * 槽位有效时间之外的槽位都视为无效槽位
	 * 
	 * 任意时刻Tx，计算槽位Sx是否有效，假定Sxv表示任意槽位有效时间最大值
	 *  Tx－Sxv ＞　timewindow 即表示 Sx为无效槽位，将其值设置为０即可
	 *  
	 * 当前槽位定义为： Tx < Sxv 且  Tx > Sxv-slotSizeInMilliseconds
	 * 
	 */
	private class Counter implements ITickerAction{
		
		private final int type;
		//时间窗口,单位毫秒,统计只保持时间窗口内的数据,超过时间窗口的数据自动丢弃
		//统计数据平滑向前移动,单位毫秒
		private final long timeWindow;
		
		//每个槽位占用时间长度,单位毫秒
		private final long slotSizeInMilliseconds;
		
		private final int slotLen;
		
		//private final String id;
		
		private final Slot[] slots;
		
		private volatile int header = -1;
		
		private final ReentrantLock locker = new ReentrantLock();
		
		private volatile AtomicLong total = new  AtomicLong(0);
		
		/**
		 * 
		 * @param timeWindow 统计时间总长值，单位是毫秒
		 * @param slotSizeInMilliseconds 每个槽位所占时间长度，最大值等于timeWindow,
		 *     并且是必须满足timeWindow=N*slotSizeInMilliseconds, N是非负整数，N就是槽位个数
		 *     
		 */
		public Counter(int type,long timeWindow,long slotSizeInMilliseconds) {
			//this.id = id;
			this.type = type;
			this.timeWindow = timeWindow;
			this.slotSizeInMilliseconds = slotSizeInMilliseconds;
			
			//计算槽位个数
			this.slotLen = (int)(timeWindow / slotSizeInMilliseconds);
			this.slots = new Slot[this.slotLen];
			
			this.header = 0;
			
			long curTime = System.currentTimeMillis();
			//槽位 有效时间=slotSizeInMilliseconds ~ (timeStart + slotSizeInMilliseconds)
			//如果当前时间在有效时间内，则定义为当前槽位
			for(int i = 0; i < this.slotLen; i++) {
				this.slots[i] = new Slot(0,0);
			}
			
			this.slots[0].setTimeEnd(curTime+slotSizeInMilliseconds);
			if(openDebug) {
				logger.info("Create Counter type:{}, timeWindow:{},slotSizeInMilliseconds:{},slotLen:{},curTime:{}",this.type,
						this.timeWindow,this.slotSizeInMilliseconds,this.slotLen,curTime);
			}
			
			
		}
		
		/**
		 * QPS
		 * 当前窗口同平均值，总值除时间窗口
		 * @return
		 */
		boolean debugGetQps = false;
		//boolean fqps = false;
		public double getQps(TimeUnit timeUnit) {
			long sum = getVal();
			long time = TimeUtils.getTime(this.timeWindow, TimeUnit.MILLISECONDS, timeUnit);
			if(debugGetQps) {
				logger.info("sum:{},time:{}",sum,time);
			}
			return ((double)sum)/time;
		}

		/**
		 * 总值，直接计算全部槽位值的总和，如果有过期槽位，上面已经设置为0，所以没有影响
		 * @return
		 */
		public long getVal() {
			long sum = 0;
			for(Slot s : slots) {
				sum += s.getVal();
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
		
		/**
		 * 当前槽位定义为： Tx < Sxv 且  Tx > Sxv-slotSizeInMilliseconds
		 * 槽位有 效时间=slotSizeInMilliseconds ~ (timeStart + slotSizeInMilliseconds)
		 * 如果当前时间在有效时间内，则定义为当前槽位
		 * @return
		 */
		private Slot currentSlot() {
			boolean isLock = false;
			try {
				if((isLock = locker.tryLock(300, TimeUnit.MILLISECONDS))) {
					return slots[header];
				}
			} catch (InterruptedException e) {
				logger.error("act",e);
			}finally {
				if(isLock) {
					locker.unlock();
				}
			}
			logger.warn("数据统计误差范围内：{}",this.type);
			return slots[header];
		}
		
		/**
		 * 任意时刻Tx，计算槽位Sx是否有效，假定Sxv表示任意槽位有效时间最大值
	     * Tx－Sxv ＞　timewindow 即表示 Sx为无效槽位，将其值设置为０即可
		 */
		@Override
		public void act(String key,Object attachement) {
			boolean isLock = false;
			try {
				if((isLock = locker.tryLock(100, TimeUnit.MILLISECONDS))) {
					Slot preSlot = this.slots[header];
					header = (header+1) % slotLen;
					//当前槽位
					this.slots[header].setTimeEnd(preSlot.getTimeEnd()+this.slotSizeInMilliseconds);
					this.slots[header].reset();
				}
			} catch (InterruptedException e) {
				logger.error("act",e);
			}finally {
				if(isLock) {
					locker.unlock();
				}
			}
			
		}

		/**
		 * 计数器启动以来的总值
		 * @return
		 */
		public long getTotal() {
			return total.get();
		}
	}
	
	static class Slot {
		
		private volatile long timeEnd;
		
		private volatile AtomicLong val;
		
		public Slot(long timeEnd,long v) {
			this.timeEnd = timeEnd;
			this.val = new AtomicLong(v);
		}
		
		public long getVal() {
			return val.get();
		}

		public long getTimeEnd() {
			return timeEnd;
		}

		public void setTimeEnd(long timeEnd) {
			this.timeEnd = timeEnd;
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





