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
package cn.jmicro.objfactory.spring;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.objfactory.spring.bean.SpringObjectSource2Jmicto;

//@Component
public class SpringAndJmicroComponent implements BeanDefinitionRegistryPostProcessor,ApplicationContextAware{

	private static AtomicInteger idgenerator = new AtomicInteger();
	
	private final static Logger log = LoggerFactory.getLogger(SpringAndJmicroComponent.class);
	
	//private Object jmicroObjectFactory;
	
	private IObjectFactory of;
	
	private ClassLoader jmicroRpcClassloader;
	
	private ApplicationContext cxt;
	
	//private BeanFactoryBridge registBridge;
	
	private BeanDefinitionRegistry registry;
	
	private final Map<String,Class<?>> jm2SpringProxyClasses = new HashMap<>();
	
	private final Map<String,Object> springObjects = new HashMap<>();
	
	public SpringAndJmicroComponent() {
	}
	
	public SpringAndJmicroComponent(ConfigurableApplicationContext cxt) {
		this.cxt = cxt;
		//this.registBridge = cxt.getBean(BeanFactoryBridge.class);
	}
	
	//@PostConstruct
	public void run(String[] args) {
		Object jmicroObjectFactory = JMicro.getObjectFactoryAndStart(args);
		jmicroRpcClassloader = jmicroObjectFactory.getClass().getClassLoader();
		of = createLazyProxyObjectByCglib(jmicroObjectFactory,IObjectFactory.class.getName(),SpringAndJmicroComponent.class.getClassLoader());
		
		SpringJMicroObjectFactory ofProxy = cxt.getBean(SpringJMicroObjectFactory.class);
		ofProxy.setOf(of,SpringAndJmicroComponent.class.getClassLoader());
		
		//of.start(null, args);
		//this.spring2Jmicro();
		//this.jmicro2Spring();
		
		IObjectSource os2Jmicro = new SpringObjectSource2Jmicto();
		Object toJmicroOS = createLazyProxyObjectByCglib(os2Jmicro,IObjectSource.class.getName(),jmicroRpcClassloader);
		of.regist("springObjectSource", toJmicroOS);
		
		/*ObjectSource2Spring os2s = cxt.getBean(ObjectSource2Spring.class);
		ofProxy.setOf(of);*/
		
	}
	
