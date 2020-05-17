package cn.jmicro.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.jmicro.common.Constants;

public class TimeUtils {

	private TimeUtils() {}
	
	private static final Map<String,TimeUnit> unitNameToTimeUnit = new HashMap<>();
	private static final Map<TimeUnit,String> timeUnitToName = new HashMap<>();
	
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
