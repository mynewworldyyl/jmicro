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
package cn.jmicro.api.service;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SBreakingRule;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.annotation.Subscribe;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.classloader.RpcClassLoaderHelper;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.Server;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.StringUtils;
import javassist.Modifier;

/**
 * 向注册中心注册服务
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:01
 */
@Component(lazy=false,level=2)
public class ServiceLoader{

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	@Cfg(value = "/startSocket",required=false)
	private boolean enable = true;
	
	//导出服务时使用的IP和端口，格式为IP的字符串形式,此值不建议在全局配置目录中配置，否则将导致全部服务会绑定在此ＩＰ上
	@Cfg(value = "/"+Constants.ExportSocketIP,required=false,defGlobal=false)
	private String exportSocketIP = null;
	
	//导出服务时使用的端口，格式为IP的字符串形式,此值不建议在全局配置目录中配置，否则将导致全部服务会绑定在此端口上
	@Cfg(value = "/exportSocketPort",required=false,defGlobal=false)
	private String exportSocketPort = null;
	
	//导出服务时使用的IP和端口，格式为IP的字符串形式,此值不建议在全局配置目录中配置，否则将导致全部服务会绑定在此ＩＰ上
	@Cfg(value = "/"+Constants.ExportHttpIP,required=false,defGlobal=false)
	private String exportHttpIP = null;
	
	//导出服务时使用的端口，格式为IP的字符串形式,此值不建议在全局配置目录中配置，否则将导致全部服务会绑定在此端口上
	@Cfg(value = "/exportHttpPort",required=false,defGlobal=false)
	private String exportHttpPort = null;
	
	@Inject(required=true)
	private IRegistry registry;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private RpcClassLoaderHelper cl;
	
	@Inject
	private ProcessInfo pi;
	
	private Map<String,IServer> servers = new HashMap<>();
	
	private Map<Integer,Object> services = new ConcurrentHashMap<Integer,Object>();
	
	private Server nettyServer = null;
	
	private Server httpServer = null;
	
	//private Map<String,Class<?>> servicesAnno = new ConcurrentHashMap<String,Class<?>>();
	
	public void ready(){
		
		if(Config.isClientOnly() || !enable){
			//纯客户端不需要导出服务,RPC端口没开放
			logger.warn(Config.getInstanceName()+" Client Only so not load service!");
			return;
		}
		
		/*this.of.masterSlaveListen((type,isMaster)->{
			if(isMaster && (IMasterChangeListener.MASTER_ONLINE == type || IMasterChangeListener.MASTER_NOTSUPPORT == type)) {
				doExportService();
			}
		});*/
		
		doExportService();
	}
	
	private void doExportService() {
		Set<IServer> ss = of.getByParent(IServer.class);
		for(IServer s : ss){
			cn.jmicro.api.annotation.Server anno = 
					s.getClass().getAnnotation(cn.jmicro.api.annotation.Server.class);
			if(servers.containsKey(anno.transport())){
				throw new CommonException("IServer:" + s.getClass().getName() + "] and ["
						+ servers.get(anno.transport()) + " with same transport name :" + anno.transport());
			}
			int cnt = 0;
			while(cnt < 10 && (StringUtils.isEmpty(s.port()) || "0".equals(s.port()))) {
				logger.info("Waiting for transport:{} ready {}S",anno.transport(),cnt+1);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("",e);
				}
				cnt++;
			}
			if(StringUtils.isEmpty(s.port())) {
				throw new CommonException("Fail to get port for transport: " +anno.transport()
				+", server:" + ss.getClass().getName());
			}
			servers.put(anno.transport(), s);
		}
		
		
		for(IServer s : this.servers.values()){
			
			String host = s.host();
			String port = s.port();
				
			Server sr = new Server();
			cn.jmicro.api.annotation.Server sano = ProxyObject.getTargetCls(s.getClass())
					.getAnnotation(cn.jmicro.api.annotation.Server.class);
			
			if(Constants.TRANSPORT_NETTY.equals(sano.transport())) {
				if( this.exportSocketIP != null) {
					host = this.exportSocketIP;
					if(StringUtils.isNotEmpty(this.exportSocketPort)  && !"0".equals(this.exportSocketPort)) {
						port = this.exportSocketPort;
					}
				}
				this.nettyServer = sr;
			} else if(Constants.TRANSPORT_NETTY_HTTP.equals(sano.transport())) {
				if(this.exportHttpIP != null) {
					host = this.exportHttpIP;
					if(StringUtils.isNotEmpty(this.exportHttpPort) && !"0".equals(this.exportHttpPort)) {
						port = this.exportHttpPort;
					}
				}
				this.httpServer = sr;
			}
			
			sr.setHost(host);
			sr.setPort(port);
			sr.setProtocol(sano.transport());
			
		}
		
