package org.jmicro.api.server;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSession implements ISession{

	private Long sessionId=-1L;

	private Map<String,Object> params = new ConcurrentHashMap<String,Object>();
	
	private ByteBuffer readBuffer;
	
	public AbstractSession(int bufferSize){
		readBuffer = ByteBuffer.allocate(bufferSize);
	}
	
	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public int hashCode() {
		return sessionId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		if(!(obj instanceof AbstractSession)) {
			return false;
		}
		AbstractSession as = (AbstractSession)obj;
		return this.sessionId.equals(as.getSessionId());
	}

	@Override
	public void close(boolean flag) {
		params.clear();
		this.sessionId=-1L;
		
	}

	@Override
	public Object getParam(String key) {
		return this.params.get(key);
	}

	@Override
	public void putParam(String key, Object obj) {
		this.params.put(key, obj);
	}

	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	public void setReadBuffer(ByteBuffer readBuffer) {
		this.readBuffer = readBuffer;
	}	
}
