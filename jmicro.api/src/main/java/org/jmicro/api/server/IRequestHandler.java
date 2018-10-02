package org.jmicro.api.server;

public interface IRequestHandler {
	public IResponse onRequest(IRequest request);
}
