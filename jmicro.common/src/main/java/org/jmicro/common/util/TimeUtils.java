package org.jmicro.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jmicro.common.Constants;

public class TimeUtils {

	private TimeUtils() {}
	
	private static final Map<String,TimeUnit> unitNameToTimeUnit = new HashMap<>();
	private static final Map<TimeUnit,String> timeUnitToName = new HashMap<>();
	
	static {
		
		unitNameToTimeUnit.put(Constants.TIME_DAY, TimeUnit.DAYS);
		unitNameToTimeUnit.put(Constants.TIME_HOUR, TimeUnit.HOURS);
		unitNameToTimeUnit.put(Constants.TIME_MINUTES, TimeUnit.MINUTES);
		unitNameToTimeUnit.put(Constants.TIME_SECONDS, TimeUnit.SECONDS);
		unitNameToTimeUnit.put(Constants.TIME_MICROSECONDS, TimeUnit.MILLISECONDS);
		unitNameToTimeUnit.put(Constants.TIME_MICROSECONDS, TimeUnit.MICROSECONDS);
		unitNameToTimeUnit.put(Constants.TIME_NANOSECONDS, TimeUnit.NANOSECONDS);
		
		timeUnitToName.put( TimeUnit.DAYS,Constants.TIME_DAY);
		timeUnitToName.put(TimeUnit.HOURS,Constants.TIME_HOUR);
		timeUnitToName.put(TimeUnit.MINUTES,Constants.TIME_MINUTES);
		timeUnitToName.put(TimeUnit.SECONDS,Constants.TIME_SECONDS);
		timeUnitToName.put(TimeUnit.MILLISECONDS,Constants.TIME_MICROSECONDS);
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
		return getMilliseconds(timeWindow,unitNameToTimeUnit.get(us));
	}
	
	public static long getMilliseconds(long timeWindow,TimeUnit timeUnit) {
		long result = timeWindow;
		if(timeUnit == TimeUnit.SECONDS) {
			result = TimeUnit.SECONDS.toMillis(timeWindow);
		}else if(timeUnit == TimeUnit.MINUTES) {
			result = TimeUnit.MINUTES.toMillis(timeWindow);
		}else if(timeUnit == TimeUnit.HOURS) {
			result = TimeUnit.HOURS.toMillis(timeWindow);
		}else if(timeUnit == TimeUnit.DAYS) {
			result = TimeUnit.DAYS.toMillis(timeWindow);
		}else if(timeUnit == TimeUnit.MICROSECONDS) {
			result = TimeUnit.MICROSECONDS.toMillis(timeWindow);
		}else if(timeUnit == TimeUnit.NANOSECONDS) {
			result = TimeUnit.NANOSECONDS.toMillis(timeWindow);
		}
		return result;
	}
	
}
