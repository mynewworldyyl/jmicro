package cn.jmicro.api.classloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.Holder;
import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.classloader.genclient.IClassloaderRpcJMSrv$JMAsyncClient;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

public class RpcClassLoaderHelper {

	private static final String CLASS_IDR = Config.getRaftBasePath("") + "/remote_classes";
	public static final String CLASS_INFO_IDR = Config.getRaftBasePath("") + "/remote_classes_info";
	
	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoaderHelper.class);
	
	private static final String COM_CLASS_LOADER_VERSION = "0.0.1";
    
    Map<String,Set<String>> classesName2Instance = new HashMap<>();
    
    private Map<String,Class<?>> ownerClasses = new HashMap<>();
    
    private Map<String,Class<?>> respClasses = new HashMap<>();
    
    private Set<String> successClasses = new HashSet<>();
    
    @Inject
    private IDataOperator op;
    
    @Inject
    private ProcessInfoJRso pi;
    
    @Inject
    private IObjectFactory of;
    
    @Inject
    private IRegistry registry;
    
    @Reference(required = false,namespace="*",version="0.0.1")
    private IClassloaderRpcJMSrv$JMAsyncClient rpcLlassloader = null;
    
    @Reference(required = false,namespace="repository",version="0.0.2")
    private IClassloaderRpcJMSrv$JMAsyncClient respClassloader = null;
    
    //@Inject(required=false)
    private IClassloaderRpcJMSrv localClassloader = null;
    
    private RpcClassLoader lc;
    
    //private ClassLoader parent = null;
    
    private RaftNodeDataListener<ClassInfoJRso> rndl = null;
    
    private IChildrenListener insNodeListener = (type,parent,instanceName)->{
    	String clsName = parent.substring(CLASS_IDR.length()+1);
    	
    	//logger.info("Remote class: {} from {}",clsName,instanceName);
    	
    	try {
			if(ownerClasses.containsKey(clsName) || RpcClassLoader.class.getClassLoader().loadClass(clsName) != null) {
				return;
			}
		} catch (ClassNotFoundException e1) {}
    	
		if(type == IListener.ADD) {
			Set<String> inses = classesName2Instance.get(clsName);
			if(inses == null) {
    			classesName2Instance.put(instanceName, (inses = new HashSet<String>()));
    		}
			inses.add(instanceName);
			logger.info("{} own by {}",clsName, instanceName);
			
			if(!RpcClassLoader.clazzes.containsKey(clsName)) {
				try {
					//logger.info("Try to load remote class: {}",clsName);
					//getClassDataByInstanceName(clsName,instanceName,true);
					//getClassDataByInstanceName(AsyncClientUtils.genAsyncServiceName(clsName),instanceName,true);
					//getClassDataByInstanceName(AsyncClientUtils.genAsyncServiceImplName(clsName),instanceName,true);
				} catch (Throwable e) {
					logger.error(clsName,e);
				}
			}
			
		} else if(type == IListener.REMOVE) {
			Set<String> inses = classesName2Instance.get(clsName);
			if(inses != null) {
				inses.remove(instanceName);
				if(inses.isEmpty()) {
					//RpcClassLoader.clazzes.remove(clsName);
					//clazzesData.remove(clsName);
				}
			}
		}
	};
	
    
    private IChildrenListener classNodeListener = (type,parent,clsName)->{
    	/*if(clsName.equals("cn.jmicro.mng.api.II8NService")) {
    		logger.info("Remote class: {}",clsName);
    	}*/
    	
    	if(logger.isTraceEnabled()) {
    		logger.trace("Notify remote class: {}",clsName);
    	}
    	
    	try {
			if(ownerClasses.containsKey(clsName)  || RpcClassLoader.class.getClassLoader().loadClass(clsName) != null) {
				//本地类，无需远程加载
				return;
			}
		} catch (ClassNotFoundException e) {
		}
		if(type == IListener.ADD) {
			if(!classesName2Instance.containsKey(clsName)) {
    			classesName2Instance.put(clsName,  new HashSet<String>());
    		}
			String p = CLASS_IDR + "/" + clsName;
			op.addChildrenListener(p, insNodeListener);
		} else if(type == IListener.REMOVE) {
			classesName2Instance.remove(clsName);
			String p = CLASS_IDR + "/" + clsName;
			op.removeChildrenListener(p, insNodeListener);
		}
	};
	
	private IRaftListener<ClassInfoJRso> classInfoListener = (type,node,ci)->{
		if(type == IListener.ADD) {
			//classInfoes.put(node, ci);
			//RpcClassLoader.clazzes.remove(node);
			//clazzesData.remove(node);
		}else if(type == IListener.REMOVE) {
			//classInfoes.remove(node);
			//RpcClassLoader.clazzes.remove(node);
			//clazzesData.remove(node);
		} else if(type == IListener.DATA_CHANGE){
			//classInfoes.put(node, ci);
			//clazzesData.remove(node);
			//RpcClassLoader.clazzes.remove(node);
		}
	};

   /*public RpcClassLoader(ClassLoader parent){
    	super(parent);
    	this.parent = parent;
    }*/
    
    public RpcClassLoaderHelper(){}
    
    public void jready() {
    	
    	IClassloaderRpcJMSrv cl = of.getByName("cn.jmicro.choreography.respository.ClassloaderRpcService");
    	if(cl != null) {
    		this.localClassloader = cl;
    	}
    	
    	rndl = new RaftNodeDataListener<>(this.op,CLASS_INFO_IDR,ClassInfoJRso.class,false);
    	rndl.addListener(classInfoListener);
    	
    	op.addChildrenListener(CLASS_IDR,classNodeListener);
    	Holder<Long> curTime = new Holder<>(TimeUtils.getCurTime());
    	TimerTicker.doInBaseTicker(30, "RpcClassLoader-registRemoteClassChecker", null, (key,att)->{
    		doCheck();
    		if(TimeUtils.getCurTime() - curTime.get() > 300000) {
    			successClasses.clear();
    			curTime.set(TimeUtils.getCurTime());
    		}
    	});
	}
    
    public void setClassLoader(RpcClassLoader cl) {
    	this.lc = cl;
    }
    
    private boolean working = false;
    
    public void registRemoteClass() {
    	
    	if(ownerClasses.isEmpty() || working || !of.isRpcReady()) {
    		return;
    	}
    	
    	working = true;
    	
    	try {
    		if(pi.isLogin() && respositoryReady()) {
        		
        		if(respClasses.isEmpty()) {
        			return;
        		}
        		
        		IClassloaderRpcJMSrv rcl = this.localClassloader != null ? this.localClassloader : this.respClassloader;
        	    
        		Set<String> keySet = new HashSet<>();
        		synchronized(respClasses) {
        			keySet.addAll(respClasses.keySet());
        		}
        		
        		for(String className : keySet) {
            		
        			Class<?> clazz = respClasses.get(className);
        			if(clazz == null) {
        				throw new CommonException("Remote class not found:"+className);
        			}
            		
        			int ver = 0;
        			int clientId = -1;
            		if(clazz.isAnnotationPresent(SO.class)) {
            			SO so = clazz.getAnnotation(SO.class);
            			ver = so.dataVersion();
            			clientId = so.clientId();
            		}else if(clazz.isAnnotationPresent(AsyncClientProxy.class)) {
            			AsyncClientProxy sa = clazz.getAnnotation(AsyncClientProxy.class);
            			ver = sa.dataVersion();
            			clientId = sa.clientId();
            		}
            		
            		RemoteClassRegisterJRso rc = new RemoteClassRegisterJRso();
        			
        			URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        			if(url == null) {
        				url = lc.getResource(className);
        			}
        			
        			if(url == null) {
        				throw new RuntimeException("Class [" + className+"] resource url not found!");
        			}
        			
        			String cp ="/"+ className.replaceAll("\\.", "/")+".class";
            		File f = new File(url.getFile()/* + "/" + cp + ".class"*/);
            		rc.getCi().setModifiedTime(f.lastModified());
            		//                                                                     cn/jmicro/api/security/JmicroPublicKey
            		//file:/root/jmicro/resp/jmicro.security-0.0.2-SNAPSHOT-with-core.jar!/cn/jmicro/api/security/JmicroPublicKey.class
            		String fullPath = url.getFile();
            		
            		if(fullPath.contains("target/classes/")) {
            			rc.getCi().setTesting(true);
            			//byte[] data = new byte[(int)f.length()];
            			/*try(InputStream is = new FileInputStream(f)) {
            				is.read(data, 0, (int)f.length());
            				rc.setData(data);
            			} catch(IOException e) {
    						logger.error(cp,e);
    					} */
            			/*try(InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            				if(is != null) {
            					is.read(data, 0, (int)f.length());
                				rc.setData(data);
            				}else {
            					logger.error("Data not found: "+cp);
            				}
            				
            			} catch(IOException e) {
    						logger.error(cp,e);
    					} */
            			rc.setData(ClassScannerUtils.getRemoteClassData(className));
            			
            			rc.getCi().setJarFileName(f.getName());
            		} else if(fullPath.endsWith("!/"+cp)) {
            			rc.getCi().setTesting(false);
            			String n = fullPath.substring(0,fullPath.length() - cp.length()-2);
            			n = n.substring(n.lastIndexOf("/")+1);
            			rc.getCi().setJarFileName(n);
            		} else {
            			rc.getCi().setTesting(false);
            			rc.getCi().setJarFileName(new File(url.getFile()).getName());
            		}
            		
            		ClassInfoJRso ci = this.rndl.getData(className);
            		if(ci != null) {
            			if(!rc.getCi().isTesting() && ci.getDataVersion() >=ver) {
            				//非测试环境，并且当前服务器版本大于或等于当前版本，不需要更新服务器数据
            				respClasses.remove(className);
            				continue;
            			}
            		}
            		
            		rc.getCi().setClazzName(className);
            		rc.getCi().setDataVersion(ver);
            		rc.getCi().setClientId(clientId);
            		
	            	 ActInfoJRso sai = null;
	   				 try {
	   					if(this.localClassloader != null) {
	   						sai = JMicroContext.get().getSysAccount();
	            			JMicroContext.get().setSysAccount(pi.getAi());
	            		}
	   					RespJRso<Boolean> r = rcl.registRemoteClass(rc);
	            		if(r != null && r.getData()) {
	            			successClasses.add(className);
	            			respClasses.remove(className);
	            			ClassScannerUtils.remoteClassData(className);
	            		}else if(r != null && r.getCode() == RespJRso.CODE_NO_PERMISSION) {
	            			respClasses.remove(className);
	            			ClassScannerUtils.remoteClassData(className);
	            			LG.log(MC.LOG_ERROR,this.getClass(), r.getMsg());
	            			logger.error(r.getMsg());
	            		} else {
	            			if(r != null) {
	            				LG.log(MC.LOG_ERROR,this.getClass(), r.getMsg());
	            				logger.error(r.getMsg() + " Class: " + className + ", try regist again after minutes");
	            			}else {
	            				LG.log(MC.LOG_ERROR,this.getClass()," response NULL");
	            				logger.error("registRemoteClass response NULL, Class: " + className + ", try regist again after minutes");
	            			}
	            		}
	   				 } finally {
	   					if(this.localClassloader != null) {
	   						JMicroContext.get().setSysAccount(sai);
	            		}
	   				 }
            	}
        	}

    		synchronized(ownerClasses) {
    			for(String className : ownerClasses.keySet()) {
            		
            		String path = CLASS_IDR +"/" + className;
                	if(!op.exist(path)) {
                		op.createNodeOrSetData(path, "", false);
                	}
                	
                	String insPath = path + "/" + Config.getInstanceName();
            		
            		if(!op.exist(insPath)) {
            			String msg = "Regist remote class:" + insPath;
            			logger.info(msg);
            			LG.log(MC.LOG_DEBUG, this.getClass(), msg);
                		op.createNodeOrSetData(insPath, Config.getExportSocketHost(), true);
                	}
            	}
    		}
    		
    	} finally {
    		working = false;
    	}
    
    }
    
    boolean respositoryReady() {
    	return pi != null && pi.isLogin() && (this.localClassloader != null || 
    			respClassloader != null && respClassloader.isReady());
    }
    
    private void doCheck() {
    	registRemoteClass();
    }
    
    public void addClassInstance(Class<?> clazz) {
    	if(clazz.isArray()) {
    		clazz = clazz.getComponentType();
		}
    	
		if(!(clazz.isAnnotationPresent(SO.class) || clazz.isAnnotationPresent(AsyncClientProxy.class))) {
			return;
		}
		
    	String className = clazz.getName();
    	if(!RpcClassLoader.isRemoteClass(className)) {
    		//系统类不需要远程加载，全部JMICRO应用都依赖于cn.jmicro:api包
    		return;
    	}
    	
    	if(successClasses.contains(className)) {
    		return;
    	}
    	
    	logger.info("Add remote class: {}",clazz.getName());
    	synchronized(ownerClasses) {
    		ownerClasses.put(className,clazz);
    	}
    	
    	synchronized(respClasses) {
    		respClasses.put(className,clazz);
    		if(clazz.isAnnotationPresent(AsyncClientProxy.class)) {
    			try {
    				String asynSrvImpl = AsyncClientUtils.genAsyncServiceImplName(className);
					respClasses.put(asynSrvImpl,clazz.getClassLoader().loadClass(asynSrvImpl));
					
					String asynSrv = AsyncClientUtils.genAsyncServiceName(className);
					respClasses.put(asynSrv,clazz.getClassLoader().loadClass(asynSrv));
					
					String gatewaySrv = AsyncClientUtils.genGatewayServiceName(className);
					respClasses.put(gatewaySrv,clazz.getClassLoader().loadClass(gatewaySrv));
					
				} catch (ClassNotFoundException e) {
					//logger.error("",e);
					throw new CommonException(e.getMessage(),e);
				}
    		}
		}
    	
    	registRemoteClass();
    	
    	/*if(!op.exist(insPath)) {
    		//延迟10秒注册类，待服务准备好接收
    		TimerTicker.doInBaseTicker(10, className, null, (key,att)->{
    			op.createNodeOrSetData(insPath, Config.getExportSocketHost(), true);
    			TimerTicker.getBaseTimer().removeListener(className, true);
        	});
    	}*/
    }  

	private Class<?> getClass0(String className) {

		/*if(className.endsWith("II8NService$Gateway$JMAsyncClient")) {
			logger.debug("getClass0");
		}*/
		
		String originClsName = className;
		className = lc.getClassName(className);
		if(RpcClassLoader.clazzes.containsKey(className)) {
			return RpcClassLoader.clazzes.get(className);
		} else {
			 className = AsyncClientUtils.genSyncServiceName(className);
			 Set<String> insNames = this.classesName2Instance.get(className);
			 
			 if(insNames  == null || insNames.isEmpty()) {
				 //logger.info("class " + originClsName + " owner server not found!");
				 return null;
			 }
			 
			 insNames.remove(Config.getInstanceName());
			 
	         Iterator<String> ite = insNames.iterator();
	         Class<?> cls=null;
	         
	         ServiceItemJRso oldItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM,null);
	         
	         try {
				while(ite.hasNext()) {
					 String insName = ite.next();
					 cls = getClassByInstanceName(originClsName,insName,true);
					 if(cls != null) {
						break;
					 }
				}
			} finally {
				if(oldItem != null) {
					JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, oldItem);
				}else {
					JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
				}
			}
	         
	         if(cls == null) {
	        	 logger.warn("class[{}] not found from [{}] ",originClsName,insNames.toString());
	         }
        	
	         return cls;
		}	
	}
	
	boolean containClass(String clazz) {
		return RpcClassLoader.clazzes.containsKey(clazz);
	}
	
	Class<?> getReClass(String clazz) {
		return RpcClassLoader.clazzes.get(clazz);
	}
	
	byte[] getByteData(String originClsName,String insName,boolean sync) {
		
		UniqueServiceKeyJRso directItem = null;
		//final String fmsg="";
		try {
			IPromise<byte[]> p = null;

			Set<UniqueServiceKeyJRso> items = this.registry.getServices(IClassloaderRpcJMSrv.class.getName());
			for (UniqueServiceKeyJRso si : items) {
				if (si.getInstanceName().equals(insName) && 
						si.getVersion().equals(COM_CLASS_LOADER_VERSION)) {
					directItem = si;
					break;
				}
			}
			
			if (directItem != null) {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
				 p = this.rpcLlassloader.getClassDataJMAsync(originClsName,0,false);
			}else {
				String desc = "Owner server not found for resource ["+originClsName+"]";
				logger.error(desc);
				LG.log(MC.LOG_ERROR, this.getClass(), desc);
				return null;
			}
			
			if(sync) {
				byte[] bytes = p.getResult();
				if (bytes != null && bytes.length > 0) {
					//clazzesData.put(originClsName, bytes);
					String desc = "Success sync load class: "+originClsName+", length:"+bytes.length;
					logger.info(desc);
					LG.log(MC.LOG_INFO, this.getClass(), desc);
					return bytes;
				} else {
					String desc = "Fail to sync load class: "+originClsName;
					logger.info(desc);
					LG.log(MC.LOG_ERROR, this.getClass(), desc);
					return null;
				}
			}else {
				p.then((bytes,fail,cxt)->{
					if (bytes != null && bytes.length > 0) {
						//clazzesData.put(originClsName, bytes);
						String desc = "Success async load class: "+originClsName+", length:" + bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						
					}else if(fail != null) {
						String desc = "Fail to async load class: "+originClsName + ", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			logger.error("error load class: " + originClsName + " from ins : " + insName, e);
		}
		return null;
	}
	
	
	private Class<?> getClassByInstanceName(String originClsName, String insName,boolean sync) {
		
		UniqueServiceKeyJRso directItem = null;
		try {
			IPromise<byte[]> p = null;

			Set<UniqueServiceKeyJRso> items = this.registry.getServices(IClassloaderRpcJMSrv.class.getName());
			for (UniqueServiceKeyJRso si : items) {
				if (si.getInstanceName().equals(insName) && 
						si.getVersion().equals(COM_CLASS_LOADER_VERSION)) {
					directItem = si;
					break;
				}
			}
			
			if (directItem != null) {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
				 p = this.rpcLlassloader.getClassDataJMAsync(originClsName,0,false);
			}else {
				String desc = "Owner server not found for resource ["+originClsName+"]";
				logger.error(desc);
				LG.log(MC.LOG_ERROR, this.getClass(), desc);
				return null;
			}
			
			if(sync) {
				byte[] bytes = p.getResult();
				if (bytes != null && bytes.length > 0) {
					//clazzesData.put(originClsName, bytes);
					String desc = "Success sync load class: "+originClsName+", length:"+bytes.length;
					logger.info(desc);
					LG.log(MC.LOG_INFO, this.getClass(), desc);
					Class<?> myClass = lc.dfClass(originClsName,bytes,true);
		        	return myClass;
				} else {
					String desc = "Fail to sync load class: "+originClsName;
					logger.info(desc);
					LG.log(MC.LOG_ERROR, this.getClass(), desc);
					return null;
				}
				
			} else {
				p.then((bytes,fail,cxt)->{
					if (bytes != null && bytes.length > 0) {
						//clazzesData.put(originClsName, bytes);
						lc.dfClass(originClsName,bytes,false);
						String desc = "Success async load class: "+originClsName+", length:" + bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
					}else if(fail != null) {
						String desc = "Fail to async load class: "+originClsName + ", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			logger.error("error load class: " + originClsName + " from ins : " + insName, e);
		}
		
		return null;
	}
	
	private boolean checkResp(String className) {
		 className = lc.getClassName(className);
		 if(className.startsWith("java.") || className.startsWith("com.sun.")) {
			 return false;
		 }
		 String cn = className.substring(className.lastIndexOf(".")+1);
		 if(Utils.isEmpty(cn)) {
			 throw new CommonException("Invalid class name: " + className);
		 }
		 return Character.isUpperCase(cn.charAt(0));
		 
	}

    public Class<?> findClass(String className){
		 //logger.info("Find class: {}",className);
		
		if(RpcClassLoader.clazzes.containsKey(className)) {
    		return RpcClassLoader.clazzes.get(className);
    	}
		
		 className = lc.getClassName(className);
		
		 if(this.rpcLlassloader == null && this.respClassloader == null) {
    		//logger.debug("RpcClassLoader is NULL when load:{}",className);
    		return null;
	     }
		 
		if(!checkResp(className)) {
			return null;
		}
    	
    	Class<?>  cls = null;
    	if(this.respositoryReady()) {
    		className = className.intern();
    		synchronized(className) {
    			if(RpcClassLoader.clazzes.containsKey(className)) {
    	    		return RpcClassLoader.clazzes.get(className);
    	    	}
    			cls = getClassFromRepository(className,true);
    		}
    	}
    	
    	if(cls == null) {
    		cls = this.getClass0(className);
    	}
    	
    	return cls;
       
    }
	
    private Class<?> getClassFromRepository(String className,boolean sync) {
    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.lc);
			 if(this.localClassloader != null) {
				 ActInfoJRso sai = JMicroContext.get().getSysAccount();
				 try {
					 JMicroContext.get().setSysAccount(pi.getAi());
					 byte[] bytes = localClassloader.getClassData(className, 0, true);
					 if(bytes != null && bytes.length > 0) {
						//clazzesData.put(className, bytes);
						String desc = "Success sync load class: "+className+", length:"+bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						Class<?> myClass = lc.dfClass(className,bytes,true);
			        	return myClass;
					} else {
						String desc = "Fail to sync load class: "+className;
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
						return null;
					}
				 }finally {
					 JMicroContext.get().setSysAccount(sai);
				 }
			 }
			 
			 IPromise<byte[]> p = respClassloader.getClassDataJMAsync(className, 0, true);
			 
			if(sync) {
				byte[] bytes = p.getResult();
				if(bytes != null && bytes.length > 0) {
					//clazzesData.put(className, bytes);
					String desc = "Success sync load class: "+className+", length:"+bytes.length;
					logger.info(desc);
					LG.log(MC.LOG_INFO, this.getClass(), desc);
					
					Class<?> myClass = lc.dfClass(className,bytes,false);
		        	return myClass;
				} else {
					String desc = "Fail to sync load class: "+className;
					logger.debug(desc);
					//LG.log(MC.LOG_ERROR, this.getClass(), desc);
					return null;
				}
			} else {
				p.then((bytes,fail,cxt)->{
					if (bytes != null && bytes.length > 0) {
						//clazzesData.put(className, bytes);
						lc.dfClass(className,bytes,false);
						String desc = "Success async load class: "+className+", length:" + bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
					}else if(fail != null) {
						String desc = "Fail to async load class: "+className + ", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			logger.error("error load class: " + className + " from repository ", e);
		}finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
		
		return null;
	
	}

   byte[] getByteDataFromRepository(String originClsName,boolean sync) {
		try {
			
			 if(this.localClassloader != null) {
				 ActInfoJRso sai = JMicroContext.get().getSysAccount();
				 try {
					 JMicroContext.get().setSysAccount(pi.getAi());
					 return localClassloader.getClassData(originClsName, 1, true);
				 }finally {
					 JMicroContext.get().setSysAccount(sai);
				 }
			 }
			 
			IPromise<byte[]> p = respClassloader.getClassDataJMAsync(originClsName, 1, true);
			
			if(sync) {
				byte[] bytes = p.getResult();
				if(bytes != null && bytes.length > 0) {
					//clazzesData.put(originClsName, bytes);
					String desc = "Success sync load class: "+originClsName+", length:"+bytes.length;
					logger.info(desc);
					LG.log(MC.LOG_INFO, this.getClass(), desc);
		        	return bytes;
				} else {
					String desc = "Fail to sync load class: "+originClsName;
					logger.info(desc);
					LG.log(MC.LOG_ERROR, this.getClass(), desc);
					return null;
				}
			} else {
				p.then((bytes,fail,cxt)->{
					if (bytes != null && bytes.length > 0) {
						//clazzesData.put(originClsName, bytes);
						String desc = "Success async load class: "+originClsName+", length:" + bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
					}else if(fail != null) {
						String desc = "Fail to async load class: "+originClsName + ", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			logger.error("error load class: " + originClsName + " from repository", e);
		}
		
		return null;
	}
	
	public Map<String,Class<?>> getComClass() {
		Map<String,Class<?>> temp = new HashMap<>();
		temp.putAll(RpcClassLoader.clazzes);
		return temp;
	}
	
	public InputStream loadByteData(String clsName) {
		
		byte[] byteData = null;
		if(respositoryReady()) {
			 byteData = getByteDataFromRepository(clsName,true);
		}
		
		if(byteData == null) {
			String originName = clsName;
			clsName = AsyncClientUtils.genSyncServiceName(clsName);
			
			Set<String> ins = classesName2Instance.get(clsName);
			if(ins != null && !ins.isEmpty()) {
				Iterator<String> ite = ins.iterator();
				while(ite.hasNext()) {
					 String insName = ite.next();
					 byteData = getByteData(originName,insName,true);
					 if(byteData != null && byteData.length > 0) {
						break;
					}
				}
			}
		}
		
		if(byteData != null && byteData.length > 0) {
			return new ByteArrayInputStream(byteData);
		}else {
			return null;
		}
	
	}

}
