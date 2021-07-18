package cn.jmicro.api.sysstatis;

import cn.jmicro.api.annotation.SO;

@SO
public class SystemStatisJRso {

	private long totalMemory;
	private long freeMemory;
	private double cpuLoad;
	
	private double avgCpuLoad;
	
	private String insName;
	
	private String sysName;
	
	private int cpuNum;
	
	public long getTotalMemory() {
		return totalMemory;
	}
	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}
	public long getFreeMemory() {
		return freeMemory;
	}
	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}
	public double getCpuLoad() {
		return cpuLoad;
	}
	public void setCpuLoad(double cpuLoad) {
		this.cpuLoad = cpuLoad;
	}
	public double getAvgCpuLoad() {
		return avgCpuLoad;
	}
	public void setAvgCpuLoad(double avgCpuLoad) {
		this.avgCpuLoad = avgCpuLoad;
	}
	public String getInsName() {
		return insName;
	}
	public void setInsName(String insName) {
		this.insName = insName;
	}
	public String getSysName() {
		return sysName;
	}
	public void setSysName(String sysName) {
		this.sysName = sysName;
	}
	public int getCpuNum() {
		return cpuNum;
	}
	public void setCpuNum(int cpuNum) {
		this.cpuNum = cpuNum;
	}
	@Override
	public String toString() {
		return "SystemStatis [totalMemory=" + totalMemory + ", freeMemory=" + freeMemory + ", cpuLoad=" + cpuLoad
				+ ", avgCpuLoad=" + avgCpuLoad + ", insName=" + insName + ", sysName=" + sysName + ", cpuNum=" + cpuNum
				+ "]";
	}
	
	
}
