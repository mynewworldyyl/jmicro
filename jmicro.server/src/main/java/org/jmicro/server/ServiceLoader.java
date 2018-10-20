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
package org.jmicro.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IServer;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.Modifier;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:01
 */
@Component(lazy=false,level=11)
public class ServiceLoader {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	@Inject(required=true)
	private IRegistry registry;
	
	private Map<String,IServer> servers = new HashMap<>();
	
	private Map<String,Object> services = new ConcurrentHashMap<String,Object>();
	
	//private Map<String,Class<?>> servicesAnno = new ConcurrentHashMap<String,Class<?>>();
	
	@JMethod("init")
	public void init(){
		List<IServer> ss = JMicro.getObjectFactory().getByParent(IServer.class);
		for(IServer s : ss){
			Component anno = s.getClass().getAnnotation(Component.class);
			if(servers.containsKey(anno.value())){
				throw new CommonException("IServer:"+s.getClass().getName()+"] and ["
						+servers.get(anno.value())+" with same component name :"+anno.value());
			}
			servers.put(anno.value(), s);
		}
		exportService();
		logger.info("export service finish!");
	}
	
	public Object getService(String clsName,String namespace,String version){
		
		namespace = ServiceItem.namespace(namespace);
		version = ServiceItem.version(version);
	
		Class<?> parentCls = ClassScannerUtils.getIns().getClassByName(clsName);			
		
		List<Object> srvs = new ArrayList<>();
		
		for(Object s : services.values()){
			if(!parentCls.isInstance(s)){
				continue;
			}
			Class<?> tc = ProxyObject.getTargetCls(s.getClass());
			Service srvAnno = tc.getAnnotation(Service.class);
			if(namespace.equals(srvAnno.namespace()) && version.equals(srvAnno.version())){
				srvs.add(s);
			}
		}
		
		if(srvs.size() > 1){
			throw new CommonException("More than one services found for name: ["+clsName
					+"] impls ["+srvs.toString()+"]");
		}
		
		if(srvs.size()==1) {
			return srvs.get(0);
		}else {
			return null;
		}
	}
	
	public Object getService(String impl){
		Class<?> cls = ClassScannerUtils.getIns().getClassByName(impl);
		if(Modifier.isAbstract(cls.getModifiers()) || Modifier.isInterface(cls.getModifiers())){
			throw new CommonException("impl is not a concrete class: "+impl);
		}
		for(Object srv : services.values()){
			if(cls.isInstance(srv)){
				return srv;
			}
		}
		return null;
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

		if(c.isInterface() || Modifier.isAbstract(c.getModifiers())){
			return;
		}
		
		Object srv = this.createService(c);
		if(srv == null){
			throw new CommonException("fail to export server "+c.getName());
		}
		registService(srv);
		String key = this.serviceName(c);
		services.put(key, srv);
		
		logger.info("Export service:"+c.getName());
	}
	
	private void registService(Object srv1) {
		Class<?> srvCls = ProxyObject.getTargetCls(srv1.getClass());
		ServiceItem item = this.getServiceItems(srvCls);
		if(item == null){
			logger.error("class "+srvCls.getName()+" is not service");
			return;
		}
		
		for(IServer s : this.servers.values()){
			Component sano = ProxyObject.getTargetCls(s.getClass()).getAnnotation(Component.class);
			item.setHost(s.host());
			item.setPort(s.port());
			item.setTransport(sano.value());
			registry.regist(item);
		}
		
	}
	
	private ServiceItem getServiceItems(Class<?> proxySrv) {
		Class<?> srvCls = ProxyObject.getTargetCls(proxySrv);
		if(!srvCls.isAnnotationPresent(Service.class)){
			throw new CommonException("Not a service class ["+srvCls.getName()+"] annotated with ["+Service.class.getName()+"]");
		}
		
		//Object srv = ProxyObject.getTarget(proxySrv);
		Service anno = srvCls.getAnnotation(Service.class);
		Class<?> interfaces = anno.infs();
		if(interfaces == null || interfaces == Void.class){
			Class<?>[] ints = srvCls.getInterfaces();
			if(ints == null || ints.length != 1) {
				throw new CommonException("service ["+srvCls.getName()+"] have to implement one and only one interface.");
			}
			interfaces = ints[0] ;
		}
		
		ServiceItem item = new ServiceItem();
		
		item.setImpl(proxySrv.getName());
	
		item.setServiceName(interfaces.getName());
		
		item.setVersion(anno.version());
		item.setNamespace(anno.namespace());
		
		item.setMaxFailBeforeFusing(anno.maxFailBeforeFusing());
		item.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade());
		item.setRetryCnt(anno.retryCnt());
		item.setRetryInterval(anno.retryInterval());
		item.setTestingArgs(anno.testingArgs());
		item.setTimeout(anno.timeout());
		item.setMaxSpeed(this.parseSpeed(anno.maxSpeed()));
		item.setSpeedUnit(this.parseSpeedUnit(anno.maxSpeed()));
		item.setAvgResponseTime(anno.avgResponseTime());
		item.setMonitorEnable(anno.monitorEnable());
		
