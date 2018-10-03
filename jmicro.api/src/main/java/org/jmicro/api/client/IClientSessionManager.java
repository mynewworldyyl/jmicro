package org.jmicro.api.client;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.Message;

public interface IClientSessionManager {

	IClientSession connect(String host,int port);
	
	void write(IRequest req, IResponseHandler handler,int retryCnt);
	
}
