package cn.jmicro.api.monitor.v2;

import cn.jmicro.api.annotation.SO;

@SO
public class MonitorServerStatus {
	
	//private int subsriberSize;
	
	private String srvKey;
	
	private double[] qps = null;
	private double[] cur = null;
	private double[] total = null;


	/*public int getSubsriberSize() {
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
	}*/

	public double[] getQps() {
		return qps;
	}

	public String getSrvKey() {
		return srvKey;
	}

	public void setSrvKey(String srvKey) {
		this.srvKey = srvKey;
	}

	public void setQps(double[] qps) {
		this.qps = qps;
	}

	public double[] getCur() {
		return cur;
	}

	public void setCur(double[] cur) {
		this.cur = cur;
	}

	public double[] getTotal() {
		return total;
	}

	public void setTotal(double[] total) {
		this.total = total;
	}

}
