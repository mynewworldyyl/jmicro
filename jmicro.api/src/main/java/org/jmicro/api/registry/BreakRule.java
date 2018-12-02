package org.jmicro.api.registry;

import java.util.concurrent.TimeUnit;

import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;

public class BreakRule {

	private boolean enable = false;
	
	private long timeInMilliseconds;
	
	private Integer[] exceptionTypes;
	
	private int percent;
	
	private String originRule;
	
	public BreakRule() {}
	
	public static BreakRule parseRule(String ruleString) {
		BreakRule rule = new BreakRule();
		rule.setEnable(true);
		rule.setOriginRule(ruleString);
		
		if(StringUtils.isEmpty(ruleString)) {
			rule.setEnable(false);
			return rule;
		}
		
		String[] arr = ruleString.split(" ");
		if(arr.length != 3) {
			rule.setEnable(false);
			throw new CommonException("Invalid rule String: " + ruleString);
		}
		
		parseTimeRule(arr[0],rule);
		if(!rule.isEnable()) {
			return rule;
		}
		
		parseEventTypeRule(arr[1],rule);
		if(!rule.isEnable()) {
			return rule;
		}
		
		parsePercentRule(arr[2],rule);
		if(!rule.isEnable()) {
			return rule;
		}
		
		rule.setEnable(true);
		return rule;
	}
	
	private static BreakRule parsePercentRule(String ruleConfig, BreakRule rule) {
		if(StringUtils.isEmpty(ruleConfig)) {
			rule.setEnable(false);
			throw new CommonException("Invalid Percent rule String: " + ruleConfig);
		}
		
		ruleConfig = ruleConfig.trim();
		if(ruleConfig.endsWith("%")) {
			ruleConfig = ruleConfig.substring(0, ruleConfig.length()-1);
		}
		
		rule.setPercent(Integer.parseInt(ruleConfig));
		
		return rule;
	}

	private static BreakRule parseEventTypeRule(String typeConfig, BreakRule rule) {
		if(StringUtils.isEmpty(typeConfig)) {
			rule.setEnable(false);
			throw new CommonException("Invalid event type rule cannot be empty: ");
		}
		
		typeConfig = typeConfig.trim();
		if(!typeConfig.startsWith("[") || !typeConfig.endsWith("]")) {
			rule.setEnable(false);
			throw new CommonException("Invalid event type String must be start with \"[\" and end with \"]\" ");
		}
		typeConfig = typeConfig.substring(1);
		typeConfig = typeConfig.substring(0, typeConfig.length()-1);
		String[] arr = typeConfig.split(",");
		if(arr == null || arr.length == 0) {
			rule.setEnable(false);
			throw new CommonException("Event type cannot be empty: " + typeConfig);
		}
		
		Integer[] types = new Integer[arr.length];
		for(int i = 0; i < types.length; i++) {
			types[i] = Integer.parseInt(arr[i],16);
		}
		rule.setExceptionTypes(types);
		return rule;
	}

	private static BreakRule parseTimeRule(String timeConfig,BreakRule rule) {
		String timeunit = timeConfig.substring(timeConfig.length()-1, timeConfig.length()).trim();
		String time = timeConfig.substring(0, timeConfig.length()-1).trim();
		
		long timeInMillis = 0;
		if(timeunit.equals("H") || timeunit.equals("h")) {
			timeInMillis =TimeUnit.HOURS.toMillis(Long.parseLong(time));
		}else if(timeunit.equals("M") || timeunit.equals("m")) {
			timeInMillis =TimeUnit.MINUTES.toMillis(Long.parseLong(time));
		}else if(timeunit.equals("S") || timeunit.equals("S")) {
			timeInMillis =TimeUnit.SECONDS.toMillis(Long.parseLong(time));
		}else if(timeunit.equals("MS") || timeunit.equals("ms")) {
			timeInMillis = Long.parseLong(time);
		}else if(timeunit.equals("N") || timeunit.equals("n")) {
			timeInMillis =TimeUnit.NANOSECONDS.toMillis(Long.parseLong(time));
		}else {
			rule.setEnable(false);
			throw new CommonException("Invalid time rule String: " + timeConfig);
		}
		
		rule.setTimeInMilliseconds(timeInMillis);
		return rule;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public long getTimeInMilliseconds() {
		return timeInMilliseconds;
	}

	public void setTimeInMilliseconds(long timeInMilliseconds) {
		this.timeInMilliseconds = timeInMilliseconds;
	}

	public Integer[] getExceptionTypes() {
		return exceptionTypes;
	}

	public void setExceptionTypes(Integer[] exceptionTypes) {
		this.exceptionTypes = exceptionTypes;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

	public String getOriginRule() {
		return originRule;
	}

	public void setOriginRule(String originRule) {
		this.originRule = originRule;
	}

	public void from(BreakRule r) {
		this.setEnable(r.isEnable());
		this.setExceptionTypes(r.getExceptionTypes());
		this.setOriginRule(r.getOriginRule());
		this.setPercent(r.getPercent());
		this.setTimeInMilliseconds(r.getTimeInMilliseconds());
	}
	
}
