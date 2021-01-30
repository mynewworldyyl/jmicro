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
package cn.jmicro.api.classloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.classloader.genclient.IClassloaderRpc$JMAsyncClient;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;


public class RpcClassLoader extends ClassLoader {

	private static final String CLASS_IDR = Config.BASE_DIR + "/remote_classes";
	
	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoader.class);
	
	static {
		registerAsParallelCapable();
	}
	
    private Map<String,Class<?>> clazzes = new HashMap<>();
    private Map<String,byte[]> clazzesData = new HashMap<>();
    
    private Map<String,Set<String>> classesName2Instance = new HashMap<>();
    
    private Set<String> ownerClasses = new HashSet<>();
    
    @Inject
    private IDataOperator op;
    
    @Inject
    private IRegistry registry;
    
    @Reference(required = false,namespace="*",version="0.0.1")
    private IClassloaderRpc$JMAsyncClient rpcLlassloader = null;
    
    private ClassLoader parent = null;
    
    private IChildrenListener insNodeListener = (type,parent,instanceName,data)->{
    	String clsName = parent.substring(CLASS_IDR.length()+1);
    	
    	//logger.info("Remote class: {} from {}",clsName,instanceName);
    	
    	try {
			if(ownerClasses.contains(clsName) || RpcClassLoader.class.getClassLoader().loadClass(clsName) != null) {
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
			
			if(!clazzes.containsKey(clsName)) {
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
					clazzes.remove(clsName);
					//clazzesData.remove(clsName);
				}
			}
		}
	};
	
    
    private IChildrenListener classNodeListener = (type,parent,clsName,data)->{
    /*	if(clsName.equals("cn.jmicro.mng.api.II8NService")) {
    		logger.info("Remote class: {}",clsName);
    	}*/
    	logger.info("Notify remote class: {}",clsName);
    	try {
			if(ownerClasses.contains(clsName)  || RpcClassLoader.class.getClassLoader().loadClass(clsName) != null) {
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
	

    public RpcClassLoader(ClassLoader parent){
    	super(parent);
    	this.parent = parent;
    }
    
    public void ready() {
    	op.addChildrenListener(CLASS_IDR,classNodeListener);
    	TimerTicker.doInBaseTicker(30, "RpcClassLoader-registRemoteClassChecker", null, (key,att)->{
    		doCheck();
    	});
	}
    
    public void registRemoteClass() {
    	
    	if(ownerClasses.isEmpty()) {
    		return;
    	}
    	
    	for(String insPath : ownerClasses) {
    		if(!op.exist(insPath)) {
    			String msg = "Regist remote class:" + insPath;
    			logger.info(msg);
    			LG.log(MC.LOG_DEBUG, this.getClass(), msg);
        		op.createNodeOrSetData(insPath, Config.getExportSocketHost(), true);
        	}
    	}
    }
    
    private void doCheck() {
    	registRemoteClass();
    }
    
    public void addClassInstance(Class<?> clazz) {
    	String className = clazz.getName();
    	if(className.startsWith("cn.jmicro.api")) {
    		//系统类不需要远程加载，全部JMICRO应用都依赖于cn.jmicro:api包
    		return;
    	}
    	
    	String path = CLASS_IDR +"/" + className;
    	if(!op.exist(path)) {
    		op.createNodeOrSetData(path, "", false);
    	}
    	
    	String insPath = path + "/" + Config.getInstanceName();
    	ownerClasses.add(insPath);
    	
    	/*if(!op.exist(insPath)) {
    		//延迟10秒注册类，待服务准备好接收
    		TimerTicker.doInBaseTicker(10, className, null, (key,att)->{
    			op.createNodeOrSetData(insPath, Config.getExportSocketHost(), true);
    			TimerTicker.getBaseTimer().removeListener(className, true);
        	});
    	}*/
    }  

	@Override
	protected URL findResource(String name) {
		
		if(!checkResp(name)) {
			return super.findResource(name);
		}

		URL url = super.findResource(name);
		if(url == null) {
			url = this.parent.getResource(name);
			if(url == null) {
				try {
					url = new URL("http","localhost",name);
				} catch (MalformedURLException e) {
					logger.error(name,e);
				}
			}
		}
		return url;
	}
	
	@Override
	public InputStream getResourceAsStream(String name0) {

		if(!checkResp(name0)) {
			return super.getResourceAsStream(name0);
		}
		
		String loname = this.getClassName(name0);
		if(clazzesData.containsKey(loname)) {
			//优先用本地缓存数据
			byte[] byteData = clazzesData.get(loname);
			if(byteData != null && byteData.length > 0) {
				return new ByteArrayInputStream(byteData);
			}
		}
		
		InputStream is = super.getResourceAsStream(name0);
		
		if(is == null) {
			is = this.parent.getResourceAsStream(name0);
			if(is == null) {
				
				byte[] byteData = null;

				String originName = name0;
				loname = AsyncClientUtils.genSyncServiceName(loname);
				
				Set<String> ins = this.classesName2Instance.get(loname);
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
				
				if(byteData != null && byteData.length > 0) {
					is = new ByteArrayInputStream(byteData);
				}
			}
		}
		return is;
	}
	
	private Class<?> getClass0(String className) {

		/*if(className.endsWith("II8NService$Gateway$JMAsyncClient")) {
			logger.debug("getClass0");
		}*/
		
		String originClsName = className;
		className = this.getClassName(className);
		if(clazzes.containsKey(className)) {
			return clazzes.get(className);
		} else {
			 className = AsyncClientUtils.genSyncServiceName(className);
			 Set<String> insNames = this.classesName2Instance.get(className);
			 
			 if(insNames  == null || insNames.isEmpty()) {
				 logger.error("class " + originClsName + " owner server not found!");
				 return null;
			 }
			 
			 insNames.remove(Config.getInstanceName());
			 
	         Iterator<String> ite = insNames.iterator();
	         Class<?> cls=null;
	         
	         ServiceItem oldItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM,null);
	         
	         try {
				while(ite.hasNext()) {
					 String insName = ite.next();
					 cls = getClassDataByInstanceName(originClsName,insName,true);
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
	
	private byte[] getByteData(String originClsName,String insName,boolean sync) {

		Set<ServiceItem> items = this.registry.getServices(IClassloaderRpc.class.getName());
		ServiceItem directItem = null;

		for (ServiceItem si : items) {
			if (si.getKey().getInstanceName().equals(insName)) {
				directItem = si;
				break;
			}
		}

		if (directItem != null) {
			try {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
				IPromise<byte[]> p = this.rpcLlassloader.getClassDataJMAsync(originClsName);
				if(sync) {
					byte[] bytes = p.getResult();
					if (bytes != null && bytes.length > 0) {
						clazzesData.put(originClsName, bytes);
						
						String desc = "Success sync load class: "+originClsName+", length:"+bytes.length+", from " + directItem.getKey().toKey(true, true, true);
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						
						return bytes;
					} else {
						
						String desc = "Fail to sync load class: "+originClsName+", from " + directItem.getKey().toKey(true, true, true);
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
						
						return null;
					}
					
				}else {
					final ServiceItem directItem0 = directItem;
					
					p.then((bytes,fail,cxt)->{
						if (bytes != null && bytes.length > 0) {
							clazzesData.put(originClsName, bytes);
							
							String desc = "Success async load class: "+originClsName+", length:"+bytes.length+", from " + directItem0.getKey().toKey(true, true, true);
							logger.info(desc);
							LG.log(MC.LOG_INFO, this.getClass(), desc);
							
						}else if(fail != null) {
							String desc = "Fail to async load class: "+originClsName+", from " + directItem0.getKey().toKey(true, true, true)+", with error: " + fail.toString();
							logger.info(desc);
							LG.log(MC.LOG_ERROR, this.getClass(), desc);
						}
					});
					return null;
				}
			} catch (Throwable e) {
				logger.error("error load class from: " + directItem.getKey().toKey(true, true, true), e);
			}
		}
		String desc = "Owner server not found for resource ["+originClsName+"]";
		logger.error(desc);
		LG.log(MC.LOG_ERROR, this.getClass(), desc);
		return null;

	}
	
	
	private Class<?> getClassDataByInstanceName(String originClsName, String insName,boolean sync) {

		Set<ServiceItem> items = this.registry.getServices(IClassloaderRpc.class.getName());
		ServiceItem directItem = null;

		for (ServiceItem si : items) {
			if (si.getKey().getInstanceName().equals(insName)) {
				directItem = si;
				break;
			}
		}

		if (directItem != null) {
			try {
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
				IPromise<byte[]> p = this.rpcLlassloader.getClassDataJMAsync(originClsName);
				if(sync) {
					byte[] bytes = p.getResult();
					if (bytes != null && bytes.length > 0) {
						clazzesData.put(originClsName, bytes);
						String desc = "Success sync load class: "+originClsName+", length:"+bytes.length+", from " + directItem.getKey().toKey(true, true, true);
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						Class<?> myClass = dfClass(originClsName,bytes);
			        	return myClass;
					}else {
						String desc = "Fail to sync load class: "+originClsName+", from " + directItem.getKey().toKey(true, true, true);
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
						return null;
					}
				}else {
					final ServiceItem directItem0 = directItem;
					p.then((bytes,fail,cxt)->{
						if (bytes != null && bytes.length > 0) {
							clazzesData.put(originClsName, bytes);
							dfClass(originClsName,bytes);
							//logger.info("Success async load class: {}, length:{}, from {}", originClsName,bytes.length,insName);
							String desc = "Success async load class: "+originClsName+", length:"+bytes.length+", from " + directItem0.getKey().toKey(true, true, true);
							logger.info(desc);
							LG.log(MC.LOG_INFO, this.getClass(), desc);
							
						}else if(fail != null) {
							String desc = "Fail to async load class: "+originClsName+", from " + directItem0.getKey().toKey(true, true, true)+", with error: " + fail.toString();
							logger.info(desc);
							LG.log(MC.LOG_ERROR, this.getClass(), desc);
						}
					});
					return null;
				}
			} catch (Throwable e) {
				String desc = "error load class ["+originClsName+"] from: " + directItem.getKey().toKey(true, true, true);
				logger.error(desc,e);
				LG.log(MC.LOG_ERROR, this.getClass(), desc,e);
			}
		}
		String desc = "Owner server not found for ["+originClsName+"]";
		logger.error(desc);
		LG.log(MC.LOG_ERROR, this.getClass(), desc);
		return null;
	}
	
	private Class<?> dfClass(String originClsName, byte[] bytes) {
		Class<?> myClass =  defineClass(originClsName, bytes, 0, bytes.length);
    	resolveClass(myClass);
    	this.clazzes.put(originClsName, myClass);
    	ClassScannerUtils.getIns().putCls(originClsName, myClass);
    	return myClass;
	}
	
	
	private String getClassName(String clazz) {
		if(clazz.indexOf("/") > 0) {
			clazz = clazz.replaceAll("/", ".");
			if(clazz.endsWith(".class")) {
				clazz = clazz.substring(0,clazz.length()-".class".length());
			}
		}
		return clazz;
	}
	
	private boolean checkResp(String className) {
		 className = this.getClassName(className);
		 if(className.startsWith("java.") || className.startsWith("com.sun.")) {
			 return false;
		 }
		 String cn = className.substring(className.lastIndexOf(".")+1);
		 if(Utils.isEmpty(cn)) {
			 throw new CommonException("Invalid class name: " + className);
		 }
		 return Character.isUpperCase(cn.charAt(0));
		 
	}

	@Override
    public Class<?> findClass(String className){
		 logger.info("Find class: {}",className);
		
		 className = this.getClassName(className);
		
		 if(this.rpcLlassloader == null) {
    		logger.error("RpcClassLoader is NULL when load:{}",className);
    		return null;
	     }
		 
		if(!checkResp(className)) {
			return null;
		}
    	
    	if(clazzes.containsKey(className)) {
    		return this.clazzes.get(className);
    	}
    	
    	return this.getClass0(className);
       
    }
	
    public ServiceItem getClassLoaderItemByInstanceName(String instanceName) {
    	Set<ServiceItem> items = this.registry.getServices(IClassloaderRpc.class.getName());
		for (ServiceItem si : items) {
			if (si.getKey().getInstanceName().equals(instanceName)) {
				return si;
			}
		}
		return null;
    }

}
