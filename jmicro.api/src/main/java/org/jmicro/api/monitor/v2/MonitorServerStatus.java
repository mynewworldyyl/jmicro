package org.jmicro.api.monitor.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.SO;

@SO
public class MonitorServerStatus {
	
	public static final short receiveItemCount = 1;
	
	public static final short receiveItemQps = 2;
	
	public static final short submitTaskCount = 3;
	
	public static final short taskNormalCount = 4;
	
	public static final short taskExceptionCount = 5;
	
	public static final short submitCount = 6;
	
	public static final short submitQps = 7;
	
	public static final short checkExceptionCount = 8;
	
	public static final Short[] TYPES  = {
			receiveItemCount,receiveItemQps,submitTaskCount,taskNormalCount,taskExceptionCount,
			submitQps,submitCount
	};
	
	private String instanceName;
	
	private int subsriberSize;
	
	private int sendCacheSize;
	
	private Map<String,Set> subsriber2Types = new HashMap<>();
	
	private Map<String,Double> statisData = null;

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public int getSubsriberSize() {
		return subsriberSize;
	}

	public void setSubsriberSize(int subsriberSize) {
		this.subsriberSize = subsriberSize;
	}

	public int getSendCacheSize() {
		return sendCacheSize;
	}

	public void setSendCacheSize(int sendCacheSize) {
		this.sendCacheSize = sendCacheSize;
	}

	public Map<String, Set> getSubsriber2Types() {
		return subsriber2Types;
	}

	public void setSubsriber2Types(Map<String, Set> subsriber2Types) {
		this.subsriber2Types = subsriber2Types;
	}

	public Map<String, Double> getStatisData() {
		return statisData;
	}

	public void setStatisData(Map<String, Double> statisData) {
		this.statisData = statisData;
	}

}
