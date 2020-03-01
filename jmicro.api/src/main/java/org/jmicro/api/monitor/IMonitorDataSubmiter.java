package org.jmicro.api.monitor;

public interface IMonitorDataSubmiter {

	boolean submit(SubmitItem item);
	
	boolean canSubmit(short type);
	
}