		ServiceMethod checkMethod = new ServiceMethod();
		item.addMethod(checkMethod);
		checkMethod.setMaxFailBeforeFusing(1);
		checkMethod.setMaxFailBeforeDegrade(1);
		checkMethod.setRetryCnt(3);
		checkMethod.setRetryInterval(500);
		checkMethod.setTestingArgs("What are you doing?");
		checkMethod.setTimeout(anno.timeout());
		checkMethod.setMaxSpeed(1000);
		checkMethod.setSpeedUnit("ms");
		checkMethod.setAvgResponseTime(anno.avgResponseTime());
		checkMethod.setMonitorEnable(anno.monitorEnable());
		checkMethod.setMethodName("wayd");
		checkMethod.setMethodParamTypes("java.lang.String");
		
		for(Method m : interfaces.getMethods()) {
			ServiceMethod sm = new ServiceMethod();
			Method srvMethod = null;
			try {
				srvMethod = srvCls.getMethod(m.getName(), m.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				throw new CommonException("Service not found: "+m.getName(),e);
			}
			item.addMethod(sm);
			
			//具体实现类的注解优先，如果实现类对就方法没有注解，则使用接口对应的方法注解
			//如果接口和实现类都没有，则使用实现类的Service注解，实现类肯定有Service注解，否则不会成为服务
			if(srvMethod.isAnnotationPresent(SMethod.class)){
				SMethod manno = srvMethod.getAnnotation(SMethod.class);
				sm.setMaxFailBeforeFusing(manno.maxFailBeforeFusing());
				sm.setMaxFailBeforeDegrade(manno.maxFailBeforeDegrade());
				sm.setRetryCnt(manno.retryCnt());
				sm.setRetryInterval(manno.retryInterval());
				sm.setTestingArgs(manno.testingArgs());
				sm.setTimeout(manno.timeout());
				sm.setMaxSpeed(this.parseSpeed(manno.maxSpeed()));
				sm.setSpeedUnit(this.parseSpeedUnit(manno.maxSpeed()));
				sm.setAvgResponseTime(manno.avgResponseTime());
				sm.setMonitorEnable(manno.monitorEnable());
				
				//checkStreamCallback(manno.streamCallback());
				
				sm.setStreamCallback(manno.streamCallback());
				sm.setNeedResponse(manno.needResponse());
				/*if(StringUtils.isEmpty(sm.getStreamCallback())){
					sm.setAsync(manno.async());
				}else {
					sm.setAsync(true);
				}*/
				
			}else if(m.isAnnotationPresent(SMethod.class)){
				SMethod manno = m.getAnnotation(SMethod.class);
				sm.setMaxFailBeforeFusing(manno.maxFailBeforeFusing());
				sm.setMaxFailBeforeDegrade(manno.maxFailBeforeDegrade());
				sm.setRetryCnt(manno.retryCnt());
				sm.setRetryInterval(manno.retryInterval());
				sm.setTestingArgs(manno.testingArgs());
				sm.setTimeout(manno.timeout());
				sm.setMaxSpeed(this.parseSpeed(manno.maxSpeed()));
				sm.setSpeedUnit(this.parseSpeedUnit(manno.maxSpeed()));
				sm.setAvgResponseTime(manno.avgResponseTime());
				sm.setMonitorEnable(manno.monitorEnable());
				sm.setStreamCallback(manno.streamCallback());
				sm.setNeedResponse(manno.needResponse());
			} else {
				sm.setMaxFailBeforeFusing(anno.maxFailBeforeFusing());
				sm.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade());
				sm.setRetryCnt(anno.retryCnt());
				sm.setRetryInterval(anno.retryInterval());
				sm.setTestingArgs(anno.testingArgs());
				sm.setTimeout(anno.timeout());
				sm.setMaxSpeed(this.parseSpeed(anno.maxSpeed()));
				sm.setSpeedUnit(this.parseSpeedUnit(anno.maxSpeed()));
				sm.setAvgResponseTime(anno.avgResponseTime());
				sm.setMonitorEnable(anno.monitorEnable());
			}
			
			sm.setMethodName(m.getName());
			sm.setMethodParamTypes(ServiceMethod.methodParamsKey( m.getParameterTypes()));
		}
		
