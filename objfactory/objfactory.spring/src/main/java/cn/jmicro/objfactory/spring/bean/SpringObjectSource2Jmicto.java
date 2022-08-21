package cn.jmicro.objfactory.spring.bean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.objfactory.spring.SpringAndJmicroComponent;

//@Component
public class SpringObjectSource2Jmicto implements IObjectSource{

	private final static Logger logger = LoggerFactory.getLogger(SpringObjectSource2Jmicto.class);
	
	private final Map<String,Object> springObjects = new HashMap<>();
	
	private final Map<Class<?>,Object> springObjectTypes = new HashMap<>();
	
	private final Map<Class,Set> springObjectParent = new HashMap<>();
	
	private ApplicationContext cxt;
	
	private ClassLoader jmicroRpcClassloader;
	
	@Override
	public <T> T  get(Class type) {
		if(springObjectTypes.containsKey(type)) return (T)springObjectTypes.get(type);
		try {
			Class<?> cls = SpringObjectSource2Jmicto.class.getClassLoader().loadClass(type.getName());
			Object obj = cxt.getBean(cls);
			if(obj != null) {
				if(this.jmicroRpcClassloader == null) {
					this.jmicroRpcClassloader = Thread.currentThread().getContextClassLoader();
				}
				
				T c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),jmicroRpcClassloader);
				springObjectTypes.put(type, c);
				return c;
			}
		} catch (NoSuchBeanDefinitionException | ClassNotFoundException e) {
			logger.debug("get: "+e.getMessage());
			return null;
		}
		return null;
	}

	@Override
	public <T> T  getByName(String name) {
		if(springObjects.containsKey(name)) return (T)springObjects.get(name);
		try {
			Object obj = cxt.getBean(name);
			if(obj == null) return null;
			T c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),jmicroRpcClassloader);
			springObjects.put(name, c);
			return c;
		} catch (NoSuchBeanDefinitionException e) {
			logger.debug("getByName: "+e.getMessage());
			return null;
		}
	}

	@Override
	public <T> Set<T> getByParent(Class type) {
		if(springObjectParent.containsKey(type)) return (Set<T>)springObjectParent.get(type);
		try {
			Class cls = SpringObjectSource2Jmicto.class.getClassLoader().loadClass(type.getName());
			Map<String,T> objs = cxt.getBeansOfType(cls);
			if(objs == null || objs.isEmpty()) return null;
			Set<T> set = new HashSet<>();
			
			ClassLoader cl = this.jmicroRpcClassloader;
			if(cl == null) {
				cl = Thread.currentThread().getContextClassLoader();
				if(cl == null) {
					cl = SpringObjectSource2Jmicto.class.getClassLoader();
				}
			}
			
			for(T o : objs.values()) {
				Object c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(o,AopUtils.getTargetClass(o).getName(),cl);
				set.add((T)c);
			}
			springObjectParent.put(type, set);
			return set;
		} catch (NoSuchBeanDefinitionException | ClassNotFoundException e) {
			logger.debug("getByParent: "+e.getMessage());
			return null;
		}
	}

	public ClassLoader getJmicroRpcClassloader() {
		return jmicroRpcClassloader;
	}

	public void setJmicroRpcClassloader(ClassLoader jmicroRpcClassloader) {
		this.jmicroRpcClassloader = jmicroRpcClassloader;
	}

	public ApplicationContext getCxt() {
		return cxt;
	}

	public void setCxt(ApplicationContext cxt) {
		this.cxt = cxt;
	}

}
