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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月8日 上午11:43:13
 */
@Component(value="onePrefixDecoder",lazy=false)
public class OnePrefixDecoder /*implements IDecoder*/{
	
	private static final Logger logger = LoggerFactory.getLogger(OnePrefixDecoder.class);
	
	@SuppressWarnings("unchecked")
	public <V> V decode(ByteBuffer buffer) {
		Class<?> cls = this.getType(buffer);
		if(cls == Void.class || cls == null) {
			return null;
		}
		
		if(TypeUtils.isMap(cls)){
			/* TypeVariable[] typeVars = cls.getTypeParameters();
			 Class<?> keyType = typeVars[0].getClass();
			 Class<?> valueType = typeVars[1].getClass();*/
			return (V)this.decodeMap(buffer, null);
		}else if(TypeUtils.isCollection(cls)){
			/* Type type = cls.getGenericSuperclass();
			 Class<?> valueType = finalParameterType((ParameterizedType)type,0);*/
			return (V)this.decodeList(buffer, null);
		}else {
			if(!TypeUtils.isFinal(cls)) {
				throw new CommonException("class {} must by final class for encode",cls.getName());
			}
			if(cls.isArray()) {
				Class<?> eltType = this.getType(buffer);
				return (V)this.decodeObjects(buffer, eltType);
			}else {
				return decodeObject(buffer,cls,null);
			}
		}
	}
	
	private <V> V decodeObject(ByteBuffer buffer,Class<?> cls, ParameterizedType paramType){
		
		Object v = null;
		
	    if(TypeUtils.isMap(cls)){
			v =  decodeMap(buffer,paramType);
		}else if(TypeUtils.isCollection(cls)){
			v =  decodeList(buffer,paramType);
		}else if(TypeUtils.isArray(cls)){
			v =  decodeObjects(buffer,cls.getComponentType());
		}else if(TypeUtils.isByteBuffer(cls)){
			v =  decodeByteBuffer(buffer);
		}else if(cls == String.class) {
			v =  decodeString(buffer);
		}else if(TypeUtils.isVoid(cls)) {
			v =  null;
		}else if(TypeUtils.isInt(cls)){
			v =  buffer.getInt();
		}else if(TypeUtils.isByte(cls)){
			v =  buffer.get();
		}else if(TypeUtils.isShort(cls)){
			v =  buffer.getShort();
		}else if(TypeUtils.isLong(cls)){
			v =  buffer.getLong();
		}else if(TypeUtils.isFloat(cls)){
			v = buffer.getFloat();
		}else if(TypeUtils.isDouble(cls)){
			v = buffer.getDouble();
		}else if(TypeUtils.isBoolean(cls)){
			v = buffer.get() == 1;
		}else if(TypeUtils.isChar(cls)){
			v = buffer.getChar();
		}else if(TypeUtils.isDate(cls)){
			v = new Date(buffer.getLong());
		} else {
			v = decodeByReflect(buffer,(Class<?>)cls);
		}
		
		return (V)v;
	}
	
	private Object decodeByteBuffer(ByteBuffer buffer) {
		int len = buffer.getShort();
		byte[] data = new byte[len];
		buffer.get(data, 0, len);
		return ByteBuffer.wrap(data);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object decodeByReflect(ByteBuffer buffer, Class<?> cls) {
		
		int m = cls.getModifiers();
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m) || !Modifier.isPublic(m)){
			logger.warn("decodeByReflect class [{}] not support decode",cls.getName());
			throw new CommonException("invalid class modifier: "+ cls.getName());
		}
		
		byte vt = buffer.get();
		if(vt == Decoder.NULL_VALUE) {
			if(logger.isDebugEnabled()) {
				logger.debug("decodeByReflect get NULL Value {}",cls.getName());
			}
			return null;
		}
		
