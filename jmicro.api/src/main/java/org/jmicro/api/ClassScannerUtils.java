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
package org.jmicro.api;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.ObjFactory;
import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.config.Config;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:45
 */
public class ClassScannerUtils {

	private final static Logger logger = LoggerFactory.getLogger(ClassScannerUtils.class);
	
	private static final ClassScannerUtils instance = new ClassScannerUtils();
	private ClassScannerUtils() {}
	
	private Map<String,Class<?>> classes = new HashMap<String,Class<?>>();
	
	public static ClassScannerUtils getIns() {
		if(instance.classes.isEmpty()) {
			instance.getClassesByParent(Config.getBasePackages(),null);
		}
		return instance;
	}
	
	interface Checker{
		boolean accept(Class<?> cls);
	}
	
	public Set<Class<?>> loadClassesByAnno(Class<? extends Annotation> annaCls){
		
		if(this.classes.isEmpty()){
			Set<Class<?>> clses = this.getClassesWithAnnotation(Config.getBasePackages(), annaCls);
			return clses;
		}else {
			Set<Class<?>> set = new HashSet<Class<?>>();
			for(Class<?> c : this.classes.values()){
				if(c.isAnnotationPresent(annaCls)){
					set.add(c);
				}
			}
			return set;
		}
	}
	
	public Set<Class<?>> loadClassByClass(Class<?> parentCls){
		if(this.classes.isEmpty()){
			return this.getClassesByParent(Config.getBasePackages(), parentCls);
		}else {
			Set<Class<?>> set = new HashSet<Class<?>>();
			for(Class<?> c : classes.values()){
				if(parentCls.isAssignableFrom(c)){
					set.add(c);
				}
			}
			return set;
		}
	}
	
	public Set<Class<?>> getComponentClass(){
		Set<Class<?>> clazzes = new HashSet<>();
		for(Class<?> c : classes.values()){
			if(this.isComponentClass(c)){
				clazzes.add(c);
			}
		}
		return clazzes;
	}
	
