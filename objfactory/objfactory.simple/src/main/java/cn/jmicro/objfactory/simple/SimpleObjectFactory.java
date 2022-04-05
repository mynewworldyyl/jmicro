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
package cn.jmicro.objfactory.simple;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.EnterMain;
import cn.jmicro.api.Holder;
import cn.jmicro.api.IListener;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.PostListener;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.classloader.RpcClassLoaderHelper;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.masterelection.VoterPerson;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.api.objectfactory.IPostInitListener;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.objectsource.IObjectSource;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.IAccountServiceJMSrv;
import cn.jmicro.api.security.genclient.IAccountServiceJMSrv$JMAsyncClient;
import cn.jmicro.api.service.IServerServiceProxy;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.SystemUtils;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.JmicroClassPool;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import javassist.CtClass;
import javassist.CtMethod;

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
public class SimpleObjectFactory implements IObjectFactory {

	private static final byte INIT = 1;
	
	private static final byte AFTER_SORT = 2;
	
	private static final byte INIT_FINISH = 127;
	
	private static AtomicInteger idgenerator = new AtomicInteger();
	
	private final static Logger logger = LoggerFactory.getLogger(SimpleObjectFactory.class);
	
	private static AtomicInteger isInit = new AtomicInteger(0);
	
	//代表当前JMicro进程实例
	private ProcessInfoJRso pi = null;
	
	private boolean fromLocal = true;
	
	private byte stage = INIT;
	
	private List<IPostFactoryListener> postReadyListeners = new ArrayList<>();
	
	private List<IPostInitListener> postListeners = new ArrayList<>();
	
	private Map<Class<?>,Object> objs = new ConcurrentHashMap<Class<?>,Object>();
	
	private Map<String,Object> nameToObjs = new ConcurrentHashMap<String,Object>();
	
	private Map<String,Object> clsNameToObjs = new ConcurrentHashMap<String,Object>();
	
	private ClientServiceProxyManager clientServiceProxyManager = null;
	
	//private HttpHandlerManager httpHandlerManager = new HttpHandlerManager(this);
	
	private RpcClassLoader rpcClassLoader = null;
	
	private	Set<IObjectSource> osSet = new HashSet<>();
	
	@Override
	public void foreach(Consumer<Object> c) {
		for(Object obj : objs.values()) {
			c.accept(obj);
		}
	}

	@Override
	public Boolean isSysLogin() {
		return pi!= null && pi.isLogin();
	}
	
	@Override
	public Boolean isRpcReady() {
		return this.stage == INIT_FINISH;
	}
	
