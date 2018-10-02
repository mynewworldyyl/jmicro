package org.jmicro.api.server;

import net.techgy.idgenerator.IDStrategy;

@IDStrategy
public interface IServerSession extends ISession{

	//server write response, or client write no need response request
	void write(Message resp);
	
}
