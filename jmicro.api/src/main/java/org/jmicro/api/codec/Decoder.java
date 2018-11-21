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
package org.jmicro.api.codec;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:07
 */
public class Decoder {
	
	public static final byte PREFIX_TYPE_SHORT = 1 << 0;
	public static final byte PREFIX_TYPE_STRING = 1 << 1;
	public static final byte PREFIX_TYPE_NULL = 1 << 2;
	
	public static final Short NON_ENCODE_TYPE = 0;

	private static Map<Short,Class<?>> Short2Clazz = new HashMap<>();
	private static Map<Class<?>,Short> clazz2Short = new HashMap<>();
	
	//static Short currentTypeCode = (short)(NON_ENCODE_TYPE + 1);
	
   private static IClientTransformClassLoader clazzLoader = null;
	
   static {
	    short type = (short)0xFFFF;
		registType(Map.class,type++);
		registType(Collection.class,type++);
		registType(List.class,type++);
		//registType(Array.class,type++);
		registType(Void.class,type++);
		registType(Short.class,type++);
		registType(Integer.class,type++);
		registType(Long.class,type++);
		registType(Double.class,type++);
		registType(Float.class,type++);
		registType(Boolean.class,type++);
		registType(Character.class,type++);
		registType(Object.class,type++);
		registType(String.class,type++);
		registType(ByteBuffer.class,type++);
		registType(Message.class,type++);
		registType(RpcRequest.class,type++);
		registType(RpcResponse.class,type++);
		registType(ApiRequest.class,type++);
		registType(ApiResponse.class,type++);
		
   }
   
   public static void setTransformClazzLoader(IClientTransformClassLoader l) {
	   if(clazzLoader != null) {
		   throw new CommonException(clazzLoader.getClass().getName()+" have been set before "+l.getClass().getName());
	   }
	   clazzLoader = l;
   }
   
    public static Class<?> getClassByProvider(Short type) {
		if(clazzLoader != null) {
			return clazzLoader.getClazz(type);
		}
		return null;
	}
	
	public static void registType(Class<?> clazz,Short type){
		if(clazz2Short.containsKey(clazz)){
			return;
		}
		clazz2Short.put(clazz,type );
		Short2Clazz.put(type, clazz);
	}
	
	 static Short getType(Class<?> cls){

		if(cls == Void.TYPE || cls == Void.class) {
			return clazz2Short.get(Void.class);
		}else if(cls == int.class || cls == Integer.TYPE || cls == Integer.class){
			return clazz2Short.get(Integer.class);
		}else if(cls == byte.class || cls == Byte.TYPE || cls == Byte.class){
			return clazz2Short.get(Byte.class);
		}else if(cls == short.class || cls == Short.TYPE || cls == Short.class){
			return clazz2Short.get(Short.class);
		}else if(cls == long.class || cls == Long.TYPE || cls == Long.class){
			return clazz2Short.get(Long.class);
		}else if(cls == float.class || cls == Float.TYPE || cls == Float.class){
			return clazz2Short.get(Float.class);
		}else if(cls == double.class || cls == Double.TYPE || cls == Double.class){
			return clazz2Short.get(Double.class);
		}else if(cls == boolean.class || cls == Boolean.TYPE || cls == Boolean.class){
			return clazz2Short.get(Boolean.class);
		}else if(cls == char.class || cls == Character.TYPE || cls == Character.class){
			return clazz2Short.get(Character.class);
		}else if(Map.class.isAssignableFrom(cls)){
			return clazz2Short.get(Map.class);
		}else if(Collection.class.isAssignableFrom(cls)){
			return clazz2Short.get(Collection.class);
		}else if(cls.isArray()){
			return clazz2Short.get(Array.class);
		}else if(cls == String.class) {
			return clazz2Short.get(String.class);
		}else if(cls == ByteBuffer.class) {
			return clazz2Short.get(ByteBuffer.class);
		}
	    //无类型编码
		return NON_ENCODE_TYPE;
	}
	
	public static Class<?> getClass(Short type){
		Class<?> clazz = Short2Clazz.get(type);
		if(clazz == null && type > 0) {
			System.out.println(type);
			 clazz = getClassByProvider(type);
			 registType(clazz,type);
		}
		return clazz;
	}
	
	/*public <T> T decode(byte[] buffer) {
		//RpcRequest msg = new RpcRequest();
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		T msg = decodeObject(bb);
		return msg;
	}*/
	