		return item;
	}
	
	private String parseSpeedUnit(String maxSpeed) {
		if(StringUtils.isEmpty(maxSpeed)){
			return "";
		}
		maxSpeed = maxSpeed.trim().toLowerCase();
		if(maxSpeed.endsWith("ms") || maxSpeed.endsWith("ns") ){
			return maxSpeed.substring(maxSpeed.length()-2, maxSpeed.length());
		}else {
			return maxSpeed.substring(maxSpeed.length()-1, maxSpeed.length());
		}
	}

	private int parseSpeed(String maxSpeed) {
		if(StringUtils.isEmpty(maxSpeed)){
			return 0;
		}
		maxSpeed = maxSpeed.trim().toLowerCase();
		if(maxSpeed.endsWith("ms") || maxSpeed.endsWith("ns") ){
			return Integer.parseInt(maxSpeed.substring(0, maxSpeed.length()-2));
		}else {
			return Integer.parseInt(maxSpeed.substring(0, maxSpeed.length()-1));
		}
	}

	private void checkStreamCallback(String streamCb) {
		if(StringUtils.isEmpty(streamCb)){
			return;
		}
		
		String msg = "Callback ["+streamCb+" params invalid";
		String[] arr = streamCb.split("#");
		if(arr.length != 2){
			throw new CommonException(msg);
		}
		
		String serviceName = arr[0];
		if(serviceName.length() == 0){
			throw new CommonException(msg);
		}
		
		/*Object srv = ComponentManager.getObjectFactory().getByName(serviceName);
		if(srv == null){
			throw new CommonException(msg);
		}*/
			
		String methodName = arr[1];
		
		int i = methodName.indexOf("(");
		if(i < 0) {
			throw new CommonException(msg);
		}
		
		int j = methodName.indexOf(")");
		if(j < 0) {
			throw new CommonException(msg);
		}
		
	}
	
	private String serviceName(Class<?> c) {
		return c.getName();
	}

	private Object createService(Class<?> class1) {
		Object srv = JMicro.getObjectFactory().get(class1);
		if(srv != null) {
			services.put(class1.getName(), srv);
		}
		return srv;
	}
	
	public static Method getServiceMethod(Object obj ,IRequest req){
		Class<?>[] pst = getMethodParamsType(req);
		try {
			Method m = obj.getClass().getMethod(req.getMethod(), pst);
			return m;
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			throw new RpcException(req,"",e);
		}
	}
	
	public static Class<?>[]  getMethodParamsType(IRequest req){
		return getMethodParamsType(req.getArgs());
	}
	
	public static Class<?>[]  getMethodParamsType(Object[] args){
		if(args == null || args.length==0){
			return new Class<?>[0];
		}
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			parameterTypes[i] = args[i].getClass();
		}
		return parameterTypes;
	}
	
	public static Method getInterfaceMethod(IRequest req){
		try {
			Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(req.getServiceName());
			Class<?>[] pst = getMethodParamsType(req);
			Method m = cls.getMethod(req.getMethod(),pst);
			return m;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			logger.error("getInterfaceMethod",e);
		}
		return null;
	}
	
	/*public static boolean isNeedResponse(ServiceLoader sl ,IRequest req){
		Method m = getServiceMethod(sl,req);
		if(m == null || !m.isAnnotationPresent(SMethod.class)){
			m = getInterfaceMethod(req);
			if(m == null || !m.isAnnotationPresent(SMethod.class)){
				return true;
			}
		}
		SMethod sm = m.getAnnotation(SMethod.class);
		return sm.needResponse();
	}*/
	
}
