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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;

public class RpcClassLoader extends ClassLoader {

	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoader.class);
	
	public static Map<String,Class<?>> clazzes = new HashMap<>(); 
	
	private String[] basePackages = null;
	
	static {
		registerAsParallelCapable();
	}
	
	private IClassLoader2JMicroBridge bridge;
	
	//private Object clHelper;
    
    public RpcClassLoader(ClassLoader parent){
    	super(parent);
    }

	public void setHelper(Object helper) {
		if(bridge != null) {
			return;
		}
		
		final Method lbdMethod;
		final Method findClassMethod;
		
		try {
			Class<?> cls = helper.getClass();
			lbdMethod = cls.getMethod("loadByteData", String.class);
			findClassMethod = cls.getMethod("findClass", String.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("",e);
		}
		
		if(lbdMethod == null || findClassMethod== null) {
			throw new RuntimeException("Method loadByteData or findClass not found!");
		}
		
		this.bridge = new IClassLoader2JMicroBridge() {
			@Override
			public InputStream loadByteData(String clsName) {
				try {
					return (InputStream)lbdMethod.invoke(helper, clsName);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("",e);
				}
			}

			@Override
			public Class<?> findClass(String className) {
				try {
					return (Class<?>)findClassMethod.invoke(helper, className);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("",e);
				}
			}
			
		};
	}

	public void setBasePackages(String[] basePackages) {
		this.basePackages = basePackages;
	}
	
	@Override
	protected URL findResource(String name0) {
		String name = name0;
		if(!name0.endsWith(".class")) {
			name = name0.replaceAll("\\.", "/");
		}
		
		URL url = super.findResource(name+".class");
		if(url == null) {
			url = this.getParent().getResource(name+".class");
			if(url == null) {
				if(url == null) {
					url = getSystemResource(name+".class");
				}
				logger.warn("NULL Resource URL:" + name);
				return null;
				/*try {
					url = new URL("http","localhost/",name);
					logger.warn("Return invalid URL: " + url.toString());
				} catch (MalformedURLException e) {
					logger.error(name,e);
				}*/
			}
		}
		return url;
	}
	
	@Override
	public InputStream getResourceAsStream(String name0) {

		//logger.debug("getResourceAsStream: " + name0);
		
		if(bridge==null || !isRemoteClass(name0)) {
			return super.getResourceAsStream(name0);
		}
		
		String loname = getClassName(name0);
		/*if(clazzesData.containsKey(loname)) {
			//优先用本地缓存数据
			byte[] byteData = clazzesData.get(loname);
			if(byteData != null && byteData.length > 0) {
				return new ByteArrayInputStream(byteData);
			}
		}*/
		
		InputStream is = super.getResourceAsStream(name0);
		
		if(is == null) {
			is = this.getParent().getResourceAsStream(name0);
			if(is == null) {
				is = bridge.loadByteData(name0);
			}
		}
		return is;
	}

	@Override
    public Class<?> findClass(String className){
		if(bridge == null) {
			return null;
		}
		return bridge.findClass(className);
    }

	//实现远程类优先从远程加载
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

		if(name.equals("cn.jmicro.mng.api.II8NService")) {
			logger.debug(name);
		}
		
		if(clazzes.containsKey(name)) {
    		return clazzes.get(name);
    	}
		
		if(name.equals(RpcClassLoader.class.getName())) {
			return super.loadClass(name,resolve);
		}
		
		//logger.debug(name);
		if((bridge == null || !isRemoteClass(name))) {
			//服务实现类，通过本地加载
			if(bridge == null && name.startsWith("cn.jmicro")) {
				return loadRpcLocalClass(name,resolve);
			} else {
				return super.loadClass(name,resolve);
			}
		} else  {
			logger.debug(name);
			Class<?> rmc = this.findClass(name);
			if(rmc != null) {
				return rmc;
			}
			return loadRpcLocalClass(name, resolve);
		}

	}

	 public  Class<?> loadRpcLocalClass(String name, boolean resolve) throws ClassNotFoundException {
		// 需要从远程服务器加载,远程类，优先从远程加载
		if(clazzes.containsKey(name)) {
			return clazzes.get(name);
		}
		String fullpath = name.replace('.', '/');
		URL furl = getResource(fullpath);
		
		if(furl == null) {
			return null;
		}

		int pos = 0;
		int len = 0;
		byte[] ds = null;

		try (InputStream is = furl.openStream()) {
			ds = new byte[is.available()];
			byte[] td = new byte[1024];
			while ((len = is.read(td, 0, td.length)) > 0) {
				System.arraycopy(td, 0, ds, pos, len);
				pos += len;
			}
		} catch (IOException e) {
			logger.error(name, e);
		}

		if (pos <= 0) {
			String desc = fullpath + " read class data error";
			logger.error(desc);
			LG.log(MC.LOG_ERROR, this.getClass(), desc);
			throw new ClassNotFoundException(desc);
		}

		Class<?> remoteClass = dfClass(name, ds,resolve);
		
		if(remoteClass != null) {
			String desc = "Load class from local RpcClassLoader: "+name+", length:" + ds.length;
			logger.debug(desc);
			//LG.log(MC.LOG_DEBUG, this.getClass(), desc);
			//clazzesData.put(name, ds);
			return remoteClass;
		}  else {
			String msg = "Remote class [" + name + "] not found form repository by [" + Config.getClientId() + "]";
			logger.info(msg);
			LG.log(MC.LOG_WARN, this.getClass(), msg);
			throw new ClassNotFoundException(msg);
		}

	}
	
	 public Class<?> dfClass(String originClsName, byte[] bytes,boolean resolve) {
		 if(clazzes.containsKey(originClsName)) {
			 return clazzes.get(originClsName);
		 }
		 synchronized(originClsName) {
			 if(clazzes.containsKey(originClsName)) {
				 return clazzes.get(originClsName);
			 }
			 Class<?> myClass =  defineClass(originClsName, bytes, 0, bytes.length);
				if(resolve) {
					resolveClass(myClass);
				}
		    	clazzes.put(originClsName, myClass);
		    	return myClass;
		 }
	}
	 
	 public String getClassName(String clazz) {
			if(clazz.indexOf("/") > 0) {
				clazz = clazz.replaceAll("/", ".");
				if(clazz.endsWith(".class")) {
					clazz = clazz.substring(0,clazz.length()-".class".length());
				}
			}
			return clazz;
		}
	 
	 public  boolean isRemoteClass(String name) {
			
			/*if(name.startsWith(Constants.CORE_CLASS)) {
				return false;
			}*/
			String[] bs = this.basePackages;
			if(bs == null || bs.length == 0) {
				return false;
			}
			for(String p : bs) {
				if(name.startsWith(p)) {
					return true;
				}
			}
			return false;
		}
}
