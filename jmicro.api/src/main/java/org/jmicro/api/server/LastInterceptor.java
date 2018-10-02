package org.jmicro.api.server;

import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor(Constants.LAST_INTERCEPTOR)
public class LastInterceptor extends AbstractInterceptor implements IInterceptor {

	private final static Logger logger = LoggerFactory.getLogger(LastInterceptor.class);
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest request) throws RpcException {
		logger.debug("LastInterceptor before");
		IResponse resp = handler.onRequest(request);
		logger.debug("LastInterceptor after");
		return resp;
	}
	
}
