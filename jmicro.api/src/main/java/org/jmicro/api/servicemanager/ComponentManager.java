package org.jmicro.api.servicemanager;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.ClassScannerUtils;
import org.jmicro.common.Constants;
import org.jmicro.common.JMicroContext;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentManager<T> {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	private static  Map<String,ComponentManager> cms = new ConcurrentHashMap<String,ComponentManager>();
	
	private static Map<String,Class<?>> classes = new ConcurrentHashMap<String,Class<?>>();
	
	public static <T> ComponentManager<T>  getCommponentManager(Class<T> cls){
		if(cms.containsKey(cls.getName())){
			return cms.get(cls.getName());
		}
		ComponentManager<T> cm = new ComponentManager<T>(cls);
		cms.put(cls.getName(), cm);
		return cms.get(cls.getName());
	}
	
	private Class cls = null;
	
	private Map<String,T> components = new ConcurrentHashMap<String,T>();
	
	private ComponentManager(Class cls){
		this.cls = cls;
	}
	
	public synchronized T getComponent(String clsName){
		if(components.isEmpty()){
			this.loadComponent();
			Class cls = null;
			try {
				cls = Class.forName(clsName);
			} catch (ClassNotFoundException e) {
				logger.error("fail to loadd: "+clsName, e);
				throw new CommonException("fail to load class: "+clsName, e);
			}
			IObjectFactory of = getObjectFactory();
			T com = (T)of.createObject(cls);
			components.put(clsName, com);
		}
		return components.get(clsName);
	}
	

	private synchronized void loadComponent() {
		if(!components.isEmpty()){
			return;
		}
		Set<Class<?>> clses = null;
		if(cls.getSuperclass() == Annotation.class){
			clses = ClassScannerUtils.getIns().loadClassesByAnno(cls);
		}else {
			clses = ClassScannerUtils.getIns().loadClassByClass(cls);
		}
		Utils.getIns().setClasses(clses, classes);
	}
	
	public static IObjectFactory getObjectFactory(){
		String name = JMicroContext.get().getString(Constants.OBJ_FACTORY_KEY,Constants.DEFAULT_OBJ_FACTORY_KEY);
		IObjectFactory of = ComponentManager.getCommponentManager(IObjectFactory.class).getComponent(name);
		return of;	
	}
	
}
