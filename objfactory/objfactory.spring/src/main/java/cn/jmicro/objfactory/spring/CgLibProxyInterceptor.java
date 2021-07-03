package cn.jmicro.objfactory.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import cn.jmicro.common.CommonException;

public class CgLibProxyInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(CgLibProxyInterceptor.class);
	
	private Object srcObj;
	private ClassLoader jmClassLoader;
	
	private ClassLoader bootstrapClassLoader;
	
	private ClassLoader systemClassLoader;
	
	public CgLibProxyInterceptor(Object srcObj) {
		if(srcObj == null) {
			throw new CommonException("Proxy source object cannot be null");
		}
		this.srcObj = srcObj;
		jmClassLoader = srcObj.getClass().getClassLoader();
		bootstrapClassLoader = Integer.class.getClassLoader();
		systemClassLoader = ClassLoader.getSystemClassLoader();
		
	}
	
	@Override
	public Object intercept(Object proxy, Method m, Object[] args, MethodProxy mp) throws Throwable {
		Class<?>[] types = m.getParameterTypes();
		
		//Object[] giveArgs = new Object[types.length];
		
		if(types != null && types.length > 0) {
			if(m.getName().equals("getRemoteServie")) {
				logger.info(m.toString());
			}
			for(int i = 0; i < types.length; i++) {
				Class<?> c = types[i];
				if(c.isPrimitive()) continue;
				ClassLoader cl = c.getClassLoader();
				if(cl == jmClassLoader || cl == bootstrapClassLoader || cl == systemClassLoader) continue;
				types[i] = jmClassLoader.loadClass(c.getName());
				if(args[i] != null) {
					args[i] = SpringAndJmicroComponent.createLazyProxyObjectByCglib(args[i],types[i].getName(),jmClassLoader);
				}
			}
		}
		
		Method srcm;
		try {
			srcm = srcObj.getClass().getMethod(m.getName(), types);
		} catch (NoSuchMethodException e) {
			logger.error(m.getDeclaringClass().getName() + "." + m.getName()+"," + argsType(types));
			throw e;
		}
		
		Object rst = null;
		try {
			rst = srcm.invoke(srcObj, args);
		} catch (InvocationTargetException e) {
			logger.error(m.getDeclaringClass().getName() + "." + m.getName()+"," + argsStr(args));
			throw e;
		}
		
		if(rst == null) return null;
		
		Class<?> rc = rst.getClass();
		
		if(rc.isPrimitive()) return rst;
		
		ClassLoader cl = rc.getClassLoader();
		if(cl == CgLibProxyInterceptor.class.getClassLoader() || cl == bootstrapClassLoader || cl == systemClassLoader) return rst;
		
		//types[i] = CgLibProxyInterceptor.class.getClassLoader().loadClass(rc.getna);
		return SpringAndJmicroComponent.createLazyProxyObjectByCglib(rst,rc.getName(),CgLibProxyInterceptor.class.getClassLoader());
	}

	private String argsType(Class<?>[] types) {
		if(types == null || types.length == 0) {
			return "null args";
		}
		StringBuffer sb = new StringBuffer();
		for(Class<?> ar : types ) {
			if(ar == null) sb.append("null,");
			else {
				sb.append(ar.getName()).append(",");
			}
		}
		return sb.toString();
	}

	private String argsStr(Object[] giveArgs) {
		if(giveArgs == null || giveArgs.length == 0) {
			return "null args";
		}
		StringBuffer sb = new StringBuffer();
		for(Object ar : giveArgs ) {
			if(ar == null) sb.append("null,");
			else {
				sb.append(ar.toString()).append(",");
			}
		}
		return sb.toString();
	}

}
