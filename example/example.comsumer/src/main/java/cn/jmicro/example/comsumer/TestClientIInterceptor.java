package cn.jmicro.example.comsumer;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.common.Constants;

@Component(side=Constants.SIDE_COMSUMER)
@Interceptor
public class TestClientIInterceptor implements IInterceptor {

	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		//System.out.println("Before Test : "+ TestClientIInterceptor.class.getName()+",Method:"+req.getMethod());
		IPromise<Object> resp = handler.onRequest(req);
		//System.out.println("After Test : "+ TestClientIInterceptor.class.getName());
		return resp;
	}

}
