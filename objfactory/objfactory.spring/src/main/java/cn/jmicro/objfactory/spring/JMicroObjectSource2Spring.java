package cn.jmicro.objfactory.spring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.support.AopUtils;

import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.common.CommonException;

//@Component
public class JMicroObjectSource2Spring  implements IObjectSource {
	
	private final Map<String,Object> jmicroObjects = new HashMap<>();
	
	private final Map<Class,Object> typeObjects = new HashMap<>();
	
	//@Autowired
	private IObjectFactory of;
	
	private ClassLoader jmicroRpcClassloader;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> T get(Class type) {
		if(typeObjects.containsKey(type)) return (T)typeObjects.get(type);
		try {
			Class jmClass = jmicroRpcClassloader.loadClass(type.getName());
			Object obj = of.get(jmClass);
			if(obj == null) return null;
			T c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),SpringAndJmicroComponent.class.getClassLoader());
			typeObjects.put(type, c);
			return c;
		} catch (ClassNotFoundException e) {
			throw new CommonException("get: " + type.getName(),e);
		}
	}

	@Override
	public <T> T getByName(String name) {
		if(jmicroObjects.containsKey(name)) return (T)jmicroObjects.get(name);
		Object obj = of.getByName(name);
		if(obj == null) return null;
		T c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),SpringAndJmicroComponent.class.getClassLoader());
		jmicroObjects.put(name, c);
		return c;
	}

	@Override
	public <T> Set<T> getByParent(Class type) {
		try {
			Class jmClass = jmicroRpcClassloader.loadClass(type.getName());
			Set<T> obj = of.getByParent(type);
			if(obj == null) return null;
			
			Set<T> rst = new HashSet<>();
			
			for(Object so : obj) {
				Object c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(so,
						AopUtils.getTargetClass(so).getName(),
						SpringAndJmicroComponent.class.getClassLoader());
				rst.add((T)c);
			}
			return rst;
		} catch (ClassNotFoundException e) {
			throw new CommonException("getByParent: " + type.getName(),e);
		}
	}

	public IObjectFactory getOf() {
		return of;
	}

	public void setOf(IObjectFactory of) {
		this.of = of;
	}

	public ClassLoader getJmicroRpcClassloader() {
		return jmicroRpcClassloader;
	}

	public void setJmicroRpcClassloader(ClassLoader jmicroRpcClassloader) {
		this.jmicroRpcClassloader = jmicroRpcClassloader;
	}
	
}
