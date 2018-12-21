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
package org.jmicro.api.service;

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
import org.jmicro.api.annotation.SBreakingRule;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IServer;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.BreakRule;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.Server;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.ReflectUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.Modifier;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:01
 */
@Component(lazy=false,level=2)
public class ServiceLoader {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	@Inject(required=true)
	private IRegistry registry;
	
	private Map<String,IServer> servers = new HashMap<>();
	
	private Map<String,Object> services = new ConcurrentHashMap<String,Object>();
	
	//private Map<String,Class<?>> servicesAnno = new ConcurrentHashMap<String,Class<?>>();
	
	@JMethod("init")
	public void init(){
		if(Config.isClientOnly()){
			return;
		}
		List<IServer> ss = JMicro.getObjectFactory().getByParent(IServer.class);
		for(IServer s : ss){
			org.jmicro.api.annotation.Server anno = 
					s.getClass().getAnnotation(org.jmicro.api.annotation.Server.class);
			if(servers.containsKey(anno.transport())){
				throw new CommonException("IServer:" + s.getClass().getName() + "] and ["
						+ servers.get(anno.transport()) + " with same component name :" + anno.transport());
			}
			servers.put(anno.transport(), s);
		}
		exportService();
		logger.info("export service finish!");
	}
	
	public Object getService(String clsName,String namespace,String version){
		
		namespace = UniqueServiceKey.namespace(namespace);
		version = UniqueServiceKey.version(version);
	
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
			Server sr = new Server();
			org.jmicro.api.annotation.Server sano = ProxyObject.getTargetCls(s.getClass())
					.getAnnotation(org.jmicro.api.annotation.Server.class);
			sr.setHost(s.host());
			sr.setPort(s.port());
			sr.setProtocol(sano.transport());
			item.getServers().add(sr);
		}
		registry.regist(item);
	}
	
	private ServiceItem getServiceItems(Class<?> proxySrv) {
		Class<?> srvCls = ProxyObject.getTargetCls(proxySrv);
		if(!srvCls.isAnnotationPresent(Service.class)){
			throw new CommonException("Not a service class ["+srvCls.getName()+"] annotated with ["+Service.class.getName()+"]");
		}
		
		//Object srv = ProxyObject.getTarget(proxySrv);
		Service anno = srvCls.getAnnotation(Service.class);
		Class<?> interfacez = anno.infs();
		if(interfacez == null || interfacez == Void.class){
			Class<?>[] ints = srvCls.getInterfaces();
			if(ints == null || ints.length != 1) {
				throw new CommonException("service ["+srvCls.getName()+"] have to implement one and only one interface.");
			}
			interfacez = ints[0];
		}
		
		Service intAnno = null;
		
		if(interfacez != null && interfacez.isAnnotationPresent(Service.class)){
			intAnno = interfacez.getAnnotation(Service.class);
		}
		
		ServiceItem item = new ServiceItem();
		UniqueServiceKey usk = new UniqueServiceKey();
		usk.setNamespace(getFieldValue(anno.namespace(),intAnno == null ? null : intAnno.namespace(),Constants.DEFAULT_NAMESPACE));
		usk.setServiceName(interfacez.getName());
		usk.setVersion(getFieldValue(anno.version(),intAnno == null ? null : intAnno.version(),Constants.VERSION));
		usk.setInstanceName(Config.getInstanceName());
		usk.setHost(Config.getHost());
		
		item.setKey(usk);
		item.setImpl(proxySrv.getName());
		
		//item.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade()!=100 || intAnno == null ?anno.maxFailBeforeDegrade():intAnno.maxFailBeforeDegrade());
		item.setRetryCnt(anno.retryCnt()!=3 || intAnno == null ?anno.retryCnt():intAnno.retryCnt());
		item.setRetryInterval(anno.retryInterval()!=500 || intAnno == null ?anno.retryInterval():intAnno.retryInterval());
		//item.setTestingArgs(getFieldValue(anno.testingArgs(),intAnno == null ? null : intAnno.testingArgs(),""));
		item.setTimeout(anno.timeout()!=2000 || intAnno == null ?anno.timeout():intAnno.timeout());
		item.setMaxSpeed(anno.maxSpeed());
		item.setBaseTimeUnit( StringUtils.isEmpty(anno.baseTimeUnit()) || intAnno == null ? anno.baseTimeUnit() : intAnno.baseTimeUnit());
		item.setTimeWindow(anno.timeWindow() <= 0 || intAnno == null ?anno.timeWindow():intAnno.timeWindow());
		item.setCheckInterval(anno.checkInterval() <= 0 || intAnno == null ?anno.checkInterval():intAnno.checkInterval());
		
		item.setAvgResponseTime(anno.avgResponseTime()!=-1 || intAnno == null ? anno.avgResponseTime() : intAnno.avgResponseTime());
		item.setMonitorEnable(anno.monitorEnable()!=-1 || intAnno == null ? anno.monitorEnable() : intAnno.monitorEnable());
		item.setLoggable(anno.loggable()!=-1 || intAnno == null ? anno.loggable() : intAnno.loggable());
		item.setDebugMode(anno.debugMode()!=-1 || intAnno == null ? anno.debugMode() : intAnno.debugMode());
		
		//测试方法
		ServiceMethod checkMethod = new ServiceMethod();
		item.addMethod(checkMethod);
		checkMethod.setMaxFailBeforeDegrade(1);
		checkMethod.setRetryCnt(3);
		checkMethod.setRetryInterval(500);
		checkMethod.setTestingArgs("What are you doing?");
		checkMethod.setTimeout(anno.timeout());
		checkMethod.setMaxSpeed(1000);
		checkMethod.setBaseTimeUnit(Constants.TIME_MILLISECONDS);
		checkMethod.setAvgResponseTime(anno.avgResponseTime());
		checkMethod.setMonitorEnable(anno.monitorEnable());
		
		checkMethod.setBreaking(false);
		checkMethod.setFailResponse("I'm breaking now");
		
		checkMethod.getKey().setUsk(usk);
		checkMethod.getKey().setMethod("wayd");
		checkMethod.getKey().setParamsStr(UniqueServiceMethodKey.paramsStr(new String[]{"java.lang.String"}));
		checkMethod.setLoggable(0);
		checkMethod.setDebugMode(0);
		
		for(Method m : interfacez.getMethods()) {
			ServiceMethod sm = new ServiceMethod();
			Method srvMethod = null;
			try {
				srvMethod = srvCls.getMethod(m.getName(), m.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				throw new CommonException("Service not found: "+m.getName(),e);
			}
			item.addMethod(sm);
			
			//具体实现类的注解优先,如果实现类对就方法没有注解,则使用接口对应的方法注解
			//如果接口和实现类都没有,则使用实现类的Service注解，实现类肯定有Service注解，否则不会成为服务
			
			SMethod manno = srvMethod.getAnnotation(SMethod.class);
			SMethod intMAnno = m.getAnnotation(SMethod.class);
			sm.setBreaking(false);
			
			SBreakingRule sbr = null;
					
			if(manno == null && intMAnno== null) {
				//sm.setMaxFailBeforeDegrade(item.getMaxFailBeforeDegrade());
				sm.setRetryCnt(item.getRetryCnt());
				sm.setRetryInterval(item.getRetryInterval());
				//sm.setTestingArgs(item.getTestingArgs());
				sm.setTimeout(item.getTimeout());
				sm.setMaxSpeed(item.getMaxSpeed());
				sm.setBaseTimeUnit(item.getBaseTimeUnit());
				sm.setTimeWindow(item.getTimeWindow());
				sm.setAvgResponseTime(item.getAvgResponseTime());
				sm.setMonitorEnable(item.getMonitorEnable());
				sm.setFailResponse("");
				sm.setLoggable(-1);
				sm.setDebugMode(-1);
				
			} else {
				 if(manno != null ) {
					sbr = manno.breakingRule();
					sm.setRetryCnt(manno.retryCnt()!=3 || intMAnno == null ?manno.retryCnt():intMAnno.retryCnt());
					sm.setRetryInterval(manno.retryInterval()!=500 || intMAnno == null ? manno.retryInterval():intMAnno.retryInterval());
					sm.setTestingArgs(getFieldValue(manno.testingArgs(),intMAnno == null ? null : intMAnno.testingArgs(),""));
					sm.setTimeout(manno.timeout()!=2000 || intMAnno == null ?manno.timeout():intMAnno.timeout());
					sm.setMaxSpeed(manno.maxSpeed());
					sm.setAvgResponseTime(manno.avgResponseTime()!=-1 || intMAnno == null ? manno.avgResponseTime() : intMAnno.avgResponseTime());
					sm.setMonitorEnable(manno.monitorEnable()!=-1 || intMAnno == null ? manno.monitorEnable() : intMAnno.monitorEnable());
					sm.setStream(manno.stream());
					sm.setDumpDownStream(manno.dumpDownStream());
					sm.setDumpUpStream(manno.dumpUpStream());
					sm.setNeedResponse(manno.needResponse());
					sm.setFailResponse(manno.failResponse());
					sm.setTimeWindow(manno.timeWindow()<=0?item.getTimeWindow():manno.timeWindow());
					sm.setCheckInterval(manno.checkInterval()<=0?item.getCheckInterval():manno.checkInterval());
					
					sm.setBaseTimeUnit(StringUtils.isEmpty(manno.baseTimeUnit())? item.getBaseTimeUnit():manno.baseTimeUnit());
					
					sm.setLoggable(manno.loggable()!=-1 || intMAnno == null ? manno.loggable() : intMAnno.loggable());
					sm.setDebugMode(manno.debugMode()!=-1 || intMAnno == null ? manno.debugMode() : intMAnno.debugMode());
					
				 } else {
					sbr = intMAnno.breakingRule();
					sm.setRetryCnt(intMAnno.retryCnt());
					sm.setRetryInterval(intMAnno.retryInterval());
					sm.setTestingArgs(intMAnno.testingArgs());
					sm.setTimeout(intMAnno.timeout());
					sm.setMaxSpeed(intMAnno.maxSpeed());
					sm.setBaseTimeUnit(intMAnno.baseTimeUnit());
					sm.setAvgResponseTime(intMAnno.avgResponseTime());
					sm.setMonitorEnable(intMAnno.monitorEnable());
					sm.setStream(intMAnno.stream());
					sm.setDumpDownStream(intMAnno.dumpDownStream());
					sm.setDumpUpStream(intMAnno.dumpUpStream());
					sm.setNeedResponse(intMAnno.needResponse());
					sm.setFailResponse(intMAnno.failResponse());
					sm.setLoggable(intMAnno.loggable());
					sm.setDebugMode(intMAnno.debugMode());
					sm.setTimeWindow(intMAnno.timeWindow()<=0?item.getTimeWindow():intMAnno.timeWindow());
					sm.setCheckInterval(intMAnno.checkInterval()<=0?item.getCheckInterval():intMAnno.checkInterval());
					sm.setBaseTimeUnit(StringUtils.isEmpty(intMAnno.baseTimeUnit())? item.getBaseTimeUnit():intMAnno.baseTimeUnit());
				}
				 
				sm.getBreakingRule().setBreakTimeInterval(sbr.breakTimeInterval());
				sm.getBreakingRule().setEnable(sbr.enable());
				sm.getBreakingRule().setPercent(sbr.percent());
				sm.getBreakingRule().setCheckInterval(sbr.checkInterval());
					
			} 
			
			sm.getKey().setUsk(usk);
			sm.getKey().setMethod(m.getName());
			sm.getKey().setParamsStr(UniqueServiceMethodKey.paramsStr(m.getParameterTypes()));
		}
		
		return item;
	}
	
	private  String getFieldValue(String anno, String intAnno,String defau) {
		if(StringUtils.isEmpty(anno) && StringUtils.isEmpty(intAnno)) {
			return defau;
		}
		if(!StringUtils.isEmpty(anno)) {
			return anno;
		}
		return intAnno;
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
		Class<?>[] pst = getMethodParamsType(req.getArgs());
		try {
			Method m = obj.getClass().getMethod(req.getMethod(), pst);
			return m;
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			throw new RpcException(req,"",e);
		}
	}
	
	public static Class<?>[]  getMethodParamsType(Object[] args){
		if(args == null || args.length==0){
			return new Class<?>[0];
		}
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			parameterTypes[i] = ReflectUtils.getPrimitiveClazz(args[i].getClass());
		}
		return parameterTypes;
	}
	
	public static Method getInterfaceMethod(IRequest req){
		try {
			Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(req.getServiceName());
			Class<?>[] pst = getMethodParamsType(req.getArgs());
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
