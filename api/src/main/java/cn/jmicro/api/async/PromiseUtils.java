package cn.jmicro.api.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.internal.async.IClientAsyncCallback;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

public class PromiseUtils {

	private static final Logger logger = LoggerFactory.getLogger(PromiseUtils.class);
	
	public static <R> IPromise<R> callService(Object remoteServiceProxy, String remoteMethod,Object contextObj, Object... args) {
		
		final JMicroContext cxt = JMicroContext.get();
		
		PromiseImpl<R> p = new PromiseImpl<R>();
		
		ServiceMethodJRso remoteMethd = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
		if(remoteMethd != null) {
			p.setTimeout(remoteMethd.getTimeout()*3);
		}
	
		p.setContext(contextObj);
		
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
						} else {
							f.setMsg("Promise got error result");
						}
					}
					p.setFail(f);
				}
				p.done();
			}
		};
		
		/*Class<?>[] types = null;
		if(args.length > 0) {
			types = new Class[args.length];
			for(int i = 0; i < args.length; i++) {
				types[i] = args[i].getClass();
			}
		} else {
			types = new Class[0];
		}*/
		
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
		} catch (InvocationTargetException e) {
			if(e.getTargetException() instanceof CommonException) {
				throw (CommonException)e.getTargetException();
			}else {
				throw new CommonException(MC.MT_SERVICE_ERROR,remoteServiceProxy.getClass().getName()+"."+remoteMethod,e);
			}
		}catch (SecurityException | IllegalAccessException | IllegalArgumentException e) {
			throw new CommonException(MC.MT_SERVICE_ERROR,remoteServiceProxy.getClass().getName()+"."+remoteMethod,e);
		}
		
		return p;
	}
}
