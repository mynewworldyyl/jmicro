package cn.jmicro.objfactory.spring.bean;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostInitListener;
import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.objfactory.spring.JMicroObjectSource2Spring;
import cn.jmicro.objfactory.spring.SpringAndJmicroComponent;
import cn.jmicro.objfactory.spring.anno.JMicroComponent;

@Configuration
@ConditionalOnClass(JMicro.class)
@Order(value=18)
public class JMicroConfiguration implements ApplicationContextAware{

	@Value("${jmicro.argStr}")
	private String argStr;
	
	private ApplicationContext cxt;
	
	//private JMicroObjectSource2Spring ofProxy;
	
	@Bean
	@Order(value=1)
	public IObjectFactory jmicroObjectFactory() {
		String[] args = null;
		if(Utils.isEmpty(argStr)) {
			args = new String[0];
		} else {
			args = argStr.split(" ");
		}
		
		Object jmicroObjectFactory = JMicro.getObjectFactoryAndStart(args);
		ClassLoader jmicroRpcClassloader = jmicroObjectFactory.getClass().getClassLoader();
		IObjectFactory of = SpringAndJmicroComponent.createLazyProxyObjectByCglib(jmicroObjectFactory,IObjectFactory.class.getName(),SpringAndJmicroComponent.class.getClassLoader());
		
		SpringObjectSource2Jmicto os2Jmicro = new SpringObjectSource2Jmicto();
		if(cxt != null) {
			os2Jmicro.setCxt(cxt);
		}
		os2Jmicro.setJmicroRpcClassloader(jmicroRpcClassloader);
		
		Object toJmicroOS = SpringAndJmicroComponent.createLazyProxyObjectByCglib(os2Jmicro,IObjectSource.class.getName(),jmicroRpcClassloader);
		of.regist("springObjectSource", toJmicroOS);
		
		JMicroObjectSource2Spring ofProxy = (JMicroObjectSource2Spring)cxt.getBean(IObjectSource.class);
		
		if(ofProxy != null) {
			ofProxy.setOf(of);
			ofProxy.setJmicroRpcClassloader(jmicroRpcClassloader);
		}
		
		//postProcessBeanFactory(cxt);
		
		return new IObjectFactory() {

			private Map<String,Object> nsSrvs = new HashMap<>();
			
			private Map<String,Object> c2nsSrvs = new HashMap<>();
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public <T> T getRemoteServie(String srvName, String namespace, String version, AsyncConfigJRso[] acs) {
				String key = srvName+"#"+namespace+"#"+version;
				if(nsSrvs.containsKey(key)) return (T)nsSrvs.get(key);
				
				T srv = of.getRemoteServie(srvName, namespace, version, null);
				/*T spi = SpringAndJmicroComponent.createLazyProxyObjectByCglib(srv,srv.getClass().getName(),
						SpringAndJmicroComponent.class.getClassLoader());*/
				nsSrvs.put(key, srv);
				
				return srv;
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public <T> T getRemoteServie(Class<T> srvCls, String ns, AsyncConfigJRso[] acs) {
				String key = srvCls.getName()+"##"+ns;
				if(c2nsSrvs.containsKey(key)) return (T)c2nsSrvs.get(key);
				
				Class jclazz = this.loadCls(srvCls.getName());
				Object srv = of.getRemoteServie(jclazz, ns, null);
				/*T spi = SpringAndJmicroComponent.createLazyProxyObjectByCglib(srv,srvCls.getName(),
						SpringAndJmicroComponent.class.getClassLoader());*/
				
				 c2nsSrvs.put(key,srv);
				
				return (T)srv;
			}

			@Override
			public <T> T getRemoteServie(ServiceItemJRso item, AsyncConfigJRso[] acs) {
				throw new UnsupportedOperationException("getRemoteServie(ServiceItem item, AsyncConfig[] acs)");
			}
			
			@Override
			public void foreach(Consumer<Object> c) {
				of.foreach(c);
			}

			@Override
			public void regist(Object obj) {
				of.regist(obj);
			}

			@Override
			public void regist(Class<?> clazz, Object obj) {
				of.regist(clazz, obj);
			}

			@Override
			public <T> void registT(Class<T> clazz, T obj) {
				of.registT(clazz, obj);
				
			}

			@Override
			public void regist(String comName, Object obj) {
				of.regist(comName, obj);
				
			}

			@Override
			public Boolean exist(Class<?> clazz) {
				return of.exist(clazz);
			}

			@Override
			public <T> T get(Class<T> cls) {
				return of.get(cls,false);
			}

			@Override
			public <T> T get(Class<T> cls, boolean create) {
				return of.get(cls,false);
			}
			
			@Override
			public <T> T getByName(String clsName) {
				return of.getByName(clsName);
			}
			
			@Override
			public <T> Set<T> getByParent(Class<T> parrentCls) {
				return of.getByParent(parrentCls);
			}

			@Override
			public void start(IDataOperator dataOperator, String[] args) {
				throw new UnsupportedOperationException("start");
			}

			@Override
			public void addPostListener(IPostInitListener listener) {
				/*IPostInitListener jlis = SpringAndJmicroComponent.createLazyProxyObjectByCglib(listener,
						IPostInitListener.class.getName(),jmicroRpcClassloader);*/
				of.addPostListener(listener);
			}

			@Override
			public Class<?> loadCls(String clsName) {
				return of.loadCls(clsName);
			}

			@Override
			public void masterSlaveListen(IMasterChangeListener l) {
				/*IMasterChangeListener jl = SpringAndJmicroComponent.createLazyProxyObjectByCglib(
						l,IMasterChangeListener.class.getName(),jmicroRpcClassloader
						);*/
				of.masterSlaveListen(l);
			}

			@Override
			public Boolean isSysLogin() {
				return of.isSysLogin();
			}

			@Override
			public Boolean isRpcReady() {
				return of.isRpcReady();
			}

			@Override
			public ProcessInfoJRso getProcessInfo() {
				ProcessInfoJRso pi = of.getProcessInfo();
				/*ProcessInfo spi = SpringAndJmicroComponent.createLazyProxyObjectByCglib(
						pi,ProcessInfo.class.getName(),
						SpringAndJmicroComponent.class.getClassLoader());*/
				return pi;
			}
			
		};
	}
	
	@Bean
	@ConditionalOnBean(IObjectFactory.class)
	public IObjectSource jmicroObjectSource() {
		JMicroObjectSource2Spring ofProxy = new JMicroObjectSource2Spring();
		return ofProxy;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.cxt = applicationContext;
	}
	
	
	private Set<FieldComponentHolder> jmicroComponentFields = new HashSet<>();
	
	//@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		 List<Field> fields = new ArrayList<>();
		
		 Class<?> beanClass = AopUtils.getTargetClass(bean);
		 
		 Utils.getIns().getFields(fields, beanClass);
		 for(Field f : fields) {
			 if(f.isAnnotationPresent(JMicroComponent.class)) {
				 JMicroComponent ann = f.getAnnotation(JMicroComponent.class);
				 FieldComponentHolder fh = new FieldComponentHolder();
				 fh.bean = bean;
				 fh.f = f;
				 fh.ann = ann;
				 String fn = "set"+Character.toUpperCase(f.getName().charAt(0))+f.getName().substring(1);
				 try {
					fh.setMethod = beanClass.getDeclaredMethod(fn, f.getType());
				} catch (NoSuchMethodException | SecurityException e) {
					f.setAccessible(true);
				}
				jmicroComponentFields.add(fh);
			 }
		 }
		return bean;
	}
	
