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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Reference;
import org.jmicro.api.client.AbstractServiceProxy;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.jmicro.common.url.ClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月6日-上午11:23:20
 */
class RemoteServiceManager {
	
	private final static Logger logger = LoggerFactory.getLogger(RemoteServiceManager.class);
	
	private SimpleObjectFactory of = null;
	
	//key is a interface class(key = serviceName + namespace + version), value is a proxy object
	private Map<String,Object> remoteObjects = new ConcurrentHashMap<String,Object>();
	
	//private Set<Class<?>> refServiceClass = new HashSet<>();
	
	RemoteServiceManager(SimpleObjectFactory o){
		this.of = o;
	}
	
	void init(){

	}
	
	Object createService(Object obj,Field f){
		if(!f.isAnnotationPresent(Reference.class)){
			logger.warn("cls:["+obj.getClass().getName()+"] not annotated with ["+Reference.class.getName()+"]");
			return null;
		}
		
		Object proxy = null;
		
		Class<?> becls = ProxyObject.getTargetCls(obj.getClass());
		Reference ref = f.getAnnotation(Reference.class);
		Class<?> type = f.getType();
		
		IRegistry registry = ComponentManager.getRegistry(null);
		
		if(List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)){
			//集合服务引用
			proxy = createSetService(obj,f,becls,ref);
		}/*else if(){
			proxy = createSetService(obj,f,becls,ref);
		}*/else{
			//单个服务引用
			//Set<ServiceItem> sis = registry.getServices(field.getName());
			if(!type.isInterface()) {
				throw new CommonException("Class ["+becls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] have to be interface class");
			}
			boolean enable = true;
			if(!registry.isExist(f.getType().getName(),ref.namespace(),ref.version())) {
				if(ref.required()){
					//服务不可用，并且服务依赖是必须满足，则报错处理
					StringBuffer sb = new StringBuffer("Class [");
					sb.append(becls.getName()).append("] field [").append(f.getName())
					.append("] dependency [").append(f.getType().getName())
					.append("] not found version [").append(ref.version()).append("] namespace [").append(ref.namespace()).append("]");
					throw new CommonException(sb.toString());
				}
				//服务不可用，但还是可以生成服务代理，直到使用时才决定服务的可用状态
				enable = false;
			} 
			Set<ServiceItem> sis = registry.getServices(f.getType().getName(),ref.namespace(),ref.version());
			if(sis == null || sis.isEmpty()){
				proxy = createProxyService(f,null,enable,obj);
			}else {
				proxy = createProxyService(f,sis.iterator().next(),enable,obj);
			}
			
		}
		return proxy;
	}
	
	Object createProxyService(Field f,ServiceItem si, boolean enable,Object srcObj){
		
		Reference ref = f.getAnnotation(Reference.class);
		Class<?> type = this.getEltType(f);
		
		String key = "";
		if(si != null){
			//call from set field
			key = si.serviceName();
		}else {
			//call from singleton service reference
			key = ServiceItem.serviceName(type.getName(),ref.namespace(),ref.version());
		}
		
		if(this.remoteObjects.containsKey(key)) {
			return remoteObjects.get(key);
		}

		//boolean enable = items != null && !items.isEmpty();
		Object proxy = null;
		if(si != null){
			//call from set field
			proxy = createDynamicServiceProxy(type,si.getNamespace(),si.getVersion(),enable);
		}else {
			//call from singleton service reference
			proxy = createDynamicServiceProxy(type,ref.namespace(),ref.version(),enable);
		}
		//ServiceItem si = items.iterator().next();
		
		setHandler(proxy,key,si);
		
		IRegistry registry = ComponentManager.getRegistry(null);
		RemoteProxyServiceListener lis = new RemoteProxyServiceListener(this,proxy,srcObj,f);
		registry.addServiceListener(ServiceItem.serviceName(f.getType().getName(),ref.namespace(),ref.version()), lis);
			
		 if(proxy != null){
			 AbstractServiceProxy asp = (AbstractServiceProxy)proxy;
			 asp.setHandler(of.getByName(Constants.DEFAULT_INVOCATION_HANDLER));
			 asp.setItem(si);
			 remoteObjects.put(key, proxy);
		 }
		 return proxy;
	}
	
	private Class<?> getEltType(Field f){

		if(Collection.class.isAssignableFrom(f.getType())){
			ParameterizedType genericType = (ParameterizedType) f.getGenericType();
			if(genericType == null) {
				throw new CommonException("Must be ParameterizedType for cls:"+ f.getDeclaringClass().getName()+",field: "+f.getName());
			}
			Class<?> ctype = (Class<?>)genericType.getActualTypeArguments()[0];
			return ctype;
		}
		
		return f.getType();
	}
	
	private Object createSetService(Object obj, Field f, Class<?> becls, Reference ref) {

	
		Class<?> ctype = getEltType(f);
		
		Class<?> type = f.getType();
		IRegistry registry = ComponentManager.getRegistry(null);
		
		if(!registry.isExist(f.getType().getName(), ref.namespace(),ref.version()) && ref.required()) {
			StringBuffer sb = new StringBuffer("Class [");
			sb.append(becls.getName()).append("] field [").append(f.getName())
			.append("] dependency [").append(f.getType().getName())
			.append("] not found version [").append(ref.version()).append("] namespace [").append(ref.namespace()).append("]");
			throw new CommonException(sb.toString());
		}
		
		//请参考Reference说明使用
		Set<ServiceItem> items = registry.getServices(ctype.getName());
		

		boolean bf = f.isAccessible();
		Object o = null;
		if(!bf) {
			f.setAccessible(true);
		}
		try {
			o = f.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new CommonException("",e);
		}
		if(!bf) {
			f.setAccessible(bf);
		}
		
		if(o == null){
			if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
				o = new HashSet<Object>();
			} else {
				try {
					//变量声明的类型是Set的子实现类型，如HashSet
					o = type.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new CommonException("",e);
				}
			}
		}
		
		RemoteProxyServiceListener lis = new RemoteProxyServiceListener(this,o,obj,f);
		//registry.addServiceListener(ServiceItem.serviceName(f.getType().getName(),ref.namespace(),ref.version()), lis);
		
		Collection<Object> el = (Collection<Object>)o;
		
		Map<String,Object> exists = new HashMap<>();
		Object po = null;
		if(!items.isEmpty()){
			for(ServiceItem si : items){
				String key = si.serviceName();
				if(exists.containsKey(key)){
					continue;
				}
				
				if(this.remoteObjects.containsKey(key)) {
					po = remoteObjects.get(key);
				}else {
					po = createDynamicServiceProxy(ctype,si.getNamespace(),si.getVersion(),true);
					//registry.addServiceListener(ServiceItem.serviceName(si.getServiceName(),si.getNamespace(),si.getVersion()), lis);
					setHandler(po,key,si);
				}
				
				if(po != null){
					el.add(po);
				}
			}
		}
		
		//接受实现相同接口的所有服务，具体使用那个服务，由使用者去判断
		registry.addServiceNameListener(ctype.getName(), lis);
		
		exists.clear();
		exists = null;
		return el;
		
	}

	
	private void setHandler(Object proxy,String key,ServiceItem si){
		 if(proxy != null){
		    	AbstractServiceProxy asp = (AbstractServiceProxy)proxy;
				asp.setHandler(of.getByName(Constants.DEFAULT_INVOCATION_HANDLER));
				asp.setItem(si);
				remoteObjects.put(key, proxy);
		}
	}
	
	public  static <T> T createDynamicServiceProxy(Class<T> cls, String namespace, String version,boolean enable) {
		 ClassGenerator classGenerator = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
		 classGenerator.setClassName(cls.getName()+"$Jmicro"+SimpleObjectFactory.idgenerator.getAndIncrement());
		 classGenerator.setSuperClass(AbstractServiceProxy.class);
		 classGenerator.addInterface(cls);
		 classGenerator.addDefaultConstructor();
		 classGenerator.addInterface(ProxyObject.class);
		 
		 classGenerator.addField("public static java.lang.reflect.Method[] methods;");
		 //classGenerator.addField("private " + InvocationHandler.class.getName() + " handler = new org.jmicro.api.client.ServiceInvocationHandler(this);");
		 classGenerator.addDefaultConstructor();
       
		 classGenerator.addField("private boolean enable="+enable+";");
		 classGenerator.addMethod("public java.lang.String getNamespace(){ return \"" + namespace + "\";}");
		 classGenerator.addMethod("public java.lang.String getVersion(){ return \"" + version + "\";}");
		 classGenerator.addMethod("public java.lang.String getServiceName(){ return \"" + cls.getName() + "\";}");
		 classGenerator.addMethod("public boolean enable(){  return this.enable;}");
		 classGenerator.addMethod("public void enable(boolean en){ this.enable=en;}");
		 //classGenerator.addMethod("public Object key(){ _init0(); return \"" + ServiceItem.serviceName(cls.getName(), namespace, version) + "\";}");
		 
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
           	 code.append(" args[").append(j).append("] = ("+pts[j].getName()+")$").append(j + 1).append(";");
            }    
            code.append(" Object ret = handler.invoke(this, methods[" + i + "], args);");
            
            if (!Void.TYPE.equals(rt))
                code.append(" return ").append(SimpleObjectFactory.asArgument(rt, "ret")).append(";");

            classGenerator.addMethod(m.getName(), m.getModifiers(), rt, pts, m.getExceptionTypes(), code.toString());      
		 }
		 
		//logger.debug(classGenerator.);
		 //classGenerator.getClassPool().
		 Class<?> clazz = classGenerator.toClass();

        try {
       	 clazz.getField("methods").set(null, ms);
			T proxy = (T)clazz.newInstance();
			return proxy; 
		} catch (InstantiationException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
			throw new CommonException("Fail to create proxy ["+ cls.getName()+"]");
		}
	}
}