	public static <V> V decodeObject(ByteBuffer buffer){
		byte prefixCodeType = buffer.get();
		if( prefixCodeType == PREFIX_TYPE_NULL){
			return null;
		}
		
		Short type = -1;
		Class<?> cls = null;
		
		if(PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
		}else if(PREFIX_TYPE_SHORT == prefixCodeType) {
			type = buffer.getShort();
			cls = getClass(type);
		}else {
			throw new CommonException("not support prefix type:" + prefixCodeType);
		}
		
		if(cls == null) {
			throw new CommonException("class not found: ");
		}
		
		Object v = null;
		if(Map.class == cls){
			v =  decodeMap(buffer);
		}else if(Collection.class == cls){
			v =  decodeList(buffer);
		}else if(cls.isArray() || Array.class == cls){
			v =  decodeObjects(buffer);
		}else if(cls == ByteBuffer.class){
			v =  decodeByteBuffer(buffer);
		}else if(cls == String.class) {
			v =  decodeString(buffer);
		}else if(cls == void.class || cls == Void.TYPE || cls == Void.class) {
			v =  null;
		}else if(cls == int.class || cls == Integer.TYPE || cls == Integer.class){
			v =  buffer.getInt();
		}else if(cls == byte.class || cls == Byte.TYPE || cls == Byte.class){
			v =  buffer.get();
		}else if(cls == short.class || cls == Short.TYPE || cls == Short.class){
			v =  buffer.getShort();
		}else if(cls == long.class || cls == Long.TYPE || cls == Long.class){
			v =  buffer.getLong();
		}else if(cls == float.class || cls == Float.TYPE || cls == Float.class){
			v = buffer.getFloat();
		}else if(cls == double.class || cls == Double.TYPE || cls == Double.class){
			v = buffer.getDouble();
		}else if(cls == boolean.class || cls == Boolean.TYPE || cls == Boolean.class){
			v = buffer.get() == 1;
		}else if(cls == char.class || cls == Character.TYPE || cls == Character.class){
			v = buffer.getChar();
		} else {	
			v = decodeByReflect(buffer,cls);
		}
		
		return (V)v;
	}
	
	private static Object decodeByteBuffer(ByteBuffer buffer) {
		int len = buffer.getInt();
		byte[] data = new byte[len];
		buffer.get(data, 0, len);
		return ByteBuffer.wrap(data);
	}

	
	
	private static Object decodeByReflect(ByteBuffer buffer,Class<?> cls) {
		if(cls == null){
			String clsName = decodeString(buffer);
			if(StringUtils.isEmpty(clsName)){
				throw new CommonException("invalid class type: "+ clsName);
			}
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
			
			if(cls == null) {
				throw new CommonException("invalid class type: "+ clsName);
			}
		}
		
		int m = cls.getModifiers() ;
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m) || !Modifier.isPublic(m)){
			throw new CommonException("invalid class modifier: "+ cls.getName());
		}
		
		Object obj = null;
		try {
			obj = cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			throw new CommonException("fail to instance class [" +cls.getName()+"]",e1);
		}
		
		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames,cls);
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		
		for(int i =0; i < fieldNames.size(); i++){
			try {
				
				Field f = Utils.getIns().getClassField(cls, fieldNames.get(i));// cls.getDeclaredField(fieldNames.get(i));
				
				Object v = decodeObject(buffer);
				boolean bf = f.isAccessible();
				if(!bf){
					f.setAccessible(true);
				}
				if(v != null){
					f.set(obj, v);
				}
				
				if(!bf){
					f.setAccessible(false);
				}
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
		}
		return obj;
	}


	private static <V> List<V> decodeList(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		List<Object> objs = new ArrayList<>();
		for(int i =0; i <len; i++){
			objs.add(decodeObject(buffer));
		}
		return (List<V>)objs;
	}
	
	private static Object decodeObjects(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		
		byte prefixCodeType = buffer.get();
		if( prefixCodeType == PREFIX_TYPE_NULL){
			return null;
		}
		
		Short type = -1;
		Class<?> cls = null;
		
		if(PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
		}else if(PREFIX_TYPE_SHORT == prefixCodeType) {
			type = buffer.getShort();
			cls = Short2Clazz.get(type);
		}else {
			throw new CommonException("not support prefix type:" + prefixCodeType);
		}
		
		if(cls == null) {
			throw new CommonException("class not found: ");
		}
		
		Object objs = Array.newInstance(cls, len);
		//Object[] objs = new Object[len];
		for(int i =0; i < len; i++){
			Object o = decodeObject(buffer);
			Array.set(objs, i, o);
		}
		
		return objs;
	}
	
	private static Map<Object,Object> decodeMap(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return Collections.EMPTY_MAP;
		}

		Map<Object,Object> map = new HashMap<>();
		for(; len > 0; len--) {
			Object key = Decoder.decodeObject(buffer);
			Object obj = Decoder.decodeObject(buffer);
			map.put(key, obj);
		}
		return map;
	}
	
	
	private static String decodeString(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		/*char[] data = new char[len];
		for(int i=0; i < len;i++){
			data[i] = buffer.getChar(i);
		}*/
		try {
			byte[] data = new byte[len];
			buffer.get(data,0,len);
			return new String(data,Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public static ByteBuffer readMessage(ByteBuffer cache){
		
		//当前写的位置，也就是可读的数据长度
		int totalLen = cache.position();
		if(totalLen < Constants.HEADER_LEN) {
			//可读的数据长度小于头部长度
			return null;
		}
		
		//保存写数据位置
		int pos = cache.position();
		cache.position(0);
		//读数据长度
		int len = cache.getInt();
		//还原写数据公位置
		cache.position(pos);
		
		if(totalLen < len+Constants.HEADER_LEN){
			//还不能构成一个足够长度的数据包
			return null;
		}
		
		//准备读数据
		cache.flip();
		
		ByteBuffer body = ByteBuffer.allocate(len+Constants.HEADER_LEN);
		body.put(cache);
		body.flip();
		
		//准备下一次读
		/**
		  System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
	      position(remaining());
	      limit(capacity());
	      discardMark();
	      return this;
		 */
		//将剩余数移移到缓存开始位置，position定位在数据长度位置，处于写状态
		cache.compact();
		//b.position(b.limit());
		//cache.limit(cache.capacity());
		
		return body;
	}
}
