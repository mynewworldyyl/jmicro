package cn.jmicro.objfactory.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

import cn.jmicro.common.Utils;

//@Component
public class BeanFactoryBridge implements BeanDefinitionRegistryPostProcessor{
	
	private BeanDefinitionRegistry registry;
	
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
	    	
	    	/*
	    	IObjectFactory of = EnterMain.getObjectFactory();
	    	if(of == null) throw new CommonException("JMicro object factory not found");
	    	
	    	of.foreach((obj)->{
	    		registBean(obj.getClass());
	    	});*/
        
	    }
	 
	    @Override
	    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	 
	    }
}
