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
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.Constants;


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
    
    @Reference(required = false)
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
    	logger.info("Notify remote class: {}",clsName);
    	try {
			if(ownerClasses.contains(clsName)  || RpcClassLoader.class.getClassLoader().loadClass(clsName) != null) {
				return;
			}
		} catch (ClassNotFoundException e) {
		}
		if(type == IListener.ADD) {
			Set<String> inses = classesName2Instance.get(clsName);
			if(inses == null) {
    			classesName2Instance.put(clsName, (inses = new HashSet<String>()));
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
    			logger.info("Regist remote class: {}", insPath);
        		op.createNodeOrSetData(insPath, Config.getExportSocketHost(), true);
        	}
    	}
    }
    
    private void doCheck() {
    	registRemoteClass();
    }
    
    public void addClassInstance(String className) {
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
	public InputStream getResourceAsStream(String name) {

		if(!checkResp(name)) {
			return super.getResourceAsStream(name);
		}
		
		InputStream is = super.getResourceAsStream(name);
		if(is == null) {
			is = this.parent.getResourceAsStream(name);
			if(is == null) {
				name = this.getClassName(name);
				byte[] byteData = null;
				if(clazzesData.containsKey(name)) {
					byteData = clazzesData.get(name);
				} else {
					String originName = name;
					name = this.getClassName(name);
					name = AsyncClientUtils.genSyncServiceName(name);
					
					Set<String> ins = this.classesName2Instance.get(name);
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
					is = new ByteArrayInputStream(byteData);
				}
			}
		}
		return is;
	}
	
	private Class<?> getClass0(String className) {

		String originClsName = className;
		className = this.getClassName(className);
		if(clazzes.containsKey(className)) {
			return clazzes.get(className);
		} else {
			 className = AsyncClientUtils.genSyncServiceName(className);
			 Set<String> insNames = this.classesName2Instance.get(className);
			 insNames.remove(Config.getInstanceName());
			 
			 if(insNames  == null || insNames.isEmpty()) {
				 logger.error("class " + originClsName + " not found!");
				 return null;
			 }
			
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
						logger.info("Success load data: {} from {}", originClsName,
								directItem.getKey().toKey(true, true, true));
						return bytes;
					}else {
						return null;
					}
					
				}else {
					p.then((bytes,fail,cxt)->{
						if (bytes != null && bytes.length > 0) {
							logger.info("Success load data: {} from {}", originClsName,insName);
							clazzesData.put(originClsName, bytes);
						}else if(fail != null) {
							logger.error(fail.toString());
						}
					});
					return null;
				}
			} catch (Throwable e) {
				logger.error("error load class from: " + directItem.getKey().toKey(true, true, true), e);
			}
		}

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
						//clazzesData.put(originClsName, bytes);
						logger.info("Success load class: {} from {}", originClsName,
								directItem.getKey().toKey(true, true, true));
						Class<?> myClass = dfClass(originClsName,bytes);
			        	return myClass;
					}else {
						return null;
					}
				}else {
					p.then((bytes,fail,cxt)->{
						if (bytes != null && bytes.length > 0) {
							dfClass(originClsName,bytes);
							logger.info("Success load class: {} from {}", originClsName,insName);
						}else if(fail != null) {
							logger.error(fail.toString());
						}
					});
					return null;
				}
			} catch (Throwable e) {
				logger.error("error load class from: " + directItem.getKey().toKey(true, true, true), e);
			}
		}
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
