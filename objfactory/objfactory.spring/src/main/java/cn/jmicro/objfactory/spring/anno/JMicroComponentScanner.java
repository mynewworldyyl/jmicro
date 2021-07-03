package cn.jmicro.objfactory.spring.anno;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JMicroComponentScanner implements  BeanFactoryPostProcessor, ApplicationContextAware {
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
    }
    
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
     /* Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
      scanner.setResourceLoader(this.applicationContext);
      scanner.scan("org.wcong.test.spring.scan");*/
    }
  }
