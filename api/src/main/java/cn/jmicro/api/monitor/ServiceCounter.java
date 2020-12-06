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
package cn.jmicro.api.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午12:16:36
 */
public class ServiceCounter implements IServiceCounter<Short>{

	private static final Logger logger = LoggerFactory.getLogger(ServiceCounter.class);
	
	private final static Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	//private static final int DEFAULT_SLOT_SIZE = 10;
	
	private boolean openDebug = false;
	
	//服务唯一标识,粒度到服务方法,=服务名+名称空间+版本+方法名+方法参数标识
	private String serviceKey;
	
	private int slotSize;
	
	private long slotSizeInMilliseconds;
	
	private long timeWindowInMilliseconds;
	
	//private Set<Short> supportTypes = new HashSet<>();
	
	private long timeWindow;
	
	private TimeUnit unit;
	
	private boolean staring = false;
	
	private long lastActiveTime = TimeUtils.getCurTime();
	
	//统计事件的类型,每种类型对应一种计数器
	private ConcurrentHashMap<Short,Counter> counters = new ConcurrentHashMap<>();
	
	private ITickerAction clock = (key,att) -> {
		for(Counter cnt : counters.values()) {
			cnt.act();
			cnt.getVal();
			if(cnt.getCheckCurEqualZeroCnt() > 10*slotSize) {
				//相当于旋转了10圈，结果值都是0，说明计数器长时间没有使用了，停止他，节省系统资源
				 stop();
			}
		}
	};
	
	/*public ServiceCounter(String serviceKey,long timeWindow,TimeUnit unit) {
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		this.serviceKey = serviceKey;
		this.timeWindow = timeWindow;
		this.unit = unit;
	}*/
	
	/**
	 * 
	 * @param serviceKey
	 * @param types
	 * @param timeWindow 统计窗口总时长
	 * @param slotInterval 单位时间大小，timeWindow/slotInterval=总单位个数，且timeWindow%slotInterval必有等于0
	 * @param unit timeWindow 和 slotInterval的时间单位
	 */
	public ServiceCounter(String serviceKey,Short[] types,long timeWindow,long slotInterval,TimeUnit unit) {
		logger.info("Add serviceCounter key:{},window:{}, slotInterval:{}, unit:{}",serviceKey,timeWindow,slotInterval,unit.name());
		if(StringUtils.isEmpty(serviceKey)) {
			throw new CommonException("Service Key cannot be null");
		}
		if(timeWindow <= 0) {
			throw new CommonException("Invalid timeWindow: " + timeWindow+",KEY: "+serviceKey);
		}
		if(slotInterval <= 0) {
			throw new CommonException("Invalid slotInterval: " + slotInterval + ",KEY: "+serviceKey);
		}
		
		this.serviceKey = serviceKey;
		this.unit = unit;
		//this.slotInterval = slotInterval;
		this.timeWindow = timeWindow;
		
		this.timeWindowInMilliseconds = TimeUtils.getTime(timeWindow, unit,TimeUnit.MILLISECONDS);
		
		if(this.timeWindowInMilliseconds <= 0) {
			throw new CommonException("Invalid timeWindow to MILLISECONDS : " + timeWindow);
		}
		
		this.slotSizeInMilliseconds = TimeUtils.getTime(slotInterval, unit,TimeUnit.MILLISECONDS);
		
		this.slotSize =(int) (timeWindowInMilliseconds / this.slotSizeInMilliseconds);
		
		if(timeWindowInMilliseconds % slotSizeInMilliseconds != 0) {
			throw new CommonException("timeWindow % slotInterval must be zero,but:" +
					timeWindow + "%" + slotInterval+"="+(timeWindow % slotInterval));
		}
		
		if(types != null && types.length > 0) {
			//supportTypes.addAll(Arrays.asList(types));
			for(Short type : types) {
				addCounter(type);
			}
		}
		
		logger.info("ServiceCounter config, timeWindowInMilliseconds:{}, slotSizeInMilliseconds:{}",timeWindowInMilliseconds,slotSizeInMilliseconds);
		
	}
	
	private void reset() {
		for(Counter cnt : counters.values()) {
			cnt.reset();
		}
	}
	
	private void stop() {
		if(!staring) {
			return;
		}
		TimerTicker.getTimer(timers, slotSizeInMilliseconds).removeListener(serviceKey, true);
		staring = false;
	}
	
