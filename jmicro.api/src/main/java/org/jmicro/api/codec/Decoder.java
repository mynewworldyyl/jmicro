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

import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:07
 */
public class Decoder implements IDecoder{
	
	public static final byte PREFIX_TYPE_BYTE = 1 << 0;
	public static final byte PREFIX_TYPE_STRING = 1 << 1;
	public static final byte PREFIX_TYPE_NULL = 1 << 2;

	private static Map<Integer,Class<?>> intToclazz = new HashMap<>();
	private static Map<Class<?>,Integer> clazzToInt = new HashMap<>();
	static int maxType=1;
	static {
		intToclazz.put(maxType++, Map.class);
		intToclazz.put(maxType++, Collection.class);
		intToclazz.put(maxType++, List.class);
		intToclazz.put(maxType++, Array.class);
		intToclazz.put(maxType++, Void.TYPE);
		intToclazz.put(maxType++, Byte.TYPE);
		intToclazz.put(maxType++, Short.TYPE);
		intToclazz.put(maxType++, Integer.TYPE);
		intToclazz.put(maxType++, Long.TYPE);
		intToclazz.put(maxType++, Double.TYPE);
		intToclazz.put(maxType++, Float.TYPE);
		intToclazz.put(maxType++, Boolean.TYPE);
		intToclazz.put(maxType++, Object.class);
		intToclazz.put(maxType++, String.class);
		
		maxType=1;
		clazzToInt.put(Map.class, maxType++);
		clazzToInt.put(Collection.class, maxType++);
		clazzToInt.put(List.class, maxType++);
		clazzToInt.put(Array.class, maxType++);
		clazzToInt.put(Void.TYPE, maxType++);
		clazzToInt.put(Byte.TYPE, maxType++);
		clazzToInt.put(Short.TYPE, maxType++);
		clazzToInt.put(Integer.TYPE, maxType++);
		clazzToInt.put(Long.TYPE, maxType++);
		clazzToInt.put(Double.TYPE, maxType++);
		clazzToInt.put(Float.TYPE, maxType++);
		clazzToInt.put(Boolean.TYPE, maxType++);
		clazzToInt.put(Object.class, maxType++);
		clazzToInt.put(String.class, maxType++);
	}
	
	public static void registType(Class<?> clazz){
		if(clazzToInt.containsKey(clazz)){
			return;
		}
		clazzToInt.put(clazz,maxType );
		intToclazz.put(maxType, clazz);
		maxType++;
	}
	
	public static int getType(Class<?> cls){

		if(cls == Void.TYPE || cls == Void.class) {
			return clazzToInt.get(Void.TYPE);
		}else if(cls == int.class || cls == Integer.TYPE || cls == Integer.class){
			return clazzToInt.get(Integer.TYPE);
		}else if(cls == byte.class || cls == Byte.TYPE || cls == Byte.class){
			return clazzToInt.get(Byte.TYPE);
		}else if(cls == short.class || cls == Short.TYPE || cls == Short.class){
			return clazzToInt.get(Short.TYPE);
		}else if(cls == long.class || cls == Long.TYPE || cls == Long.class){
			return clazzToInt.get(Long.TYPE);
		}else if(cls == float.class || cls == Float.TYPE || cls == Float.class){
			return clazzToInt.get(Float.TYPE);
		}else if(cls == double.class || cls == Double.TYPE || cls == Double.class){
			return clazzToInt.get(Double.TYPE);
		}else if(cls == boolean.class || cls == Boolean.TYPE || cls == Boolean.class){
			return clazzToInt.get(Boolean.TYPE);
		}else if(cls == char.class || cls == Character.TYPE || cls == Character.class){
			return clazzToInt.get(Character.TYPE);
		}else if(Map.class.isAssignableFrom(cls)){
			return clazzToInt.get(Map.class);
		}else if(Collection.class.isAssignableFrom(cls)){
			return clazzToInt.get(Collection.class);
		}else if(cls.isArray()){
			return clazzToInt.get(Array.class);
		}else if(cls == String.class) {
			return clazzToInt.get(String.class);
		}
	
		return 0;
	}
	
	public static Class<?> getClass(int type){
		return intToclazz.get(type);
	}
	
	@Override
	public RpcRequest decode(byte[] buffer) {
		//RpcRequest msg = new RpcRequest();
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		RpcRequest msg = decodeObject(bb);
		return msg;
	}
	
	public static <V> V decodeObject(ByteBuffer buffer){
		byte prefixCodeType = buffer.get();
		if( prefixCodeType == PREFIX_TYPE_NULL){
			return null;
		}
		
		int type = -1;
		Class<?> cls = null;
		
		if(PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			cls = ClassScannerUtils.getIns().getClassByName(clsName);
		}else if(PREFIX_TYPE_BYTE == prefixCodeType) {
			type = buffer.get();
			cls = intToclazz.get(type);
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
		}else if(cls == Array.class){
			v =  decodeObjects(buffer);
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
			v = decodeByReflect(buffer,cls,type);
		}
		
		return (V)v;
	}
	
	public static List<String>  sortFieldNames(Class cls) {
		List<String> fieldNames = new ArrayList<>();
		Field[] fs = cls.getDeclaredFields();
		for(Field f: fs){
			if(Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class){
				continue;
			}
			fieldNames.add(f.getName());
		}
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		return fieldNames;
	}
	
	private static Object decodeByReflect(ByteBuffer buffer,Class<?> cls,int type) {
		if(cls == null){
			String clsName = decodeString(buffer);
			if(StringUtils.isEmpty(clsName)){
				throw new CommonException("invalid class type: "+ clsName);
			}
			cls = ClassScannerUtils.getIns().getClassByName(clsName);
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
		
		List<String> fieldNames = sortFieldNames(cls);
		
		for(int i =0; i < fieldNames.size(); i++){
			try {
				Field f = cls.getDeclaredField(fieldNames.get(i));
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
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
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
	
	private static Object[] decodeObjects(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		Object[] objs = new Object[len];
		for(int i =0; i < len; i++){
			objs[i] = decodeObject(buffer);
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
	
	public static ByteBuffer readMessage(ByteBuffer src,ByteBuffer cache){
		//先把网络数据存起来，放到缓存中
		cache.put(src);
		
		//当前写的位置，也就是可读的数据长度
		int totalLen = cache.position();
		if(totalLen < Message.HEADER_LEN) {
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
		
		if(totalLen < len+Message.HEADER_LEN){
			//还不能构成一个足够长度的数据包
			return null;
		}
		
		//准备读数据
		cache.flip();
		
		ByteBuffer body = ByteBuffer.allocate(len+Message.HEADER_LEN);
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