		if(logger.isDebugEnabled() && needLog()) {
			//ServiceMethod sm = this.getMethod();
			//logger.debug("method: {}",sm.getKey().toKey(true, true, false));
			//logger.debug("decodeByReflect class {}",cls.getName());
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

			Field f = Utils.getIns().getClassField(cls, fieldNames.get(i));

			Object v = null;
			Class<?> valueType = null;
			
			if(!TypeUtils.isFinal(f.getType())) {
				valueType = this.getType(buffer);
				if(valueType == Void.class) {
					continue;
				}
			} else {
				valueType = f.getType();
			}
			
			if(TypeUtils.isMap(valueType)){
				v = this.decodeMap(buffer,(ParameterizedType)f.getGenericType());
				Map map = (Map)TypeUtils.getFieldValue(obj, f);
				if(v == null) {
					continue;
				}
				if(map == null) {
					TypeUtils.setFieldValue(obj, v, f);
				}else {
					map.putAll((Map)v);
				}
				
			}else if(TypeUtils.isCollection(valueType)){
				v = this.decodeList(buffer, (ParameterizedType)f.getGenericType());
				if(v == null) {
					continue;
				}
				Collection coll = (Collection)TypeUtils.getFieldValue(obj, f);
				if(coll == null) {
					TypeUtils.setFieldValue(obj, v, f);
				}else {
					coll.addAll((Collection)v);
				}
			} else {
				if(TypeUtils.isArray(f.getType())) {
					Class<?> eltType = this.getType(buffer);
					if(eltType == Void.class) {
						continue;
					}
					v = decodeObjects(buffer,eltType);
				}else {
					v = decodeObject(buffer,valueType,null);
				}
				
				if(logger.isDebugEnabled() && needLog()) {
					logger.debug("type {} : {}={}",valueType.getName(),f.getName(),v == null ? "null":v.toString());
				}
				
				TypeUtils.setFieldValue(obj, v, f);
			}
		}
		return obj;
	}


	private boolean needLog() {
		ServiceMethod sm = this.getMethod();
		return sm != null && "intrest".equals(sm.getKey().getMethod());
	}

	private <V> List<V> decodeList(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getShort();
		if(len <= 0) {
			return null;
		}
		
	    Class<?> objType = TypeUtils.finalParameterType(paramType,0);
		boolean keyFlag = TypeUtils.isFinalParameterType(paramType,0);
		
		List<Object> objs = new ArrayList<>();
		
		for(int i =0; i <len; i++){
			if(!keyFlag) {
				objType = this.getType(buffer);
			}
			Object obj = null;
			if(objType != Void.class) {
				obj = decodeObject(buffer,objType,null);
			}
			objs.add(obj);
		}
		return (List<V>)objs;
	}
	
	private Object decodeObjects(ByteBuffer buffer,Class<?> eltType){

		int len = buffer.getShort();
		if(len <= 0) {
			return null;
		}
		
		boolean isFinal = Modifier.isFinal(eltType.getModifiers());
		
		Object objs = Array.newInstance(eltType, len);
		
		//Object[] objs = new Object[len];
		for(int i = 0; i < len; i++){
			if(!isFinal) {
				eltType = this.getType(buffer);
			}
			
			Object obj = null;
			if(eltType != Void.class) {
				
				obj = decodeObject(buffer,eltType,null);
			}
			Array.set(objs, i, obj);
		}
		
		return objs;
	}
	
	private Map<Object,Object> decodeMap(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getShort();
		if(len <= 0) {
			return Collections.EMPTY_MAP;
		}
		
		Class<?> keyType = TypeUtils.finalParameterType(paramType,0);
		Class<?> objType = TypeUtils.finalParameterType(paramType,1);
		
		boolean keyFlag = TypeUtils.isFinalParameterType(paramType,0);
		boolean valueFlag = TypeUtils.isFinalParameterType(paramType,1);
		
		Map<Object,Object> map = new HashMap<>();
		for(; len > 0; len--) {
			if(!keyFlag) {
				keyType = this.getType(buffer);
			}
			Object key = null;
			if(keyType != Void.class) {
				key = decodeObject(buffer,keyType,null);
			}
			
			if(!valueFlag) {
				objType = this.getType(buffer);
			}
			
			Object obj = null;
			if(objType != Void.class) {
				obj = decodeObject(buffer,objType,null);
			}
			map.put(key, obj);
		}
		
		return map;
	}
		
	public static String decodeString(ByteBuffer buffer){
		int len = buffer.getShort();
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
	private Class<?> getType(ByteBuffer buffer) {
		byte prefixCodeType = buffer.get();
		if(prefixCodeType == Decoder.PREFIX_TYPE_NULL){
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
					
					ServiceMethod sm = this.getMethod();
					/*if(sm != null && "intrest".equals(sm.getKey().getMethod())) {
						logger.debug("eltType: {}",clsName);
					}*/
					
				} else {
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
	
	
	private ServiceMethod getMethod() {
		return JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY,null);
	}
}
