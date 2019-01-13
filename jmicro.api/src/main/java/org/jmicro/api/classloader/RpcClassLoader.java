package org.jmicro.api.classloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RpcClassLoader extends AbstractClientClassLoader {

	private final static Logger logger = LoggerFactory.getLogger(RpcClassLoader.class);
	
    private Set<IClassloaderRpc> rpcLoaders = null;
    
    private Map<String,Class<?>> clazzes = new HashMap<>();
    private Map<String,byte[]> clazzesData = new HashMap<>();
    
    private ClassLoader parent = null;

    public RpcClassLoader(ClassLoader parent,Set<IClassloaderRpc> rpcLoaders){
    	super(parent);
    	this.parent = parent;
    	this.rpcLoaders = rpcLoaders;
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
	         for(IClassloaderRpc rpc: this.rpcLoaders) {
	        	bytes = rpc.getClassData(className);
	        	if(bytes != null && bytes.length > 0) {
	        		logger.warn("load class {} from {} ",className,rpc.info());
	        		break;
	        	}
	         }
	         if(bytes != null && bytes.length > 0) {
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
		
    	if(this.rpcLoaders == null || this.rpcLoaders.isEmpty()) {
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
