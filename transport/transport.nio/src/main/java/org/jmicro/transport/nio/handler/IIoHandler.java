package org.jmicro.transport.nio.handler;

import org.jmicro.api.server.IServerSession;
import org.jmicro.api.server.Message;

public interface IIoHandler {

	void handleRequestMessage(IServerSession session,Message msg);
	
	void handleResponseMessage(IServerSession session,Message msg);
	
}
