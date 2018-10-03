package org.jmicro.api.server;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.limitspeed.Limiter;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor(Constants.FIRST_INTERCEPTOR)
public class FirstInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(FirstInterceptor.class);
	
	@Cfg(value ="limiterName", required=false, changeListener="limiterName")
	private String limiterName;
	
	private Limiter limiter=null;
	
	public FirstInterceptor() {}
	
	public void init() {
		limiterName("limiterName");
	}
	
	public void limiterName(String fieldName){
		if(fieldName == null || "".equals(fieldName.trim())){
			return;
		}
		
		if(fieldName != null && fieldName.trim().equals("limiterName")){
			limiter = ComponentManager.getObjectFactory().getByName(limiterName);
		}
		
	}
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		logger.debug("FirstInterceptor before");
		if(limiter != null){
			int r = limiter.apply(req);
			if(r < 0){
				return fastFail(req);
			}
		}
		IResponse resp = handler.onRequest(req);
		logger.debug("FirstInterceptor after");
		return resp;
	}

	private IResponse fastFail(IRequest req) {
		ServerError se = new ServerError();
		se.setErrorCode(ServerError.SE_LIMITER);
		se.setMsg("");
		return new RpcResponse(req.getRequestId(),se);
	}
}
