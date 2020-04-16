package org.jmicro.api.monitor.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.monitor.v1.MonitorConstant;

@SO
public class MonitorServerStatus {
	
	public static final short receiveItemCount = MonitorConstant.Monotor_Server_ReceiveItemCount;
	
	public static final short receiveItemQps = MonitorConstant.Monotor_Server_ReceiveItemQps;
	
	public static final short submitTaskCount = MonitorConstant.Monotor_Server_SubmitTaskCount;
	
	public static final short taskNormalCount = MonitorConstant.Monotor_Server_TaskNormalCount;
	
	public static final short taskExceptionCount = MonitorConstant.Monotor_Server_TaskExceptionCount;
	
	public static final short submitCount = MonitorConstant.Monotor_Server_SubmitCount;
	
	public static final short submitQps = MonitorConstant.Monotor_Server_SubmitQps;
	
	public static final short checkExceptionCount = MonitorConstant.Monotor_Server_CheckExceptionCount;
	
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