		exportService();
		logger.info("export service finish!");
	}
	
	public boolean hasServer() {
		return !this.servers.isEmpty();
	}

	/*private Object getService(String clsName,String namespace,String version){
		
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
	}*/
	
	public Object getService(Integer code){
		//Class<?> cls = ClassScannerUtils.getIns().getClassByName(impl);
		/*
		if(Modifier.isAbstract(cls.getModifiers()) || Modifier.isInterface(cls.getModifiers())){
			throw new CommonException("impl is not a concrete class: "+impl);
		}
		*/
		/*for(Object srv : services.values()){
			if(cls.isInstance(srv)){
				return srv;
			}
		}*/
		return services.get(code);
	}
	
	private Set<Class<?>> loadServiceClass() {
		Set<Class<?>> clses = ClassScannerUtils.getIns().loadClassesByAnno(Service.class);
		return clses;
	}
	
	private boolean exportService(){
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
	
	private void exportOne(Class<?> c) {

		if(c.isInterface() || Modifier.isAbstract(c.getModifiers())){
			return;
		}
		
		Object srv = of.get(c);
		if(srv == null){
			throw new CommonException("fail to export server, service instance is NULL "+c.getName());
		}
		
		/*if(c.getName().contains("AccountService")) {
			logger.info(c.getName());
		}*/
		
		ServiceItem si = createSrvItemByClass(c);
		
		int code = idGenerator.getIntId(ServiceItem.class);
		si.setCode(code);
		
		registService(si,srv);
		
		//of.regist(code+"", srv);
		
		//String key = this.serviceName();
		//services.put(si.getImpl(), srv);
		
		logger.info("Export service:"+c.getName());
	}
	
	public void unregistService(ServiceItem item) {
		registry.unregist(item);
	}
	
	public ServiceItem registService(ServiceItem item,Object srv) {
		
		if(Config.isClientOnly()) {
			logger.warn("Client only cannot export service!");
			return null;
		}
		
		if(item == null){
			logger.error("Service item cannot be NULL");
			return null;
		}
		
		if(srv != null && !services.containsKey(item.getCode())) {
			services.put(item.getCode(), srv);
		}
		
		//if(item.getClientId() >10) {
		try {
			Class<?> clazz = this.getClass().getClassLoader().loadClass(item.getKey().getServiceName());
			cl.addClassInstance(clazz);
		} catch (ClassNotFoundException e) {
			throw new CommonException("",e);
		}
		//}
		
		item.setInsId(pi.getId());
		registry.regist(item);
		
		return item;
	}
	
	private ServiceItem createSrvItemByClass(Class<?> cls) {
		Class<?> srvCls = ProxyObject.getTargetCls(cls);
		ServiceItem item = this.getServiceItems(srvCls,null,null);
		if(item == null){
			logger.error("class "+srvCls.getName()+" is not service");
			return null;
		}
		return item;
	}
	
	public ServiceItem createSrvItem(Class<?> interfacez, String ns, String ver, String impl,int clientId) {
		if(Config.isClientOnly()) {
			logger.warn("Client only cannot export service!");
			return null;
		}
		if(!interfacez.isInterface()) {
			logger.error("RPC service have to be public interface: "+interfacez.getName());
		}
		//ServiceItem 
		ServiceItem si = null;
		if(interfacez.isAnnotationPresent(Service.class)) {
			si = this.getServiceItems(interfacez,ns,ver);
			if(StringUtils.isNotEmpty(impl)) {
				si.setImpl(impl);
			}
		} else {
			
			Set<Class<?>> clses = new HashSet<>();
			clses.add(interfacez);
			this.needRegist(clses);
			
			si = this.createSrvItem(interfacez.getName(),ns, ver, impl,clientId);
			for(Method m : interfacez.getMethods()) {
				createSrvMethod(si,m.getName(),m.getParameterTypes());
			}
		}
		
		if(si.getCode() <= 0) {
			si.setCode(idGenerator.getIntId(ServiceItem.class));
		}
		
		return si;
	}
	
	public ServiceItem createSrvItem(String srvName,String ns,String ver, String impl,int clientId) {
		ServiceItem item = new ServiceItem();
		UniqueServiceKey usk = new UniqueServiceKey();
		if(Utils.isEmpty(ns)) {
			usk.setNamespace(Config.getNamespace());
		}else {
			usk.setNamespace(ns);
		}
		
		usk.setServiceName(srvName);
		usk.setVersion(ver);
		usk.setInstanceName(Config.getInstanceName());
		usk.setHost(Config.getExportSocketHost());
		usk.setPort(this.nettyServer.getPort());
		
		usk.setSnvHash(HashUtils.FNVHash1(usk.toKey(false, false, false)));
		
		item.setKey(usk);
		item.setImpl(impl);
		
		item.getServers().add(this.nettyServer);
		item.getServers().add(this.httpServer);
		item.setExternal(false);
		item.setShowFront(true);
		
		item.setClientId(clientId);
		item.setCreatedBy(Config.getClientId());
		
		//item.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade()!=100 || intAnno == null ?anno.maxFailBeforeDegrade():intAnno.maxFailBeforeDegrade());
		//item.setRetryCnt();
		//item.setRetryInterval();
		//item.setTestingArgs(getFieldValue(anno.testingArgs(),intAnno == null ? null : intAnno.testingArgs(),""));
//		item.setTimeout();
//		item.setMaxSpeed();
//		item.setBaseTimeUnit();
//		item.setTimeWindow();
//		item.setSlotSize();
//		item.setCheckInterval();
//		
//		item.setAvgResponseTime();
//		item.setMonitorEnable();
//		item.setLoggable();
//		item.setDebugMode();
//		
//		item.setHandler();
		
		return item;
	}
	
	public ServiceMethod createSrvMethod(ServiceItem item,String methodName, Class[] args) {

		ServiceMethod sm = new ServiceMethod();
		sm.setBreaking(false);
		
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
		sm.setLogLevel(MC.LOG_ERROR);
		sm.setDebugMode(-1);
		sm.setMaxSpeed(item.getMaxSpeed());
		
		sm.getKey().setUsk(item.getKey());
		sm.getKey().setMethod(methodName);
		if(args == null || args.length == 0) {
			sm.getKey().setParamsStr("");
		}else {
			sm.getKey().setParamsStr(UniqueServiceMethodKey.paramsStr(args));
		}
		
		sm.getKey().setSnvHash(HashUtils.FNVHash1(sm.getKey().toKey(false, false, false)));
		
		ServiceMethod conflictMethod = srvMng.checkConflictServiceMethodByHash(sm.getKey().getSnvHash(),
				sm.getKey().toKey(false, false, false));
		if(conflictMethod != null) {
			String msg = "Service method hash conflict: [" + sm.getKey().toKey(false, false, false) + 
					"] with exist sm [" + conflictMethod.getKey().toKey(false, false, false)+"] fail to load service!";
			throw new CommonException(msg);
		}
		
		item.addMethod(sm);
		
		Set<Class<?>> clses = new HashSet<>();
		
		for(Class<?> acls : args) {
			this.getClassByType(acls, clses);
		}
		
		this.needRegist(clses);
		
		return sm;
	
	}

	private ServiceItem getServiceItems(Class<?> proxySrv,String ns,String version) {
		Class<?> srvCls = ProxyObject.getTargetCls(proxySrv);
		if(!srvCls.isAnnotationPresent(Service.class)){
			throw new CommonException("Not a service class ["+srvCls.getName()+"] annotated with ["+Service.class.getName()+"]");
		}
		
		//Object srv = ProxyObject.getTarget(proxySrv);
		Service anno = srvCls.getAnnotation(Service.class);
		Class<?> interfacez = anno.infs();
		if(interfacez == null || interfacez == Void.class){
			if(proxySrv.isInterface()) {
				interfacez = srvCls;
			} else {
				Class<?>[] ints = srvCls.getInterfaces();
				if(ints == null || ints.length != 1) {
					throw new CommonException("service ["+srvCls.getName()+"] have to implement one and only one interface.");
				}
				interfacez = ints[0];
			}
		}
		
		Service intAnno = null;
		
		if(interfacez != null && interfacez.isAnnotationPresent(Service.class)){
			intAnno = interfacez.getAnnotation(Service.class);
		}
		
		ServiceItem item = new ServiceItem();
		
		//Netty Socket 作为必选端口开放
		item.getServers().add(this.nettyServer);
		item.getServers().add(this.httpServer);
		
		UniqueServiceKey usk = new UniqueServiceKey();
		usk.setPort(this.nettyServer.getPort());
		//服务名称肯定是接口全限定类名称
		usk.setServiceName(interfacez.getName());
		usk.setInstanceName(Config.getInstanceName());
		usk.setHost(Config.getExportSocketHost());
		
		if(Utils.isEmpty(ns)) {
			usk.setNamespace(Config.getNamespace());
		}else if(Config.isOwnerRes()) {
			usk.setNamespace(ns);
		} else {
			usk.setNamespace(ns + "." + Config.getAccountName());
		}
		
		if(StringUtils.isNotEmpty(version)) {
			usk.setVersion(UniqueServiceKey.version(version));
		}else {
			usk.setVersion(getFieldValue(anno.version(),intAnno == null ? null : intAnno.version(),Constants.VERSION));
		}
		
		usk.setSnvHash(HashUtils.FNVHash1(usk.toKey(false, false, false)));
		
		item.setKey(usk);
		item.setImpl(proxySrv.getName());
		item.setActName(Config.getAccountName());
		item.setExternal(anno.external());
		item.setShowFront(anno.showFront());
		
		item.setCreatedBy(Config.getClientId());
		
		if(anno.clientId() == Constants.USE_SYSTEM_CLIENT_ID) {
			item.setClientId(Config.getClientId());
		} else {
			item.setClientId(anno.clientId());
		}
		
		//item.setMaxFailBeforeDegrade(anno.maxFailBeforeDegrade()!=100 || intAnno == null ?anno.maxFailBeforeDegrade():intAnno.maxFailBeforeDegrade());
		item.setRetryCnt(anno.retryCnt()!=3 || intAnno == null ?anno.retryCnt():intAnno.retryCnt());
		item.setRetryInterval(anno.retryInterval()!=500 || intAnno == null ?anno.retryInterval():intAnno.retryInterval());
		//item.setTestingArgs(getFieldValue(anno.testingArgs(),intAnno == null ? null : intAnno.testingArgs(),""));
		item.setTimeout(anno.timeout()!=2000 || intAnno == null ? anno.timeout() : intAnno.timeout());
		item.setMaxSpeed(anno.maxSpeed() > 0 || intAnno == null ? anno.maxSpeed() : intAnno.maxSpeed());
		item.setBaseTimeUnit( StringUtils.isEmpty(anno.baseTimeUnit()) || intAnno == null ? anno.baseTimeUnit() : intAnno.baseTimeUnit());
		item.setTimeWindow(anno.timeWindow() <= 0 || intAnno == null ?anno.timeWindow():intAnno.timeWindow());
		item.setSlotSize(anno.slotInterval() <= 0 || intAnno == null ?anno.slotInterval():intAnno.slotInterval());
		item.setCheckInterval(anno.checkInterval() <= 0 || intAnno == null ?anno.checkInterval():intAnno.checkInterval());
		
		item.setAvgResponseTime(anno.avgResponseTime()!=-1 || intAnno == null ? anno.avgResponseTime() : intAnno.avgResponseTime());
		item.setMonitorEnable(anno.monitorEnable()!=-1 || intAnno == null ? anno.monitorEnable() : intAnno.monitorEnable());
		item.setLogLevel(anno.logLevel()!=-1 || intAnno == null ? anno.logLevel() : intAnno.logLevel());
		item.setDebugMode(anno.debugMode()!=-1 || intAnno == null ? anno.debugMode() : intAnno.debugMode());
		
		if(anno.limit2Packages() != null && anno.limit2Packages().length > 0) {
			item.getLimit2Packages().addAll(Arrays.asList(anno.limit2Packages()));
		}
		
		if(intAnno != null && intAnno.limit2Packages() != null && intAnno.limit2Packages().length > 0) {
			item.getLimit2Packages().addAll(Arrays.asList(intAnno.limit2Packages()));
		}
		
		item.setHandler(anno.handler() != null && !anno.handler().trim().equals("") ? anno.handler():(intAnno != null?intAnno.handler():null));
		
		//测试方法
		ServiceMethod checkMethod = new ServiceMethod();
		
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
		checkMethod.getKey().setParamsStr(UniqueServiceMethodKey.paramsStr(new Class[] {String.class}));
		checkMethod.setLogLevel(MC.LOG_ERROR);;
		checkMethod.setDebugMode(0);
		checkMethod.getKey().setSnvHash(HashUtils.FNVHash1(checkMethod.getKey().toKey(false, false, false)));
		
		item.addMethod(checkMethod);
		
		for(Method m : interfacez.getMethods()) {
			ServiceMethod sm = new ServiceMethod();
			Method srvMethod = null;
			try {
				srvMethod = srvCls.getMethod(m.getName(), m.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				throw new CommonException("Service not found: "+m.getName(),e);
			}
			
			//具体实现类的注解优先,如果实现类对就方法没有注解,则使用接口对应的方法注解
			//如果接口和实现类都没有,则使用实现类的Service注解，实现类肯定有Service注解，否则不会成为服务
			
			SMethod manno = srvMethod.getAnnotation(SMethod.class);
			SMethod intMAnno = m.getAnnotation(SMethod.class);
			
			//订阅信息
			Subscribe mSub = srvMethod.getAnnotation(Subscribe.class);
			Subscribe intSub = m.getAnnotation(Subscribe.class);
			
			if(mSub != null) {
				 sm.setTopic(mSub.topic());
			 }else if(intSub != null) {
				 sm.setTopic(intSub.topic());
			 }else {
				 sm.setTopic(null);
			 }
			
			sm.setBreaking(false);
			
			SBreakingRule sbr = null;
					
			if(manno == null && intMAnno== null) {
				//sm.setMaxFailBeforeDegrade(item.getMaxFailBeforeDegrade());
				sm.setRetryCnt(item.getRetryCnt());
				sm.setRetryInterval(item.getRetryInterval());
				//sm.setTestingArgs(item.getTestingArgs());
				sm.setTimeout(item.getTimeout());
				if(item.getTimeout() <= 0) {
					throw new CommonException("Invalid timeout val with 0 for " + item.getImpl());
				}
				sm.setMaxSpeed(item.getMaxSpeed());
				sm.setBaseTimeUnit(item.getBaseTimeUnit());
				sm.setTimeWindow(item.getTimeWindow());
				sm.setAvgResponseTime(item.getAvgResponseTime());
				sm.setMonitorEnable(item.getMonitorEnable());
				sm.setFailResponse("");
				sm.setLogLevel(MC.LOG_ERROR);
				sm.setDebugMode(-1);
				sm.setMaxSpeed(item.getMaxSpeed());
				sm.setPerType(false);
				sm.setNeedLogin(false);
				sm.setMaxPacketSize(0);
				sm.setUpSsl(false);
				sm.setDownSsl(false);
				sm.setTxType(TxConstants.TYPE_TX_NO);
			} else {
				 if(manno != null ) {
					 //实现类方法配置具有高优先级
					sbr = manno.breakingRule();
					sm.setRetryCnt(manno.retryCnt()!=3 || intMAnno == null ?manno.retryCnt():intMAnno.retryCnt());
					sm.setRetryInterval(manno.retryInterval()!=500 || intMAnno == null ? manno.retryInterval():intMAnno.retryInterval());
					sm.setTestingArgs(getFieldValue(manno.testingArgs(),intMAnno == null ? null : intMAnno.testingArgs(),""));
					sm.setTimeout(manno.timeout()!=2000 || intMAnno == null ?manno.timeout():intMAnno.timeout());
					sm.setMaxSpeed(manno.maxSpeed() > 0 || intMAnno == null ? manno.maxSpeed():intMAnno.maxSpeed());
					if(sm.getMaxSpeed() <=0) {
						sm.setMaxSpeed(item.getMaxSpeed());
					}
					sm.setAvgResponseTime(manno.avgResponseTime()!=-1 || intMAnno == null ? manno.avgResponseTime() : intMAnno.avgResponseTime());
					sm.setMonitorEnable(manno.monitorEnable()!=-1 || intMAnno == null ? manno.monitorEnable() : intMAnno.monitorEnable());
					//sm.setStream(manno.stream());
					sm.setDumpDownStream(manno.dumpDownStream());
					sm.setDumpUpStream(manno.dumpUpStream());
					sm.setNeedResponse(manno.needResponse());
					sm.setFailResponse(manno.failResponse());
					sm.setTimeWindow(manno.timeWindow()<=0?item.getTimeWindow():manno.timeWindow());
					sm.setSlotInterval(manno.slotInterval()<=0?item.getSlotSize():manno.slotInterval());
					
					sm.setCheckInterval(manno.checkInterval()<=0?item.getCheckInterval():manno.checkInterval());
					
					sm.setBaseTimeUnit(StringUtils.isEmpty(manno.baseTimeUnit())? item.getBaseTimeUnit():manno.baseTimeUnit());
					
					sm.setLogLevel(manno.logLevel()!=-1 || intMAnno == null ? manno.logLevel() : intMAnno.logLevel());
					sm.setDebugMode(manno.debugMode()!=-1 || intMAnno == null ? manno.debugMode() : intMAnno.debugMode());
					
					sm.setAsyncable(manno.asyncable());
					sm.setPerType(manno.perType());
					sm.setNeedLogin(manno.needLogin() || manno.perType());
					sm.setMaxPacketSize(manno.maxPacketSize());
					sm.setUpSsl(manno.upSsl());
					sm.setDownSsl(manno.downSsl());
					sm.setEncType(manno.encType());
					sm.setLimitType(manno.limitType());
					sm.setForType(manno.forType());
					sm.setTxType(manno.txType());
				 } else {
					 //使用接口方法配置
					sbr = intMAnno.breakingRule();
					sm.setRetryCnt(intMAnno.retryCnt());
					sm.setRetryInterval(intMAnno.retryInterval());
					sm.setTestingArgs(intMAnno.testingArgs());
					sm.setTimeout(intMAnno.timeout());
					sm.setMaxSpeed(intMAnno.maxSpeed());
					if(sm.getMaxSpeed() <=0) {
						sm.setMaxSpeed(item.getMaxSpeed());
					}
					
					sm.setBaseTimeUnit(intMAnno.baseTimeUnit());
					sm.setAvgResponseTime(intMAnno.avgResponseTime());
					sm.setMonitorEnable(intMAnno.monitorEnable());
					//sm.setStream(intMAnno.stream());
					sm.setDumpDownStream(intMAnno.dumpDownStream());
					sm.setDumpUpStream(intMAnno.dumpUpStream());
					sm.setNeedResponse(intMAnno.needResponse());
					sm.setFailResponse(intMAnno.failResponse());
					sm.setLogLevel(intMAnno.logLevel());
					sm.setDebugMode(intMAnno.debugMode());
					sm.setTimeWindow(intMAnno.timeWindow()<=0?item.getTimeWindow():intMAnno.timeWindow());
					sm.setSlotInterval(intMAnno.slotInterval()<=0?item.getSlotSize():intMAnno.slotInterval());
					sm.setCheckInterval(intMAnno.checkInterval()<=0?item.getCheckInterval():intMAnno.checkInterval());
					sm.setBaseTimeUnit(StringUtils.isEmpty(intMAnno.baseTimeUnit())? item.getBaseTimeUnit():intMAnno.baseTimeUnit());
					
					sm.setAsyncable(intMAnno.asyncable());
					sm.setPerType(intMAnno.perType());
					sm.setNeedLogin(intMAnno.needLogin()  || intMAnno.perType());
					sm.setMaxPacketSize(intMAnno.maxPacketSize());
					
					sm.setUpSsl(intMAnno.upSsl());
					sm.setDownSsl(intMAnno.downSsl());
					sm.setEncType(intMAnno.encType());
					
					sm.setLimitType(intMAnno.limitType());
					sm.setForType(intMAnno.forType());
					sm.setTxType(intMAnno.txType());
				 }
				 
				 if(sm.getTimeout() <= 0) {
					throw new CommonException("Invalid timeout val with 0 for  " +item.getImpl() + "."+ m.getName());
				 }
				 
				sm.getBreakingRule().setBreakTimeInterval(sbr.breakTimeInterval());
				sm.getBreakingRule().setEnable(sbr.enable());
				sm.getBreakingRule().setPercent(sbr.percent());
				sm.getBreakingRule().setCheckInterval(sbr.checkInterval());
					
			} 
			
			sm.getKey().setUsk(usk);
			sm.getKey().setMethod(m.getName());
			/*if(m.getName().equals("updateActPermissions")) {
				logger.debug("debug");
			}*/
			
			Type[] types = m.getGenericParameterTypes();
			
			sm.getKey().setParamsStr(UniqueServiceMethodKey.paramsStr(m.getParameterTypes()));
			sm.getKey().setReturnParam(ReflectUtils.getDesc(m.getReturnType()));
			
			sm.getKey().setSnvHash(HashUtils.FNVHash1(sm.getKey().toKey(false, false, false)));
			
			Set<Class<?>> clses = new HashSet<>();
			
			//Type[] types = m.getGenericParameterTypes();
			 Class<?>[]  pts = m.getParameterTypes();
			 
			for(int i = 0; i < types.length; i++) {
				//needRegist(pts[i]/*,types[i]*/);
				getClassByType(pts[i],clses);
				getClassByType(types[i],clses);
			}
			
			getClassByType(m.getReturnType(),clses);
			getClassByType(m.getGenericReturnType(),clses);
			
			clses.add(interfacez);
			needRegist(clses);
			
			if(sm.isAsyncable()) {
				//允许异步调用的RPC必须以方法全限定名为主题
				sm.setTopic(sm.getKey().toKey(false, false, false));
			}
			
			item.addMethod(sm);
			
			/*if(sm.getBreakingRule().isEnable()) {
				sm.setMonitorEnable(1);
				createStatisConfig(sm);
			}*/
		}
		
		return item;
	}
	
	private void getClassByType(Type type,Set<Class<?>> clses) {
		TypeUtils.finalParameterType(type, clses);
	}
	
	private void needRegist(Set<Class<?>>  clses) {
		
		/*if(type.getName().equals("cn.jmicro.api.Resp")) {
			logger.debug("");
		}*/
		
		/*Set<Class<?>> clses = new HashSet<>();
		
		TypeUtils.finalParameterType(type, clses);
		
		if(cls.isArray()) {
			TypeUtils.finalParameterType(cls.getComponentType(), clses);
			clses.add(cls.getComponentType());
		} else {
			clses.add(cls);
		}
		*/
		
		for(Class<?> c : clses) {
			if(c == null || void.class == c) {
				continue;
			}
			
			if(c.isPrimitive()||
					c.getName().startsWith("java") || 
					c.getName().startsWith("sun") ||
					c.getName().startsWith("com.sun") ||
					c.getClassLoader() == null || 
					c.getClassLoader().getClass().getName().startsWith("sun.misc.Launcher$ExtClassLoader")) {
				continue;
			}
			
			logger.debug(c.getName());
			cl.addClassInstance(c);
			
			/*if(cls.isArray()) {
				needRegist(cls.getComponentType(),cls.getComponentType().getGenericSuperclass());
			} else {
				
			}*/
		}
		
		/*if(Collection.class.isAssignableFrom(cls)) {
			ParameterizedType gt = TypeCoder.genericType(genericType);
			Class<?> eltType = TypeUtils.finalParameterType(gt, 0);
			needRegist(eltType,eltType.getGenericSuperclass());
		} else if(Map.class.isAssignableFrom(cls)) {
			ParameterizedType gt = TypeCoder.genericType(cls.getGenericSuperclass());
			Class<?> keyType = TypeUtils.finalParameterType(gt, 0);
			Class<?> valueType = TypeUtils.finalParameterType(gt, 1);
			needRegist(keyType,null);
			needRegist(valueType,null);
		}else if(Resp.class == cls) {
			ParameterizedType gt = TypeCoder.genericType(genericType);
			Class<?> eltType = TypeUtils.finalParameterType(gt, 0);
			if(eltType != null) {
				needRegist(eltType,eltType.getGenericSuperclass());
			}
		} */
		
		
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
	
	public void setRegistry(IRegistry registry) {
		this.registry = registry;
	}
	
}
