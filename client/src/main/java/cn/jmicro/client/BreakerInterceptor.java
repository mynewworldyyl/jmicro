package cn.jmicro.client;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.BreakerException;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.exception.TimeoutException;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.common.Constants;

@Component(side=Constants.SIDE_COMSUMER)
@Interceptor
public class BreakerInterceptor implements IInterceptor {

	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		IPromise<Object> resp = null;
		try {
			resp = handler.onRequest(req);
		} catch (BreakerException | TimeoutException e) {
			resp =  FirstClientInterceptor.doFastFail(req,e,MC.MT_SERVICE_BREAK);
		}
		return resp;
	}
}
