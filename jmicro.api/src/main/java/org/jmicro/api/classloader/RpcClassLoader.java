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
package org.jmicro.api.classloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.IListener;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RpcClassLoader extends ClassLoader {

	private static final String CLASS_IDR = Config.BASE_DIR + "/remote_classes";
	
	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoader.class);
	
    private Map<String,Class<?>> clazzes = new HashMap<>();
    private Map<String,byte[]> clazzesData = new HashMap<>();
    
    private Map<String,Set<String>> classesName2Instance = new HashMap<>();
    
    @Inject
    private IDataOperator op;
    
    @Inject
    private IRegistry registry;
    
    @Reference
    private IClassloaderRpc rpcLlassloader = null;
    
    private ClassLoader parent = null;
    
    private IChildrenListener insNodeListener = (type,parent,child,data)->{
		if(type == IListener.ADD) {
			String clsName = parent.substring(CLASS_IDR.length()+1);
			Set<String> inses = classesName2Instance.get(clsName);
			if(inses == null) {
    			classesName2Instance.put(child, (inses = new HashSet<String>()));
    		}
			inses.add(child);
		} else if(type == IListener.REMOVE) {
			Set<String> inses = classesName2Instance.get(child);
			if(inses != null) {
				inses.remove(child);
			}
		}
	};
	
    
    private IChildrenListener classNodeListener = (type,parent,child,data)->{
		if(type == IListener.ADD) {
			Set<String> inses = classesName2Instance.get(child);
			if(inses == null) {
    			classesName2Instance.put(child, (inses = new HashSet<String>()));
    		}
			String p = CLASS_IDR + "/" + child;
			op.addChildrenListener(p, insNodeListener);
		
		} else if(type == IListener.REMOVE) {
			classesName2Instance.remove(child);
			String p = CLASS_IDR + "/" + child;
			op.removeChildrenListener(p, insNodeListener);
		}
	};
	

    public RpcClassLoader(ClassLoader parent){
    	super(parent);
    	this.parent = parent;
    }
    
    public void ready() {
    	op.addChildrenListener(CLASS_IDR,classNodeListener);
	}
    
    public void addClassInstance(String className) {
    	String path = CLASS_IDR +"/" + className;
    	if(!op.exist(path)) {
    		op.createNode(path, "", false);
    	}
    	
    	String insPath = path + "/" + Config.getInstanceName();
    	if(!op.exist(insPath)) {
    		op.createNode(insPath, Config.getHost(), true);
    	}
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
				byte[] bytes = this.getData(name);
				if(bytes != null && bytes.length > 0) {
					is = new ByteArrayInputStream(bytes);
				}
			}
		}
		return is;
	}
	
	private synchronized byte[] getData(String className) {

		className = this.getClassName(className);
		if(clazzesData.containsKey(className)) {
			return clazzesData.get(className);
		} else {
			 Set<String> insNames = this.classesName2Instance.get(className);
			 if(insNames  == null || insNames.isEmpty()) {
				 logger.warn("class owner servernot found: {} ",className);
				 return null;
			 }
			 
			 Set<ServiceItem> items = this.registry.getServices(IClassloaderRpc.class.getName());
			 ServiceItem directItem = null;
	         Iterator<String> ite = insNames.iterator();
	         byte[] bytes=null;
	         
	         ServiceItem oldItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM,null);
	         
	         try {
				while(ite.hasNext()) {
					
					 String insName = ite.next();
					 for(ServiceItem si: items) {
				    	 if(si.getKey().getInstanceName().equals(insName)) {
				    		 directItem = si;
				    		 break;
				    	 }
				     }
				     
				     if(directItem == null) {
				    	continue;
				     }

				 	JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
					try {
						bytes = this.rpcLlassloader.getClassData(className);
					} catch (Throwable e) {}
					
					if(bytes != null && bytes.length > 0) {
						logger.warn("load class {} from {} ",className,directItem.getKey().toKey(true, true, true));
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
	         
	         if(bytes == null || bytes.length == 0) {
	        	 logger.warn("class[{}] not found from [{}] ",className,insNames.toString());
	         }
			
        	
	         return bytes;
		}	
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
		logger.debug(className);
		
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
    	
    	String locker = className.intern();
    	
    	synchronized(locker) {
    		
    		if(clazzes.containsKey(className)) {
        		return this.clazzes.get(className);
        	}
    		
		 	byte[] bytes = this.getData(className);
	        
	        Class<?> myClass = null;
	        if(bytes != null && bytes.length > 0) {
	        	myClass =  defineClass(className, bytes, 0, bytes.length);
	        	resolveClass(myClass);
	        	this.clazzes.put(className, myClass);
	        	ClassScannerUtils.getIns().putCls(className, myClass);
	        }
	        return myClass;
    	}
    	
       
        
       
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
