package org.jmicro.api.monitor.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.SO;

@SO
public class MonitorInfo {

	private Map<String,Set> subsriber2Types = new HashMap<>();
	
	private String instanceName;
	
	private String group;
	
	private String srvKey;
	
	private Short[] types = null;
	
	private String[] typeLabels = null;
	
	private boolean running;
	
	public String getSrvKey() {
		return srvKey;
	}
	public void setSrvKey(String srvKey) {
		this.srvKey = srvKey;
	}
	public Short[] getTypes() {
		return types;
	}
	public void setTypes(Short[] types) {
		this.types = types;
	}
	public String[] getTypeLabels() {
		return typeLabels;
	}
	public void setTypeLabels(String[] typeLabels) {
		this.typeLabels = typeLabels;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	public Map<String, Set> getSubsriber2Types() {
		return subsriber2Types;
	}
	public void setSubsriber2Types(Map<String, Set> subsriber2Types) {
		this.subsriber2Types = subsriber2Types;
	}
	public String getInstanceName() {
		return instanceName;
	}
	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}
	
	
}
