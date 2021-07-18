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
package cn.jmicro.api.codec;

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

import cn.jmicro.api.gateway.ApiRequestJRso;
import cn.jmicro.api.gateway.ApiResponseJRso;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:07
 */
public class Decoder {
	
	/*public static byte PREFIX_TYPE_ID = -128;
	//空值编码
	public static final byte PREFIX_TYPE_NULL = PREFIX_TYPE_ID++;
	
	//FINAL
	public static final byte PREFIX_TYPE_FINAL = PREFIX_TYPE_ID++;
		
	//类型编码写入编码中
	public static final byte PREFIX_TYPE_SHORT = PREFIX_TYPE_ID++;
	//全限定类名作为前缀串写入编码中
	public static final byte PREFIX_TYPE_STRING = PREFIX_TYPE_ID++;
	
	//以下对高使用频率非final类做快捷编码
	
	//列表类型编码，指示接下业读取一个列表，取列表编码器直接解码
	public static final byte PREFIX_TYPE_LIST = PREFIX_TYPE_ID++;
	//集合类型编码，指示接下来读取一个集合，取SET编码器直接解码
	public static final byte PREFIX_TYPE_SET = PREFIX_TYPE_ID++;
	//Map类型编码，指示接下来读取一个Map，取Map编码器直接解码
	public static final byte PREFIX_TYPE_MAP = PREFIX_TYPE_ID++;
	
	public static final byte PREFIX_TYPE_BYTE = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_SHORTT = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_INT = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_LONG = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_FLOAT = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_DOUBLE = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_CHAR = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_BOOLEAN = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_STRINGG = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_DATE = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_BYTEBUFFER = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_REQUEST = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_RESPONSE = PREFIX_TYPE_ID++;
	public static final byte PREFIX_TYPE_PROXY = PREFIX_TYPE_ID++;*/
	
	//public static final byte PREFIX_TYPE_STRING = PREFIX_TYPE_ID--;
	static {
		checkPrefix(DecoderConstant.PREFIX_TYPE_ID);
	}
	
	public static final Short NON_ENCODE_TYPE = 0;
	public static final byte NULL_VALUE = 0;
	public static final byte NON_NULL_VALUE = 1;

	private static Map<Short,Class<?>> Short2Clazz = new HashMap<>();
	private static Map<Class<?>,Short> clazz2Short = new HashMap<>();
	
	//static Short currentTypeCode = (short)(NON_ENCODE_TYPE + 1);
	
   private static final void checkPrefix(byte prefix) {
	  if((byte)(prefix) == 127) {
		  throw new CommonException("Prefix value overflow");
	  }
   }
	
   static {
	    short type = (short)0xFFFF;
		registType(Map.class,type--);
		registType(Collection.class,type--);
		registType(List.class,type--);
		//registType(Array.class,type--);
		registType(Void.class,type--);
		registType(Short.class,type--);
		registType(Integer.class,type--);
		registType(Long.class,type--);
		registType(Double.class,type--);
		registType(Float.class,type--);
		registType(Boolean.class,type--);
		registType(Character.class,type--);
		registType(Object.class,type--);
		registType(String.class,type--);
		registType(ByteBuffer.class,type--);
		registType(Message.class,type--);
		//registType(RpcRequest.class,type--);
		//registType(RpcResponse.class,type--);
		registType(ApiRequestJRso.class,type--);
		registType(ApiResponseJRso.class,type--);
		//registType(MRpcItem.class,type--);
		registType(java.util.Date.class,type--);
		registType(java.sql.Date.class,type--);
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
		}else {
			Short t = clazz2Short.get(cls);
			if(t==null) {
				 //无类型编码
				t = NON_ENCODE_TYPE;
			}
			return t;
		}
	   
	}
	
	public static Class<?> getClass(Short type){
		Class<?> clazz = Short2Clazz.get(type);
		/*if(clazz == null && type > 0) {
			 System.out.println(type);
			 clazz = getClassByProvider(type);
			 registType(clazz,type);
		}*/
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
		if( prefixCodeType == DecoderConstant.PREFIX_TYPE_NULL){
			return null;
		}
		
		Short type = -1;
		Class<?> cls = null;
		
		if(DecoderConstant.PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
		}else if(DecoderConstant.PREFIX_TYPE_SHORT == prefixCodeType) {
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
			if(Utils.isEmpty(clsName)){
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
		if( prefixCodeType == DecoderConstant.PREFIX_TYPE_NULL){
			return null;
		}
		
		Short type = -1;
		Class<?> cls = null;
		
		if(DecoderConstant.PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			try {
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
		}else if(DecoderConstant.PREFIX_TYPE_SHORT == prefixCodeType) {
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

}
