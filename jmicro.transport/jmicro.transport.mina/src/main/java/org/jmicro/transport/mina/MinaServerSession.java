package org.jmicro.transport.mina;

import java.nio.ByteBuffer;

import org.apache.mina.api.IoSession;
import org.jmicro.api.server.IServerSession;
import org.jmicro.api.server.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MinaServerSession extends AbstractMinaSession implements IServerSession{

	static final Logger LOG = LoggerFactory.getLogger(MinaServerSession.class);
	
	public MinaServerSession(IoSession ioSession) {
		super(ioSession,1024*2);
	}
	
	public MinaServerSession(IoSession ioSession,int readBufferSize) {
		super(ioSession,readBufferSize);
	}

	@Override
	public void write(Message msg) {
		this.getIoSession().write(ByteBuffer.wrap(msg.encode()));
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
	}

}
