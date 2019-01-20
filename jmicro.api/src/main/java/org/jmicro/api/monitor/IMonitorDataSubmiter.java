package org.jmicro.api.monitor;

import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;

public interface IMonitorDataSubmiter {

	boolean submit(int type, IReq req, IResp resp,Throwable exp, String... args);
	
	boolean submit(int type, IReq req,Throwable exp, String... args);
	
	boolean submit(int type, IResp resp,Throwable exp, String... args);
	
	boolean submit(int type, Message msg,Throwable exp, String... args);
	
	boolean submit(int type,Throwable exp, String... args);
	
	boolean submit(int type,String... args);
	
	boolean submit(SubmitItem item);
	
	boolean canSubmit(int type);
}
