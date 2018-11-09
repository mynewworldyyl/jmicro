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
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月8日 上午11:43:13
 */
@Component(value="onePrefixDecoder",lazy=false)
public class OnePrefixDecoder /*implements IDecoder*/{
	
	public  <V> V decode(ByteBuffer buffer) {
		Class<?> cls = this.getClazz(buffer);
		if(cls == null) {
			throw new CommonException("class not found: ");
		}
		
		Object obj = decodeObject(buffer,cls,null);
		return (V)obj;
	}
	
	private Class<?> getClazz(ByteBuffer buffer) {
		byte prefixCodeType = buffer.get();
		if( prefixCodeType == Decoder.PREFIX_TYPE_NULL){
			return null;
		}
		
		Short type = -1;
		Class<?> cls = null;
		
		if(Decoder.PREFIX_TYPE_STRING == prefixCodeType) {
			String clsName = decodeString(buffer);
			try {
				if(clsName.startsWith("[L")) {
					clsName = clsName.substring(2,clsName.length()-1);
					cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
					cls = Array.newInstance(cls, 0).getClass();
				}else {
					cls = Thread.currentThread().getContextClassLoader()
							.loadClass(clsName);
				}
				
			} catch (ClassNotFoundException e) {
				throw new CommonException("class not found:" + clsName,e);
			}
		}else if(Decoder.PREFIX_TYPE_SHORT == prefixCodeType) {
			type = buffer.getShort();
			cls = Decoder.getClass(type);
		} else {
			throw new CommonException("not support prefix type:" + prefixCodeType);
		}
		return cls;
	}

	private <V> V decodeObject(ByteBuffer buffer,Class<?> cls, ParameterizedType paramType){
		
		Object v = null;
	    if(OnePrefixTypeEncoder.isMap(cls)){
			v =  decodeMap(buffer,paramType);
		}else if(OnePrefixTypeEncoder.isCollection(cls)){
			v =  decodeList(buffer,paramType);
		}else if(OnePrefixTypeEncoder.isArray(cls)){
			Class<?> clazz = (Class<?>)cls;
			v =  decodeObjects(buffer,clazz.getComponentType());
		}else if(OnePrefixTypeEncoder.isByteBuffer(cls)){
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
			v = decodeByReflect(buffer,(Class<?>)cls);
		}
		
		return (V)v;
	}
	
	private Object decodeByteBuffer(ByteBuffer buffer) {
		int len = buffer.getInt();
		byte[] data = new byte[len];
		buffer.get(data, 0, len);
		return ByteBuffer.wrap(data);
	}
	
	private Object decodeByReflect(ByteBuffer buffer,Class<?> cls ) {
		
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
				
				Field f = Utils.getIns().getClassField(cls, fieldNames.get(i));
				Class<?> fileType = f.getType();
				
				if(!OnePrefixTypeEncoder.isFinal(fileType)) {
					fileType = this.getClazz(buffer);
				}else if(f.getType().isArray()) {
					fileType = this.getClazz(buffer);
				}
				
				Object v = null;
				if(f.getGenericType() instanceof ParameterizedType) {
					v = decodeObject(buffer,fileType,(ParameterizedType)f.getGenericType());
				} else {
					v = decodeObject(buffer,fileType,null);
				}
						
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


	private <V> List<V> decodeList(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		
	    Class<?> objType = OnePrefixTypeEncoder.finalParameterType(paramType,0);
		boolean keyFlag = OnePrefixTypeEncoder.isFinalParameterType(paramType,0);
		
		List<Object> objs = new ArrayList<>();
		
		for(int i =0; i <len; i++){
			if(!keyFlag) {
				objType = this.getClazz(buffer);
			}
			objs.add(decodeObject(buffer,objType,null));
		}
		return (List<V>)objs;
	}
	
	private Object decodeObjects(ByteBuffer buffer,Class<?> clazz){
		Class<?> eltType = this.getClazz(buffer);
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}
		
		boolean isFinal = Modifier.isFinal(eltType.getModifiers());
		
		Object objs = Array.newInstance(eltType, len);
		//Object[] objs = new Object[len];
		for(int i =0; i < len; i++){
			if(!isFinal) {
				eltType = this.getClazz(buffer);
			}
			Object o = decodeObject(buffer,eltType,null);
			Array.set(objs, i, o);
		}
		
		return objs;
	}
	
	private Map<Object,Object> decodeMap(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getInt();
		if(len <= 0) {
			return Collections.EMPTY_MAP;
		}
		
		Class<?> keyType = OnePrefixTypeEncoder.finalParameterType(paramType,0);
		Class<?> objType = OnePrefixTypeEncoder.finalParameterType(paramType,1);
		
		boolean keyFlag = OnePrefixTypeEncoder.isFinalParameterType(paramType,0);
		boolean valueFlag = OnePrefixTypeEncoder.isFinalParameterType(paramType,1);
		
		Map<Object,Object> map = new HashMap<>();
		for(; len > 0; len--) {
			if(!keyFlag) {
				keyType = this.getClazz(buffer);
			}
			Object key = decodeObject(buffer,keyType,null);
			if(!valueFlag) {
				objType = this.getClazz(buffer);
			}
			Object obj = decodeObject(buffer,objType,null);
			map.put(key, obj);
		}
		
		return map;
	}
		
	private String decodeString(ByteBuffer buffer){
		int len = buffer.getInt();
		if(len <= 0) {
			return null;
		}

		try {
			byte[] data = new byte[len];
			buffer.get(data,0,len);
			return new String(data,Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
}
