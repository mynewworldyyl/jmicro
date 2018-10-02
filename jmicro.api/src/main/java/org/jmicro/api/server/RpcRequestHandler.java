package org.jmicro.api.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.annotation.Handler;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.servicemanager.ServiceLoader;
import org.jmicro.common.Constants;

@Handler(Constants.DEFAULT_HANDLER)
public class RpcRequestHandler extends AbstractHandler implements IRequestHandler {

	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Override
	public IResponse onRequest(IRequest request) {
		Object obj = serviceLoader.getService(request.getServiceName()
				,request.getNamespace(),request.getVersion());
		
		Object[] args = request.getArgs();
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			parameterTypes[i] = args[i].getClass();
		}
		
		RpcResponse resp = null;
		try {
			Method m = obj.getClass().getMethod(request.getMethod(), parameterTypes);
			if(m != null) {
				Object result = m.invoke(obj, args);
				resp = new RpcResponse(request.getRequestId(),result);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RpcException(request,"",e);
		}
		return resp;
	}

}
