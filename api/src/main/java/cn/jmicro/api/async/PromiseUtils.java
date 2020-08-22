package cn.jmicro.api.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.internal.async.IClientAsyncCallback;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.common.Constants;

public class PromiseUtils {

	private static final Logger logger = LoggerFactory.getLogger(PromiseUtils.class);
	
	public static <R> IPromise<R> callService(Object remoteServiceProxy, String remoteMethod,Map<String,Object> context, Object... args) {
		PromiseImpl<R> p = new PromiseImpl<R>();
		p.setContext(context);
		
		final JMicroContext cxt = JMicroContext.get();
		IClientAsyncCallback cb = new IClientAsyncCallback() {
			@SuppressWarnings("unchecked")
			@Override
			public void onResponse(IResponse resp) {
				if(resp.isSuccess()) {
					p.setResult((R)resp.getResult());
				} else {
					AsyncFailResult f = new AsyncFailResult();
					if(resp.getResult() instanceof ServerError) {
						ServerError se = (ServerError)resp.getResult();
						f.setCode(se.getErrorCode());
						f.setMsg(se.getMsg());
					} else {
						f.setCode(1);
						if(resp.getResult() != null) {
							f.setMsg(resp.getResult().toString());
						}else {
							f.setMsg("Promise got error result");
						}
					}
					p.setFail(f);
				}
				p.done();
			}
		};
		
		cxt.setObject(Constants.CONTEXT_CALLBACK_CLIENT, cb);
		
		Class<?>[] types = null;
		if(args.length > 0) {
			types = new Class[args.length];
			for(int i = 0; i < args.length; i++) {
				types[i] = args[i].getClass();
			}
		} else {
			types = new Class[0];
		}
		
		try {
			
			Method m = null;
			for(Method sm : remoteServiceProxy.getClass().getMethods()){
				if(sm.getName().equals(remoteMethod)) {
					m = sm;
					break;
				}
			}
			//Method m = remoteServiceProxy.getClass().getMethod(remoteMethod, types);
			m.invoke(remoteServiceProxy, args);
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("",e);
		}finally {
			cxt.removeParam(Constants.CONTEXT_CALLBACK_CLIENT);
		}
		
		return p;
	}
}
