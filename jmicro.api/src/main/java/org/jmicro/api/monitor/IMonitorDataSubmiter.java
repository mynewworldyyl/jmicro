package org.jmicro.api.monitor;

import org.jmicro.api.net.Message;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

public interface IMonitorDataSubmiter {

	void submit(int type, IRequest req, IResponse resp,Throwable exp, String... args);
	
	void submit(int type, IRequest req,Throwable exp, String... args);
	
	void submit(int type, IResponse resp,Throwable exp, String... args);
	
	void submit(int type, Message msg,Throwable exp, String... args);
	
	void submit(int type,Throwable exp, String... args);
	
	void submit(int type,String... args);
	
	void submit(SubmitItem item);
}
