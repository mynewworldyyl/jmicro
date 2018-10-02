package org.jmicro.transport.mina;

import java.nio.ByteBuffer;

import org.apache.mina.api.IoSession;
import org.jmicro.api.client.IClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MinaClientSession extends AbstractMinaSession implements IClientSession{

	static final Logger LOG = LoggerFactory.getLogger(MinaClientSession.class);

	public MinaClientSession(IoSession ioSession) {
		this(ioSession,1024*4);
	}
	
	public MinaClientSession(IoSession ioSession,int readBufferSize) {
		super(ioSession,readBufferSize);
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		this.getIoSession().write(buffer);
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
	}
	
}