	public Class<?> getClassByName(String clsName){
		if(classes.isEmpty()){
			this.getClassesByParent(Config.getBasePackages(), null);
		}
		Class<?> cls = this.classes.get(clsName);
		if(cls == null){
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return cls;
	}
	
	public Class<?> getClassByAnnoName(String annoName) {
		for(Class<?> cls : classes.values()) {
			if(cls.isAnnotationPresent(Component.class)){
				Component n = cls.getAnnotation(Component.class);
				if(annoName.equals(n.value())) {
					return cls;
			}
			/*else if(cls.isAnnotationPresent(Server.class)){
				Server n = cls.getAnnotation(Server.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Channel.class)){
				Channel n = cls.getAnnotation(Channel.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Handler.class)){
				Handler n = cls.getAnnotation(Handler.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Interceptor.class)){
				Interceptor n = cls.getAnnotation(Interceptor.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Registry.class)){
				Registry n = cls.getAnnotation(Registry.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Selector.class)){
				Selector n = cls.getAnnotation(Selector.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(Service.class)){
				Service n = cls.getAnnotation(Service.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(ObjFactory.class)){
				ObjFactory n = cls.getAnnotation(ObjFactory.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}*/
			}/*else if(cls.isAnnotationPresent(Reference.class)){
				Reference n = cls.getAnnotation(Reference.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}else if(cls.isAnnotationPresent(CodecFactory.class)){
				CodecFactory n = cls.getAnnotation(CodecFactory.class);
				if(annoName.equals(n.value())) {
					return cls;
				}
			}*/

		}
		return null;
	}
	
	private Set<Class<?>> getClassesByParent(String []basePackages, Class<?> parentCls) {
		return this.getClassesChecker(basePackages, c -> {
			if(parentCls != null) {
				//System.out.println(parentCls.getName());
				return parentCls.isAssignableFrom(c);
			} else {
				return isComponentClass(c);
			}
		});
	}
	
	private Set<Class<?>> getClassesWithAnnotation(String []basePackages,final Class<? extends Annotation> annoCls) {
		return this.getClassesChecker(basePackages, c -> {
			if(annoCls != null) {
				return c.isAnnotationPresent(annoCls);
			} else {
				return isComponentClass(c);
			}
		});
	}
	
	private boolean isComponentClass(Class<?> cls){

		return  cls.isAnnotationPresent(Component.class)
				||cls.isAnnotationPresent(Service.class)
				||cls.isAnnotationPresent(PostListener.class)
				||cls.isAnnotationPresent(ObjFactory.class)
				/*||cls.isAnnotationPresent(Name.class)
				||cls.isAnnotationPresent(Server.class)
				||cls.isAnnotationPresent(Channel.class)
				||cls.isAnnotationPresent(Handler.class)
				||cls.isAnnotationPresent(Interceptor.class)
				||cls.isAnnotationPresent(Registry.class)
				||cls.isAnnotationPresent(Selector.class)
				
				||cls.isAnnotationPresent(ObjFactory.class)
				||cls.isAnnotationPresent(CodecFactory.class)
				||cls.isAnnotationPresent(Reference.class)
				||cls.isAnnotationPresent(PostListener.class)*/
				;
	}
	
	private Set<Class<?>> getClassesChecker(String [] packs,Checker checker) {
		if(packs == null || packs.length == 0) {
			return Collections.EMPTY_SET;
		}
		Set<Class<?>> clses = new HashSet<Class<?>>();
		for(String p : packs) {
			Set<Class<?>> cset = this.getClassesByPackageName(p.trim());
			if(cset != null && !cset.isEmpty()) {
				Iterator<Class<?>> ite = cset.iterator();
				while(ite.hasNext()){
					Class<?> c = ite.next();
					if(this.isComponentClass(c)){
						this.classes.put(c.getName(), c);
						for(Class<?> inr : c.getInterfaces()){
							if(!this.classes.containsKey(inr.getName())){
								this.classes.put(inr.getName(), inr);
							}
						}
					}
					//logger.debug(c.getName());
					/*if(c.getName().equals("org.jmicro.api.server.FirstInterceptor")) {
						logger.debug(c.getName());
					}*/
					if(checker.accept(c)){
						//logger.debug(c.getName());
						clses.add(c);
					}	
				}
			}
		}
		return clses;
	}

	
	private Set<Class<?>> getClassesByPackageName(String pack) {
		  
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();  
        boolean recursive = true;  
        String packageName = pack;  
        String packageDirName = packageName.replace('.', '/');  
        Enumeration<URL> dirs;  
        try {  
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);  
            while (dirs.hasMoreElements()) {  
                URL url = dirs.nextElement();  
                String f = url.getFile();
                String protocol = url.getProtocol();  
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");  
                    findAndAddClassesInPackageByFile(pack, filePath,recursive, classes);  
                } else if ("jar".equals(protocol)) {
                    JarFile jar;  
                    try {  
                        jar = ((JarURLConnection) url.openConnection())  
                                .getJarFile();  
                        Enumeration<JarEntry> entries = jar.entries();  
                        while (entries.hasMoreElements()) {  
                            JarEntry entry = entries.nextElement();  
                            String name = entry.getName();  
                            if (name.charAt(0) == '/') {  
                                name = name.substring(1);  
                            }  
                            if (name.startsWith(packageDirName)) {  
                                int idx = name.lastIndexOf('/');  
                                if (idx != -1) {  
                                    packageName = name.substring(0, idx)  
                                            .replace('/', '.');  
                                }  
                                if ((idx != -1) || recursive) {  
                                    if (name.endsWith(".class")  
                                            && !entry.isDirectory()) {  
                                        String className = name.substring(  
                                                packageName.length() + 1, name  
                                                        .length() - 6);  
                                        try {  
                                            classes.add(Class  
                                                    .forName(packageName + '.'  
                                                            + className));  
                                        } catch (ClassNotFoundException e) {  
                                            e.printStackTrace();  
                                        }  
                                    }  
                                }  
                            }  
                        }  
                    } catch (IOException e) {  
                         logger.error("鍦ㄦ壂鎻忕敤鎴峰畾涔夎鍥炬椂浠巎ar鍖呰幏鍙栨枃浠跺嚭閿�");  
                    }  
                } else if ("bundleresource".equals(protocol)) {
                    //System.err.println("file绫诲瀷鐨勬壂鎻�");  
                   // String filePath = url.getFile(); 
                    //URL fileUrl = FileLocator.toFileURL(url);
                   // String filePath = URLDecoder.decode(fileUrl.getPath(), "UTF-8"); 
                    //findAndAddClassesInPackageByFile(pack, filePath,recursive, classes);  
                } 
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
        return classes;  
    }  

	/** 
     *  
     * @param packageName 
     * @param packagePath 
     * @param recursive 
     * @param classes 
     */  
	private void findAndAddClassesInPackageByFile(String packageName,  String packagePath, final boolean recursive, Set<Class<?>> classes) {
    	if(classes == null) {
    		throw new NullPointerException("classes can not be null");
    	}
        File dir = new File(packagePath);  
        if (!dir.exists() || !dir.isDirectory()) {  
            return;  
        }  
        File[] dirfiles = dir.listFiles(new FileFilter() {  
            public boolean accept(File file) {  
                return (recursive && file.isDirectory())  || (file.getName().endsWith(".class"));  
            }  
        });  
        
        for (File file : dirfiles) {  
            if (file.isDirectory()) {  
                findAndAddClassesInPackageByFile(packageName + "."  + file.getName(), file.getAbsolutePath(), recursive,  classes);  
            } else {  
                String className = file.getName().substring(0,  file.getName().length() - 6);                 
                String cn = null;
                try {  
                	cn = packageName + '.' + className;
                	Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(cn);
                    classes.add(cls);    
                    } catch (ClassNotFoundException e) {  
	                    logger.error("ERROR: "+file.getAbsolutePath() +" for class " + cn,e);  
                    }  catch (SecurityException e) {
                    	 logger.error("ERROR: "+file.getAbsolutePath() +" for class " + cn,e);  
                    }
            }  
        }  
    }
	
	public static List<String> getClasspathResourcePaths(String packageName,String patern) {
		  
		List<String> paths = new ArrayList<String>();  
        boolean recursive = true;
        String packageDirName = "";
		if(packageName == null || "".equals(packageName.trim())) {
			packageDirName = ".";
		}else {
			packageDirName = packageName.replace('.', '/');
		}
		
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);  
            while (dirs.hasMoreElements()) {  
                URL url = dirs.nextElement();
                String f = url.getFile();
                String protocol = url.getProtocol();  
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    File fs = new File(filePath);
                    if(fs.isDirectory()) {
                    	//logger.info("List directory:{}",fs.getAbsolutePath());
                    	fromDirectory(packageDirName,fs,patern,paths);
                    } else {
                    	if(StringUtils.isEmpty(patern) || match(fs.getName(),patern)) {
                    		paths.add(packageDirName+"/"+fs.getName());
            	        	logger.info("fromFile: {}",fs.getAbsolutePath());
                    	}
                    }
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {  
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();  
                        Enumeration<JarEntry> entries = jar.entries();  
                        while (entries.hasMoreElements()) {  
                            JarEntry entry = entries.nextElement();  
                            String name = entry.getName();  
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);  
                            }  
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');  
                                if (idx != -1) {
                                    packageName = name.substring(0, idx);  
                                }  
                                if ((idx != -1) || recursive) {  
                                    if (match(name,patern) && !entry.isDirectory()) {    
                                    	paths.add(name);
                                    	logger.info("fromJar: {}",name);
                                    }  
                                }  
                            }  
                        }  
                    } catch (IOException e) {
                         logger.error("鍦ㄦ壂鎻忕敤鎴峰畾涔夎鍥炬椂浠巎ar鍖呰幏鍙栨枃浠跺嚭閿�");  
                    }
                } else if ("bundleresource".equals(protocol)) {
                    //System.err.println("file绫诲瀷鐨勬壂鎻�");  
                   // String filePath = url.getFile(); 
                    //URL fileUrl = FileLocator.toFileURL(url);
                   // String filePath = URLDecoder.decode(fileUrl.getPath(), "UTF-8"); 
                    //findAndAddClassesInPackageByFile(pack, filePath,recursive, classes);  
                } 
            }  
        } catch (IOException e) {  
            logger.error("",e);
        }  
        return paths;
	}

	private static boolean match(String name, String patern) {
		
		boolean matchStart = false;
		boolean matchEnd = false;
		
		if(patern.startsWith("*")) {
			patern = patern.substring(1);
			matchEnd = true;
		}
		
		if(patern.endsWith("*")) {
			patern = patern.substring(0,patern.length()-1);
			matchStart = true;
		}
		
		if(matchStart && !name.startsWith(patern)) {
			return false;
		}
		
		if(matchEnd && !name.endsWith(patern)) {
			return false;
		}
		
		return true;
	}

	private static void fromDirectory(String packageName,File f, String endWith, List<String> paths) {
		//String filePath = f.getAbsolutePath();
		for(File fs : f.listFiles()) {
			if(fs.isDirectory()) {
				fromDirectory(packageName+"/"+fs.getName(),fs,endWith,paths);
			}else if(fs.isFile() && match(fs.getName(),endWith)) {
				String p = packageName+"/"+fs.getName();
	        	paths.add(p);
	        	logger.info("fromFile: {}",fs.getAbsoluteFile());
			} 
		}
	}  
    
}