	//@Override
	public void onApplicationEvent(ApplicationEvent event) {
		 if (event instanceof ContextRefreshedEvent) { 
			 postProcessBeanFactory((ApplicationContext)event.getSource());
	     } 
	}

	public void postProcessBeanFactory(ApplicationContext beanFactory) throws BeansException {
		if(jmicroComponentFields.isEmpty()) return;
		
		IObjectSource jmicroSource = beanFactory.getBean(IObjectSource.class);
		for(FieldComponentHolder fc : jmicroComponentFields) {
			Object com = null;
			if(fc.ann.remoteService()) {
				//jmicroSource.get
				IObjectFactory of = beanFactory.getBean(IObjectFactory.class);
				com = of.getRemoteServie(fc.f.getType().getName(), fc.ann.namespace(), fc.ann.version(), null);
			}else {
				if(Utils.isEmpty(fc.ann.value())) {
					 com = jmicroSource.get(fc.f.getType());
				} else {
					 com = jmicroSource.getByName(fc.ann.value());
				}
			}
			
			if(com == null) {
				if(fc.ann.required())
					throw new CommonException("Required jmicro component not found: " + fc.f.getType().getName() + " in "+fc.f.getDeclaringClass().getName());
				continue;
			}
			
			if(fc.setMethod == null) {
				try {
					fc.f.set(fc.bean, com);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new CommonException("Fail to set field " + fc.f.getType().getName() + " in "+fc.f.getDeclaringClass().getName(),e);
				}
			} else {
				try {
					fc.setMethod.invoke(fc.bean, com);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new CommonException("Set component error " + fc.f.getType().getName() + " in "+fc.f.getDeclaringClass().getName(),e);
				}
			}
		}
	}
	
	private class FieldComponentHolder {
		private Object bean;
		private Field f;
		private JMicroComponent ann;
		private Method setMethod;
	}
	
}
