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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostInitListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.ILoginStatusListener;
import cn.jmicro.common.CommonException;

/**
 * 1. 创建对像全部单例,所以不保证线程安全
 * 2. 存在3种类型代理对象,分别是：
 * 		a. Component.lazy注解指定的本地懒加载代理对像，由cn.jmicro.api.objectfactory.ProxyObject接口标识;
 * 		b. Service注解确定的远程服务对象，由cn.jmicro.api.service.IServerServiceProxy抽像标识
 * 		c. Reference注解确定的远程服务代理对象cn.jmicro.api.client.AbstractClientServiceProxy
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:12:24
 */
/*@ObjFactory(Constants.DEFAULT_OBJ_FACTORY)
@Component(Constants.DEFAULT_OBJ_FACTORY)*/
public class SpringObjectFactory implements IObjectFactory {

	private final static Logger logger = LoggerFactory.getLogger(SpringObjectFactory.class);
	
	private IObjectFactory ori;
	
	private ConfigurableApplicationContext cxt;
	
	private SpringApplication app;
	
	private BeanFactoryBridge registBridge;
	
	private List<Object> registTemps = new ArrayList<>();
	
	public SpringObjectFactory(IObjectFactory original) {
		this.ori = original;
	}
	
	@Override
	public void start(IDataOperator dataOperator,String[] args) {
		
		Config cfg = this.ori.get(Config.class);
		String springMainClass = cfg.getString("spring.main", null);
		if(springMainClass == null) {
			throw new CommonException("Spring main class not found");
		}
		
		Class<?> primarySources = this.ori.loadCls(springMainClass);
		if(primarySources == null) {
			throw new CommonException("Spring main class " + springMainClass + " not found!");
		}
		
		 app = new SpringApplication(primarySources);
		
		 try {
			cxt = app.run(args);
		} catch (Exception e1) {
			logger.error("",e1);
			e1.printStackTrace();
		}
		 
		 registBridge = cxt.getBean(BeanFactoryBridge.class);
		 
		 if(!registTemps.isEmpty()) {
			 registTemps.forEach((e)->{
				 registBridge.registBean(e.getClass(),e);
			 });
		 }
		 
		 sprint2Jmicro();
		
	}
	
	private void sprint2Jmicro() {
		for(String beanName:cxt.getBeanDefinitionNames()){
			Object obj = cxt.getBean(beanName);
			if(!ori.exist(obj.getClass())) {
				ori.regist(obj);
			}
		}
	}

	private void reg2Spring(Object obj) {
		if(registBridge == null) {
			registTemps.add(obj);
		} else {
			registBridge.registBean(obj.getClass(),obj);
		}
	}
	
	@Override
	public void regist(Object obj) {
		ori.regist(obj);
		reg2Spring(obj);
	}

	@Override
	public void regist(Class<?> clazz, Object obj) {
		ori.regist(clazz, obj);
		reg2Spring(obj);
	}

	@Override
	public <T> void registT(Class<T> clazz, T obj) {
		ori.registT(clazz, obj);
		reg2Spring(obj);
	}

	@Override
	public void regist(String comName, Object obj) {
		ori.regist(comName, obj);
		reg2Spring(obj);
	}

	@Override
	public Boolean exist(Class<?> clazz) {
		return ori.exist(clazz) || cxt.getBean(clazz) != null;
	}

	@Override
	public <T> T get(Class<T> cls, boolean create) {
		T o = ori.get(cls,false);
		if(o == null && cxt != null) {
			o = cxt.getBean(cls);
		}
		return o;
	}
	
	@Override
	public void foreach(Consumer<Object> c) {
		throw new CommonException("Not support");
	}

	@Override
	public <T> T get(Class<T> cls) {
		T o = ori.get(cls,false);
		if(o == null && cxt != null) {
			o = cxt.getBean(cls);
		}
		return o;
	}

	@Override
	public <T> T getByName(String clsName) {
		T o = ori.getByName(clsName);
		if(o == null && cxt != null) {
			o = (T)cxt.getBean(clsName);
		}
		return o;
	}

	@Override
	public <T> T getRemoteServie(String srvName, String namespace, String version, AsyncConfigJRso[] acs) {
		T srv = ori.getRemoteServie(srvName, namespace, version,acs);
		reg2Spring(srv);
		return srv;
	}

	@Override
	public <T> T getRemoteServie(Class<T> srvCls, String ns, AsyncConfigJRso[] acs) {
		T srv = ori.getRemoteServie(srvCls, ns, acs);
		reg2Spring(srv);
		return srv;
	}

	@Override
	public <T> T getRemoteServie(ServiceItemJRso item, AsyncConfigJRso[] acs) {
		T srv =  ori.getRemoteServie(item, acs);
		reg2Spring(srv);
		return srv;
	}
	
	@Override
	public void addLoginStatusListener(ILoginStatusListener listener) {
		ori.addLoginStatusListener(listener);
	}
	
	@Override
	public <T> Set<T> getByParent(Class<T> parrentCls) {
		 Set<T> set = ori.getByParent(parrentCls);
		 Map<String, T> types = cxt.getBeansOfType(parrentCls);
		 if(types != null) {
			 set.addAll(types.values());
		 }
		 return set;
	}

	@Override
	public void addPostListener(IPostInitListener listener) {
		ori.addPostListener(listener);
	}

	@Override
	public Class<?> loadCls(String clsName) {
		return ori.loadCls(clsName);
	}

	@Override
	public void masterSlaveListen(IMasterChangeListener l) {
		ori.masterSlaveListen(l);
	}

	@Override
	public Boolean isSysLogin() {
		return ori.isSysLogin();
	}

	@Override
	public Boolean isRpcReady() {
		return ori.isRpcReady();
	}

	@Override
	public ProcessInfoJRso getProcessInfo() {
		return ori.getProcessInfo();
	}

	@Override
	public void notifyPostListener(Object obj) {
		this.ori.notifyPostListener(obj);
	}
	
}
