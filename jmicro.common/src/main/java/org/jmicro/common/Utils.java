package org.jmicro.common;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jmicro.common.config.Config;

public class Utils {
	
	private static Utils ins = new Utils();
	private Utils() {}
	
	public static Utils getIns(){
		return ins;
	}
	
	public void setClasses(Set<Class<?>> clses,Map<String,Class<?>> classMap) {
		Iterator<Class<?>> ite = clses.iterator();
		while(ite.hasNext()){
			Class<?> c = ite.next();
			String key = c.getName();
			classMap.put(key, c);
		}	
	}
	
}
