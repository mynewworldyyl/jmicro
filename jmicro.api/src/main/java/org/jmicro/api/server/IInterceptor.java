package org.jmicro.api.server;

import org.jmicro.api.exception.RpcException;

public interface IInterceptor {

	IResponse intercept(IRequestHandler handler,IRequest req) throws RpcException;
}
