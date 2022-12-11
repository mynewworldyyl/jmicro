package cn.jmicro.api.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import cn.jmicro.common.Constants;

public class TimeUtils {
	
	//一天的毫秒数
	public static final long MS_BY_DATE = 24*60*60*1000;

	private TimeUtils() {}
	
	private static final Map<String,TimeUnit> unitNameToTimeUnit = new HashMap<>();
	private static final Map<TimeUnit,String> timeUnitToName = new HashMap<>();
	
	private static final AtomicLong curTime = new AtomicLong(0);
	private static final AtomicBoolean isChecking = new AtomicBoolean(false);
	private static final Object timeCheckLocker = new Object();
	
	private static final AtomicLong lastUseTime = new AtomicLong(0);
	private static final long STOP_CHECKER_TIMEOUT = 1000*60*3;
	
	//每间隔BASE_TIME_INTERVAL毫秒调用一次System.currentTimeMillis()更新当前时间
	//调用者必须接受在BASE_TIME_INTERVAL毫秒内的误差
	private static final long BASE_TIME_INTERVAL = 10;
	
	private static final Runnable timeGetter = () -> {
		isChecking.set(true);
		boolean runing = true;
		try {
			while(runing) {
				curTime.set(System.currentTimeMillis());
				//超过STOP_CHECKER_TIMEOUT毫秒内没有调用getCurTime方法，停止时间更新器，释放线程
				if((curTime.get() - lastUseTime.get()) > STOP_CHECKER_TIMEOUT ) {
					isChecking.set(false);
					runing = false;
				}
				synchronized(timeCheckLocker) {
					try {
						//控制线间隔BASE_TIME_INTERVAL毫秒更新一次时间值
						timeCheckLocker.wait(BASE_TIME_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}finally {
			isChecking.set(false);
		}
	};
	
	public static long getCurTime() {
		if(isChecking.get()) {
			//时间更新器在更新中，直接取即可
			return curTime.get();
		}else {
			return updateTime();
		}
	}
	
	public static Date getCurDatetime() {
		return new Date(getCurTime());
	}
	
	public static long getCurTime(boolean aonce) {
		if(aonce) {
			return System.currentTimeMillis();
		}
		return getCurTime();
	}
	
	private static long updateTime() {
		long ct = System.currentTimeMillis();
		if(!isChecking.get()) {
			lastUseTime.set(ct);
			curTime.set(ct);
			//启动时间更新器，避免大并发下高速调用 System.currentTimeMillis()造成性能瓶颈
			new Thread(timeGetter).start();
		}
		return ct;
	}

	static {
		
		unitNameToTimeUnit.put(Constants.TIME_DAY, TimeUnit.DAYS);
		unitNameToTimeUnit.put(Constants.TIME_HOUR, TimeUnit.HOURS);
		unitNameToTimeUnit.put(Constants.TIME_MINUTES, TimeUnit.MINUTES);
		unitNameToTimeUnit.put(Constants.TIME_SECONDS, TimeUnit.SECONDS);
		unitNameToTimeUnit.put(Constants.TIME_MILLISECONDS, TimeUnit.MILLISECONDS);
		unitNameToTimeUnit.put(Constants.TIME_MICROSECONDS, TimeUnit.MICROSECONDS);
		unitNameToTimeUnit.put(Constants.TIME_NANOSECONDS, TimeUnit.NANOSECONDS);
		
		timeUnitToName.put( TimeUnit.DAYS,Constants.TIME_DAY);
		timeUnitToName.put(TimeUnit.HOURS,Constants.TIME_HOUR);
		timeUnitToName.put(TimeUnit.MINUTES,Constants.TIME_MINUTES);
		timeUnitToName.put(TimeUnit.SECONDS,Constants.TIME_SECONDS);
		timeUnitToName.put(TimeUnit.MILLISECONDS,Constants.TIME_MILLISECONDS);
		timeUnitToName.put(TimeUnit.MICROSECONDS,Constants.TIME_MICROSECONDS);
		timeUnitToName.put(TimeUnit.NANOSECONDS,Constants.TIME_NANOSECONDS);
		
	}
	
	
	public static String getUnitName(TimeUnit unit) {
		return timeUnitToName.get(unit);
	}
	
	public static TimeUnit getTimeUnit(String name) {
		return unitNameToTimeUnit.get(name);
	}
	
	public static long getMilliseconds(long timeWindow,String us) {
		return getTime(timeWindow,unitNameToTimeUnit.get(us),TimeUnit.MILLISECONDS);
	}
	
	public static long getTime(long timeWindow,TimeUnit srcTimeUnit,TimeUnit targetTimeUnit) {
		long result = timeWindow;
		if(targetTimeUnit == TimeUnit.SECONDS) {
			result = srcTimeUnit.toSeconds(timeWindow);
		}else if(targetTimeUnit == TimeUnit.MINUTES) {
			result = srcTimeUnit.toMinutes(timeWindow);
		}else if(targetTimeUnit == TimeUnit.HOURS) {
			result = srcTimeUnit.toHours(timeWindow);
		}else if(targetTimeUnit == TimeUnit.DAYS) {
			result = srcTimeUnit.toDays(timeWindow);
		}else if(targetTimeUnit == TimeUnit.MICROSECONDS) {
			result = srcTimeUnit.toMicros(timeWindow);
		}else if(targetTimeUnit == TimeUnit.NANOSECONDS) {
			result = srcTimeUnit.toNanos(timeWindow);
		}else if(targetTimeUnit == TimeUnit.MILLISECONDS) {
			result = srcTimeUnit.toMillis(timeWindow);
		}
		return result;
	}
	
}
