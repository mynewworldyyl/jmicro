package org.jmicro.api.monitor;

import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;

public interface IMonitorDataSubmiter {

	void submit(int type, IReq req, IResp resp,Throwable exp, String... args);
	
	void submit(int type, IReq req,Throwable exp, String... args);
	
	void submit(int type, IResp resp,Throwable exp, String... args);
	
	void submit(int type, Message msg,Throwable exp, String... args);
	
	void submit(int type,Throwable exp, String... args);
	
	void submit(int type,String... args);
	
	void submit(SubmitItem item);
}
