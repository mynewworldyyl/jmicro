package org.jmicro.client;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.BreakerException;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.exception.TimeoutException;
import org.jmicro.api.net.IInterceptor;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.common.Constants;

@Component(side=Constants.SIDE_COMSUMER)
@Interceptor
public class BreakerInterceptor implements IInterceptor {

	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		IResponse resp = null;
		try {
			resp = handler.onRequest(req);
		} catch (BreakerException | TimeoutException e) {
			e.printStackTrace();
			resp =  FirstClientInterceptor.doFastFail(req,e);
		}
		return resp;
	}
}
