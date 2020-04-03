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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmicro.api.annotation.Async;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.api.service.ICheckable;
import org.jmicro.common.CommonException;
import org.jmicro.common.Utils;
import org.jmicro.common.util.ClassGenerator;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.ReflectUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yulei Ye
 * @date 2018年10月6日-上午11:23:20
 */
class ClientServiceProxyManager {
	
	private final static Logger logger = LoggerFactory.getLogger(ClientServiceProxyManager.class);
	
	
	private IRegistry registry = null;
	
	//key is a interface class(key = serviceName + namespace + version), value is a proxy object
	//private Map<String,Object> remoteObjects = new ConcurrentHashMap<String,Object>();
	
	//private Set<Class<?>> refServiceClass = new HashSet<>();
	
	private SimpleObjectFactory of;
	
	ClientServiceProxyManager(SimpleObjectFactory of){
		this.of = of;
	}
	
	void init(){
		this.registry = of.get(IRegistry.class);
	}
	
	/**
	 * 取客户端服务动态代理实例
	 * 创建对应 srvName, namespace, version的服务代理，其中acs指定的forMethod方法将会被异步调用
	 * @param srvName
	 * @param namespace
	 * @param version
	 * @param cl
	 * @param acs 需要异步调用的目标方法 ，即forMethod
	 * @return
	 */
	@SuppressWarnings("unchecked")
	<T> T  getRefRemoteService(String srvName,String namespace,String version,
			ClassLoader cl, AsyncConfig[] acs){
		String key = UniqueServiceKey.serviceName(srvName, namespace, version).toString();
		/*Object proxy = remoteObjects.get(key);
		if(proxy != null){
			return (T)proxy;
		}*/
		ClassLoader useCl = Thread.currentThread().getContextClassLoader();
		Object proxy = null;
		try {
			Class<?> cls = this.loadClass(srvName, cl);
			proxy = createDynamicServiceProxy(cls,namespace,version,acs);
			Set<ServiceItem> items = registry.getServices(srvName, namespace, version);
			if(items != null && !items.isEmpty()) {
				AbstractClientServiceProxy ap = (AbstractClientServiceProxy)proxy;
				ServiceItem si = items.iterator().next();
				ap.setItem(si);
				registerAsyncService(acs,items);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(useCl);
		}
		this.initProxy(proxy,key);
		return (T)proxy;
	}
	
	@SuppressWarnings("unchecked")
	<T> T  getRefRemoteService(String srvName, AsyncConfig[] acs){
		Class<?> cls = loadClass(srvName,null);
		return getRefRemoteService(cls,acs);
	}
	
	/**
	 * 由接口类生成远程代理对象
	 * @param srvClazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	<T> T getRefRemoteService(Class<?> srvClazz, AsyncConfig[] acs){
		if(!srvClazz.isAnnotationPresent(Service.class)){
			//通过接口创建的服务必须有Service注解,否则没办法获取服务3个座标信息
			throw new CommonException("Cannot create service proxy ["+srvClazz.getName()
			+"[ without anno [" +Service.class.getName() +"]");
		}
		Service srvAnno = srvClazz.getAnnotation(Service.class);
		return this.getRefRemoteService(srvClazz.getName(), srvAnno.namespace(),srvAnno.version(), null,acs);
	}
	
	/**
	 * 创建对应item的服务代理，其中acs指定的forMethod方法将会被异步调用
	 * @param item
	 * @param cl
	 * @param acs 需要异步调用的目标方法 ，即forMethod
	 * @return
	 */
	<T> T getRefRemoteService(ServiceItem item,ClassLoader cl, AsyncConfig[] acs) {
		/*Object proxy = remoteObjects.get(item.serviceName());
		if(proxy != null){
			return (T)proxy;
		}*/
		Class<?> cls = this.loadClass(item.getKey().getServiceName(), cl);

		Object proxy = createDynamicServiceProxy(cls,item.getKey().getNamespace(),item.getKey().getVersion(),acs);
		AbstractClientServiceProxy asp = (AbstractClientServiceProxy)proxy;
		asp.setItem(item);
		this.initProxy(proxy, item.serviceKey());
		if(acs != null && acs.length > 0) {
			//注册异步服务
			Set<ServiceItem> items = new HashSet<>();
			items.add(item);
			registerAsyncService(acs,items);
		}
		
		return (T)proxy;
	}
	
    void processReference(Object obj) {
		
		Class<?> cls = ProxyObject.getTargetCls(obj.getClass());
		List<Field> fields = new ArrayList<>();
		Utils.getIns().getFields(fields, cls);
		
		for(Field f : fields) {
			
			if(!f.isAnnotationPresent(Reference.class)) {
				continue;
			}
			
			Reference ref = f.getAnnotation(Reference.class);
			
			Object srv = null;
			boolean isRequired = ref.required();
			
			//系统启动时可以为某些类指定特定的实现
			//srv = of.getCommandSpecifyConponent(f);
		
			try {
				srv = createRefService(obj,f);
			} catch (CommonException e) {
				if(!isRequired) {
					logger.error("optional dependence cls ["+ f.getType().getName()
							+"] not found for class ["+obj.getClass().getName()+"]",e.getMessage());
				} else {
					throw e;
				}
			}
		
			
			if(srv != null) {
				SimpleObjectFactory.setObjectVal(obj, f, srv);
			} else if(isRequired) {
				throw new CommonException("Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] not found");
			} else {
				if(f.isAnnotationPresent(Reference.class)) {
					logger.warn("Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] not found");
				}
			}
			
		}
	}
    
    Class<?> getEltType(Field f){

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
    
    AsyncConfig[] getAcs(Reference ref) {
    	AsyncConfig[] acs = null;
		if(ref.asyncs() != null && ref.asyncs().length > 0) {
			acs = new AsyncConfig[ref.asyncs().length];
			int i = 0;
			for(Async a : ref.asyncs()) {
				AsyncConfig ac = new AsyncConfig();
				ac.setCondition(a.condition());
				ac.setEnable(a.enable());
				ac.setMethod(a.method());
				ac.setNamespace(a.namespace());
				ac.setServiceName(a.serviceName());
				ac.setVersion(a.version());
				ac.setForMethod(a.forMethod());
				ac.setParamStr(a.paramStr());
				acs[i] = ac;
				i++;
			}
		}
		return acs;
    }
	
	/**
	 * 创建对应items的服务代理，其中acs指定的forMethod方法将会被异步调用
	 * @param acs
	 * @param items
	 */
	private void registerAsyncService(AsyncConfig[] acs, Set<ServiceItem> items) {
		if(acs ==null || acs.length == 0) {
			return;
		}
		
		Set<ServiceItem> updates = new HashSet<>();
		
		for(AsyncConfig a : acs) {
			if(a == null) {
				continue;
			}
			
			for(ServiceItem si : items) {
				Set<ServiceMethod> sms = si.getMethods();
				
				boolean flag = false;
				for(ServiceMethod sm : sms) {
					if(sm.getKey().getMethod().equals(a.getForMethod())) {
						//方法名相同的都注册为异步调用方法
						String mkey = sm.getKey().toKey(false, false, false);
						if(StringUtils.isNotEmpty(sm.getTopic()) && !mkey.equals(sm.getTopic())) {
							throw new CommonException("Callback service method topic is not valid:" + JsonUtils.getIns().toJson(a) 
									+", Service: " + si.getKey().toKey(true, true, true)+",topic:" + sm.getTopic());
						}
						
						if(StringUtils.isEmpty(sm.getTopic())) {
							sm.setTopic(mkey);
							updates.add(si);
							flag = true;
						}else {
							boolean f = false;
							String[] ts = sm.getTopic().split(";");
							for(String t : ts) {
								if(mkey.equals(t)) {
									f = true;
									break;
								}
							}
							
							if(!f) {
								sm.setTopic(sm.getTopic()+";"+mkey);
								updates.add(si);
								flag = true;
							}
							
						}
					}
				}
				
				if(!flag) {
					throw new CommonException("Callback service method not found for:" + JsonUtils.getIns().toJson(a) +", Service: " + si.getKey().toKey(true, true, true));
				}
				
			}
			
			if(StringUtils.isNotEmpty(a.getServiceName())) {
				
				Set<ServiceItem> callbackItems = registry.getServices(a.getServiceName(), a.getNamespace(), a.getVersion());
				if(callbackItems == null || callbackItems.isEmpty()) {
					throw new CommonException("Callback service not found:" + JsonUtils.getIns().toJson(a));
				}
				
				for(ServiceItem si : callbackItems) {
					Set<ServiceMethod> sms = si.getMethods();
					
					boolean flag = false;
					for(ServiceMethod sm : sms) {
						if(sm.getKey().getMethod().equals(a.getMethod())) {
							flag = true;
							break;
						}
					}
					
					if(!flag) {
						throw new CommonException("Async service method not found for:" + JsonUtils.getIns().toJson(a) +", Service: " + si.getKey().toKey(true, true, true));
					}
				}
			}
			
		}
		
		if(!updates.isEmpty()) {
			for(ServiceItem si : updates) {
				registry.update(si);
			}
		}
		
	}
	
	private Class<?> loadClass(String clsName,ClassLoader cl) {
		try {
			return Thread.currentThread().getContextClassLoader().loadClass(clsName);
		} catch (ClassNotFoundException e) {
			try {
				 return this.getClass().getClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e1) {
				if(cl == null) {
					cl = of.get(RpcClassLoader.class);
				}
				
				if(cl != null) {
					try {
						Class<?> c = cl.loadClass(clsName);
						if(c != null) {
							Thread.currentThread().setContextClassLoader(cl);
						}
						return c;
					} catch (ClassNotFoundException e2) {
					}
				}
				
				throw new CommonException("class["+clsName+"] not found",e);
			}
		}
	}
	
    private Object createRefService(Object srcObj,Field f){
		if(!f.isAnnotationPresent(Reference.class)){
			logger.warn("cls:["+srcObj.getClass().getName()+"] not annotated with ["+Reference.class.getName()+"]");
			return null;
		}
		
		Class<?> becls = ProxyObject.getTargetCls(srcObj.getClass());
		Reference ref = f.getAnnotation(Reference.class);
		Class<?> type = f.getType();
		
		Object proxy = null;
		
		if(List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)){
			//集合服务引用
			proxy = createCollectionService(srcObj,f,becls,ref);
		} else {
			//单个服务引用,远程对象只能是接口,不能是类
			//Set<ServiceItem> sis = registry.getServices(field.getName());
			if(!type.isInterface()) {
				throw new CommonException("Class ["+becls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] have to be interface class");
			}
			
			//支持namespace及version的模糊匹配
			if(!registry.isExists(type.getName(),ref.namespace(),ref.version())) {
				if(ref.required()){
					//服务不可用，并且服务依赖是必须满足，则报错处理
					StringBuffer sb = new StringBuffer("Class [");
					sb.append(becls.getName()).append("] field [").append(f.getName())
					.append("] dependency [").append(f.getType().getName())
					.append("] not found version [").append(ref.version()).append("] namespace [").append(ref.namespace()).append("]");
					throw new CommonException(sb.toString());
				}
				//服务不可用，但还是可以生成服务代理，直到使用时才决定服务的可用状态
			} 
			
			AsyncConfig[] acs = this.getAcs(ref);
			
			proxy = this.getRefRemoteService(type.getName(), ref.namespace(), ref.version(), null,acs);
			
			String key = UniqueServiceKey.serviceName(type.getName(),ref.namespace(),ref.version()).toString();
			
			//this.initProxyField(proxy, key, srcObj, f);
			FieldServiceProxyListener lis = new FieldServiceProxyListener(this,srcObj,f,this.registry);
			registry.addExistsServiceListener(key, lis);
		}
		
		return proxy;
	}
    
	private Object createCollectionService(Object srcObj, Field f, Class<?> becls, Reference ref) {
	
		Class<?> ctype = getEltType(f);
		
		boolean existsItem = registry.isExists(ctype.getName(),ref.namespace(),ref.version());
		if(ref.required() && !existsItem) {
				StringBuffer sb = new StringBuffer("Class [");
				sb.append(becls.getName()).append("] field [").append(f.getName())
				.append("] dependency [").append(f.getType().getName())
				.append("] not found version [").append(ref.version()).append("] namespace [").append(ref.namespace()).append("]");
				throw new CommonException(sb.toString());
		}

		boolean bf = f.isAccessible();
		Object o = null;
		if(!bf) {
			f.setAccessible(true);
		}
		try {
			o = f.get(srcObj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new CommonException("",e);
		}
		if(!bf) {
			f.setAccessible(bf);
		}
		
		if(o == null){
			if(Modifier.isAbstract(f.getType().getModifiers())) {
				o = new HashSet<Object>();
			} else {
				try {
					//变量声明的类型是Set的子实现类型，如HashSet
					o = f.getType().newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new CommonException("",e);
				}
			}
		}
		
		Collection<Object> el = (Collection<Object>)o;
		
		Set<String> exists = new HashSet<>();
		AbstractClientServiceProxy po = null;
		
		//请参考Reference说明使用
		Set<ServiceItem> items = null;
		if(existsItem) {
			items = registry.getServices(ctype.getName());
		}
		
		AsyncConfig[] acs = this.getAcs(ref);
		
		if(items != null && !items.isEmpty()){
			for(ServiceItem si : items){
				String key = si.serviceKey();
				if(exists.contains(key)){
					continue;
				}
				exists.add(key);
				
				po = this.getRefRemoteService(si, null,acs);
				
				//监听每一个元素，元素加入或删除时，要从集合中删除
				//CollectionElementServiceProxyListener lis = new CollectionElementServiceProxyListener(this,el,srcObj,f,po);
				//registry.addExistsServiceListener(key, lis);
				
				if(po != null){
					el.add(po);
				}
			}
		}
		
		//接受实现相同接口的所有服务，具体使用那个服务，由使用者去判断
		RemoteProxyServiceFieldListener lis = new RemoteProxyServiceFieldListener(this,el,srcObj,f,this.registry);
		//集合服务引用根据服务名称做监听，只要匹配名称的服务都加入集合里面
		//集合元素唯一性是服务名称，名称空间，版本
		//registry.addServiceNameListener(ctype.getName(), lis);
		//registry.addExistsServiceNameListener(ctype.getName(), lis);
		
		registry.addServiceNameListener(ctype.getName(), lis);
		
		exists.clear();
		exists = null;
		return el;
		
	}

	private void initProxy(Object proxy,String key){
		 if(proxy == null){
			 return;
		 }
		//String key = UniqueServiceKey.serviceName(srvName,namespace,version).toString();
    	AbstractClientServiceProxy asp = (AbstractClientServiceProxy)proxy;
		asp.setOf(of);
		//remoteObjects.put(key, proxy);
		registry.addExistsServiceListener(key, asp);
	}
	
	/**
	 * 	创建无状态的远程服务代理对象，每个servicename,namespace,version创建一个服务代理对象
	 * @param cls
	 * @param namespace
	 * @param version
	 * @return
	 */
	private <T> T createDynamicServiceProxy(Class<T> cls, String namespace, String version, AsyncConfig[] acs) {
		 ClassGenerator classGenerator = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
		 classGenerator.setClassName(cls.getName()+"$Jmicro"+SimpleObjectFactory.idgenerator.getAndIncrement());
		 classGenerator.setSuperClass(AbstractClientServiceProxy.class);
		 classGenerator.addInterface(cls);
		 classGenerator.addDefaultConstructor();
		 classGenerator.addInterface(ProxyObject.class);
		 classGenerator.addInterface(ICheckable.class);
		 
		 classGenerator.addField("public static java.lang.reflect.Method[] ms = null;");
		 //classGenerator.addField("private " + InvocationHandler.class.getName() + " handler = new org.jmicro.api.client.ServiceInvocationHandler(this);");
       
		 //classGenerator.addField("private boolean enable="+enable+";");
		 classGenerator.addMethod("public java.lang.String getNamespace(){ return \"" + namespace + "\";}");
		 classGenerator.addMethod("public java.lang.String getVersion(){ return \"" + version + "\";}");
		 classGenerator.addMethod("public java.lang.String getServiceName(){ return \"" + cls.getName() + "\";}");
		 
		 //classGenerator.addMethod("public boolean enable(){  return this.enable;}");
		 //classGenerator.addMethod("public void enable(boolean en){ this.enable=en;}");
		 
		 classGenerator.addMethod("public void backupAndSetContext(){super.backupAndSetContext();}");
		 classGenerator.addMethod("public void restoreContext(){super.restoreContext();}");
		 
		 Method[] ms1 = cls.getMethods();
		 Method[] checkMethods = ICheckable.class.getMethods();
		 
		 Method[] ms = new Method[ms1.length + checkMethods.length];
		 System.arraycopy(ms1, 0, ms, 0, ms1.length);
		 System.arraycopy(checkMethods, 0, ms, ms1.length, checkMethods.length);
		 
		 for(int i =0; i < ms.length; i++) {
			 Method m = ms[i];
			 if (m.getDeclaringClass() == Object.class || !Modifier.isPublic(m.getModifiers())){
				 continue;
			 }
			 
			Class<?> rt = m.getReturnType();
            Class<?>[] pts = m.getParameterTypes();

            StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
            for(int j = 0; j < pts.length; j++){
           	 	code.append(" args[").append(j).append("] = ");
           	 	if(pts[j].isPrimitive()) {
           	 		code.append(" new ").append(ReflectUtils.getFullClassName(pts[j])).append(" ($").append(j + 1).append(") ");
           	 	}else {
           	 		code.append(" ( ").append(ReflectUtils.getFullClassName(pts[j])).append(" )$").append(j + 1);
           	 	}
           	 	code.append("; ");
            }
            
           /* code.append(" if(getItem() == null) {throw new org.jmicro.common.CommonException(\"Service ")
            	.append(cls.getName()).append(" not available\"); }");*/
            
            code.append(" try { ");
            
            code.append(" this.backupAndSetContext();");
            
            //backup pre service info
            code.append(" java.lang.String ns = org.jmicro.api.JMicroContext.get().getString(org.jmicro.api.JMicroContext.CLIENT_NAMESPACE, \"\");");
            code.append(" java.lang.String sn = org.jmicro.api.JMicroContext.get().getString(org.jmicro.api.JMicroContext.CLIENT_SERVICE, \"\");");
            code.append(" java.lang.String ver = org.jmicro.api.JMicroContext.get().getString(org.jmicro.api.JMicroContext.CLIENT_VERSION, \"\");");
            code.append(" java.lang.String mt = org.jmicro.api.JMicroContext.get().getString(org.jmicro.api.JMicroContext.CLIENT_METHOD, \"\");");
            
            //set this invoke info
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_NAMESPACE, getNamespace());");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_SERVICE, getServiceName());");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_VERSION, getVersion());");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_METHOD, \""+m.getName()+"\");");
            
