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
package org.jmicro.api.servicemanager;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IServer;
import org.jmicro.common.Constants;
import org.jmicro.common.url.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:01
 */
@Component(lazy=false,level=11)
public class ServiceLoader {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	/*private static ServiceLoader loader = new ServiceLoader();
	
	static {
		loader.exportService();
		logger.info("export service finish!");
	}
	private ServiceLoader(){}
	public static ServiceLoader getIns(){
		return loader;
	}*/
	
	@Inject(required=true)
	private IRegistry registry;
	
	@Inject(required=true)
	private IServer server;
	
	private Map<String,Object> services = new ConcurrentHashMap<String,Object>();
	
	//private Map<String,Class<?>> servicesAnno = new ConcurrentHashMap<String,Class<?>>();
	
	@JMethod("init")
	public void init(){
		exportService();
		logger.info("export service finish!");
	}
	
	public Object getService(String clsName,String namespace,String version){
		Object srv = null;
		
		if(StringUtils.isEmpty(namespace)){
			namespace = Constants.DEFAULT_NAMESPACE;
		}
		if(StringUtils.isEmpty(version)){
			version = "0.0.0";
		}
		Class<?> parentCls = ClassScannerUtils.getIns().getClassByName(clsName);			
		for(Object s : services.values()){
			if(!parentCls.isInstance(s)){
				continue;
			}
			Class<?> tc = ProxyObject.getTargetCls(s.getClass());
			Service srvAnno = tc.getAnnotation(Service.class);
			if(namespace.equals(srvAnno.namespace()) && version.equals(srvAnno.version())){
				srv = s;
				break;
			}
		}
		
		return srv;
	}
	
	private Set<Class<?>> loadServiceClass() {
		Set<Class<?>> clses = ClassScannerUtils.getIns().loadClassesByAnno(Service.class);
		return clses;
	}
	
	
	public boolean exportService(){
		if(!services.isEmpty()){
			//throw new CommonException("NO service to export");
			return true;
		}
		synchronized(this) {
			//loadServiceClass();
			return this.doExport();
		}	
	}
	
	private boolean doExport() {
		Set<Class<?>> clses =  loadServiceClass();
		Iterator<Class<?>> ite = clses.iterator();
		boolean flag = false;
		while(ite.hasNext()){
			exportOne(ite.next());
			flag = true;
		}
		return flag;
	}
	
	public void exportOne(Class<?> c) {

		Object srv = this.createService(c);
		if(srv == null){
			throw new CommonException("fail to export server "+c.getName());
		}
		registService(srv);
		String key = this.serviceName(c);
		services.put(key, srv);
		
		logger.info("Export service "+c.getName());
	}
	
	private void registService(Object srv) {
		srv = ProxyObject.getTarget(srv);
		ServiceItem[] items = this.getServiceItems(srv);
		if(items == null || items.length == 0){
			logger.error("class "+srv.getClass().getName()+" is not service");
			return;
		}
		Service srvAnno = srv.getClass().getAnnotation(Service.class);
		IRegistry registry = ComponentManager.getRegistry(srvAnno.registry());
		if(registry == null){
			registry = this.registry;
		}
		for(ServiceItem item : items){
			registry.regist(item);
		}
	}
	
	private ServiceItem[] getServiceItems(Object srv) {
		srv = ProxyObject.getTarget(srv);
		Class<?> srvCls = srv.getClass();
		if(!srvCls.isAnnotationPresent(Service.class)){
			return null;
		}
		Service anno = srvCls.getAnnotation(Service.class);
		IServer server = this.getServer(srvCls);
		Class<?>[] interfaces = anno.interfaces();
		if(interfaces.length ==0 ){
			interfaces = srvCls.getInterfaces();
			if(interfaces == null || interfaces.length == 0) {
				throw new CommonException("service ["+srvCls.getName()+"] have to implement at least one interface.");
			}
		}
		
		ServiceItem[] sitems = new ServiceItem[interfaces.length];
		int index = 0;
		String addr = server.host();
		int port = server.port();
		
		for(Class<?> in : interfaces){
			if(!in.isInstance(srv)){
				throw new CommonException("service ["+srvCls.getName()+"] not implement interface ["+in.getName()+"].");
			}
			
			ServiceItem item = new ServiceItem();
			item.setHost(addr);
			item.setPort(port);
			item.setServiceName(in.getName());
			item.setVersion(anno.version());
			item.setNamespace(anno.namespace());
			
			item.setMaxFailBeforeFusing(anno.maxFailBeforeFusing());
			item.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade());
			item.setRetryCnt(anno.retryCnt());
			item.setRetryInterval(anno.retryInterval());
			item.setTestingArgs(anno.testingArgs());
			item.setTimeout(anno.timeout());
			item.setMaxSpeed(anno.maxSpeed());
			item.setMinSpeed(anno.minSpeed());
			item.setAvgResponseTime(anno.avgResponseTime());
			item.setMonitorEnable(anno.monitorEnable());
			
			for(Method m : in.getMethods()) {
				ServiceMethod sm = new ServiceMethod();
				if(m.isAnnotationPresent(SMethod.class)){
					SMethod manno = m.getAnnotation(SMethod.class);
					sm.setMaxFailBeforeFusing(manno.maxFailBeforeFusing());
					sm.setMaxFailBeforeDegrade(manno.maxFailBeforeDegrade());
					sm.setRetryCnt(manno.retryCnt());
					sm.setRetryInterval(manno.retryInterval());
					sm.setTestingArgs(manno.testingArgs());
					sm.setTimeout(manno.timeout());
					sm.setMaxSpeed(manno.maxSpeed());
					sm.setMinSpeed(manno.minSpeed());
					sm.setAvgResponseTime(manno.avgResponseTime());
					sm.setMonitorEnable(manno.monitorEnable());
				} else {
					sm.setMaxFailBeforeFusing(anno.maxFailBeforeFusing());
					sm.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade());
					sm.setRetryCnt(anno.retryCnt());
					sm.setRetryInterval(anno.retryInterval());
					sm.setTestingArgs(anno.testingArgs());
					sm.setTimeout(anno.timeout());
					sm.setMaxSpeed(anno.maxSpeed());
					sm.setMinSpeed(anno.minSpeed());
					sm.setAvgResponseTime(anno.avgResponseTime());
					sm.setMonitorEnable(anno.monitorEnable());
				}
				
				sm.setMethodName(m.getName());
				sm.setMethodParamTypes(ServiceMethod.methodParamsKey( m.getParameterTypes()));
				
				item.addMethod(sm);
			}
			
			sitems[index] = item;
		}
		return sitems;
	}
	
	private IServer getServer(Class<?> srvCls){
		srvCls = ProxyObject.getTargetCls(srvCls);
		Service srvAnno = srvCls.getAnnotation(Service.class);
		String serverName = srvAnno.server();
		if(StringUtils.isEmpty(serverName)) {
			serverName = Constants.DEFAULT_SERVER;
		}
		
		IServer server = ComponentManager.getCommponentManager(IServer.class).getComponent(serverName);
		if(server == null){
			return this.server;
		}
		return server;
	}
	
	private String serviceName(Class<?> c) {
		return c.getName();
	}

	private Object createService(Class<?> class1) {
		Object srv = ComponentManager.getObjectFactory().get(class1);
		if(srv != null) {
			services.put(class1.getName(), srv);
		}
		return srv;
	}	
	
}
