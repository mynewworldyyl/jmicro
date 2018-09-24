package org.jmicro.api.server;

public interface IServer {

	void init();
	
	void start();
	
	void stop();
	
	void addHandler(IHandler handler);
	
	String host();
}
