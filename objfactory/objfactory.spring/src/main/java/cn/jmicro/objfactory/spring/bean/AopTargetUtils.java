package cn.jmicro.objfactory.spring.bean;

import java.lang.reflect.Field;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;

import cn.jmicro.objfactory.spring.CgLibProxyInterceptor;

public class AopTargetUtils {  
	  
    
    /** 
     * 获取 目标对象 
     * @param proxy 代理对象 
     * @return  
     * @throws Exception 
     */  
    public static Object getTarget(Object proxy) throws Exception {  
          
    	String name = proxy.getClass().getName();
    	if(!name.contains("$$EnhancerBy")) {
    		return proxy;
    	}
    	
    	 Object obj =  getCglibProxyTargetObject(proxy);  
    	 return getTarget(obj);
    	 
        /*if(!AopUtils.isAopProxy(proxy)) {  
            return proxy;//不是代理对象  
        } */ 
          
      /*  if(AopUtils.isJdkDynamicProxy(proxy)) {  
            return getJdkDynamicProxyTargetObject(proxy);  
        } else { //cglib  
            return getCglibProxyTargetObject(proxy);  
        }  */
          
    }  
  
  
    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {  
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");  
        h.setAccessible(true);  
        Object dynamicAdvisedInterceptor = h.get(proxy);  
          
        if(dynamicAdvisedInterceptor instanceof CgLibProxyInterceptor) {
        	CgLibProxyInterceptor da = (CgLibProxyInterceptor)dynamicAdvisedInterceptor;
        	return da.getSrcObj();
        }
        return dynamicAdvisedInterceptor;  
    }  
  
  
    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {  
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");  
        h.setAccessible(true);  
        AopProxy aopProxy = (AopProxy) h.get(proxy);  
          
        Field advised = aopProxy.getClass().getDeclaredField("advised");  
        advised.setAccessible(true);  
          
        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();  
          
        return target;  
    }  
      
}  