	 private void start() {
		if(staring) {
			return;
		}
		staring = true;
		reset();
		TimerTicker.getTimer(timers, slotSizeInMilliseconds).addListener(serviceKey,null,true, clock);
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
	public long getByTypes(Short...types) {
		if(types == null || types.length == 0) {
			//返回一个无效值，使用方应该判断此值是否有效才能使用
			return -1;
		}
		long sum = 0;
		for(Short type : types) {
			Counter c = getCounter(type,false);
			if(c == null) {
				continue;
			}
			long v = this.get(type);
			if(v != -1) {
				sum += v;
			}
		}
		return sum;
	}

	@Override
	public boolean add(Short type, long val) {
		if(!staring) {
			this.start();
		}
		Counter c = getCounter(type,true);
		if(c != null) {
			this.setLastActiveTime(TimeUtils.getCurTime());
			c.add(val,0);
			return true;
		}
		//失败
		return false;
	}
	
	@Override
	public boolean add(Short type, long val,long timeDiff) {
		if(!staring) {
			this.start();
		}
		Counter c = getCounter(type,true);
		if(c != null) {
			this.setLastActiveTime(TimeUtils.getCurTime());
			c.add(val,timeDiff);
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
				continue;
			}
			sum += c.getTotal();
		}
		return sum;
	}
	
	public Long getAndResetTotal(Short... types) {
		long sum = 0;
		for(Short type : types) {
			Counter c = getCounter(type,false);
			if(c == null) {
				continue;
			}
			sum += c.getTotal();
			c.resetTotal();
		}
		return sum;
	}
	
	/**
	 * @param tounit
	 * @param types
	 * @return
	 */
	public double getQps(TimeUnit tounit, Short... types) {
		if(types.length == 1) {
			Counter c = getCounter(types[0],false);
			if(c != null) {
				return c.getQps(tounit);
			}
		} else {
			double sum = this.getByTypes(types);
			long time = TimeUtils.getTime(this.timeWindow, this.unit, tounit);
			return sum/time;
		}
		
		return -1;
	}

