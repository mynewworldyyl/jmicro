package org.jmicro.api.server;

import org.jmicro.api.Init;

public interface IServer extends Init{

	void init();
	
	void start();
	
	void stop();
	
	String host();
	
	int port();
}
