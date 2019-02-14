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
package org.jmicro.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.ObjFactory;
import org.jmicro.api.codec.PrefixTypeEncoder;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.common.Base64Utils;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-上午10:21:03
 */
public class JMicro {

	private static final Map<String,IObjectFactory> objFactorys = new HashMap<>();
	//确保每个对像工厂只会创建一个实例
	
	private static boolean isInit = false;
	
	private static void init0() {
		if(isInit) {
			return;
		}
		
		isInit = true;
		
		Set<Class<?>> objClazzes = ClassScannerUtils.getIns().loadClassesByAnno(ObjFactory.class);
		for(Class<?> c : objClazzes) {
			if(Modifier.isAbstract(c.getModifiers()) || Modifier.isInterface(c.getModifiers())){
				throw new CommonException("Object Factory must not abstract or interface:"+c.getName());
			}
			try {
				Set<Class<?>> subCls = ClassScannerUtils.getIns().loadClassByClass(c);
				if(subCls.size() > 1) {
					//不是final类，也就是还有子类，不能实例化
					continue;
				}
				ObjFactory anno = c.getAnnotation(ObjFactory.class);
				IObjectFactory of = (IObjectFactory)c.newInstance();
				if(objFactorys.containsKey(anno.value())){
					throw new CommonException("Redefined Object Factory with name: "+anno.value()
					+",cls:"+c.getName()+",exists:"+ objFactorys.get(anno.value()).getClass().getName());
				}
				objFactorys.put(anno.value(), of);
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CommonException("Instance ObjectFactory exception: "+c.getName(),e);
			}
		}
	
	}
	
	public static IObjectFactory getObjectFactoryNotStart(String[] args,String name){
		Config.parseArgs(args);
		init0();
		if(StringUtils.isEmpty(name)){
			name = JMicroContext.get().getString(Constants.OBJ_FACTORY_KEY,Constants.DEFAULT_OBJ_FACTORY);
		}
		IObjectFactory of = objFactorys.get(name);
		if(of == null) {
			throw new CommonException("ObjectFactory ["+name+"] not found, please check the ["
					+ IObjectFactory.class.getName() +"] implementation is include in the classpath and "
					+ "retry again");
		}
		return of;
	}

	public static IObjectFactory getObjectFactoryAndStart(String[] args){
		System.out.println(System.getProperty("java.class.path"));
		IObjectFactory of =  getObjectFactoryNotStart(args,null);
		of.start();
		return of;
	}
	
	public static IObjectFactory getObjectFactory(String name){
		if(!isInit) {
			throw new CommonException("Object Factory not init");
		}
		if(StringUtils.isEmpty(name)){
			name = JMicroContext.get().getString(Constants.OBJ_FACTORY_KEY,Constants.DEFAULT_OBJ_FACTORY);
		}
		IObjectFactory of = objFactorys.get(name);
		if(of == null){
			throw new CommonException("ObjectFactory with name ["+name+"] not found");
		}
		return of;
	}
	
	public static IObjectFactory getObjectFactory(){
		return getObjectFactory(null);
	}
	
	
	public static IRegistry getRegistry(String registryName){
		if(StringUtils.isEmpty(registryName)) {
			registryName = Constants.REGISTRY_KEY;
		}
		IRegistry registry = getObjectFactory().get(IRegistry.class);
		if(registry == null){
			throw new CommonException("Registry with name ["+registryName+"] not found");
		}
		return registry;
	}
	
	public static String getClassAnnoName(Class<?> cls) {

		cls = ProxyObject.getTargetCls(cls);
		/*if(cls.isAnnotationPresent(Name.class)){
			return cls.getAnnotation(Name.class).value();
		}else */if(cls.isAnnotationPresent(Component.class)){
			return cls.getAnnotation(Component.class).value();
		}
		/*else if(cls.isAnnotationPresent(Server.class)){
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
		}else if(cls.isAnnotationPresent(Reference.class)){
			return cls.getAnnotation(Reference.class).value();
		}else if(cls.isAnnotationPresent(CodecFactory.class)){
			return cls.getAnnotation(CodecFactory.class).value();
		}*/
		return cls.getName();
	
	}
	
	public static void waitForShutdown() {
		Utils.getIns().waitForShutdown();
	}
	
	public static <T> T getRpcServiceTestingArgs(Class<T> srvClazz) {
		Object srv = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] {srvClazz}, 
				new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if(args == null || args.length == 0) {
							System.out.println("no need args for testing");
							return null;
						}
						PrefixTypeEncoder encoder = new PrefixTypeEncoder();
						ByteBuffer bb = encoder.encode(args);
						//bb.flip();
						byte[] data = new byte[bb.remaining()];
						bb.get(data, 0, bb.limit());
						String str = new String(Base64Utils.encode(data),Constants.CHARSET);
						System.out.println(str);
						return null;
					}
				});
		return (T)srv;
	}
	
	public static void main(String[] args) {
		 System.out.println(Arrays.asList(args));
		 JMicro.getObjectFactoryAndStart(args);
	}
	
}