	private void spring2Jmicro() {
		for(String beanName : cxt.getBeanDefinitionNames()){
			Object obj = cxt.getBean(beanName);
			if(!of.exist(obj.getClass()) && of.getByName(beanName) == null) {
				Object c = createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),jmicroRpcClassloader);
				of.regist(c);
			}
		}
	}

	private void jmicro2Spring() {
		ClassLoader cl = SpringAndJmicroComponent.class.getClassLoader();
		of.foreach((obj)->{
			//非Spring中的对象才需要注册到JMicro
			if(!(AopUtils.isAopProxy(obj) || AopUtils.isCglibProxy(obj))) {
				Object c = createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),cl);
				registBean(c.getClass(),c);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <T>  T createLazyProxyObjectByCglib(Object to, String className,ClassLoader cl) {
		Class<?> tCls = AopUtils.getTargetClass(to);
		if(tCls.getClassLoader() == cl) {
			return (T)to;//同一个类空间，无需代理
		}
		
        try {
        	log.info(tCls.getName());
        	/*if(tCls.getName().startsWith("com.mongodb.client.internal.MongoDatabaseImpl")) {
        		log.info(tCls.getName());
        	}*/
        	
        	CgLibProxyInterceptor intProxy = new CgLibProxyInterceptor(to);
            Enhancer enhancer = new Enhancer();
            String cn = className;
            if(cn.contains("$$EnhancerBy")) {
            	cn = cn.substring(0, cn.indexOf("$$EnhancerBy"));
            }
			enhancer.setSuperclass(cl.loadClass(cn));
			enhancer.setCallback(intProxy);
			
			return (T)enhancer.create();
		} catch (ClassNotFoundException | IllegalArgumentException e) {
			try {
				CgLibProxyInterceptor intProxy = new CgLibProxyInterceptor(to);
	            Enhancer enhancer = new Enhancer();
	            String cn = tCls.getInterfaces()[0].getName();
				enhancer.setSuperclass(cl.loadClass(cn));
				enhancer.setCallback(intProxy);
				return (T)enhancer.create();
			} catch (ClassNotFoundException e1) {
				throw new CommonException(tCls.getName(),e);
			}
		}
       
	}
	

	private Class<?> createProxyClass(Class<?> cls) {
		log.debug("Jmicro2Spring proxy: " + cls.getName());
		
		ClassGenerator cg = ClassGenerator.newInstance(cls.getClassLoader());
		try {
			cg.setClassName(cls.getName()+"$Jmicro2Spring"+idgenerator.getAndIncrement());
			cg.setSuperClass(cls.getName());
			cg.addInterface(JmicroWithSpringBridge.class);
			
			int index = 0;
			List<Method> methods = new ArrayList<>();
			Utils.getIns().getMethods(methods, cls);
			List<Method> publicMethods = new ArrayList<>();
			
			cg.addMethod(" public void setSrcObj("+cls.getName()+" obj){this.srcObj = obj;}");
			cg.addField(" private "+cls.getName()+" srcObj;");
			
			for(Method m : methods){
				if(!Modifier.isPublic(m.getModifiers()) || m.getDeclaringClass() == Object.class){
					continue;
				}
				publicMethods.add(m);
				
				StringBuffer sb = new StringBuffer(/*cls.getName()+" obj = ("+cls.getName()+")(this.getSrcObj());"*/);
				
				Class<?> rt = m.getReturnType();
				
				if (!Void.TYPE.equals(rt)) {
					sb.append(" return ("+ReflectUtils.getName(rt)+") __methods[").append(index).append("].invoke(this.srcObj,$args); ");	
				} else {
					sb.append(" __methods[").append(index).append("].invoke(this.srcObj,$args); ");	
				}
				
				/*if (!Void.TYPE.equals(rt)) {
					sb.append(" return v ;");
				}*/
				cg.addMethod(m.getName(), m.getModifiers(), m.getReturnType(), m.getParameterTypes(),
						m.getExceptionTypes(), sb.toString());
				
				System.out.println(m.getName()+"===>"+sb.toString());
				
				index++;
			} 
			cg.addField("public static java.lang.reflect.Method[] __methods;");
			
			Class<?> cl = cg.toClass(this.getClass().getClassLoader(),this.getClass().getProtectionDomain());
			Method[] ms = new Method[publicMethods.size()];
			publicMethods.toArray(ms);
			cl.getField("__methods").set(null, ms);
			jm2SpringProxyClasses.put(cls.getName(), cl);
			return cl;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException  e) {
			log.error("Create lazy proxy error for: "+ cls.getName(), e);
		}finally {
			cg.release();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private <T>  T createLazyProxyObject(Object to) {
		Class<?> tCls = to.getClass();
		if(tCls.getClassLoader() == this.getClass().getClassLoader()) {
			return (T)to;//无需代理
		}
		
		Class<?> cls = jm2SpringProxyClasses.get(tCls.getName());
		if(cls == null) {
			cls = createProxyClass(tCls);
		}
		
		try {
			Object obj = cls.newInstance();
			JmicroWithSpringBridge po = (JmicroWithSpringBridge)obj;
			po.setSrcObj(to);
			return (T)obj;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException(tCls.getName(),e);
		}
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.cxt = (ConfigurableApplicationContext)applicationContext;
	}

	public void registBean(Class<?> beanClazz,Object val) {
		if(Utils.isEmpty(beanClazz.getSimpleName())) {
			return;
		}
		 //Class<?> beanClazz = obj.getClass();
		 BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClazz);
         GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

        //在这里，我们可以给该对象的属性注入对应的实例。
        //比如mybatis，就在这里注入了dataSource和sqlSessionFactory，
        // 注意，如果采用definition.getPropertyValues()方式的话，
        // 类似definition.getPropertyValues().add("interfaceType", beanClazz);
        // 则要求在FactoryBean（本应用中即ServiceFactory）提供setter方法，否则会注入失败
        // 如果采用definition.getConstructorArgumentValues()，
        // 则FactoryBean中需要提供包含该属性的构造方法，否则会注入失败
        definition.getConstructorArgumentValues().addGenericArgumentValue(beanClazz);
        definition.getConstructorArgumentValues().addGenericArgumentValue(val);

        //注意，这里的BeanClass是生成Bean实例的工厂，不是Bean本身。
        // FactoryBean是一种特殊的Bean，其返回的对象不是指定类的一个实例，
        // 其返回的是该工厂Bean的getObject方法所返回的对象。
        definition.setBeanClass(JMicroFactoryBean.class);

        //这里采用的是byType方式注入，类似的还有byName等
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        registry.registerBeanDefinition(beanClazz.getSimpleName(), definition);
	
	}
	
	 @Override
	    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
	        //这里我为了简单起见，直接写了2个固定接口，一般我们是通过反射获取需要代理的接口的clazz列表
	        //比如判断包下面的类，或者通过某注解标注的类等等
	    	this.registry = registry;
	    }
	 
	    @Override
	    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	 
	    }
	    
	   /* public class ObjectSource2Jmicto implements IObjectSource {

	    	public ObjectSource2Jmicto() {}
	    	
			@Override
			public Object get(String name) {
				if(springObjects.containsKey(name)) return springObjects.get(name);
				Object obj = cxt.getBean(name);
				if(obj == null) return null;
				Object c = createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),jmicroRpcClassloader);
				springObjects.put(name, c);
				return c;
			}
		
	    }*/
	    
}
