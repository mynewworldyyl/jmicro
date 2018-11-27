package org.jmicro.example.comsumer;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.net.IInterceptor;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.common.Constants;

@Component(side=Constants.SIDE_COMSUMER)
@Interceptor
public class TestClientIInterceptor implements IInterceptor {

	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		System.out.println("Before Test : "+ TestClientIInterceptor.class.getName());
		IResponse resp = handler.onRequest(req);
		System.out.println("After Test : "+ TestClientIInterceptor.class.getName());
		return resp;
	}

}