	@Override
	public boolean increment(Short type) {
		if(!staring) {
			this.start();
		}
		Counter c = getCounter(type,true);
		if(c != null) {
			this.setLastActiveTime(TimeUtils.getCurTime());
			c.add(1,0);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean increment(Short type,long actTime) {
		if(!staring) {
			this.start();
		}
		Counter c = getCounter(type,true);
		if(c != null) {
			this.setLastActiveTime(actTime);
			c.add(1,0);
			return true;
		}
		return false;
	}

	private Counter getCounter(Short type,boolean doAdd) {
		Counter c = counters.get(type);
		if(c == null && doAdd) {
			synchronized(counters) {
				c = counters.get(type);
				if(c == null) {
					if(this.addCounter(type)) {
						c = counters.get(type);
					}
				}
			}
		}
		if(c == null && doAdd) {
			throw new CommonException("Fail to add type:" + Integer.toHexString(type));
		}
		return c;
	}
	
	public boolean addCounter(Short type) {
		if(counters.containsKey(type)) {
			throw new CommonException("Type[" + MC.MONITOR_VAL_2_KEY.get(type) + "] exists for service [" + this.serviceKey + "]");
		}

		//String key = serviceKey+"_"+type;
		
		logger.info("Add Counter for type:{},KEY:{},window:{}, slotSizeInMilliseconds:{}",
				MC.MONITOR_VAL_2_KEY.get(type),
				serviceKey,timeWindowInMilliseconds,slotSizeInMilliseconds);
		
		Counter cnt = new Counter(type/*,timeWindowInMilliseconds,slotSizeInMilliseconds*/);
		//TimerTicker.getTimer(timers, slotSizeInMilliseconds).addListener(key, cnt,null,true);
		counters.put(type, cnt);
		//supportTypes.add(type);
		return true;
	}
	
	public boolean existType(Short type) {
		return counters.contains(type);
	}
	
/*	@Override
	public void getAll(Map<Short, Double> values) {
		if(values == null) {
			throw new NullPointerException("value is NULL");
		}
		
		for(Map.Entry<Short, Counter> c : this.counters.entrySet()) {
			values.put(c.getKey(), new Double(c.getValue().getVal()));
		}
		
	}*/

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void setLastActiveTime(long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	/**
	 * 指定类型所占总请求数的百分比
	 * @param counter
	 * @param type
	 * @return
	 */
	public static double takePercent(ServiceCounter counter,Short type) {
		Long totalReq = counter.get(MC.MT_REQ_START);
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
		case MC.STATIS_FAIL_PERCENT:
			Long totalReq = counter.get(MC.MT_REQ_START);
			if(totalReq != 0) {
				double totalFail = new Double(counter.getByTypes(MC.MT_SERVICE_ERROR,MC.MT_REQ_TIMEOUT));
				result = (totalFail/totalReq)*100;
				//logger.debug("totalReq:{},totalFail:{},Percent:{}",totalReq,totalFail,result);
			}
			break;
		case MC.MT_REQ_START:
			result = 1.0 * counter.getTotal(MC.MT_REQ_START);		
			break;
		case MC.STATIS_TOTAL_RESP:
			result = new Double(counter.getTotal(MC.MT_REQ_END));
			break;
		case MC.STATIS_TOTAL_SUCCESS:
			result =  1.0 * counter.getTotal(MC.MT_REQ_SUCCESS);
			break;
		case MC.STATIS_TOTAL_FAIL:
			result = 1.0 * counter.getTotal(MC.MT_CLIENT_RESPONSE_SERVER_ERROR)+
			counter.getTotal(MC.MT_SERVICE_ERROR)+
			counter.getTotal(MC.MT_REQ_TIMEOUT_FAIL)+
			counter.getTotal(MC.MT_REQ_ERROR);
			break;
		case MC.STATIS_SUCCESS_PERCENT:
			totalReq = counter.get(MC.MT_REQ_START);
			if(totalReq != 0) {
				result =  1.0 * counter.get(MC.MT_REQ_SUCCESS)/*+
						counter.get(MonitorConstant.CLIENT_REQ_OK)*/;
						result = (result*1.0/totalReq)*100;
			}
			break;
		case MC.MT_REQ_TIMEOUT_FAIL:
			result = 1.0 * counter.get(MC.MT_REQ_TIMEOUT_FAIL);
			break;
		case MC.STATIS_TIMEOUT_PERCENT:
			totalReq = counter.get(MC.MT_REQ_START);
			if(totalReq != 0) {
				result = 1.0 * counter.get(MC.MT_REQ_TIMEOUT_FAIL);
				result = (result/totalReq)*100;
			}
			break;
		case MC.MT_REQ_START+1:
			result = counter.getQps(TimeUnit.SECONDS,MC.MT_REQ_START);
			break;
		default:
			result = new Double(counter.get(type));
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
	private class Counter {
		
		private final int type;
		//时间窗口,单位毫秒,统计只保持时间窗口内的数据,超过时间窗口的数据自动丢弃
		//统计数据平滑向前移动,单位毫秒
		//private final long timeWindow;
		
		//每个槽位占用时间长度,单位毫秒
		//private final long slotSizeInMilliseconds;
		
		//private final int slotLen;
		
		//private final String id;
		
		//最后一次检测等于0的时间，用于确定计数器是否长时间处于空闲状态
		private int checkCurEqualZeroCnt = 0;
		
		private final Slot[] slots;
		
		private volatile int header = -1;
		
		private final ReentrantLock locker = new ReentrantLock();
		
		private volatile AtomicLong total = new  AtomicLong(0);
		
		//每个槽位的最大值，如果当前槽位值大于此值，则强制进入下一个槽位
		//private long maxSlotVal = Integer.MAX_VALUE;
		
		/**
		 * 
		 * @param timeWindow 统计时间总长值，单位是毫秒
		 * @param slotSizeInMilliseconds 每个槽位所占时间长度，最大值等于timeWindow,
		 *     并且是必须满足timeWindow=N*slotSizeInMilliseconds, N是非负整数，N就是槽位个数
		 *     
		 */
		public Counter(int type/*,long timeWindow,long slotSizeInMilliseconds*/) {
			//this.id = id;
			this.type = type;
			//this.timeWindow = timeWindow;
			//this.slotSizeInMilliseconds = slotSizeInMilliseconds;
			
			//计算槽位个数
			//this.slotLen = (int)(timeWindow / slotSizeInMilliseconds);
			this.slots = new Slot[slotSize];
			this.header = 0;
			
			//long curTime = TimeUtils.getCurTime();
			//槽位 有效时间=slotSizeInMilliseconds ~ (timeStart + slotSizeInMilliseconds)
			//如果当前时间在有效时间内，则定义为当前槽位
			for(int i = 0; i < slotSize; i++) {
				this.slots[i] = new Slot(0,0);
			}
			
			//this.slots[0].setTimeEnd(curTime+slotSizeInMilliseconds);
			if(openDebug) {
				logger.info("Create Counter type:{}, timeWindow:{},slotSizeInMilliseconds:{},slotLen:{}",
						MC.MONITOR_VAL_2_KEY.get(type),timeWindowInMilliseconds,slotSizeInMilliseconds,slotSize);
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
			long time = TimeUtils.getTime(timeWindow,unit, timeUnit);
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
			if(sum == 0) {
				checkCurEqualZeroCnt++;
			} else {
				//有值，可以重新计数
				checkCurEqualZeroCnt = 0;
			}
			return sum;
		}
		
		/**
		 * 递增1
		 */
		public void increment() {
			total.addAndGet(1);
			currentSlot(0).increment();
		}
		
		/**
		 * 增加指定值
		 * @param v
		 */
		public void add(long v,long timeDiff) {
			total.addAndGet(v);
			Slot s= currentSlot(timeDiff);
			s.add(v);
		}
		
		/**
		 * 当前槽位定义为： Tx < Sxv 且  Tx > Sxv-slotSizeInMilliseconds
		 * 槽位有 效时间=slotSizeInMilliseconds ~ (timeStart + slotSizeInMilliseconds)
		 * 如果当前时间在有效时间内，则定义为当前槽位
		 * @return
		 */
		private Slot currentSlot(long timeDiff) {
			boolean isLock = false;
			try {
				if(isLock = locker.tryLock(300, TimeUnit.MILLISECONDS)) {
					if(timeDiff <= 0) {
						return slots[header];
					} else {
						//还原真实数据产生时间
						//计算时间差内等价多少个槽位时间间隔
						int diffIndex = (int)(timeDiff/slotSizeInMilliseconds);
						
						//如果超过一个时间周期，则计算在当前周期内，确保数据不丢失
						diffIndex = diffIndex % slotSize;
						
						if(diffIndex == 0) {
							return slots[header];
						}
						
						//回退idx个
						int idx = (slotSize - diffIndex + header) % slotSize;
						if(type == MC.MT_SERVER_LIMIT_MESSAGE_POP) {
							logger.debug("Cur slot Index: " + idx);
						}
						return slots[idx];
					}
				}
			} catch (InterruptedException e) {
				logger.error("act",e);
			}finally {
				if(isLock) {
					locker.unlock();
				}
			}
			//取锁失败时返回原来的槽位
			logger.warn("数据统计误差范围内：{}",this.type);
			return slots[header];
		}
		
		/**
		 * 任意时刻Tx，计算槽位Sx是否有效，假定Sxv表示任意槽位有效时间最大值
	     * Tx－Sxv ＞　timewindow 即表示 Sx为无效槽位，将其值设置为０即可
		 */
		public void act(/*String key,Object attachement*/) {
			boolean isLock = false;
			try {
				if((isLock = locker.tryLock(100, TimeUnit.MILLISECONDS))) {
					//Slot preSlot = this.slots[this.header];
					//当前槽位
					int h = (this.header+1) % slotSize;
					//this.slots[this.header].setTimeEnd(preSlot.getTimeEnd() + slotSizeInMilliseconds);
					this.slots[h].reset();
					this.header = h;
				}
				//maxSlotVal = (this.getVal()/slotSize)*5;
			} catch (InterruptedException e) {
				logger.error("act",e);
			} finally {
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
		
		public void reset() {
			for(int i = 0; i < slotSize; i++) {
				this.slots[i].reset();
			}
			this.header = 0;
			total.set(0);
			//this.slots[0].setTimeEnd(TimeUtils.getCurTime()+slotSizeInMilliseconds);
		}
		
		public int getCheckCurEqualZeroCnt() {
			return checkCurEqualZeroCnt;
		}

		public void resetTotal() {
			total.set(0);
		}
	}
	
	static class Slot {
		
		//private volatile long timeEnd;
		
		private volatile AtomicLong val;
		
		public Slot(long timeEnd,long v) {
			//this.timeEnd = timeEnd;
			this.val = new AtomicLong(v);
		}
		
		public long getVal() {
			return val.get();
		}

		/*public long getTimeEnd() {
			return timeEnd;
		}

		public void setTimeEnd(long timeEnd) {
			this.timeEnd = timeEnd;
		}*/

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