	@Override
	public ProcessInfoJRso getProcessInfo() {
		return this.pi;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteServie(String srvName, String namespace, String version,AsyncConfigJRso[] acs) {
		return (T)this.clientServiceProxyManager.getRefRemoteService(this.getClass().getName(),srvName,namespace,version,rpcClassLoader,acs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteServie(ServiceItemJRso item,AsyncConfigJRso[] acs) {
		return (T)this.clientServiceProxyManager.getRefRemoteService(item,this.rpcClassLoader,acs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteServie(Class<T> srvCls,String ns, AsyncConfigJRso[] acs) {
		return (T)this.clientServiceProxyManager.getRefRemoteService(srvCls,ns,acs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> cls) {
		return get(cls,false);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> cls,boolean create) {
		checkStatu();
		Object obj = null;
		if(cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
			Set<T> l = this.getByParent(cls);
			if(l.size() == 1) {
				obj =  l.iterator().next();
			}else if(l.size() > 1) {
				throw new CommonException("More than one instance of class ["+cls.getName()+"].");
			}
			if(obj == null && cls.isAnnotationPresent(Service.class)){
				obj = this.clientServiceProxyManager.getRefRemoteService(cls,Config.getNamespace(),null);
			}
		} else {
			obj = objs.get(cls);
			if(obj == null && create){
				logger.warn("Create ins: {} by get method:",cls.getName());
				obj = this.createObject(cls,true);
				if(obj != null) {
					cacheObj(cls,obj,null);
				}
			}
		}
		
		if(cls.getName().equals("cn.jmicro.shop.db.service.UserService")) {
			logger.info(cls.getName());
		}
		
		if(obj != null) {
			Class<?> tc = ProxyObject.getTargetCls(obj.getClass());
			if(this.validForPackage(getSecurityPackageName(), tc)) {
				return (T)obj;
			}
			logger.warn(getSecurityPackageName() + " cannot get instance of " + tc.getName());
		}
		
		if(!this.osSet.isEmpty() && !cls.getName().startsWith("cn.jmicro.api")) {
			for(IObjectSource os: osSet) {
				Object co = os.get(cls);
				if(co != null) return (T)co;//外部数据源直接返回
			}
		}
		
		return null;
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getByName(String clsName) {
		checkStatu();
		Object co = this.clsNameToObjs.get(clsName);
		if(co == null){
			co = this.nameToObjs.get(clsName);
		}
		
		if(co == null) {
			Class<?> cls = this.loadCls(clsName);
			if(cls != null){
				co = get(cls);
			}
		}
		
		if(co != null) {
			Class<?> tc = ProxyObject.getTargetCls(co.getClass());
			if(this.validForPackage(getSecurityPackageName(), tc)) {
				return (T)co;
			}
			logger.warn(getSecurityPackageName() + " cannot get instance of " + tc.getName());
		}
		
		if(!this.osSet.isEmpty() && clsName.startsWith(Constants.SYSTEM_PCK_NAME_PREFIXE)) {
			for(IObjectSource os: osSet) {
				co = os.getByName(clsName);
				if(co != null) return (T)co;
			}
		}
		
		return null;
	}
	
	private String getSecurityPackageName() {
		StackTraceElement se = Thread.currentThread().getStackTrace()[3];
		return se.getClassName();
	}

	@Override
	public <T> Set<T> getByParent(Class<T> parrentCls) {
		Set<T> set = new HashSet<>();
		Set<Class<?>> clazzes = ClassScannerUtils.getIns().loadClassByClass(parrentCls);
		for(Class<?> c: clazzes) {
			if(parrentCls.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())
					&& !Modifier.isInterface(c.getModifiers()) && Modifier.isPublic(c.getModifiers())
					&& c.isAnnotationPresent(Component.class)) {
				Component anno = c.getAnnotation(Component.class);
				if(anno != null && anno.active()) {
					//logger.info(c.getName());
					/*if(c.getSimpleName().equals("AuthIntercepter")) {
						logger.info(c.getName());
					}*/
					Object obj = this.get(c);
					/*if(obj != null) {
						set.add((T)obj);
					}*/
				}
			}
		}
		
		for(Class<?> c : this.objs.keySet()) {
			if(parrentCls.isAssignableFrom(c)) {
				Class<?> tc = ProxyObject.getTargetCls(c);
				if(this.validForPackage(getSecurityPackageName(), tc)) {
					set.add((T)this.objs.get(c));
				}
			}
		}
		
		if(!osSet.isEmpty() && !parrentCls.getName().startsWith(Constants.SYSTEM_PCK_NAME_PREFIXE)) {
			for(IObjectSource os : osSet) {
				Set<T> rst = os.getByParent(parrentCls);
				if(rst != null && !rst.isEmpty()) {
					set.addAll(rst);
				}
			}
		}
		
		/*Object obj = this.objs.get(parrentCls);
		if(obj != null){
			set.add((T)obj);
		}*/
		return set;
	}
	
	private void checkStatu(){
		if(isInit.get() == 1 && fromLocal) {
			return;
		}
		if(isInit.get() < 2){
			throw new CommonException("Object Factory not init finish");
		}
	}

	public Object createNoProxy(Class<?> cls) {
		checkStatu();
		Object obj = objs.get(cls);
		if(obj != null && !(obj instanceof ProxyObject)){
			return  obj;
		}
		try {
			obj = cls.newInstance();
			doAfterCreate(obj,null);
			//will replace the proxy object if exist, this is no impact to client
			cacheObj(cls,obj,null);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException("Fail to create obj for ["+cls.getName()+"]",e);
		}
		return obj;
	}
	
	private Object cacheObj(Class<?> cls,Object obj,String componentName){
		boolean success = false;
		cls = ProxyObject.getTargetCls(cls);
		if(!objs.containsKey(cls)){
			objs.put(cls, obj);
			success = true;
		/*	throw new CommonException("class["+cls.getName()+"] instance exist"
					+ this.objs.get(cls).getClass().getName());*/
		} else {
			success = objs.get(cls) == obj;
		}
		
		if(!StringUtils.isEmpty(componentName) && !this.nameToObjs.containsKey(componentName)) {
			this.nameToObjs.put(componentName, obj);
			success = true;
			/*throw new CommonException("componentName["+componentName+"] exist: " 
					+ this.nameToObjs.get(componentName).getClass().getName());*/
		} else {
			String comName = this.getComName(cls);
			if(!StringUtils.isEmpty(comName)){
				this.nameToObjs.put(comName, obj);
				success = true;
			}
		}
		
		//objs.put(cls, obj);
		if(!clsNameToObjs.containsKey(cls.getName())){
			this.clsNameToObjs.put(cls.getName(), obj);
			success = true;
		}
		
		if(success && obj instanceof IObjectSource) {
			this.osSet.add((IObjectSource)obj);
		}
		
		if(!success) {
			logger.warn("class["+cls.getName()+"] instance exist");
			//throw new CommonException("class["+cls.getName()+"] instance exist");
		}
		return objs.get(cls);
	}
	
	private boolean canCreate(Class<?> cls) {
		return !IObjectFactory.class.isAssignableFrom(cls) && cls.isAnnotationPresent(Component.class);
	}
	
	private <T> T createObject(Class<T> cls,boolean doAfterCreate) {
		if(!canCreate(cls)) {
			return null;
		}
		Object obj = null;
		try {
			if(!isLazy(cls)) {
				obj = cls.newInstance();
				objs.put(cls, obj);//确保不重复创建同一对象
				if(doAfterCreate){
					 doAfterCreate(obj,null);
				}
			} else {
				//创建目标对象的代理对像,直到客户端调用其中方法时才真正创建目标对象
				obj = createLazyProxyObject(cls);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException("Fail to create obj for ["+cls.getName()+"]",e);
		}
		return  (T)obj;
	}
	
	private void doAfterCreate(Object obj,Config cfg) {
    	 if(cfg == null){
    		  cfg = (Config)objs.get(Config.class);
    	 }
    	 if(cfg == null){
    		 throw new CommonException("Config not load!");
    	 }
    	 
    	 if(!(obj instanceof ProxyObject)){
    		 injectDepependencies(obj);
    		 notifyPreInitPostListener(obj,cfg);
        	 doInit(obj);
        	 doReady(obj);
    		 notifyAfterInitPostListener(obj,cfg);
    	 }
	}
     
     private void notifyAfterInitPostListener(Object obj,Config cfg) {
 		if(this.postListeners.isEmpty()) {
 			return;
 		}
 		for(IPostInitListener l : this.postListeners){
 			l.afterInit(obj,cfg);
 		}	
 	}
     
	private void notifyPreInitPostListener(Object obj,Config cfg) {
		if(this.postListeners.isEmpty()) {
			return;
		}
		for(IPostInitListener l : this.postListeners){
			l.preInit(obj,cfg);
		}	
	}

	private boolean isLazy(Class<?> cls) {
		if(cls.isAnnotationPresent(Component.class)){
			Component lazy = cls.getAnnotation(Component.class);
			return lazy.lazy();
		}
		return true;
	}

	@Override
	public void masterSlaveListen(IMasterChangeListener l) {
		Config cfg = this.get(Config.class);
		boolean isMasterSlaveModel = !Utils.isEmpty(cfg.getString(Constants.MASTER_SLAVE_TAG, null));
		VoterPerson lp = this.get(VoterPerson.class);
		if(lp == null || !isMasterSlaveModel) {
			l.masterChange(IMasterChangeListener.MASTER_NOTSUPPORT, true);
		} else {
			lp.addListener(l);
		}
	}

	public synchronized void start(IDataOperator dataOperator,String[] args){
		if(!isInit.compareAndSet(0, 1)){
			//防止多线程同时进来多次实例化相同实例
			if(isInit.get() == 1) {
				//前面线程正在做初始化，等待其初始化完成后直接返回即可
				synchronized(isInit) {
					try {
						isInit.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			return;
		}
		
		final RpcClassLoaderHelper clHelper = new RpcClassLoaderHelper();
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		if(cl != null && cl instanceof RpcClassLoader) {
			rpcClassLoader = (RpcClassLoader) cl;
		} else {
			if(cl != null) {
				rpcClassLoader = new RpcClassLoader(cl);
			} else {
				rpcClassLoader = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
			}
		}
		
		if(dataOperator == null) {
			throw new CommonException("Data operator cannot be NULL");
		}
		
		clHelper.setClassLoader(this.rpcClassLoader);
		rpcClassLoader.setHelper(clHelper);
		rpcClassLoader.addBasePackages(Config.getBasePackages());
		
		//ClassScannerUtils.setClassLoader(clHelper);
		
		//logger.debug(RpcClassLoaderHelper.class.getClassLoader().toString());
		//logger.debug(ClassScannerUtils.class.getClassLoader().toString());
		IObjectSource so = IObjectSource.getObjectSource();
		if(so != null) {
			this.cacheObj(IObjectSource.class, so,"sysObjectSource");
		}
		
		this.cacheObj(RpcClassLoaderHelper.class, clHelper,"rpcClHelper");
		this.cacheObj(RpcClassLoader.class, rpcClassLoader,"rpcClassLoader");
		
		/*rpcClassLoader = new RpcClassLoader(this.getClass().getClassLoader());
		if(!clsNameToObjs.containsKey(RpcClassLoader.class.getName())) {
		}*/
		
		Thread.currentThread().setContextClassLoader(rpcClassLoader);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				 logger.warn("进程终止： "+JsonUtils.getIns().toJson(pi));
				 String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
				 dataOperator.deleteNode(p);
			}
		});
		
		this.cacheObj(dataOperator.getClass(), dataOperator,null);
		Config cfg = (Config)this.createOneComponent(Config.class,Config.isClientOnly(),Config.isServerOnly());
		
		//初始化配置目录
		//cfg.setDataOperator(dataOperator);
		//IConfigLoader具体的配置加载类
		
		/*
		Set<Class<?>> configLoaderCls = ClassScannerUtils.getIns().loadClassByClass(IConfigLoader.class);
		for(Class<?> c : configLoaderCls) {
			this.createOneComponent(c, Config.isClientOnly(), Config.isServerOnly());
		}
		Set<IConfigLoader> configLoaders = this.getByParent(IConfigLoader.class);
		//加载配置，并调用init0方法做初始化
		cfg.loadConfig(configLoaders);
		configLoaderCls.add(Config.class);
		*/
		
		cfg.loadConfig(dataOperator);
		
		createProccessInfo(dataOperator,cfg);
		
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		
		/**
		 * 意味着此两个参数只能在命令行或环境变量中做配置，不能在ZK中配置，因为此时ZK还没启动，此配置正是ZK的启动配置
		 * 后其可以使用其他实现，如ETCD等
		 */
		//String dataOperatorName = Config.getCommandParam(Constants.DATA_OPERATOR, String.class, Constants.DEFAULT_DATA_OPERATOR);
		
		
		Runnable r = ()-> {
			
			//查找全部对像初始化监听器
			createPostListener();
			//registerSOClass();
			Set<Class<?>> clses = ClassScannerUtils.getIns().getComponentClass();
			//clses.removeAll(configLoaderCls);
			
			Set<Object> systemObjs = new HashSet<>();
			createComponentOrService(dataOperator,systemObjs,clses,cfg);
			
			//IDataOperator注册其内部实例到ObjectFactory
			dataOperator.objectFactoryStarted(this);
			
			clientServiceProxyManager = new ClientServiceProxyManager(this);
			clientServiceProxyManager.init();
			
			//取得全部工厂监听器
			Set<IPostFactoryListener> postL = this.getByParent(IPostFactoryListener.class);
			postReadyListeners.addAll(postL);
			postReadyListeners.sort(new Comparator<IPostFactoryListener>(){
				@Override
				public int compare(IPostFactoryListener o1, IPostFactoryListener o2) {
					return o1.runLevel() > o2.runLevel()?1:o1.runLevel() == o2.runLevel()?0:-1;
				}
			});
			
			//对像工厂初始化前监听器
			for(IPostFactoryListener lis : this.postReadyListeners){
				//在这里可以注册实例
				lis.preInit(this);
			}
			
			List<Object> lobjs = new ArrayList<>();
			lobjs.addAll(this.objs.values());
			
			//根据对像定义level级别排序，level值越小，初始化及别越高，就也就越优先初始化
			lobjs.sort(new Comparator<Object>(){
				@Override
				public int compare(Object o1, Object o2) {
					Component c1 = ProxyObject.getTargetCls(o1.getClass()).getAnnotation(Component.class);
					Component c2 = ProxyObject.getTargetCls(o2.getClass()).getAnnotation(Component.class);
					int l1 = 0;
					int l2 = 0;
					
					if(c1 == null) {
						if(o1.getClass() == RpcClassLoader.class) {
							l1 = 10;
						}else {
							l1 = 0;
						}
					}else {
						l1 = c1.level();
					}
					
					if(c2 == null) {
						if(o2.getClass() == RpcClassLoader.class) {
							l2 = 10;
						}else {
							l2 = 0;
						}
					} else {
						l2 = c2.level();
					}
					
					return l1 > l2 ? 1: l1 == l2 ? 0 : -1;
					
				}
			});
			
			//将自己也保存到实例列表里面
			this.cacheObj(this.getClass(), this, null);
			
			IRegistry registry = (IRegistry)this.get(IRegistry.class);
			
			notifyPreInitPostListener(registry,cfg);
			
			stage = AFTER_SORT;
			
			if(!lobjs.isEmpty()){
				
				//组件开始初始化,在此注入Cfg配置，enable字段也在此注入
				preInitPostListener0(lobjs,cfg,systemObjs);
				
				for(Iterator<Object> ite = lobjs.iterator(); ite.hasNext(); ){
					Object o = ite.next();
					if(!this.isEnable(o)) {
						//删除enable=false的组合
						logger.info("disable component: "+o.getClass().getName());
						ite.remove();
					}
				}
				
				//依赖注入
				injectDepependencies0(lobjs,cfg,systemObjs);
				
				//调用各组件的init方法
				doInit0(lobjs,cfg,systemObjs);
				
				//注入服务引用
				processReference0(lobjs,cfg,systemObjs);
				
				LG.initLog(SimpleObjectFactory.this);
				
				//组件初始化完成
				notifyAfterInitPostListener0(lobjs,cfg,systemObjs);
				
				doReady0(lobjs,systemObjs);
			}
			
			loadAccountInfo(dataOperator,cfg);
			
			ServiceLoader sl = this.get(ServiceLoader.class, false);
			sl.jready0();
			
			stage = INIT_FINISH;
			
			//persistProcessInfo(dataOperator);
			
			if(pi.isLogin()) {
				clHelper.registRemoteClass();
			}else {
				logger.warn("System not in security model so not regist remote class!");
			}
			
			fromLocal = false;
			
			isInit.set(2);
			synchronized(isInit){
				isInit.notifyAll();
			}
			
			//对像工厂初始化后监听器
			for(IPostFactoryListener lis : this.postReadyListeners){
				lis.afterInit(this);
			}
			
			persistProcessInfo(dataOperator);
			
			String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
			TimerTicker.doInBaseTicker(60,Config.getInstanceName() + "_Choy_checker",null,(key,at)->{
				if(!dataOperator.exist(p) && pi.isActive()) {
					String js0 = JsonUtils.getIns().toJson(pi);
					String msg = "Recreate process info node by checker: " + js0;
					logger.warn(msg);
					LG.log(MC.LOG_WARN, this.getClass(),msg);
					dataOperator.createNodeOrSetData(p,js0,true);
				}
			});
			
		};
		
		String tag = cfg.getString(Constants.MASTER_SLAVE_TAG, null);
		/*boolean[] isMast = new boolean[1];
		isMast[0] = false;*/
		//Holder<Boolean> h = new Holder<>(false);
		
		setExitListener(dataOperator);
		
		if(!Utils.isEmpty(tag)) {
			VoterPerson lp = new VoterPerson(dataOperator,tag);
			this.cacheObj(VoterPerson.class, lp,null);
			logger.info("Wait for master!");
			this.masterSlaveListen((type,isMaster)->{
				if(isMaster && (IMasterChangeListener.MASTER_ONLINE == type 
				  || IMasterChangeListener.MASTER_NOTSUPPORT == type)) {
					 //参选成功
					 //h.set(true);
					 logger.info(Config.getInstanceName() + " got as master");
					 if(!pi.isMaster()) {
						 pi.setMaster(true);
						 String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
						 String js = JsonUtils.getIns().toJson(pi);
					     dataOperator.setData(p, js);
					 }
					 
					 try {
						r.run();
					} catch (Throwable e) {
						logger.error("", e);
						LG.log(MC.LOG_ERROR,SimpleObjectFactory.class , "", e);
						EnterMain.waitTime(5000);
						//System.exit(0);
					}
					 
					 LG.log(MC.LOG_INFO, SimpleObjectFactory.class
							 , "Got master and started: "+JsonUtils.getIns().toJson(pi));
					 MT.nonRpcEvent(Config.getInstanceName(), MC.MT_SERVER_START);
				} else if(isMaster) {
					 //失去master资格，退出
					 if(pi.isMaster()) {
						pi.setMaster(false);
						String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
						final String js = JsonUtils.getIns().toJson(pi);
						dataOperator.setData(p, js);
					 }
					
					 LG.log(MC.LOG_ERROR, SimpleObjectFactory.class
							 , "Lost master and exit: "+JsonUtils.getIns().toJson(pi));
					 MT.nonRpcEvent(Config.getInstanceName(), MC.MT_SERVER_STOP);
					 EnterMain.waitTime(10000);
					 
					 logger.error(Config.getInstanceName() + " lost master, need restart server!");
					 System.exit(0);
				}
			});
			
		} else {
			try {
				r.run();
			} catch (Throwable e) {
				logger.error("", e);
				LG.log(MC.LOG_ERROR,SimpleObjectFactory.class , "", e);
				EnterMain.waitTime(5000);
				System.exit(0);
			}
		}
		
		if(oldCl != null) {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
		
		//if(Config.isClientOnly()) {}
		/*String wfs = Config.getCommandParam("waitForShutdown");
		if( wfs == null || Boolean.parseBoolean(wfs) ) {
			logger.info("Wait for shutdown!");
			synchronized(waitForShutdown) {
				try {
					waitForShutdown.wait(Long.MAX_VALUE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			logger.info("Server shutdown!");
		}*/
	}
	
	private void setExitListener(IDataOperator op) {
		String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
		op.addNodeListener(p, (int type, String path,String data)->{
			//防止被误删除，只要此进程还在，此结点就不应该消失
			if(type == IListener.DATA_CHANGE) {
				ProcessInfoJRso pi0 = JsonUtils.getIns().fromJson(data, ProcessInfoJRso.class);
				if(!pi0.isActive()) {
					pi.setActive(false);
					String msg = "JVM exit by other system";
					LG.log(MC.LOG_WARN, this.getClass(),msg+"data: "+data);
					MT.nonRpcEvent(Config.getInstanceName(), MC.MT_PROCESS_REMOVE);
					logger.warn(msg);
					op.deleteNode(p);
					EnterMain.waitTime(4000);
					System.exit(0);
				} else {
					pi.setHaEnable(pi0.isHaEnable());
					pi.setOpTime(pi0.getOpTime());
					pi.setMaster(pi0.isMaster());
					//pi.setActName(pi0.getActName());
					pi.setAgentProcessId(pi0.getAgentProcessId());
					pi.setAgentInstanceName(pi0.getAgentInstanceName());
					pi.setAgentHost(pi0.getAgentHost());
					pi.setAgentId(pi0.getAgentId());
					pi.setCmd(pi0.getCmd());
					pi.setInstanceName(pi0.getInstanceName());
					pi.setMonitorable(pi0.isMonitorable());
					pi.setPid(pi0.getPid());
					pi.setWorkDir(pi0.getWorkDir());
					pi.setMetadatas(pi0.getMetadatas());
					pi.setLogLevel(pi0.getLogLevel());
				}
			}
		});
		
	}

	private void loadAccountInfo(IDataOperator op,Config cfg) {
		
		   /* if(pi.getActName() != null) {
		    	return;
		    }*/
		
			IAccountServiceJMSrv as = this.get(IAccountServiceJMSrv.class,false);
			boolean self = false;
			if(as == null) {
				self = false;
				//非Security实例
				IAccountServiceJMSrv$JMAsyncClient asyncAs = null;
				try {
					asyncAs = this.getRemoteServie(IAccountServiceJMSrv$JMAsyncClient.class.getName(), "*","*",null);
				} catch(CommonException e) {}
				if(asyncAs == null || !asyncAs.isReady()) {
					logger.error("Security account service not found or not ready so work in not secrity model!");
					pi.setActName(null);
					return;
				}
				as = asyncAs;
			} else {
				self = true;
			}
			
			int clientId = Config.getClientId();
			/*try {
				clientId = Config.getClientId();
			} catch(CommonException e) {}
			*/
			
			if(clientId < 0) {
				pi.setActName(null);
				return;
			}
			
			String token = cfg.getString("token", null);
			if(Utils.isEmpty(token)) {
				throw new CommonException("token cannot be null when specify clientId to start server: " + clientId);
			}
			
			RespJRso<ActInfoJRso> r = as.loginWithClientToken(token).getResult();

			//Resp<String> r = as.getNameById(clientId);
			if(r != null && r.getCode() == RespJRso.CODE_SUCCESS) {
				
				if(r.getData().getDefClientId() != clientId) {
					throw new CommonException("租户ID不匹配令牌:" + clientId);
				}
				
				pi.setAi(r.getData());
				Config.setAccountName(r.getData().getActName());
				pi.setActName(Config.getAccountName());
				String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
				op.setData(p,JsonUtils.getIns().toJson(pi));
				
				if(self) {
					return;//安全中心自身不需要心跳刷新账号
				}
				
				final IAccountServiceJMSrv as0 = as;
				Holder<Integer> loginCnt = new Holder<>(0);
				TimerTicker.doInBaseTicker(60*1, "actLoginCheck", null, (k,a)->{
					RespJRso<Boolean> resp = null;
					if(loginCnt.get() == 0 && !Utils.isEmpty(pi.getAi().getLoginKey())) {
						try {
							resp = as0.hearbeat(pi.getAi().getLoginKey());
						} catch (Exception e) {
							logger.error(e.getMessage());
						}
						//刷新系统账号，防止超时
						if(resp != null && resp.getData()) {
							return;
						}
					}
					
					if(LG.isLoggable(MC.LOG_WARN)) {
						String msg = "Hearbeat fail: act:" + pi.getAi().getActName()+",actId: "+ pi.getAi().getId();
						if(resp != null) {
							msg += "msg: " + resp.getMsg()+", code: " + resp.getCode();
						}
						LG.log(MC.LOG_WARN, SimpleObjectFactory.class,msg);
					}
					
					RespJRso<ActInfoJRso> lr = as0.loginWithClientToken(token).getResult();
					if(lr != null && lr.getCode() == RespJRso.CODE_SUCCESS) {
						pi.setAi(r.getData());
						//op.setData(p,JsonUtils.getIns().toJson(pi));
						loginCnt.set(0);
						if(LG.isLoggable(MC.LOG_INFO)) {
							String msg = "Relogin success: act:" + pi.getAi().getActName()+",actId: "+ pi.getAi().getId();
							LG.log(MC.LOG_INFO, SimpleObjectFactory.class,msg);
						}
					} else {
						//pi.setActName(null);
						loginCnt.set(loginCnt.get() +1);
						String msg = "Login fail cnt"+loginCnt.get()+" act:" + pi.getAi().getActName()+",actId: "+ pi.getAi().getId();
						if(lr != null) {
							msg += ",resp msg: " + lr.getMsg();
						}
						LG.log(MC.LOG_WARN, SimpleObjectFactory.class, msg);
						if(loginCnt.get() > 5) {
							LG.log(MC.LOG_ERROR, SimpleObjectFactory.class
									 , "System login fail and exit jvm cnt"+loginCnt.get()+" act:" + pi.getAi().getActName()+",actId: "+ pi.getAi().getId());
							EnterMain.waitTime(3000);
							System.exit(0);
						} 
					}
				});
			} else {
				pi.setActName(null);
				String msg = "Account name not found for client: " + clientId;
				if(r != null) {
					msg += " ,msg" +r.getMsg();
				}
				LG.log(MC.LOG_ERROR, SimpleObjectFactory.class, msg);
				throw new CommonException(msg);
			}
		
			/*int adminClientId = -1;
			try {
				adminClientId = Config.getAdminClientId();
			} catch(CommonException e) {
			}
			
			if(adminClientId < 0) {
				return;
			}
			
			if(clientId == adminClientId) {
				Config.setAdminAccountName(Config.getAccountName());
				return;
			}

			Resp<String> rr = as.getNameById(adminClientId);
			if(rr.getCode() == Resp.CODE_SUCCESS) {
				Config.setAdminAccountName(rr.getData());
			} else {
				throw new CommonException("Account name not found for: " + adminClientId);
			}*/
		
	}

	private void notifyAfterInitPostListener0(List<Object> lobjs, Config cfg, Set<Object> systemObjs) {
		Set<Object> haveInits = new HashSet<>();
		for(int i =0; i < lobjs.size(); i++){
			Object o = lobjs.get(i);
			
			 if(o instanceof ProxyObject){
	    		continue;
	    	 }
			
			if(systemObjs.contains(o) || haveInits.contains(o)) {
				continue;
			}
			haveInits.add(o);
			//通知初始化完成
			notifyAfterInitPostListener(o,cfg);
		}
	}

	private void processReference0(List<Object> lobjs, Config cfg, Set<Object> systemObjs) {
		Set<Object> dones = new HashSet<>();
		for(int i =0; i < lobjs.size(); i++){
			Object o = lobjs.get(i);
			
			 //代理对像延迟到目标对像创建才做注入
			if(o instanceof ProxyObject){
				continue;
	    	}
			
			if(systemObjs.contains(o) || dones.contains(o)) {
				continue;
			}
			dones.add(o);
			//通知初始化完成
			//processReference(o);
			clientServiceProxyManager.processReference(o);
		}
		
	}

	private void doInit0(List<Object> lobjs, Config cfg, Set<Object> systemObjs) {
		Set<Object> haveInits = new HashSet<>();
		for(int i =0; i < lobjs.size(); i++){
			Object o = lobjs.get(i);
			
			 if(o instanceof ProxyObject){
	    		continue;
	    	 }
			 
			if(systemObjs.contains(o) || haveInits.contains(o)) {
				continue;
			}
			
			haveInits.add(o);
			//logger.debug(o.getClass().getName());
			doInit(o);
		}
	}
	
	private void doReady0(List<Object> lobjs, Set<Object> systemObjs) {
		Set<Object> haveReadies = new HashSet<>();
		for(int i =0; i < lobjs.size(); i++){
			 Object o = lobjs.get(i);
			
			 if(o instanceof ProxyObject){
	    		continue;
	    	 }
			 
			if(systemObjs.contains(o) || haveReadies.contains(o)) {
				continue;
			}
			
			haveReadies.add(o);
			doReady(o);
		}
		
	}


	private void injectDepependencies0(List<Object> lobjs, Config cfg, Set<Object> systemObjs) {
		Set<Object> dones = new HashSet<>();
		for(int i =0; i < lobjs.size(); i++){
			Object o = lobjs.get(i);
			if(o.getClass().getName().equals(RpcClassLoaderHelper.class.getName())) {
				logger.info("Debug test: " + o.getClass().getName());
			}
			 //代理对像,还没真正创建目标对象,所以不需要
			 if(o instanceof ProxyObject){
	    		continue;
	    	 }
			
			if(systemObjs.contains(o) || dones.contains(o)) {
				continue;
			}
			
			dones.add(o);
			injectDepependencies(o);
		}
		
	}

	private void preInitPostListener0(List<Object> lobjs, Config cfg,Set<Object> systemObjs) {
		//Set<Object> haveInits = new HashSet<>();
		
		for(int i =0; i < lobjs.size(); i++){
			 Object obj = lobjs.get(i);
			
			 if(obj instanceof ProxyObject){
	    		continue;
	    	 }
			 
			if(systemObjs.contains(obj)) {
				continue;
			}
			
			//haveInits.add(obj);
			//只要在初始化前注入配置信息
			notifyPreInitPostListener(obj,cfg);
		}
		
	}

	private void createComponentOrService(IDataOperator dop, Set<Object> systemObjs, Set<Class<?>> clses,
			Config cfg) {
		
		//是否只启动服务端实例，命令行或环境变量中做配置
		boolean serverOnly = Config.isServerOnly();

		//是否只启动客户端实例，命令行或环境变量中做配置
		boolean clientOnly = Config.isClientOnly();
		
		String registryName = Config.getCommandParam(Constants.REGISTRY_KEY, String.class, Constants.DEFAULT_REGISTRY);
		
		IRegistry registry = null;
		
		ServiceManager srvManager = null;
		
		if(clses != null && !clses.isEmpty()) {
			for(Class<?> c : clses){
				//logger.info("Conponent class: " + c.getName());
				if(!(c.isAnnotationPresent(Service.class) || c.isAnnotationPresent(Component.class))) {
					continue;
				}
				
				Object obj = createOneComponent(c,clientOnly,serverOnly);
				if(obj == null) {
					continue;
				}
				
				Component cann = c.getAnnotation(Component.class);
				if(ServiceManager.class == c) {
					if(srvManager == null) {
						srvManager = (ServiceManager)obj;
					} else {
						throw new CommonException("More than one [" +ServiceManager.class.getName()+"] to be found ["+c.getName()+", "+srvManager.getClass().getName()+"]" );
					}
					systemObjs.add(srvManager);
				}
				
				if(IRegistry.class.isAssignableFrom(c) && registryName.equals(cann.value())){
					if(registry == null) {
						registry = (IRegistry)obj;
					}else {
						throw new CommonException("More than one [" +registryName+"] to be found ["+c.getName()+", "+registry.getClass().getName()+"]" );
					}
					systemObjs.add(registry);
				}
			}
		}
		
		if(registry == null){
			logger.info("IRegistry classloader: " + IRegistry.class.getClassLoader().getClass().getName());
			throw new CommonException("IRegistry with name: "+registryName +" not found!");
		}
		
		srvManager.setDataOperator(dop);
		notifyPreInitPostListener(srvManager,cfg);
		srvManager.init();
		
		registry.setDataOperator(dop);
		registry.setSrvManager(srvManager);
		registry.init();
		notifyPreInitPostListener(registry,cfg);
		
	}

	private  Object createOneComponent(Class<?> c,boolean clientOnly,boolean serverOnly) {
		if(!this.canCreate(c)){
			return null;
		}
		
		Component cann = c.getAnnotation(Component.class);
		if(!cann.active()){
			logger.debug("disable com: "+c.getName());
			return null;
		}
		
		if(serverOnly && isComsumerSide(ProxyObject.getTargetCls(c))) {
			//指定了服务端或客户端，不需要另一方所特定的组件
			logger.debug("serverOnly server disable: "+c.getName());
			return null;
		}
		
		if(clientOnly && isProviderSide(ProxyObject.getTargetCls(c))) {
			logger.debug("clientOnly client disable: "+c.getName());
			return null;
		}
		
		Object obj = null;
		if(c.isAnnotationPresent(Service.class)) {
			 obj = createDynamicService(c);
			 //obj = createServiceObject(obj,false);
			 //doAfterCreate(obj,null);
		} else {
			if(!objs.containsKey(c)) {
				obj = this.createObject(c, false);
			}else {
				return objs.get(c);
			}
		}
		return this.cacheObj(c, obj, null);
		//return obj;
	}

	private void createPostListener() {
		Set<Class<?>> listeners = ClassScannerUtils.getIns().loadClassByClass(IPostInitListener.class);
		if(listeners != null && !listeners.isEmpty()) {
			for(Class<?> c : listeners){
				PostListener comAnno = c.getAnnotation(PostListener.class);
				int mod = c.getModifiers();
				if((comAnno != null && !comAnno.value())|| Modifier.isAbstract(mod) 
						|| Modifier.isInterface(mod) || !Modifier.isPublic(mod)
						){
					continue;
				}
				
				try {
					IPostInitListener l = (IPostInitListener)c.newInstance();
					this.addPostListener(l);
				} catch (InstantiationException | IllegalAccessException e) {
					logger.error("Create IPostInitListener Error",e);
				}
			}
		}
	}

	private boolean isEnable(Object o) {
		if(o == null) {
			return false;
		}
		
		try {
			Method m = o.getClass().getMethod( "isEnable", new Class<?>[0]);
			if(m != null) {
				return (Boolean)m.invoke(o, new Object[0]);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			try {
				Method m = o.getClass().getMethod("isEnable", new Class<?>[0]);
				return (Boolean)m.invoke(o, new Object[0]);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				try {
					Field f = o.getClass().getField("enable");
					if(f != null) {
						boolean acc = f.isAccessible();
						if(!acc) {
							f.setAccessible(true);
						}
						Boolean v = f.getBoolean(o);
						f.setAccessible(acc);
						return v;
					}
					
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
				}
			}
		}
		
		return true;
	}

	public Class<?> loadCls(String clsName) {
		
		Class<?> cls = ClassScannerUtils.getIns().getClassByName(clsName);
		if(cls == null) {
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
			}
		}
		
		if(cls == null) {
			try {
				cls = this.getClass().getClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {}
		}
		
		if(cls == null) {
			RpcClassLoader cl = this.rpcClassLoader;
			if(cl != null) {
				try {
					cls = cl.loadClass(clsName);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		
		return cls;
		
	}

	@Override
	public Boolean exist(Class<?> clazz) {
		checkStatu();
		Object o = this.objs.get(clazz);
		if(o == null) return false;
		return this.validForPackage(getSecurityPackageName(), ProxyObject.getTargetCls(o.getClass()));
	}

	@Override
	public void regist(Object obj) {
		this.cacheObj(obj.getClass(), obj,null);
		if(AFTER_SORT <= this.stage) {
			this.doAfterCreate(obj, null);
		}
	}

	@Override
	public void regist(Class<?> clazz, Object obj) {
		this.cacheObj(clazz, obj,null);
		if(AFTER_SORT <= this.stage) {
			this.doAfterCreate(obj, null);
		}
	}
	
	@Override
	public void regist(String comName, Object obj) {
		this.cacheObj(obj.getClass(), obj,comName);
		if(AFTER_SORT <= this.stage) {
			this.doAfterCreate(obj, null);
		}
	}

	@Override
	public <T> void registT(Class<T> clazz, T obj) {
		this.cacheObj(clazz, obj,null);
		if(AFTER_SORT <= this.stage) {
			this.doAfterCreate(obj, null);
		}
	}

	@Override
	public void addPostListener(IPostInitListener listener) {
		for(IPostInitListener l : postListeners){
			if(l.getClass() == listener.getClass()) return;//快有相同实例，直接返回
		}
		postListeners.add(listener);
	}
	
	/*@Override
	public void addPostReadyListener(IFactoryListener listener) {
		for(IFactoryListener l : postReadyListeners){
			if(l.getClass() == listener.getClass()) return;
		}
		postReadyListeners.add(listener);
	}*/
	
	private boolean isProviderSide(Class<?> cls){
		//Class<?> cls = ProxyObject.getTargetCls(o.getClass());
		Component comAnno = cls.getAnnotation(Component.class);
		if(comAnno == null){
			return true;
		}
		return Constants.SIDE_PROVIDER.equals(comAnno.side());
	}
	
	private boolean isComsumerSide(Class<?> cls){
		//Class<?> cls = ProxyObject.getTargetCls(o.getClass());
		Component comAnno = cls.getAnnotation(Component.class);
		if(comAnno == null){
			return true;
		}
		return Constants.SIDE_COMSUMER.equals(comAnno.side());
	}
	
	private Set<?> filterProviderSide(Set<?> list){
		if(list == null || list.isEmpty()){
			return null;
		}
		Iterator<?> ite = list.iterator();
		while(ite.hasNext()){
			Class<?> c = ProxyObject.getTargetCls(ite.next().getClass());
			if(c.isAnnotationPresent(Component.class) && isProviderSide(c)){
				ite.remove();
			}
		}
		return list;
	}
	
	private  Set<?> filterComsumerSide(Set<?> list){
		if(list == null || list.isEmpty()){
			return null;
		}
		Iterator<?> ite = list.iterator();
		while(ite.hasNext()){
			Class<?> c = ProxyObject.getTargetCls(ite.next().getClass());
			if(c.isAnnotationPresent(Component.class) && isComsumerSide(c)){
				ite.remove();
			}
		}
		return list;
	}
	
	static void setObjectVal(Object obj,Field f,Object srv) {

		String setMethodName = "set"+f.getName().substring(0, 1).toUpperCase()+f.getName().substring(1);
		Method m = null;
		try {
			 m = obj.getClass().getMethod(setMethodName, f.getType());
			 m.invoke(obj, srv);
		} catch (InvocationTargetException | NoSuchMethodException e1) {
		    boolean bf = f.isAccessible();
			if(!bf) {
				f.setAccessible(true);
			}
			try {
				if(f.getName().equals("respo")) {
					ClassLoader cl0 = f.getDeclaringClass().getClassLoader();
					ClassLoader cl1 = srv.getClass().getClassLoader();
					logger.debug("test respo debug");
				}
				f.set(obj, srv);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
			if(!bf) {
				f.setAccessible(bf);
			} 
		}catch(SecurityException | IllegalAccessException | IllegalArgumentException e1){
			throw new CommonException("Class ["+obj.getClass().getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] error",e1);
		}
	
	}
	
	Object getCommandSpecifyConponent(Field f) {
		//系统启动时可以为某些类指定特定的实现
		Object srv = null;
		/*if(f.getType().getName().equals("cn.jmicro.api.mng.JmicroInstanceManager")) {
			logger.debug(f.getType().getName());
		}*/
		String commandComName = Config.getCommandParam(f.getType().getName(), String.class, null);
		if(!StringUtils.isEmpty(commandComName) && ( f.isAnnotationPresent(Inject.class) || f.isAnnotationPresent(Reference.class) )) {
			//对于注入或引用的服务,命令行指定实现组件名称
			srv = this.getByName(commandComName);
			/*if(this.nameToObjs.containsKey(commandComName)) {
				srv = this.nameToObjs.get(commandComName);
			} else */if(srv == null){
				//指定的组件不存在
				throw new CommonException("Component Name["+commandComName+"] for service ["+f.getType().getName()+"] not found");
			}
			
			if(!f.getType().isInstance(srv)) {
				//指定的组件不是该类的实例
				throw new CommonException("Component with Name["+commandComName+"] not a instance of class ["+f.getType().getName()+"]");
			}
		}
		
		return srv;
	}

	/**
	 * 注入两种注解分别为Inject和Reference，前者注入本地对像，后者注入远程对象
	 * @param obj
	 */
	private void injectDepependencies(Object obj) {
		Class<?> cls = ProxyObject.getTargetCls(obj.getClass());
		List<Field> fields = new ArrayList<>();
		Utils.getIns().getFields(fields, cls);
		
		//Component comAnno = cls.getAnnotation(Component.class);
		
		boolean isProvider = isProviderSide(ProxyObject.getTargetCls(obj.getClass()));
		boolean isComsumer =  isComsumerSide(ProxyObject.getTargetCls(obj.getClass()));
		
		//logger.debug(obj.getClass().getName()+" : "+ Inject.class.getClassLoader().toString());
		//logger.debug(obj.getClass().getName()+" : "+ obj.getClass().getClassLoader().toString());
		
		for(Field f : fields) {
			Object srv = null;
			boolean isRequired = false;
			Class<?> refCls = f.getType();
			
			/*if(refCls.getName().equals("cn.jmicro.api.choreography.ProcessInfo")) {
				logger.debug("cn.jmicro.api.choreography.ProcessInfo");
			}*/
			
			//对某些类,命令行可以指定特定组件实例名称,系统对该类使用指定实例,忽略别的实例
			srv = this.getCommandSpecifyConponent(f);
			
			if(srv == null && (f.isAnnotationPresent(Inject.class) /*|| f.isAnnotationPresent(Resource.class)*/)){
				//Inject the local component
				String name = null;
				isRequired = true;
				
				/*if(f.isAnnotationPresent(Resource.class)) {
					Resource res = f.getAnnotation(Resource.class);
					name = res.name();
					isRequired = true;//默认不可少
				}else {*/
					Inject inje = f.getAnnotation(Inject.class);
					name = inje.value();
					isRequired = inje.required();
				//}
				
				Class<?> type = f.getType();
				
				if(type.isArray()) {
					Class<?> ctype = type.getComponentType();
					Set<?> l = this.getByParent(ctype);
					
					if(isProvider){
						l = this.filterComsumerSide(l);
					}else if(isComsumer){
						l = this.filterProviderSide(l);
					}
					
					filterByLimitPackages(cls.getName(),l);
					
					if(l != null && l.size() > 0){
						Object[] arr = new Object[l.size()];
						l.toArray(arr);
						srv = arr;
					}
				}else if(List.class.isAssignableFrom(type)){
					ParameterizedType genericType = (ParameterizedType) f.getGenericType();
					if(genericType == null){
						throw new CommonException("must be ParameterizedType for cls:"+ cls.getName()+",field: "+f.getName());
					}
					Class<?> ctype = (Class<?>)genericType.getActualTypeArguments()[0];
					
					Set<?> l = this.getByParent(ctype);
					if(isProvider){
						l = this.filterComsumerSide(l);
					}else if(isComsumer){
						l = this.filterProviderSide(l);
					}
					
					if(l != null && l.size() > 0){
						
						filterByLimitPackages(cls.getName(),l);
						
						boolean bf = f.isAccessible();
						Object o = null;
						if(!bf) {
							f.setAccessible(true);
						}
						try {
							o = f.get(obj);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new CommonException("",e);
						}
						if(!bf) {
							f.setAccessible(bf);
						}
						
						if(o == null){
							if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
								o = new ArrayList<Object>();
							} else {
								try {
									o = type.newInstance();
								} catch (InstantiationException | IllegalAccessException e) {
									throw new CommonException("",e);
								}
							}
						}
						List<Object> el = (List<Object>)o;
						el.addAll(l);
						srv = el;
					}
					
				}else if(Set.class.isAssignableFrom(type)){
					
					ParameterizedType genericType = (ParameterizedType) f.getGenericType();
					if(genericType == null){
						throw new CommonException("must be ParameterizedType for cls:"+ cls.getName()+",field: "+f.getName());
					}
					Class<?> ctype = (Class<?>)genericType.getActualTypeArguments()[0];
					Set<?> l = this.getByParent(ctype);
					
					if(isProvider){
						l = this.filterComsumerSide(l);
					}else if(isComsumer){
						l = this.filterProviderSide(l);
					}
					
					if(l != null && l.size() > 0){
						filterByLimitPackages(cls.getName(),l);
						boolean bf = f.isAccessible();
						Object o = null;
						if(!bf) {
							f.setAccessible(true);
						}
						try {
							o = f.get(obj);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new CommonException("",e);
						}
						if(!bf) {
							f.setAccessible(bf);
						}
						
						if(o == null){
							if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
								o = new HashSet<Object>();
							} else {
								try {
									o = type.newInstance();
								} catch (InstantiationException | IllegalAccessException e) {
									throw new CommonException("",e);
								}
							}
						}
						Set<Object> el = (Set<Object>)o;
						el.addAll(l);
						srv = el;
					}
					
				}else if(Map.class.isAssignableFrom(type)){
					ParameterizedType genericType = (ParameterizedType) f.getGenericType();
					if(genericType == null){
						throw new CommonException("must be ParameterizedType for cls:"+ cls.getName()+",field: "+f.getName());
					}
					Class<?> keyType = (Class<?>)genericType.getActualTypeArguments()[0];
					if(keyType != String.class) {
						throw new CommonException("Map inject only support String as key");
					}
					
					Class<?> valueType = (Class<?>)genericType.getActualTypeArguments()[1];
					if(valueType == Object.class) {
						logger.warn("{} as map key will cause all components to stop in class {} field {}",
								Object.class.getName(), cls.getName(),f.getName());
					}
					
					Set<?> l = this.getByParent(valueType);
					if(isProvider){
						l = this.filterComsumerSide(l);
					}else if(isComsumer){
						l = this.filterProviderSide(l);
					}
					
					if(l != null && !l.isEmpty()) {
						
						filterByLimitPackages(cls.getName(),l);
						
						boolean bf = f.isAccessible();
						Map map = null;
						if(!bf) {
							f.setAccessible(true);
						}
						try {
							map = (Map)f.get(obj);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new CommonException("",e);
						}
						if(!bf) {
							f.setAccessible(bf);
						}
						
						if(map == null){
							map = new HashMap();
						}
						
						for(Object com : l) {
							String comName = this.getComName(com.getClass());
							map.put(comName, com);
						}
						srv = map;
					}
				}else if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
					/*if(type == IObjectFactory.class) {
						logger.debug(type.getName());
					}*/
					Set<?> l = this.getByParent(type);
					
					if(isProvider){
						l = this.filterComsumerSide(l);
					}else if(isComsumer){
						l = this.filterProviderSide(l);
					}
					
					filterByLimitPackages(cls.getName(),l);
					
					if(l != null && !l.isEmpty() && StringUtils.isEmpty(name)) {
						if(l.size() == 1) {
							srv =  l.iterator().next();
						}else if(l.size() > 1) {
							StringBuffer sb = new StringBuffer("More implement for type [").append(cls.getName()).append("] ");
							for(Object s : l) {
								sb.append(s.getClass()).append(",");
							}
							throw new CommonException(sb.toString());
						}
					} else if(l != null && !l.isEmpty()){
						for(Object s : l) {
							String n = EnterMain.getClassAnnoName(s.getClass());
							if(name.equals(n)){
								srv = s;
							}
						}
					}
				} else {
					
					if(name != null && !"".equals(name.trim())){
						srv = this.getByName(name);
						if(srv == null){
							this.getByParent(type);
						}
					}
					if(srv == null) {
						srv = this.get(type);
					}
					if(srv != null){
						Class<?> clazz = ProxyObject.getTargetCls(srv.getClass());
						if(!this.validForPackage(cls.getName(), clazz)) {
							srv = null;
							logger.warn(cls.getName() + " cannot reference instance of " + clazz.getName());
						}
						//如果没有Component注解，默认服务提供者及消费者都可用flag=true，只有定义了一方可用的组件，才需要做检测
						boolean flag = this.isComsumerSide(clazz) && this.isProviderSide(clazz);
						if(!flag) {
							if(isProvider && this.isComsumerSide(clazz)){
								throw new CommonException("Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] side should provider");
							}else if(isComsumer && this.isProviderSide(ProxyObject.getTargetCls(srv.getClass()))){
								throw new CommonException("Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] side should comsumer");
							}
						}
					}
				}
			}
			
			if(srv != null) {
				setObjectVal(obj, f, srv);
			} else if(isRequired) {
				String msg = "Class ["+cls.getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] not found";
				LG.log(MC.LOG_ERROR, getClass(), msg);
				EnterMain.waitTime(5000);
				throw new CommonException(msg);
			}
		}
	}
	
	private void filterByLimitPackages(String srcPkgName, Collection<?> l) {
		if(l == null || l.isEmpty()) return ;
		Iterator<?> ite = l.iterator();
		for(;ite.hasNext();) {
			Class<?> cls = ite.next().getClass();
			if(!validForPackage(srcPkgName,cls)) {
				logger.warn(srcPkgName + " cannot reference instance of " + cls.getName());
				ite.remove();
			}
		}
	}

	private boolean validForPackage(String srcPkgName, Class<?> cls) {
		if(!cls.isAnnotationPresent(Component.class)) return true;
		
		Component an = cls.getAnnotation(Component.class);
		String[] pks = an.limit2Packages();
		if(pks == null || pks.length == 0) return true;
		
		for(String pn : pks) {
			if(srcPkgName.startsWith(pn)) return true;
		}
		
		return false;
	}

	public Object createDynamicService(Class<?> cls) {
		 //String wayd(String msg);
		 ClassLoader cl = Thread.currentThread().getContextClassLoader();
		 JmicroClassPool cp = ClassGenerator.getClassPool(cl);
		 try {
			 CtClass ct = cp.makeClass(cls.getName() + "$JmicroSrv" + SimpleObjectFactory.idgenerator.getAndIncrement());
			 ct.setSuperclass(cp.getCtClass(cls.getName()));
			 ct.setInterfaces(new CtClass[]{cp.getCtClass(IServerServiceProxy.class.getName())});
			 CtMethod waydMethod = CtMethod.make("public java.lang.String wayd(java.lang.String msg){ return msg;}", ct);
			 ct.addMethod(waydMethod);
			 Class<?> clazz = ct.toClass(cls.getClassLoader(),cls.getProtectionDomain());
			 Object proxy = clazz.newInstance();
			 return proxy;
		} catch (Throwable e) {
			 throw new CommonException("Gen service class error: " + cls.getName(),e);
		}finally {
			if(cp != null) {
				cp.release();
			}
			 if(cl != null) {
				 Thread.currentThread().setContextClassLoader(cl);
			 }
		}
	}
	
	/*public Object createDynamicService(Class<?> cls) {
		 if(cls.getName().equals("cn.jmicro.example.pubsub.impl.AsyncRpcCallbackImpl")
			|| cls.getName().equals("cn.jmicro.example.pubsub.impl.SimplePubsubImpl")) {
			 logger.debug("createDynamicService");
		 }
		 ClassGenerator classGenerator = ClassGenerator.newInstance(cls.getClassLoader());
		 
		try {
			classGenerator
					.setClassName(cls.getName() + "$JmicroSrv" + SimpleObjectFactory.idgenerator.getAndIncrement());
			classGenerator.setSuperClass(cls);
			classGenerator.addInterface(IServerServiceProxy.class);
			classGenerator.addDefaultConstructor();

			Service srvAnno = cls.getAnnotation(Service.class);
			Class<?> srvInterface = srvAnno.infs();
			if (srvInterface == null || srvInterface == Void.class) {
				if (cls.getInterfaces() == null || cls.getInterfaces().length != 1) {
					throw new CommonException(
							"Class [" + cls.getName() + "] must implements one and only one service interface");
				}
				srvInterface = cls.getInterfaces()[0];
			}
			classGenerator.addInterface(srvInterface);

			// classGenerator.addField("public static java.lang.reflect.Method[] methods;");
			// classGenerator.addField("private " + InvocationHandler.class.getName() + "
			// handler = new cn.jmicro.api.client.ServiceInvocationHandler(this);");

			
			 * classGenerator.addField("private boolean enable=true;");
			 * classGenerator.addMethod("public java.lang.String getNamespace(){ return \""
			 * + ServiceItem.namespace(srvAnno.namespace()) + "\";}");
			 * classGenerator.addMethod("public java.lang.String getVersion(){ return \"" +
			 * ServiceItem.version(srvAnno.version()) + "\";}"); classGenerator.
			 * addMethod("public java.lang.String getServiceName(){ return \"" +
			 * srvInterface.getName() + "\";}");
			 * classGenerator.addMethod("public boolean enable(){  return this.enable;}");
			 * classGenerator.addMethod("public void enable(boolean en){ this.enable=en;}");
			 
			classGenerator.addMethod("public java.lang.String wayd(java.lang.String msg){ return msg;}");

			// 只为代理接口生成代理方法,别的方法继承自原始类
			Method[] ms1 = srvInterface.getMethods();

			// Method[] ms2 = new Method[ms1.length];
			
			 * if(cls.getName().equals("cn.jmicro.example.provider.TestRpcServiceImpl")) {
			 * System.out.println(""); }
			 
			logger.debug("Create Service: {}", cls.getName());
			for (int i = 0; i < ms1.length; i++) {
				// Method m1 = ms1[i];
				Method m = null;
				try {
					m = cls.getMethod(ms1[i].getName(), ms1[i].getParameterTypes());
				} catch (NoSuchMethodException | SecurityException e) {
					throw new CommonException("Method not found: " + ms1[i].getName());
				}
				if (m.getDeclaringClass() == Object.class || !Modifier.isPublic(m.getModifiers())) {
					continue;
				}

				// ms2[i] = m;

				Class<?> rt = m.getReturnType();
				Class<?>[] pts = m.getParameterTypes();

				StringBuilder code = new StringBuilder();

				if (!Void.TYPE.equals(rt)) {
					code.append(ReflectUtils.getName(rt)).append(" ret = ");
				}
				code.append(" super.").append(m.getName()).append("(");
				for (int j = 0; j < pts.length; j++) {
					code.append("$").append(j + 1);
					if (j < pts.length - 1) {
						code.append(",");
					}
				}
				code.append(");");

				if (!Void.TYPE.equals(rt)) {
					code.append(" return ret;");
				}
				logger.debug(code.toString());
				classGenerator.addMethod(m.getName(), m.getModifiers(), rt, pts, m.getExceptionTypes(),
						code.toString());
			}

			Class<?> clazz = classGenerator.toClass(cls.getClassLoader(),cls.getProtectionDomain());
			// clazz.getField("methods").set(null, ms2);
			Object proxy = clazz.newInstance();
			return proxy;
		} catch (Throwable e1) {
			throw new CommonException("Fail to create proxy ["+ cls.getName()+"]",e1);
		 } finally {
			 classGenerator.release();
		}
	}*/

	@SuppressWarnings("unchecked")
	private <T>  T createLazyProxyObject(Class<T> cls) {
		logger.debug("createLazyProxyObject: " + cls.getName());
		
		ClassGenerator cg = ClassGenerator.newInstance(cls.getClassLoader());
		try {
			cg.setClassName(cls.getName() + "$Jmicro" + idgenerator.getAndIncrement());
			cg.setSuperClass(cls.getName());
			cg.addInterface(ProxyObject.class);
			//cg.addDefaultConstructor();
			Constructor<?>[] cons = cls.getConstructors();
			Map<String,java.lang.reflect.Constructor<?>> consMap = new HashMap<>();
			String conbody = "this.conArgs=$args; for(int i = 0; i < $args.length; i++) { Object arg = $args[i]; this.conKey = this.conKey + arg.getClass().getName();}";
			for(Constructor<?> c : cons){
				String key = null;
				Class<?>[] ps = c.getParameterTypes();
				for(Class<?> p: ps){
					key = key + p.getName();
				}
				consMap.put(key, c);
				cg.addConstructor(c.getModifiers(),c.getParameterTypes(),c.getExceptionTypes(),conbody);
			}
			
			cg.addMethod("private void _init0() { if (this.init) return; this.init=true; this.target = ("+cls.getName()+")(__factory.createNoProxy("+cls.getName()+".class));}");
			cg.addMethod("public Object getTarget(){ _init0(); return this.target;}");
			
			int index = 0;
			List<Method> methods = new ArrayList<>();
			Utils.getIns().getMethods(methods, cls);
			for(Method m : methods){
				if(Modifier.isPrivate(m.getModifiers()) || m.getDeclaringClass() == Object.class){
					continue;
				}
				StringBuffer sb = new StringBuffer();
				//sb.append("if (!this.init) { System.out.println( \"lazy init class:"+cls.getName()+"\"); this.init=true; this.target = ("+cls.getName()+")((java.lang.reflect.Constructor)constructors.get(this.conKey)).newInstance(this.conArgs);}");
				sb.append(" _init0();");
				Class<?> rt = m.getReturnType();
				
				if (!Void.TYPE.equals(rt)) {
					sb.append(ReflectUtils.getName(rt)).append(" v = ");
				}
				
				sb.append(" __methods[").append(index).append("].invoke(this.target,$args); ");	
				
				if (!Void.TYPE.equals(rt)) {
					sb.append(" return v ;");
				}
				cg.addMethod(m.getName(), m.getModifiers(), m.getReturnType(), m.getParameterTypes(),
						m.getExceptionTypes(), sb.toString());
				index++;
			} 
			cg.addField("private boolean init=false;");
			cg.addField("private "+cls.getName()+" target=null; ");
			cg.addField("private java.lang.String conKey;");
			cg.addField("private java.lang.Object[] conArgs;");
			
			cg.addField("public static java.lang.reflect.Method[] __methods;");
			cg.addField("public static cn.jmicro.objfactory.simple.SimpleObjectFactory __factory;");
			
			Class<?> cl = cg.toClass(cls.getClassLoader(),cls.getProtectionDomain());
			
			cl.getField("__methods").set(null, cls.getMethods());
			cl.getField("__factory").set(null, this);
			Object o = cl.newInstance();
			//doAfterCreate(o);
			return (T)o;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | InstantiationException e) {
			logger.error("Create lazy proxy error for: "+ cls.getName(), e);
		}finally {
			cg.release();
		}
		return null;
	}

	 private String getComName(Class<?> cls) {
		 return EnterMain.getClassAnnoName(cls);
	 }
	 
	private void doInit(Object obj) {
		Class<?> tc = ProxyObject.getTargetCls(obj.getClass());
		Method initMethod1 = null;
		//Method initMethod2 = null;
		List<Method> methods = new ArrayList<>();
		Utils.getIns().getMethods(methods, tc);
		for(Method m : methods ) {
			if(m.isAnnotationPresent(JMethod.class)) {
				JMethod jm = m.getAnnotation(JMethod.class);
				if("init".equals(jm.value())) {
					initMethod1 = m;
					break;
				}
			}
			/*else if(m.getName().equals("init") && m.getParameterCount()==0) {
				initMethod2 = m;
			}*/
		}
		try {
			if(initMethod1 != null) {
				initMethod1.invoke(obj, new Object[]{});
			}/*else if(initMethod2 != null){
				initMethod2.invoke(obj, new Object[]{});
			}*/
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("Component init error:"+obj.getClass().getName(),e);
		}
	}
	
	private void doReady(Object obj) {
		Class<?> tc = ProxyObject.getTargetCls(obj.getClass());
		Method readyMethod1 = null;
		Method readyMethod2 = null;
		List<Method> methods = new ArrayList<>();
		Utils.getIns().getMethods(methods, tc);
		for(Method m : methods ) {
			if(m.isAnnotationPresent(JMethod.class) ) {
				JMethod jm = m.getAnnotation(JMethod.class);
				if(Constants.JMICRO_READY_METHOD_NAME.equals(jm.value())) {
					readyMethod1 = m;
					break;
				}
			}/*else if(m.isAnnotationPresent(PostConstruct.class)) {
				readyMethod1 = m;
				break;
			}*/ else if(m.getName().equals(Constants.JMICRO_READY_METHOD_NAME)) {
				readyMethod2 = m;
			}
		}
		try {
			if(readyMethod1 != null) {
				if(logger.isTraceEnabled()) {
					logger.trace(readyMethod1.toString());
				}
				readyMethod1.invoke(obj, new Object[]{});
			}else if(readyMethod2 != null){
				if(logger.isTraceEnabled()) {
					logger.info(readyMethod2.toString());
				}
				readyMethod2.invoke(obj, new Object[]{});
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("Component init error:"+obj.getClass().getName(),e);
		}
	}
	
	private void createProccessInfo(IDataOperator op,Config cfg) {
	
		String initProcessInfoPath = cfg.getString(ChoyConstants.PROCESS_INFO_FILE,null);
		String json = "";
		
		if(StringUtils.isEmpty(initProcessInfoPath)) {
			String dataDir = cfg.getString(Constants.INSTANCE_DATA_DIR,null);
			if(StringUtils.isEmpty(dataDir)) {
				throw new CommonException("Data Dir ["+Constants.INSTANCE_DATA_DIR+"] cannot be a file");
			}
			initProcessInfoPath = dataDir + File.separatorChar + "processInfo.json";
		} 
		
		File processInfoData = new File(initProcessInfoPath);
		
		if(processInfoData.exists()) {
			json = SystemUtils.getFileString(processInfoData);
		} else {
			try {
				processInfoData.createNewFile();
			} catch (IOException e) {
				throw new CommonException("Fail to create file [" +processInfoData+"]");
			}
		}
		
		logger.info("Origit ProcessInfo:" + json);
		
		if(StringUtils.isNotEmpty(json)) {
			pi = JsonUtils.getIns().fromJson(json, ProcessInfoJRso.class);
			pi.getMetadatas().clear();
			//编排环境下启动的实例
			//checkPreProcess(pi);
		} else {
			//非编排环境下启动的实例
			pi = new ProcessInfoJRso();
			pi.setAgentHost(Config.getExportSocketHost());
			pi.setAgentId(Config.getCommandParam(ChoyConstants.ARG_AGENT_ID));
			pi.setDepId(Config.getCommandParam(ChoyConstants.ARG_DEP_ID));
			
			String id = Config.getCommandParam(ChoyConstants.ARG_INSTANCE_ID);
			if(StringUtils.isNotEmpty(id)) {
				pi.setId(Integer.parseInt(id));
			} else {
				int processId;
				if(op.exist(ChoyConstants.ID_PATH)) {
					processId = Integer.parseInt(op.getData(ChoyConstants.ID_PATH))+1;
					op.setData(ChoyConstants.ID_PATH, processId+"");
				} else {
					processId = 1;
					op.createNodeOrSetData(ChoyConstants.ID_PATH, processId+"", false);
				}
				pi.setId(processId);
			}
			pi.setAgentProcessId(Config.getCommandParam(ChoyConstants.ARG_MYPARENT_ID));
		}
		
		boolean ismlModel = !Utils.isEmpty(cfg.getString(Constants.MASTER_SLAVE_TAG, null));
		String pid = SystemUtils.getProcessId();
		logger.info("Process ID:" + pid);
		pi.setActName(Config.getAccountName());
		pi.setPid(pid);
		pi.setActive(true);
		pi.setInstanceName(Config.getInstanceName());
		pi.setHost(Config.getExportSocketHost());
		pi.setWorkDir(cfg.getString(Constants.INSTANCE_DATA_DIR,null));
		pi.setOpTime(TimeUtils.getCurTime());
		pi.setHaEnable(ismlModel);
		pi.setMaster(false);
		pi.setStartTime(TimeUtils.getCurTime());
		pi.setInfoFilePath(initProcessInfoPath);
		pi.setOsName(System.getProperty("os.name"));
		pi.setLogLevel(Config.getCommandParam(Constants.SYSTEM_LOG_LEVEL, Byte.class, MC.LOG_INFO));
		pi.setClientId(Config.getClientId());
		
		//pi.setTimeOut(0);
		
		String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
		final String js = JsonUtils.getIns().toJson(pi);
		if(op.exist(p)) {
			String oldJson = op.getData(p);
			ProcessInfoJRso pri = JsonUtils.getIns().fromJson(oldJson, ProcessInfoJRso.class);
			//LG.log(MC.LOG_ERROR,getClass(),"Process exist[" +oldJson+"]");
			//JMicro.waitTime(5000);
			if(pri != null && pri.isActive()) {
				throw new CommonException("Process exist[" +oldJson+"]");
			}
			logger.warn("Delete exist process info: " + js);
			op.deleteNode(p);
		}
		
		this.cacheObj(ProcessInfoJRso.class, pi,null);
		
		op.createNodeOrSetData(p,js ,IDataOperator.EPHEMERAL);
		
		//SF.eventLog(MC.MT_PROCESS_ADD,MC.LOG_INFO, this.getClass().getSimpleName(),js);
		
	}
	
	//JMICRO容器启的最后调用
	private void persistProcessInfo(IDataOperator op) {
		//initProcessInfoPath = cfg.getString(Constants.INSTANCE_DATA_DIR,null) + File.separatorChar + "processInfo.json";
		String js = JsonUtils.getIns().toJson(pi);
		String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
		SystemUtils.setFileString(pi.getInfoFilePath(), js);
		op.createNodeOrSetData(p,js ,IDataOperator.EPHEMERAL);
		logger.info("Update ProcessInfo:" + js);
		
	}

}
