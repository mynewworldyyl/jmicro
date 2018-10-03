package org.jmicro.api.servicemanager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Channel;
import org.jmicro.api.annotation.CodecFactory;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Handler;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.annotation.Name;
import org.jmicro.api.annotation.ObjFactory;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.Registry;
import org.jmicro.api.annotation.Selector;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.Modifier;

public class ComponentManager<T> {

	private final static Logger logger = LoggerFactory.getLogger(ComponentManager.class);
	
	private static  Map<String,ComponentManager> cms = new ConcurrentHashMap<String,ComponentManager>();
	
	private Map<String,Class<?>> clsnameToClass = new ConcurrentHashMap<String,Class<?>>();
	private Map<String,Class<?>> annoToClass = new ConcurrentHashMap<String,Class<?>>();
	
	public static <T> ComponentManager<T>  getCommponentManager(Class<T> cls){
		if(cms.containsKey(cls.getName())){
			return cms.get(cls.getName());
		}
		ComponentManager<T> cm = new ComponentManager<T>(cls);
		cms.put(cls.getName(), cm);
		return cms.get(cls.getName());
	}
	
	private Class<?> cls = null;
	
	private Map<String,T> components = new ConcurrentHashMap<String,T>();

	private boolean isLoaded = false;
	
	private ComponentManager(Class cls){
		this.cls = cls;
	}
	
	public synchronized Collection<T> getComponents(){
		if(components.isEmpty()){
			getComponent(cls.getName());
		}
		Set<T> sets = new HashSet<T>();
		sets.addAll(components.values());
		return sets;
	}
	
	@SuppressWarnings("unchecked")
	private T instanceCommponent(Class<?> cls) {
		if(cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {	
			return null;
		}
		
		T com = null;
		if(!IObjectFactory.class.isAssignableFrom(cls)) {
			IObjectFactory of = getObjectFactory();
			com = (T)of.get(cls);
		} else {
			try {
				com = (T)cls.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CommonException("Instance ObjectFactory exception: "+cls.getName(),e);
			}
		}
		
		if(com != null) {
			components.put(cls.getName(), com);
			String annoName = getClassAnnoName(cls);
			if(!StringUtils.isEmpty(annoName)) {
				components.put(annoName, com);
			}
		}
		return com;
	}
	
	public synchronized T getComponent(String clsName){
		
		if(components.isEmpty()){
			this.loadClass();
			for(Class<?> c : clsnameToClass.values()){
		    	if(c.isInterface() || Modifier.isAbstract(c.getModifiers())) {	
		    		continue;
				}
		    	instanceCommponent(c);
		    }	
		}
		
		return components.get(clsName);
			
	}
	
	public static String getClassAnnoName(Class<?> cls) {

		cls = ProxyObject.getTargetCls(cls);
		if(cls.isAnnotationPresent(Name.class)){
			return cls.getAnnotation(Name.class).value();
		}else if(cls.isAnnotationPresent(Server.class)){
			return cls.getAnnotation(Server.class).value();
		}else if(cls.isAnnotationPresent(Channel.class)){
			return cls.getAnnotation(Channel.class).value();
		}else if(cls.isAnnotationPresent(Handler.class)){
			return cls.getAnnotation(Handler.class).value();
		}else if(cls.isAnnotationPresent(Interceptor.class)){
			return cls.getAnnotation(Interceptor.class).value();
		}else if(cls.isAnnotationPresent(Registry.class)){
			return cls.getAnnotation(Registry.class).value();
		}else if(cls.isAnnotationPresent(Selector.class)){
			return cls.getAnnotation(Selector.class).value();
		}else if(cls.isAnnotationPresent(Service.class)){
			return cls.getAnnotation(Service.class).value();
		}else if(cls.isAnnotationPresent(ObjFactory.class)){
			return cls.getAnnotation(ObjFactory.class).value();
		}else if(cls.isAnnotationPresent(Component.class)){
			return cls.getAnnotation(Component.class).value();
		}else if(cls.isAnnotationPresent(Reference.class)){
			return cls.getAnnotation(Reference.class).value();
		}else if(cls.isAnnotationPresent(CodecFactory.class)){
			return cls.getAnnotation(CodecFactory.class).value();
		}
		return cls.getName();
	
	}

	private synchronized void loadClass() {
		if(isLoaded || !components.isEmpty()){
			return;
		}
		isLoaded=true;
		Set<Class<?>> clses = ClassScannerUtils.getIns().loadClassByClass(cls);
		Utils.getIns().setClasses(clses, clsnameToClass);
	}

	public static synchronized IObjectFactory getObjectFactory(){
		String name = JMicroContext.get().getString(Constants.OBJ_FACTORY_KEY,Constants.DEFAULT_OBJ_FACTORY);
		IObjectFactory of = ComponentManager.getCommponentManager(IObjectFactory.class).getComponent(name);
		return of;
	}
	
	public static IRegistry getRegistry(String registryName){
		if(StringUtils.isEmpty(registryName)) {
			registryName = Constants.REGISTRY_KEY;
		}
		IRegistry registry = ComponentManager.getCommponentManager(IRegistry.class)
				.getComponent(registryName);
		if(registry == null){
			registry = ComponentManager.getCommponentManager(IRegistry.class)
					.getComponent(Constants.DEFAULT_REGISTRY);
		}
		if(registry == null){
			throw new CommonException("Registry not found");
		}
		return registry;
	}
	
}
