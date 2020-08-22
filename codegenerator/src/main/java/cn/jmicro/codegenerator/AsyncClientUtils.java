package cn.jmicro.codegenerator;

import java.lang.reflect.Method;

public class AsyncClientUtils {

	
	public static final String genServiceName(String fullName) {
		String iname = fullName;
		if(fullName.endsWith(AsyncClientProxy.IMPL_SUBFIX)) {
			 String cn = fullName.substring(0, fullName.indexOf(AsyncClientProxy.IMPL_SUBFIX));
			 String pkgName = cn.substring(0,cn.lastIndexOf("."));
			 pkgName = pkgName.substring(0, pkgName.indexOf(AsyncClientProxy.PKG_SUBFIX));
			 String simpleClassName = "I"+cn.substring(cn.lastIndexOf(".")+1,cn.length());
			 iname = pkgName + simpleClassName;
		 }else if(fullName.endsWith(AsyncClientProxy.INT_SUBFIX)) {
			 String cn = fullName.substring(0, fullName.indexOf(AsyncClientProxy.INT_SUBFIX));
			 String pkgName = cn.substring(0,cn.lastIndexOf("."));
			 pkgName = pkgName.substring(0, pkgName.indexOf(AsyncClientProxy.PKG_SUBFIX));
			 String simpleClassName = cn.substring(cn.lastIndexOf(".")+1,cn.length());
			 iname = pkgName + simpleClassName;
		 }
		 return iname;
	}
	
	public static final String genAsyncServiceName(String fullName) {

		 if(fullName.endsWith(AsyncClientProxy.INT_SUBFIX)) {
			 return fullName;
		 }

		 if(fullName.endsWith(AsyncClientProxy.IMPL_SUBFIX)) {
			 //fullName是异步接口实现类
			 return fullName.substring(0,fullName.length() - AsyncClientProxy.IMPL_SUBFIX.length());
		 }
		
		 //fullName是服务名称
		 String pkgName = "";
		 
		 int idx = fullName.lastIndexOf(".");
		 if(idx > 0) {
			 pkgName = fullName.substring(0,idx) + "." + AsyncClientProxy.PKG_SUBFIX;
		 } else {
			 pkgName = AsyncClientProxy.PKG_SUBFIX;
		 }
		
		 String cln = fullName.substring(idx+1)+AsyncClientProxy.INT_SUBFIX;
		 return pkgName + "." + cln;
	}
	
	public static final String genAsyncServiceImplName(String fullName) {
		
		 if(fullName.endsWith(AsyncClientProxy.IMPL_SUBFIX)) {
			 return fullName;
		 }

		 if(fullName.endsWith(AsyncClientProxy.INT_SUBFIX)) {
			 //fullName是异步接口名称
			 return fullName + AsyncClientProxy.IMPL;
		 }
		
		 //fullName是服务名称
		 String pkgName = "";
		 
		 int idx = fullName.lastIndexOf(".");
		 if(idx > 0) {
			 pkgName = fullName.substring(0,idx) + "." + AsyncClientProxy.PKG_SUBFIX;
		 } else {
			 pkgName = AsyncClientProxy.PKG_SUBFIX;
		 }
		
		 String cln = fullName.substring(idx+1);
		 if(cln.startsWith("I")) {
			  cln = cln.substring(1) + AsyncClientProxy.IMPL_SUBFIX;
		 } else {
			 cln = cln + AsyncClientProxy.IMPL_SUBFIX;
		 }
		 
		 return pkgName + "." + cln;
	}
	
	public static final String genAsyncMethodName(String methodName) {
		if(!methodName.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX)) {
			return methodName + AsyncClientProxy.ASYNC_METHOD_SUBFIX;
		}
		return methodName;
	}
	
	
	public static final Class<?> parseServiceClass(Class<?> type) {
		String cn = type.getSimpleName();
		if(cn.endsWith(AsyncClientProxy.INT_SUBFIX)) {
			return type.getInterfaces()[0];
		}else if(cn.endsWith(AsyncClientProxy.IMPL_SUBFIX)) {
			return type.getInterfaces()[0].getInterfaces()[0];
		}
		return type;
	}
	
	public static final Method getMethod(Class<?> cls,String methodName) {
		Method[] ms = cls.getMethods();
		if(ms != null && ms.length > 0) {
			for(Method m : ms) {
				if(m.getName().equals(methodName)) {
					return m;
				}
			}
		}
		return null;
	}
	
	public static final Method getMethod(Class<?> cls,String methodName,Class<?> paramClses) throws NoSuchMethodException, SecurityException {
		return cls.getMethod(methodName, paramClses);
	}
	
}
