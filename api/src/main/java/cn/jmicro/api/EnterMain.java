package cn.jmicro.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.codec.TypeCoderFactoryUtils;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

public class EnterMain {

	private static final String SPRING_OF_NAME = "cn.jmicro.objfactory.spring.SpringObjectFactory";
	
	private static final String SIMPLE_OF_NAME = "cn.jmicro.objfactory.simple.SimpleObjectFactory";
	
	private final static Logger logger = LoggerFactory.getLogger(EnterMain.class);
	
	//private static final Map<String,IObjectFactory> objFactorys = new HashMap<>();
	
	private static IObjectFactory sof = null;
	
	private static IObjectFactory of = null;
	
	private static Object lc = new Object();
	
	//确保每个对像工厂只会创建一个实例
	//private static boolean isInit = false;
	
	private static IDataOperator dataOperator = null;
	
	private static void init0(String[] args) {
		
		Map<String,String> params = Utils.parseCommondParams(args);
		
		Config.parseArgs(params);
		
		dataOperator = createOrGetDataOperator();
		
		Class<?> objCls = loadClass(SIMPLE_OF_NAME);
		
		IObjectFactory of0 = (IObjectFactory)newInstance(objCls);
		
		//String objName = Constants.DEFAULT_OBJ_FACTORY;
		/*ObjFactory anno = objCls.getAnnotation(ObjFactory.class);
		if(anno != null) {
			objName = anno.value();
		}*/
		
		IObjectFactory sof0 = null;
		
		String objClass  = Config.getCommandParam(Constants.OBJ_FACTORY_KEY);
		if(objClass != null && objClass.equals(SPRING_OF_NAME)) {
			Class<?> sobjCls = loadClass(SPRING_OF_NAME);
			if(sobjCls == null) {
				new CommonException(SPRING_OF_NAME + " not found!");
			}
			try {
				sof0 = (IObjectFactory)sobjCls.getConstructor(IObjectFactory.class).newInstance(of0);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new CommonException("fail create ["+sobjCls.getName()+"] ",e);
			}
		}
		
		of = of0;
		
		if(of0 != null) of0.start(dataOperator, args);
		
		if(sof0 != null) sof0.start(dataOperator, args);
		
		sof = sof0;
		
		
		synchronized(lc) {
			lc.notifyAll();
		}
		
		/*if(objFactorys.containsKey(objName)){
			throw new CommonException("Redefined Object Factory with name: "+objName
			+",cls:"+objClass+",exists:"+ objFactorys.get(objName).getClass().getName());
		}
		
		objFactorys.put(objName, of);*/
		
	/*	Set<Class<?>> objClazzes = ClassScannerUtils.getIns().loadClassesByAnno(ObjFactory.class);
		for(Class<?> c : objClazzes) {
			if(Modifier.isAbstract(c.getModifiers()) || Modifier.isInterface(c.getModifiers())){
				throw new CommonException("Object Factory must not abstract or interface:"+c.getName());
			}
			try {
				Set<Class<?>> subCls = ClassScannerUtils.getIns().loadClassByClass(c);
				if(subCls.size() > 1) {
					//不是final类，也就是还有子类，不能实例化
					continue;
				}
				ObjFactory anno = c.getAnnotation(ObjFactory.class);
				IObjectFactory of = (IObjectFactory)c.newInstance();
				if(objFactorys.containsKey(anno.value())){
					throw new CommonException("Redefined Object Factory with name: "+anno.value()
					+",cls:"+c.getName()+",exists:"+ objFactorys.get(anno.value()).getClass().getName());
				}
				objFactorys.put(anno.value(), of);
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CommonException("Instance ObjectFactory exception: "+c.getName(),e);
			}
		}*/
	}
	
	public static IDataOperator createOrGetDataOperator() {
		if(dataOperator != null) {
			return dataOperator;
		}
		String objClass  = getVal(Constants.DATA_OPERATOR,"cn.jmicro.zk.ZKDataOperator");
		
		Class<?> objCls = loadClass(objClass);
		
		dataOperator = (IDataOperator)newInstance(objCls);
		dataOperator.init0();
		
		return dataOperator;
	}
	
