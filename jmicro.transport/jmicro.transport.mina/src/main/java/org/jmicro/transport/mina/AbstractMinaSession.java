package org.jmicro.transport.mina;

import org.apache.mina.api.IoSession;
import org.jmicro.api.server.AbstractSession;
import org.jmicro.api.server.ISession;

public abstract class AbstractMinaSession extends AbstractSession implements ISession{

	private IoSession ioSession;
	
	public AbstractMinaSession(int readBufferSize){super(readBufferSize);}
	
	public AbstractMinaSession(IoSession ioSession,int readBufferSize) {
		super(readBufferSize);
		this.ioSession = ioSession;
	}
	public IoSession getIoSession() {
		return ioSession;
	}
	public void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}
	@Override
	public void close(boolean flag) {
		this.ioSession.close(true);
	}
	
}
