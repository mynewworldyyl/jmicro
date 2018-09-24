package org.jmicro.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jmicro.common.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassScannerUtils {

	private final static Logger logger = LoggerFactory.getLogger(ClassScannerUtils.class);
	
	private static final ClassScannerUtils instance = new ClassScannerUtils();
	private ClassScannerUtils() {}
	
	public static ClassScannerUtils getIns() {
		return instance;
	}
	
	interface Checker{
		boolean accept(Class<?> cls);
	}
	
	public Set<Class<?>> loadClassesByAnno(Class<? extends Annotation> annaCls){
		Config cfg = JMicroContext.get().getCfg();
		Set<Class<?>> clses = this.getClassesWithAnnotation(cfg.getBasePackages(), annaCls);
		return clses;
	}
	
	public Set<Class<?>> loadClassByClass(Class<?> parentCls){
		Config cfg = JMicroContext.get().getCfg();
		Set<Class<?>> clses = this.getClasses(cfg.getBasePackages(), parentCls);
		return clses;
	}
	
	public Set<Class<?>> getClasses(String []basePackages, Class<?> parentCls) {
		return this.getClasses(basePackages, c -> {
			return parentCls.isAssignableFrom(c);
		});
	}
	
	public Set<Class<?>> getClassesWithAnnotation(String []basePackages,final Class<? extends Annotation> annoCls) {
		return this.getClasses(basePackages, c -> {
			return c.isAnnotationPresent(annoCls);
		});
		
	}
	
	private Set<Class<?>> getClasses(String [] packs,Checker checker) {
		if(packs == null || packs.length == 0) {
			return Collections.EMPTY_SET;
		}
		Set<Class<?>> clses = new HashSet<Class<?>>();
		for(String p : packs) {
			Set<Class<?>> cset = this.getClasses(p.trim());
			if(cset != null && !cset.isEmpty()) {
				Iterator<Class<?>> ite = cset.iterator();
				while(ite.hasNext()){
					Class<?> c = ite.next();
					if(checker.accept(c)){
						clses.add(c);
					}	
				}
			}
		}
		return clses;
	}

	
	public Set<Class<?>> getClasses(String pack) {
		  
        // 绗竴涓猚lass绫荤殑闆嗗悎  
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();  
        // 鏄惁寰幆杩唬  
        boolean recursive = true;  
        // 鑾峰彇鍖呯殑鍚嶅瓧 骞惰繘琛屾浛鎹�  
        String packageName = pack;  
        String packageDirName = packageName.replace('.', '/');  
        // 瀹氫箟涓�涓灇涓剧殑闆嗗悎 骞惰繘琛屽惊鐜潵澶勭悊杩欎釜鐩綍涓嬬殑things  
        Enumeration<URL> dirs;  
        try {  
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);  
            // 寰幆杩唬涓嬪幓  
            while (dirs.hasMoreElements()) {  
                // 鑾峰彇涓嬩竴涓厓绱�  
                URL url = dirs.nextElement();  
                String f = url.getFile();
                // 寰楀埌鍗忚鐨勫悕绉�  
                String protocol = url.getProtocol();  
                // 濡傛灉鏄互鏂囦欢鐨勫舰寮忎繚瀛樺湪鏈嶅姟鍣ㄤ笂  
                if ("file".equals(protocol)) {
                    //System.err.println("file绫诲瀷鐨勬壂鎻�");  
                    // 鑾峰彇鍖呯殑鐗╃悊璺緞  
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");  
                    // 浠ユ枃浠剁殑鏂瑰紡鎵弿鏁翠釜鍖呬笅鐨勬枃浠� 骞舵坊鍔犲埌闆嗗悎涓�  
                    findAndAddClassesInPackageByFile(pack, filePath,recursive, classes);  
                } else if ("jar".equals(protocol)) {
                    // 濡傛灉鏄痡ar鍖呮枃浠�  
                    // 瀹氫箟涓�涓狫arFile  
                    //System.err.println("jar绫诲瀷鐨勬壂鎻�");  
                    JarFile jar;  
                    try {  
                        // 鑾峰彇jar  
                        jar = ((JarURLConnection) url.openConnection())  
                                .getJarFile();  
                        // 浠庢jar鍖� 寰楀埌涓�涓灇涓剧被  
                        Enumeration<JarEntry> entries = jar.entries();  
                        // 鍚屾牱鐨勮繘琛屽惊鐜凯浠�  
                        while (entries.hasMoreElements()) {  
                            // 鑾峰彇jar閲岀殑涓�涓疄浣� 鍙互鏄洰褰� 鍜屼竴浜沯ar鍖呴噷鐨勫叾浠栨枃浠� 濡侻ETA-INF绛夋枃浠�  
                            JarEntry entry = entries.nextElement();  
                            String name = entry.getName();  
                            // 濡傛灉鏄互/寮�澶寸殑  
                            if (name.charAt(0) == '/') {  
                                // 鑾峰彇鍚庨潰鐨勫瓧绗︿覆  
                                name = name.substring(1);  
                            }  
                            // 濡傛灉鍓嶅崐閮ㄥ垎鍜屽畾涔夌殑鍖呭悕鐩稿悓  
                            if (name.startsWith(packageDirName)) {  
                                int idx = name.lastIndexOf('/');  
                                // 濡傛灉浠�"/"缁撳熬 鏄竴涓寘  
                                if (idx != -1) {  
                                    // 鑾峰彇鍖呭悕 鎶�"/"鏇挎崲鎴�"."  
                                    packageName = name.substring(0, idx)  
                                            .replace('/', '.');  
                                }  
                                // 濡傛灉鍙互杩唬涓嬪幓 骞朵笖鏄竴涓寘  
                                if ((idx != -1) || recursive) {  
                                    // 濡傛灉鏄竴涓�.class鏂囦欢 鑰屼笖涓嶆槸鐩綍  
                                    if (name.endsWith(".class")  
                                            && !entry.isDirectory()) {  
                                        // 鍘绘帀鍚庨潰鐨�".class" 鑾峰彇鐪熸鐨勭被鍚�  
                                        String className = name.substring(  
                                                packageName.length() + 1, name  
                                                        .length() - 6);  
                                        try {  
                                            // 娣诲姞鍒癱lasses  
                                            classes.add(Class  
                                                    .forName(packageName + '.'  
                                                            + className));  
                                        } catch (ClassNotFoundException e) {  
                                            // log  
                                            // .error("娣诲姞鐢ㄦ埛鑷畾涔夎鍥剧被閿欒 鎵句笉鍒版绫荤殑.class鏂囦欢");  
                                            e.printStackTrace();  
                                        }  
                                    }  
                                }  
                            }  
                        }  
                    } catch (IOException e) {  
                        // log.error("鍦ㄦ壂鎻忕敤鎴峰畾涔夎鍥炬椂浠巎ar鍖呰幏鍙栨枃浠跺嚭閿�");  
                        e.printStackTrace();  
                    }  
                } else if ("bundleresource".equals(protocol)) {
                    //System.err.println("file绫诲瀷鐨勬壂鎻�");  
                    // 鑾峰彇鍖呯殑鐗╃悊璺緞  
                   // String filePath = url.getFile(); 
                    //URL fileUrl = FileLocator.toFileURL(url);
                   // String filePath = URLDecoder.decode(fileUrl.getPath(), "UTF-8"); 
                    // 浠ユ枃浠剁殑鏂瑰紡鎵弿鏁翠釜鍖呬笅鐨勬枃浠� 骞舵坊鍔犲埌闆嗗悎涓�  
                    //findAndAddClassesInPackageByFile(pack, filePath,recursive, classes);  
                } 
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
        return classes;  
    }  
	
    /** 
     * 浠ユ枃浠剁殑褰㈠紡鏉ヨ幏鍙栧寘涓嬬殑鎵�鏈塁lass 
     *  
     * @param packageName 
     * @param packagePath 
     * @param recursive 
     * @param classes 
     */  
    public void findAndAddClassesInPackageByFile(String packageName,  String packagePath, final boolean recursive, Set<Class<?>> classes) {
    	if(classes == null) {
    		throw new NullPointerException("classes can not be null");
    	}
        // 鑾峰彇姝ゅ寘鐨勭洰褰� 寤虹珛涓�涓狥ile  
        File dir = new File(packagePath);  
        // 濡傛灉涓嶅瓨鍦ㄦ垨鑰� 涔熶笉鏄洰褰曞氨鐩存帴杩斿洖  
        if (!dir.exists() || !dir.isDirectory()) {  
            // log.warn("鐢ㄦ埛瀹氫箟鍖呭悕 " + packageName + " 涓嬫病鏈変换浣曟枃浠�");  
            return;  
        }  
        // 濡傛灉瀛樺湪 灏辫幏鍙栧寘涓嬬殑鎵�鏈夋枃浠� 鍖呮嫭鐩綍  
        File[] dirfiles = dir.listFiles(new FileFilter() {  
            // 鑷畾涔夎繃婊よ鍒� 濡傛灉鍙互寰幆(鍖呭惈瀛愮洰褰�) 鎴栧垯鏄互.class缁撳熬鐨勬枃浠�(缂栬瘧濂界殑java绫绘枃浠�)  
            public boolean accept(File file) {  
                return (recursive && file.isDirectory())  || (file.getName().endsWith(".class"));  
            }  
        });  
        
        // 寰幆鎵�鏈夋枃浠�  
        for (File file : dirfiles) {  
            // 濡傛灉鏄洰褰� 鍒欑户缁壂鎻�  
        	
            if (file.isDirectory()) {  
                findAndAddClassesInPackageByFile(packageName + "."  + file.getName(), file.getAbsolutePath(), recursive,  classes);  
            } else {  
                // 濡傛灉鏄痡ava绫绘枃浠� 鍘绘帀鍚庨潰鐨�.class 鍙暀涓嬬被鍚�  
                String className = file.getName().substring(0,  file.getName().length() - 6);                 
                String cn = null;
                try {  
                    // 娣诲姞鍒伴泦鍚堜腑鍘�  
                    //classes.add(Class.forName(packageName + '.' + className));  
                     //缁忚繃鍥炲鍚屽鐨勬彁閱掞紝杩欓噷鐢╢orName鏈変竴浜涗笉濂斤紝浼氳Е鍙憇tatic鏂规硶锛屾病鏈変娇鐢╟lassLoader鐨刲oad骞插噣 
                	cn = packageName + '.' + className;
                	Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(cn);
                    classes.add(cls);    
                    } catch (ClassNotFoundException e) {  
	                    logger.error("ERROR: "+file.getAbsolutePath() +" for class " + cn,e);  
	                    //e.printStackTrace();  
	                    //logger.warn(e.getMessage());
                    }  catch (SecurityException e) {
                    	 logger.error("ERROR: "+file.getAbsolutePath() +" for class " + cn,e);  
                    }
            }  
        }  
    }
    
}