            //code.append(" org.jmicro.api.registry.ServiceMethod poSm = super.getItem().getMethod(\"").append(m.getName()).append("\", args); ");
            //code.append(" org.jmicro.api.JMicroContext.get().configMonitor(poSm.getMonitorEnable(), super.getItem().getMonitorEnable()); ");
            //code.append(" org.jmicro.api.JMicroContext.get().setParam(org.jmicro.common.Constants.SERVICE_METHOD_KEY, poSm);");
            //code.append(" org.jmicro.api.JMicroContext.get().setParam(org.jmicro.common.Constants.SERVICE_ITEM_KEY, super.getItem());");
            
            code.append(" java.lang.Object ret = (java.lang.Object)this.invoke(this, ms[" + i + "], args);");
            
            //restore pre service info
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_NAMESPACE, ns);");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_SERVICE, sn);");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_VERSION, ver);");
            code.append(" org.jmicro.api.JMicroContext.get().setString(org.jmicro.api.JMicroContext.CLIENT_METHOD, mt);");
            
            if (!Void.TYPE.equals(rt)){
             //code.append("System.out.println(ret);");
           	 code.append(" return ").append(Utils.getIns().asArgument(rt, "ret")).append(";");
            }
            
            code.append("} finally { ");
            
            	code.append(" this.restoreContext();");
            
            code.append(" } ");

            classGenerator.addMethod(m.getName(), m.getModifiers(), rt, pts, m.getExceptionTypes(), code.toString());      
		 }
		 
		 //logger.debug(classGenerator.);
		 //classGenerator.getClassPool().
		 Class<?> clazz = classGenerator.toClass();

        try {
       	    clazz.getField("ms").set(null, ms);
			@SuppressWarnings("unchecked")
			T proxy = (T)clazz.newInstance();
			if(acs != null && acs.length  > 0) {
				AbstractClientServiceProxy p = (AbstractClientServiceProxy) proxy;
				p.setAsyncConfig(acs);
			}
			return proxy;
		} catch (InstantiationException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e1) {
			throw new CommonException("Fail to create proxy ["+ cls.getName()+"]");
		} 
	}
}
