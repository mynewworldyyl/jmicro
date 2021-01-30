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
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.classloader.genclient.IClassloaderRpc$JMAsyncClient;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;


public class RpcClassLoaderFromServer extends ClassLoader {

	private static final String CLASS_IDR = Config.BASE_DIR + "/remote_classes";
	
	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoaderFromServer.class);
	
	static {
		registerAsParallelCapable();
	}
	
    private Map<String,Class<?>> clazzes = new HashMap<>();
    private Map<String,byte[]> clazzesData = new HashMap<>();
    
    private Set<String> ownerClasses = new HashSet<>();
    
    @Inject
    private IRegistry registry;
    
    @Reference(required = false,namespace="classServer",version="0.0.2")
    private IClassloaderRpc$JMAsyncClient rpcLlassloader = null;
    
    private ClassLoader parent = null;

    public RpcClassLoaderFromServer(ClassLoader parent){
    	super(parent);
    	this.parent = parent;
    }
    
    public void ready() {
    	/*TimerTicker.doInBaseTicker(30, "RpcClassLoader-registRemoteClassChecker", null, (key,att)->{
    		doCheck();
    	});*/
	}
    
    public void addClassInstance(Class<?> clazz) {
    	
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
				byte[] byteData = getByteData(name0,true);
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
		if(!clazzes.containsKey(className)) {
			 Class<?> cls = getClassDataByInstanceName(originClsName,true);
			 if(cls != null) {
				 clazzes.put(className, cls);
			 }
		}
		return clazzes.get(className);
	}
	
	private byte[] getByteData(String originClsName,boolean sync) {

		try {
			IPromise<byte[]> p = this.rpcLlassloader.getClassDataJMAsync(originClsName);
			if(sync) {
				byte[] bytes = p.getResult();
				if (bytes != null && bytes.length > 0) {
					clazzesData.put(originClsName, bytes);
					
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
						clazzesData.put(originClsName, bytes);
						
						String desc = "Success async load class: "+originClsName+", length:"+bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						
					}else if(fail != null) {
						String desc = "Fail to async load class: "+originClsName+", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			logger.error("error load class: "+originClsName, e);
		}
		
		String desc = "Owner server not found for resource ["+originClsName+"]";
		logger.error(desc);
		LG.log(MC.LOG_ERROR, this.getClass(), desc);
		return null;

	}
	
	private Class<?> getClassDataByInstanceName(String originClsName,boolean sync) {

		try {
			IPromise<byte[]> p = this.rpcLlassloader.getClassDataJMAsync(originClsName);
			if(sync) {
				byte[] bytes = p.getResult();
				if (bytes != null && bytes.length > 0) {
					clazzesData.put(originClsName, bytes);
					String desc = "Success sync load class: "+originClsName+", length:"+bytes.length;
					logger.info(desc);
					LG.log(MC.LOG_INFO, this.getClass(), desc);
					Class<?> myClass = dfClass(originClsName,bytes);
		        	return myClass;
				}else {
					String desc = "Fail to sync load class: "+originClsName;
					logger.info(desc);
					LG.log(MC.LOG_ERROR, this.getClass(), desc);
					return null;
				}
			} else {
				p.then((bytes,fail,cxt)->{
					if (bytes != null && bytes.length > 0) {
						clazzesData.put(originClsName, bytes);
						dfClass(originClsName,bytes);
						//logger.info("Success async load class: {}, length:{}, from {}", originClsName,bytes.length,insName);
						String desc = "Success async load class: "+originClsName+", length:"+bytes.length;
						logger.info(desc);
						LG.log(MC.LOG_INFO, this.getClass(), desc);
						
					}else if(fail != null) {
						String desc = "Fail to async load class: "+originClsName+", with error: " + fail.toString();
						logger.info(desc);
						LG.log(MC.LOG_ERROR, this.getClass(), desc);
					}
				});
				return null;
			}
		} catch (Throwable e) {
			String desc = "error load class ["+originClsName+"]";
			logger.error(desc,e);
			LG.log(MC.LOG_ERROR, this.getClass(), desc,e);
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
