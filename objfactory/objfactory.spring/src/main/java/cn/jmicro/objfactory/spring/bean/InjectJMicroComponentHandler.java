package cn.jmicro.objfactory.spring.bean;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.objfactory.spring.anno.JMicroComponent;

@Component
public class InjectJMicroComponentHandler implements BeanPostProcessor/*,ApplicationListener<ApplicationEvent>*/{

	private Set<FieldComponentHolder> jmicroComponentFields = new HashSet<>();
	
	@Override
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
	
	/*@Override
	public void onApplicationEvent(ApplicationEvent event) {
		 if (event instanceof ContextRefreshedEvent) { 
			 postProcessBeanFactory((ApplicationContext)event.getSource());
	     } 
	}*/

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
