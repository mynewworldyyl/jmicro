package org.jmicro.limit;

import org.jmicro.api.registry.ServiceItem;

public class LimitData {

	private long reqTime = System.currentTimeMillis();
	
	private ServiceItem si;

	public long getReqTime() {
		return reqTime;
	}

	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}

	public ServiceItem getSi() {
		return si;
	}

	public void setSi(ServiceItem si) {
		this.si = si;
	}
	
	
	
}
