package cn.jmicro.objfactory.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.objfactory.spring.bean.AopTargetUtils;

public class CgLibProxyInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(CgLibProxyInterceptor.class);
	
	private Object srcObj;
	private ClassLoader srcClassLoader;
	
	private ClassLoader bootstrapClassLoader;
	
	private ClassLoader systemClassLoader;
	
	public CgLibProxyInterceptor(Object srcObj) {
		if(srcObj == null) {
			throw new CommonException("Proxy source object cannot be null");
		}
		this.srcObj = srcObj;//被代理对象
		srcClassLoader = srcObj.getClass().getClassLoader();
		bootstrapClassLoader = Integer.class.getClassLoader();
		systemClassLoader = ClassLoader.getSystemClassLoader();
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	@Override
	public Object intercept(Object proxy, Method m, Object[] args, MethodProxy mp) throws Throwable {
		Class<?>[] types = m.getParameterTypes();
		//logger.info("actInfo is null: {} ",JMicroContext.get().getAccount()==null);
		
		//Object[] giveArgs = new Object[types.length];
		
		if(types != null && types.length > 0) {
			if(m.getName().equals("getRemoteServie")) {
				logger.info(m.toString());
			}
			for(int i = 0; i < types.length; i++) {
				Class<?> c = types[i];
				if(c.isPrimitive()) continue;
				ClassLoader cl = c.getClassLoader();
				if(cl == srcClassLoader || cl == bootstrapClassLoader || cl == systemClassLoader) continue;
				types[i] = srcClassLoader.loadClass(c.getName());
				if(args[i] != null) {
					args[i] = SpringAndJmicroComponent.createLazyProxyObjectByCglib(args[i],types[i].getName(),srcClassLoader);
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
		if(cl == proxy.getClass().getClassLoader() || cl == bootstrapClassLoader || cl == systemClassLoader) return rst;
		
		//types[i] = CgLibProxyInterceptor.class.getClassLoader().loadClass(rc.getna);
		
		if(rst instanceof RespJRso) {
			Class<?> trst = proxy.getClass().getClassLoader().loadClass(RespJRso.class.getName());
			Object tobj = copyRespData((RespJRso)rst,trst);
			return tobj;
		} else if(rst instanceof PromiseImpl){
			PromiseImpl p = (PromiseImpl)rst;
			Object o = p.getResult();
			if(o != null && o instanceof RespJRso) {
				Class<?> trst = proxy.getClass().getClassLoader().loadClass(RespJRso.class.getName());
				Object tobj = copyRespData((RespJRso)o,trst);
				p.setResult(copyRespData((RespJRso)o,trst));
			}
			return SpringAndJmicroComponent.createLazyProxyObjectByCglib(rst,rc.getName(),proxy.getClass().getClassLoader());
		} else {
			return SpringAndJmicroComponent.createLazyProxyObjectByCglib(rst,rc.getName(),proxy.getClass().getClassLoader());
		}
		
	}

	private Object copyRespData(RespJRso rst, Class<?> trst) {
		try {
			Object o = AopTargetUtils.getTarget(rst);
			return JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(o), trst);
		} catch (Exception e) {
			logger.error("",e);
		}
		return null;
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

	public Object getSrcObj() {
		return srcObj;
	}

}
