package org.jmicro.api.server;

public class AbstractHandler implements IHandler {

	private Object srv = null;
	
	public AbstractHandler(Object srv){
		this.srv = srv;
	}
	
	
}
