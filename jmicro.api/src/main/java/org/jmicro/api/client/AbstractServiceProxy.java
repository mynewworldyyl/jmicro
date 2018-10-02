package org.jmicro.api.client;

import java.lang.reflect.InvocationHandler;

public class AbstractServiceProxy {

	protected InvocationHandler handler = null;

	public InvocationHandler getHandler() {
		return handler;
	}

	public void setHandler(InvocationHandler handler) {
		this.handler = handler;
	}
	
	
}
