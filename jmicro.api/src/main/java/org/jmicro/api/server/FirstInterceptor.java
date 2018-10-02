package org.jmicro.api.server;

import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor(Constants.FIRST_INTERCEPTOR)
public class FirstInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(FirstInterceptor.class);
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		logger.debug("FirstInterceptor before");
		IResponse resp = handler.onRequest(req);
		logger.debug("FirstInterceptor after");
		return resp;
	}
}