	public static String getVal(String key,String defaultVal) {
		String objClass  = Config.getValue(key, String.class, defaultVal);
		
		if(StringUtils.isEmpty(objClass) && dataOperator != null) {
			String path = Config.getRaftBasePath(Config.GROBAL_CONFIG) + "/" + key;
			objClass = dataOperator.getData(path);
		}
		
		if(StringUtils.isEmpty(objClass)) {
			objClass = defaultVal;
		}
		
		return defaultVal;
	}
	
	
	public static Class<?> loadClass(String className) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = JMicro.class.getClassLoader();
		}
		
		Class<?> objCls;
		try {
			objCls = cl.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new CommonException(className+" not found!",e);
		}
		return objCls;
	}
	
	
	public static Object newInstance(Class<?> cls) {
		try {
			return cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException(cls+" newInstance Error!",e);
		}
	}
	
	public static IObjectFactory getObjectFactory(){
		return sof != null ? sof : of;
	}
	
	public static IObjectFactory getObjectFactoryAndStart(String[] args) {
		if(of == null) {
			/*synchronized(lc) {
				if(of == null) {
					init0(args);
				}
			}*/
			init0(args);
		}
		return sof != null ? sof : of;
	}
	
	
	public static IRegistry getRegistry(String registryName){
		if(StringUtils.isEmpty(registryName)) {
			registryName = Constants.REGISTRY_KEY;
		}
		IRegistry registry = getObjectFactory().get(IRegistry.class,false);
		if(registry == null){
			throw new CommonException("Registry with name ["+registryName+"] not found");
		}
		return registry;
	}
	
	public static String getClassAnnoName(Class<?> cls) {

		cls = ProxyObject.getTargetCls(cls);
		/*if(cls.isAnnotationPresent(Name.class)){
			return cls.getAnnotation(Name.class).value();
		}else */if(cls.isAnnotationPresent(Component.class)){
			return cls.getAnnotation(Component.class).value();
		}
		/*else if(cls.isAnnotationPresent(Server.class)){
			return cls.getAnnotation(Server.class).value();
		}else if(cls.isAnnotationPresent(Channel.class)){
			return cls.getAnnotation(Channel.class).value();
		}else if(cls.isAnnotationPresent(Handler.class)){
			return cls.getAnnotation(Handler.class).value();
		}else if(cls.isAnnotationPresent(Interceptor.class)){
			return cls.getAnnotation(Interceptor.class).value();
		}else if(cls.isAnnotationPresent(Registry.class)){
			return cls.getAnnotation(Registry.class).value();
		}else if(cls.isAnnotationPresent(Selector.class)){
			return cls.getAnnotation(Selector.class).value();
		}else if(cls.isAnnotationPresent(Service.class)){
			return cls.getAnnotation(Service.class).value();
		}else if(cls.isAnnotationPresent(ObjFactory.class)){
			return cls.getAnnotation(ObjFactory.class).value();
		}else if(cls.isAnnotationPresent(Reference.class)){
			return cls.getAnnotation(Reference.class).value();
		}else if(cls.isAnnotationPresent(CodecFactory.class)){
			return cls.getAnnotation(CodecFactory.class).value();
		}*/
		return null;
	
	}
	
	public static void waitForShutdown() {
		Utils.getIns().waitForShutdown();
	}
	
	public static void waitTime(int i) {
		try {
			synchronized(lc) {
				lc.wait(i);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static <T> T getRpcServiceTestingArgs(Class<T> srvClazz,Map<String,String> result, byte protocol) {
		Object srv = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] {srvClazz}, 
				new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if(args == null || args.length == 0) {
							System.out.println("no need args for testing");
							return null;
						}
						
						if(Message.PROTOCOL_BIN == protocol) {
							JDataOutput jo = new JDataOutput(2048);
							
							TypeCoderFactoryUtils.getIns().getDefaultCoder().encode(jo, args, null, null);
							
							ByteBuffer bb = jo.getBuf();
							
							//ByteBuffer bb = decoder.encode(args);
							//bb.flip();
							
							byte[] data = new byte[bb.remaining()];
							bb.get(data, 0, bb.limit());
							String str = new String(Base64Utils.encode(data),Constants.CHARSET);
							System.out.println(str);
							if(result != null) {
								String pd = UniqueServiceMethodKeyJRso.paramsStr(args);
								result.put(pd, str);
							}
						} else {
							String str = JsonUtils.getIns().toJson(args);
							System.out.println(str);
							if(result != null) {
								String pd = UniqueServiceMethodKeyJRso.paramsStr(args);
								result.put(pd, str);
							}
						}
						
						return null;
					}
				});
		return (T)srv;
	}
	
}
