package cn.jmicro.objfactory.spring;

import org.springframework.beans.factory.FactoryBean;

import cn.jmicro.api.EnterMain;

public class JMicroFactoryBean<T> implements FactoryBean<T>{

	private Class<T> targetClazz;
	
	public JMicroFactoryBean(Class<T> ta,Object obj) {
		this.targetClazz = ta;
	}
	
	@Override
	public T getObject() throws Exception {
		if(EnterMain.getObjectFactory().exist(targetClazz)) {
			return EnterMain.getObjectFactory().get(targetClazz);
		}
		return null;
	}

	@Override
	public Class<T> getObjectType() {
		return targetClazz;
	}

}
