package org.jmicro.api.client;

import java.nio.ByteBuffer;

import org.jmicro.api.server.ISession;

public interface IClientSession extends ISession{

	void write(ByteBuffer writeBuffer);

}
