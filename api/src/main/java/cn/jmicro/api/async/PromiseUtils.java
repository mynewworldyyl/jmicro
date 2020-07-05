package cn.jmicro.api.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.internal.async.IClientAsyncCallback;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.common.Constants;

public class PromiseUtils {

	public static <R> IPromise<R> callService(Object remoteServiceProxy, String remoteMethod, Object... args) {
		PromiseImpl<R> p = new PromiseImpl<R>();
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
						}
					}
					p.setFail(f);
				}
				cxt.removeParam(Constants.CONTEXT_CALLBACK_CLIENT);
				p.done();
			}
		};
		
		JMicroContext.get().setObject(Constants.CONTEXT_CALLBACK_CLIENT, cb);
		
		Class<?>[] types = null;
		if(args.length > 0) {
			types = new Class[args.length];
			for(int i = 0; i < args.length; i++) {
				types[i] = args[i].getClass();
			}
		}else {
			types = new Class[0];
		}
		
		try {
			Method m = remoteServiceProxy.getClass().getMethod(remoteMethod, types);
			m.invoke(remoteServiceProxy, args);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return p;
	}
}
