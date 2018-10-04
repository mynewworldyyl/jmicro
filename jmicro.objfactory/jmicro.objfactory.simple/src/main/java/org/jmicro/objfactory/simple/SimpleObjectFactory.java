/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.objfactory.simple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Lazy;
import org.jmicro.api.annotation.ObjFactory;
import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.client.AbstractServiceProxy;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IPostInitListener;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:12:24
 */
@ObjFactory(Constants.DEFAULT_OBJ_FACTORY)
public class SimpleObjectFactory implements IObjectFactory {

	private final static Logger logger = LoggerFactory.getLogger(ComponentManager.class);
	
	private static AtomicInteger idgenerator = new AtomicInteger();
	
	private static boolean isInit = false;
	
	private List<IPostInitListener> postListeners = new ArrayList<>();
	
	private Map<Class<?>,Object> objs = new ConcurrentHashMap<Class<?>,Object>();
	
	private Map<String,Object> nameToObjs = new ConcurrentHashMap<String,Object>();
	
	private Map<String,Object> clsNameToObjs = new ConcurrentHashMap<String,Object>();
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> cls) {
		Object obj = null;
		if(cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
			List<T> l = this.getByParent(cls);
			if(l.size() == 1) {
				obj =  l.get(0);
			}else if(l.size() > 1) {
				throw new CommonException("More than one instance of class ["+cls.getName()+"].");
			}
		}else {
			obj = objs.get(cls);
			if(obj != null){
				return  (T)obj;
			}
			obj = this.createObject(cls);
			if(obj != null) {
				cacheObj(cls,obj);
			}
		}
		return (T)obj;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getByName(String clsName) {
		if(this.clsNameToObjs.containsKey(clsName)){
			return (T) this.clsNameToObjs.get(clsName);
		}
		if(this.nameToObjs.containsKey(clsName)){
			return (T) this.nameToObjs.get(clsName);
		}
		Class<?> cls = ClassScannerUtils.getIns().getClassByAnnoName(clsName);
		if(cls != null){
			return (T)get(cls);
		}
		return null;
	}

	@Override
	public <T> List<T> getByParent(Class<T> parrentCls) {
		List<T> list = new ArrayList<>();
		Set<Class<?>> clazzes = ClassScannerUtils.getIns().loadClassByClass(parrentCls);
		for(Class<?> c: clazzes) {
			if(parrentCls.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())
					&& !Modifier.isInterface(c.getModifiers()) && Modifier.isPublic(c.getModifiers())) {
				Object obj = this.get(c);
				if(obj != null) {
					list.add((T)obj);
				}
			}
		}
		return list;
	}

	public Object createNoProxy(Class cls) {
		Object obj = objs.get(cls);
		if(obj != null && !(obj instanceof ProxyObject)){
			return  obj;
		}
		try {
			obj = cls.newInstance();
			doAfterCreate(obj);
			//will replace the proxy object if exist, this is no impact to client
			cacheObj(cls,obj);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException("Fail to create obj for ["+cls.getName()+"]",e);
		}
		return obj;
	}
	
	private void cacheObj(Class<?> cls,Object obj){
		objs.put(cls, obj);
		String comName = this.getComName(cls);
		if(!StringUtils.isEmpty(comName)){
			this.nameToObjs.put(comName, obj);
		}
		this.clsNameToObjs.put(cls.getName(), obj);
	}
	
	private <T> T createObject(Class<T> cls) {
		Object obj = null;
		try {
			if(!isLazy(cls)) {
				obj = cls.newInstance();
				 doAfterCreate(obj);
			} else {
				obj = createLazyProxyObject(cls);
			}
			
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException("Fail to create obj for ["+cls.getName()+"]",e);
		}
		return  (T)obj;
	}
	
     private void doAfterCreate(Object obj) {
    	 injectDepependencies(obj);
    	 notifyPrePostListener(obj);
    	 doInit(obj);
		 notifyAfterPostListener(obj);
	}
     
     private void notifyAfterPostListener(Object obj) {
 		if(this.postListeners.isEmpty()) {
 			return;
 		}
 		for(IPostInitListener l : this.postListeners){
 			l.afterInit(obj);
 		}	
 	}
     
	private void notifyPrePostListener(Object obj) {
		if(this.postListeners.isEmpty()) {
			return;
		}
		for(IPostInitListener l : this.postListeners){
			l.preInit(obj);
		}	
	}

	private boolean isLazy(Class<?> cls) {
		if(cls.isAnnotationPresent(Lazy.class)){
			Lazy lazy = cls.getAnnotation(Lazy.class);
			return lazy.value();
		}else if(cls.isAnnotationPresent(Component.class)){
			Component lazy = cls.getAnnotation(Component.class);
			return lazy.lazy();
		}
		return true;
	}

	public synchronized void start(){
		if(isInit){
			return;
		}
		isInit = true;
		
		Set<Class<?>> listeners = ClassScannerUtils.getIns().loadClassesByAnno(PostListener.class);
		if(listeners != null && !listeners.isEmpty()) {
			for(Class<?> c : listeners){
				PostListener comAnno = c.getAnnotation(PostListener.class);
				int mod = c.getModifiers();
				if(!comAnno.value() || Modifier.isAbstract(mod) 
						|| Modifier.isInterface(mod) || !Modifier.isPublic(mod)){
					continue;
				}
				
				try {
					IPostInitListener l = (IPostInitListener)c.newInstance();
					this.addPostListener(l);
				} catch (InstantiationException | IllegalAccessException e) {
					logger.error("Create IPostInitListener Error",e);
				}
			}
		}
		
		Set<Class<?>> clses = ClassScannerUtils.getIns().loadClassesByAnno(Component.class);
		if(clses != null && !clses.isEmpty()) {
			for(Class<?> c : clses){
				this.get(c);
				/*Component comAnno = c.getAnnotation(Component.class);
				if(comAnno.lazy()){
					Object obj = createLazyProxyObject(c);
					objs.put(c, obj);
					String comName = this.getComName(c);
					if(!StringUtils.isEmpty(comName)){
						this.nameToObjs.put(comName, obj);
					}
				} else {
					//createObject(c);
					this.get(c);
				}*/
			}
		}
	}

	@Override
	public void addPostListener(IPostInitListener listener) {
		for(IPostInitListener l : postListeners){
			if(l.getClass() == listener.getClass()) return;
		}
		postListeners.add(listener);
	}

	private void injectDepependencies(Object obj) {
		Class<?> cls = obj.getClass();
		Field[] fs = cls.getDeclaredFields();
		for(Field f : fs) {
			Object srv = null;
			boolean isRequired = false;
			if(f.isAnnotationPresent(Reference.class)){
				//Inject the remote service obj
				Reference ref = f.getAnnotation(Reference.class);
				/*
				String name = ref.value();
				if(StringUtils.isEmpty(name)) {
					name = f.getClass().getName();
				}
				*/
				isRequired = ref.required();
				srv = getServiceProxy(cls,f,ref);
			}else if(f.isAnnotationPresent(Inject.class)){
				//Inject the local component
				Inject inje = f.getAnnotation(Inject.class);
				String name = inje.value();
				isRequired = inje.required();
				Class<?> type = f.getType();
				if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
					List<?> l = this.getByParent(type);
					if(StringUtils.isEmpty(name)) {
						if(l.size() == 1) {
							srv =  l.get(0);
						}else if(l.size() > 1) {
							StringBuffer sb = new StringBuffer("More implement for type [").append(cls.getName()).append("] ");
							for(Object s : l) {
								sb.append(s.getClass()).append(",");
							}
							throw new CommonException(sb.toString());
						}
					}else {
						for(Object s : l) {
							String n = ComponentManager.getClassAnnoName(s.getClass());
							if(name.equals(n)){
								srv = s;
							}
						}
					}
				}else {
					srv = this.get(type);
				}
			}
			
			if(srv != null) {
				boolean bf = f.isAccessible();
				if(!bf) {
					f.setAccessible(true);
				}
				try {
					f.set(obj, srv);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new CommonException("",e);
				}
				if(!bf) {
					f.setAccessible(bf);
				}
			}else if(isRequired) {
				throw new CommonException("Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] not found");
			}
		}
		
	}

	private Object getServiceProxy(Class<?> becls,Field field,Reference ref) {
		IRegistry registry = ComponentManager.getRegistry(ref.registry());
		if(!registry.isExist(field.getType().getName()) && ref.required()) {
			throw new CommonException("Class ["+becls.getName()+"] field ["+ field.getName()+"] dependency ["+field.getType().getName()+"] not found");
		}
		
		//Set<ServiceItem> sis = registry.getServices(field.getName());
		if(!field.getType().isInterface()) {
			throw new CommonException("Class ["+becls.getName()+"] field ["+ field.getName()+"] dependency ["+field.getType().getName()+"] have to be interface class");
		}
		
		if(objs.containsKey(field.getType().getName())) {
			return objs.get(field.getType().getName());
		}
		
		Object proxy = createDynamicServiceProxy(field.getType());
		
		if(proxy instanceof AbstractServiceProxy){
			AbstractServiceProxy asp = (AbstractServiceProxy)proxy;
			asp.setHandler(this.getByName(Constants.DEFAULT_INVOCATION_HANDLER));
		}
		
		return proxy;
	}
	
	@SuppressWarnings("unchecked")
	private <T>  T createLazyProxyObject(Class<T> cls) {
		logger.debug("createLazyProxyObject: " + cls.getName());
		ClassGenerator cg = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
		cg.setClassName(cls.getName()+"$Jmicro"+idgenerator.getAndIncrement());
		cg.setSuperClass(cls.getName());
		cg.addInterface(ProxyObject.class);
		//cg.addDefaultConstructor();
		Constructor<?>[] cons = cls.getConstructors();
		Map<String,java.lang.reflect.Constructor<?>> consMap = new HashMap<>();
		String conbody = "this.conArgs=$args; for(int i = 0; i < $args.length; i++) { Object arg = $args[i]; this.conKey = this.conKey + arg.getClass().getName();}";
		for(Constructor<?> c : cons){
			String key = null;
			Class<?>[] ps = c.getParameterTypes();
			for(Class<?> p: ps){
				key = key + p.getName();
			}
			consMap.put(key, c);
			cg.addConstructor(c.getModifiers(),c.getParameterTypes(),c.getExceptionTypes(),conbody);
		}
		
		cg.addMethod("private void _init0() { if (this.init) return; this.init=true; this.target = ("+cls.getName()+")(factory.createNoProxy("+cls.getName()+".class));}");
		cg.addMethod("public Object getTarget(){ _init0(); return this.target;}");
		
		int index = 0;
		for(Method m : cls.getMethods()){
			if(Modifier.isPrivate(m.getModifiers()) || m.getDeclaringClass() == Object.class){
				continue;
			}
			StringBuffer sb = new StringBuffer();
			//sb.append("if (!this.init) { System.out.println( \"lazy init class:"+cls.getName()+"\"); this.init=true; this.target = ("+cls.getName()+")((java.lang.reflect.Constructor)constructors.get(this.conKey)).newInstance(this.conArgs);}");
			sb.append(" _init0();");
			sb.append(" Object v = methods[").append(index).append("].invoke(this.target,$args); ");	
			Class<?> rt = m.getReturnType();
			if (!Void.TYPE.equals(rt)) {
				sb.append(" return ").append(asArgument(rt, "v")).append(";");
			}
			cg.addMethod(m.getName(), m.getModifiers(), m.getReturnType(), m.getParameterTypes(),
					m.getExceptionTypes(), sb.toString());
			index++;
		} 
		cg.addField("private boolean init=false;");
		cg.addField("private "+cls.getName()+" target=null; ");
		cg.addField("private java.lang.String conKey;");
		cg.addField("private java.lang.Object[] conArgs;");
		
		cg.addField("public static java.lang.reflect.Method[] methods;");
		cg.addField("public static org.jmicro.objfactory.simple.SimpleObjectFactory factory;");
		
		Class<?> cl = cg.toClass();
		
		try {
			cl.getField("methods").set(null, cls.getMethods());
			cl.getField("factory").set(null, this);
			Object o = cl.newInstance();
			//doAfterCreate(o);
			return (T)o;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | InstantiationException e) {
			logger.error("Create lazy proxy error for: "+ cls.getName(), e);
		}
		return null;
	}

	public  static <T> T createDynamicServiceProxy(Class<T> cls) {
		 ClassGenerator classGenerator = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
		 classGenerator.setClassName(cls.getName()+"$Jmicro"+idgenerator.getAndIncrement());
		 classGenerator.setSuperClass(AbstractServiceProxy.class);
		 classGenerator.addInterface(cls);
		 classGenerator.addDefaultConstructor();
		 classGenerator.addInterface(ProxyObject.class);
		 
		 classGenerator.addField("public static java.lang.reflect.Method[] methods;");
		 //classGenerator.addField("private " + InvocationHandler.class.getName() + " handler = new org.jmicro.api.client.ServiceInvocationHandler(this);");
		 classGenerator.addDefaultConstructor();
        
		 //StringBuffer conBody = new StringBuffer();
		 
		 //ccm.addMethod("public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new " + pcn + "($1); }");
		 //classGenerator.addConstructor(Modifier.PUBLIC, new Class[]{InvocationHandler.class}, body);
		 
		/* StringBuffer sb = new StringBuffer( "public org.jmicro.objfactory.simple.ProxyService ps = new org.jmicro.objfactory.simple.ProxyService(\"");
		 sb.append(cls.getName()).append("\");");
		 classGenerator.addField(sb.toString());
		 StringBuffer body = new StringBuffer();*/
		
		 //public Object invoke(String method,Object... args) 
		 //classGenerator.addMethod("public Object invoke(String method,Object... args) {return super(method,args);} ");
		 //classGenerator.addMethod(AbstractServiceProxy.class.getMethod("invoke", {}));
		 
		 Method[] ms = cls.getDeclaredMethods();
		 
		 for(int i =0; i < ms.length; i++) {
			 Method m = ms[i];
			 if (m.getDeclaringClass() == Object.class || !Modifier.isPublic(m.getModifiers())){
				 continue;
			 }
			 
			 Class<?> rt = m.getReturnType();
             Class<?>[] pts = m.getParameterTypes();

             StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
             for (int j = 0; j < pts.length; j++){
            	 code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
             }    
             code.append(" Object ret = handler.invoke(this, methods[" + i + "], args);");
             
             if (!Void.TYPE.equals(rt))
                 code.append(" return ").append(asArgument(rt, "ret")).append(";");

             classGenerator.addMethod(m.getName(), m.getModifiers(), rt, pts, m.getExceptionTypes(), code.toString());      
		 }
		 
		 Class<?> clazz = classGenerator.toClass();

         try {
        	 clazz.getField("methods").set(null, ms);
			return (T)clazz.newInstance();
		} catch (InstantiationException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
			throw new CommonException("Fail to create proxy ["+ cls.getName()+"]");
		}
	}

	 public static String asArgument(Class<?> cl, String name) {
	        if (cl.isPrimitive()) {
	            if (Boolean.TYPE == cl)
	                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
	            if (Byte.TYPE == cl)
	                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
	            if (Character.TYPE == cl)
	                return name + "==null?(char)0:((Character)" + name + ").charValue()";
	            if (Double.TYPE == cl)
	                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
	            if (Float.TYPE == cl)
	                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
	            if (Integer.TYPE == cl)
	                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
	            if (Long.TYPE == cl)
	                return name + "==null?(long)0:((Long)" + name + ").longValue()";
	            if (Short.TYPE == cl)
	                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
	            throw new RuntimeException(name + " is unknown primitive type.");
	        }
	        return "(" + ReflectUtils.getName(cl) + ")" + name;
	    }
	 
	 private String getComName(Class<?> cls) {
		 return ComponentManager.getClassAnnoName(cls);
	 }
	 
	private void doInit(Object obj) {
		Method initMethod1 = null;
		Method initMethod2 = null;
		for(Method m : obj.getClass().getDeclaredMethods()) {
			if(m.isAnnotationPresent(JMethod.class)) {
				JMethod jm = m.getAnnotation(JMethod.class);
				if("init".equals(jm.value())) {
					initMethod1 = m;
					break;
				}
			}else if(m.getName().equals("init")) {
				initMethod2 = m;
			}
		}
		try {
			if(initMethod1 != null) {
				initMethod1.invoke(obj, new Object[]{});
			}else if(initMethod2 != null){
				initMethod2.invoke(obj, new Object[]{});
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
