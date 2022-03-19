package cn.jmicro.api.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.internal.async.IClientAsyncCallback;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

public class PromiseUtils {

	private static final Logger logger = LoggerFactory.getLogger(PromiseUtils.class);
	
	public static <R> IPromise<R> callService(Object remoteServiceProxy, String remoteMethod,Object contextObj, Object... args) {
		
		final JMicroContext cxt = JMicroContext.get();
		
		Promise<R> p = new Promise<R>();
		
		ServiceMethodJRso remoteMethd = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
		if(remoteMethd != null) {
			p.setTimeout(remoteMethd.getTimeout()*3);
		}
	
		p.setContext(contextObj);
		
		IClientAsyncCallback cb = new IClientAsyncCallback() {
			@SuppressWarnings("unchecked")
			@Override
			public void onResponse(RespJRso resp) {
				if(resp.getCode() == RespJRso.CODE_SUCCESS) {
					p.setResult((R)resp.getResult());
				} else {
					AsyncFailResult f = new AsyncFailResult();
					f.setCode(resp.getCode());
					f.setMsg(resp.getMsg());
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
