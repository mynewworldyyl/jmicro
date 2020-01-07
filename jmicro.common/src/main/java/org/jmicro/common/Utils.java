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
package org.jmicro.common;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.ReflectUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:00
 */
public class Utils {

	private static Utils ins = new Utils();

	private Utils() {
	}

	public static Utils getIns() {
		return ins;
	}
	
	public String toString(Object[] arr) {
		StringBuilder sb = new StringBuilder("[");
		for(Object o : arr) {
			if(o != null) {
				sb.append(o.toString()).append(",");
			}
		}
		return sb.substring(0, sb.length()-1).toString()+"]";
	}

	public void setClasses(Set<Class<?>> clses, Map<String, Class<?>> classMap) {
		Iterator<Class<?>> ite = clses.iterator();
		while (ite.hasNext()) {
			Class<?> c = ite.next();
			String key = c.getName();
			classMap.put(key, c);
		}
	}

	public String encode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public String decode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public void waitForShutdown(){
		 synchronized(Utils.ins){
			 try {
				 ins.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }
	}
	
	public void shutdown(){
		 synchronized(Utils.ins){
			 Utils.ins.notifyAll();
		 }
	}
	
	public List<String> getLocalIPList() {
        List<String> ipList = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress != null && inetAddress instanceof Inet4Address) { // IPV4
                        ip = inetAddress.getHostAddress();
                        if(ip.startsWith("127.")){
                        	continue;
                        }
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if(ipList.isEmpty()) {
        	ipList.add("127.0.0.1");
        }
        return ipList;
    }
	
	public void getMethods(List<Method> methods ,Class<?> clazz){
		Method[] ms = clazz.getDeclaredMethods();
		for(Method f: ms){
			if(Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class){
				continue;
			}
			methods.add(f);
		}
		
		if(clazz.getSuperclass() != Object.class) {
			getMethods(methods,clazz.getSuperclass());
		}
	}
	
	public void getFields(List<Field> fields ,Class<?> clazz){
		Field[] fs = clazz.getDeclaredFields();
		for(Field f: fs){
			if( Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class){
				continue;
			}
			fields.add(f);
		}
		
		if(clazz.getSuperclass() != Object.class) {
			getFields(fields,clazz.getSuperclass());
		}
	}

	public void  getFieldNames(List<String> fieldNames,Class cls) {
		if(cls == null) {
			return;
		}
		Field[] fs = cls.getDeclaredFields();
		for(Field f: fs){
			if(Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class){
				continue;
			}
			fieldNames.add(f.getName());
		}
		
		if(cls.getSuperclass() != Object.class) {
			getFieldNames(fieldNames,cls.getSuperclass());
		}
		
	}

	public Field getClassField(Class<?> cls, String fn) {
		Field f = null;
		try {
			 return cls.getDeclaredField(fn);
		} catch (NoSuchFieldException e) {
			cls = cls.getSuperclass();
			if(cls == Object.class) {
				return null;
			} else {
				return getClassField(cls,fn);
			}
		}
	}
	
	public Object getValue(Type type, String str,Type gt) {
		Class<?> cls = null;
		if(type instanceof Class){
			cls = (Class)type;
		}
		Object v = null;
		if(type == Boolean.TYPE || type == Boolean.class){
			v = Boolean.parseBoolean(str);
		}else if(type == Short.TYPE || type == Short.class){
			v = Short.parseShort(str);
		}else if(type == Integer.TYPE || type == Integer.class){
			v = Integer.parseInt(str);
		}else if(type == Long.TYPE || type == Long.class){
			v = Long.parseLong(str);
		}else if(type == Float.TYPE || type == Float.class){
			v = Float.parseFloat(str);
		}else if(type == Double.TYPE || type == Double.class){
			v = Double.parseDouble(str);
		}else if(type == Byte.TYPE || type == Byte.class){
			v = Byte.parseByte(str);
		}/*else if(type == Character.TYPE){
			v = Character(str);
		}*/else if(cls != null && cls.isArray()) {
			Class<?> ctype = ((Class)type).getComponentType();
			String[] elts = str.split(",");
			Object arr = Array.newInstance(ctype, elts.length);
			int i =0;
			for(int j = 0; j < elts.length; j++){
				Object vv = this.getValue(ctype, elts[j], gt);
				Array.set(arr, j, vv);
				
			}
			v = arr;
		}else if(cls != null && List.class.isAssignableFrom(cls)){
			Class<?> ctype = ((Class)type).getComponentType();
			String[] elts = str.split(",");
			List list = new ArrayList();
			for(String e: elts ){
				list.add(getValue(gt,e,gt));
			}
			v = list;
		}else if(cls != null && Collection.class.isAssignableFrom(cls)){
			String[] elts = str.split(",");
			Set set = new HashSet();
			for(String e: elts ){
				set.add(getValue(gt,e,null));
			}
			v = set;
		}else if(cls != null && Map.class.isAssignableFrom(cls)){
			String[] elts = str.split("&");
			Map<String,String> map = new HashMap<>();
			for(String e: elts ){
				String[] kv = e.split("=");
				map.put(kv[0], kv[1]);
			}
			v = map;
		} else if(cls == String.class){
			v = str;
		}else {
			v = JsonUtils.getIns().fromJson(str, type);
		}
		
		return v;
	}
	
	 public String asArgument(Class<?> cl, String name) {
	        if (cl.isPrimitive()) {
	            if (Boolean.TYPE == cl)
	                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
	            if (Byte.TYPE == cl)
	                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
	            if (Character.TYPE == cl)
	                return name + "==null?(char)0:((Character)" + name + ").charValue()";
	            if (Double.TYPE == cl)
	                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
	            if (Float.TYPE == cl)
	                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
	            if (Integer.TYPE == cl)
	                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
	            if (Long.TYPE == cl)
	                return name + "==null?(long)0:((Long)" + name + ").longValue()";
	            if (Short.TYPE == cl)
	                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
	            throw new RuntimeException(name + " is unknown primitive type.");
	        }
	        return "(" + ReflectUtils.getName(cl) + ")" + name;
	    }
	 
	 	public String defaultVal(Class<?> cl) {
	        if (cl.isPrimitive()) {
	            if (Boolean.TYPE == cl)
	                return "false";
	            if (Byte.TYPE == cl)
	            	 return "0";
	            if (Character.TYPE == cl)
	            	 return "' '";
	            if (Double.TYPE == cl)
	            	 return "0D";
	            if (Float.TYPE == cl)
	            	 return "0F";
	            if (Integer.TYPE == cl)
	            	 return "0";
	            if (Long.TYPE == cl)
	            	 return "0";
	            if (Short.TYPE == cl)
	            	 return "0";
	            throw new RuntimeException(cl.getName() + " is unknown primitive type.");
	        }
	        return "null";
	    }
	
}
