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
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RpcClassLoader extends AbstractClientClassLoader {

	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoader.class);
	
    private Map<String,Class<?>> clazzes = new HashMap<>();
    private Map<String,byte[]> clazzesData = new HashMap<>();
    
    //@Cfg(value="")
    private Set<String> validRemoteLoaderPackages = new HashSet<>();
    
    //@Reference
	//private Set<IClassloaderRpc> rpcLoaders = new HashSet<>();
    
    @Inject
    private IRegistry registry;
    
    @Reference
    private IClassloaderRpc rpcLlassloader = null;
    
    private ClassLoader parent = null;

    public RpcClassLoader(ClassLoader parent){
    	super(parent);
    	this.parent = parent;
    }
    
    public void init() {
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
		}else {
			byte[] bytes=null;
			if(JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null) == null) {
				Set<ServiceItem> items = this.registry.getServices(IClassloaderRpc.class.getName());
		         for(ServiceItem si: items) {
		        	JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, si);
		        	try {
						bytes = this.rpcLlassloader.getClassData(className);
					} catch (Throwable e) {
					}
		        	if(bytes != null && bytes.length > 0) {
		        		logger.warn("load class {} from {} ",className,si.getKey().toKey(true, true, true));
		        		break;
		        	}
		         }
			} else {
				ServiceItem si = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
				bytes = this.rpcLlassloader.getClassData(className);
	        	if(bytes != null && bytes.length > 0) {
	        		logger.warn("load class {} from {} ",className,si.getKey().toKey(true, true, true));
	        	}
	        	JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
			}
			
			if(bytes == null || bytes.length == 0) {
        		logger.warn("load class {} not found ",className);
        	}else {
        		clazzesData.put(className, bytes);
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
		 if(!className.startsWith("org.jmicro")) {
			 return false;
		 }
		 String cn = className.substring(className.lastIndexOf(".")+1);
		 
		 return Character.isUpperCase(cn.charAt(0));
		 
	}

	@Override
    public Class<?> findClass(String className){
		logger.debug(className);
		
		 className = this.getClassName(className);
		
		if(!checkResp(className)) {
			return null;
		}
		
    	if(this.rpcLlassloader == null) {
    		logger.error("RpcClassLoader is NULL when load:{}",className);
    		return null;
    	}
    	
    	if(clazzes.containsKey(className)) {
    		return this.clazzes.get(className);
    	}
    	
        byte[] bytes = this.getData(className);
        
        Class<?> myClass = null;
        if(bytes != null && bytes.length > 0) {
        	myClass =  defineClass(className, bytes, 0, bytes.length);
        	resolveClass(myClass);
        	this.clazzes.put(className, myClass);
        }
        return myClass;
    }

}
