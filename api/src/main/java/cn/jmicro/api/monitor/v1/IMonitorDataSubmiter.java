package cn.jmicro.api.monitor.v1;

public interface IMonitorDataSubmiter {

	boolean submit(SubmitItem item);
	
	boolean canSubmit(short type);
	
}
