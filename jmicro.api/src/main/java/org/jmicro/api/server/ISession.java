package org.jmicro.api.server;

import java.nio.ByteBuffer;

public interface ISession {

     Long getSessionId();
	
	void close(boolean flag);
	
	Object getParam(String key);
	
	void putParam(String key,Object obj);
	
	ByteBuffer getReadBuffer();
}
