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
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
/**
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:25
 */
@Component(value="onePrefixTypeEncoder",lazy=false)
public class OnePrefixTypeEncoder implements IEncoder<ByteBuffer>{

	@Cfg(value="/OnePrefixTypeEncoder")
	private int encodeBufferSize = 4096;
	
	@Override
	public ByteBuffer encode(Object obj) {
		if(obj == null) {
			throw new CommonException("Encode must not be NULL");
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(encodeBufferSize);
		putType(buffer,obj.getClass());
		
		Class<?> cls = obj.getClass();
		
		if(isMap(obj.getClass())){
			/* TypeVariable[] typeVars = cls.getTypeParameters();
			 Class<?> keyType = typeVars[0].getClass();
			 Class<?> valueType = typeVars[1].getClass();*/
			 encodeMap(buffer,(Map<Object,Object>)obj,null,null);
		}else if(isCollection(obj.getClass())){
			/* Type type = cls.getGenericSuperclass();
			 Class<?> valueType = finalParameterType((ParameterizedType)type,0);*/
			 encodeList(buffer,(Collection<?>)obj,null);
		}else {
			if(!isFinal(obj.getClass())) {
				throw new CommonException("class {} must by final class for encode",obj.getClass().getName());
			}
			if(isArray(obj.getClass())) {
				//数组需要写入元素类型，用于数组创建
				this.putType(buffer, obj.getClass().getComponentType());
			}
			encodeObject(buffer,obj,obj.getClass());
		}
		return buffer;
	}
	
	public Class<?> putType(ByteBuffer buffer,Class<?> cls) {
        Short type = Decoder.getType(cls);
        
		if(ByteBuffer.class.isAssignableFrom(cls)){
			cls = ByteBuffer.class;
		}
		
		if(isMap(cls)) {
			cls = Map.class;
		}else if(isCollection(cls)) {
			cls = Collection.class;
		}else if(isByteBuffer(cls)) {
			cls = Map.class;
		}/*else if(cls.isArray()) {
			cls = Array.class;
		}*/
		
		if(type == null || type == Decoder.NON_ENCODE_TYPE ) {
			buffer.put(Decoder.PREFIX_TYPE_STRING);
			encodeString(buffer,cls.getName());
		} else {
			cls = Decoder.getClass(type);
			buffer.put(Decoder.PREFIX_TYPE_SHORT);
			buffer.putShort(type);
		}
		return cls;
	}

	private <V> void encodeObject(ByteBuffer buffer,V obj, Class<?> genericType){
	
		Class<?> cls = null;
		if( obj == null ) {
			cls = genericType;
		} else {
			cls = obj.getClass();
		}
		
		if(isByteBuffer(cls)){
			 encodeByteBuffer(buffer,(ByteBuffer)obj);
		}else if(isArray(cls)){
			encodeObjects(buffer,obj);
		}else if(cls == String.class) {
			encodeString(buffer,(String)obj);
		}else if(cls == void.class || cls == Void.class  || cls == Void.TYPE) {
		
		}else if(cls == int.class || cls == Integer.class || cls == Integer.TYPE){
			if(obj == null) {
				buffer.putInt(0);
			}else {
				buffer.putInt((Integer)obj);
			}
		}else if(cls == byte.class || cls == Byte.class || cls == Byte.TYPE){
			if(obj == null) {
				buffer.put((byte)0);
			}else {
				buffer.put((Byte)obj);
			}
		}else if(cls == short.class || cls == Short.class || cls == Short.TYPE){
			if(obj == null) {
				buffer.putShort((short)0);
			}else {
				buffer.putShort((Short)obj);
			}
		}else if(cls == long.class || cls == Long.class || cls == Long.TYPE){
			if(obj == null) {
				buffer.putLong(0);
			}else {
				buffer.putLong((Long)obj);
			}
		}else if(cls == float.class || cls == Float.class || cls == Float.TYPE){
			if(obj == null) {
				buffer.putFloat(0);
			}else {
				buffer.putFloat((Float)obj);
			}
		}else if(cls == double.class || cls == Double.class || cls == Double.TYPE){
			if(obj == null) {
				buffer.putDouble(0);
			}else {
				buffer.putDouble((Double)obj);
			}
		}else if(cls == boolean.class || cls == Boolean.class || cls == Boolean.TYPE){
			if(obj == null) {
				buffer.put((byte)0);
			}else {
				boolean b = (Boolean)obj;
				buffer.put(b?(byte)1:(byte)0);
			}
		}else if(cls == char.class || cls == Character.class || cls == Character.TYPE){
			if(obj == null) {
				buffer.putChar((char)0);
			}else {
				buffer.putChar((Character)obj);
			}
		} else {
			encodeByReflect(buffer,cls,obj);
		}
	

	}
	
	private void encodeByteBuffer(ByteBuffer buffer, ByteBuffer obj) {
		buffer.putInt(obj.remaining());
		buffer.put(obj);
	}

	private void encodeByReflect(ByteBuffer buffer, Class<?> cls, Object obj) {
		
		if(!Modifier.isPublic(cls.getModifiers())) {
			throw new CommonException("should be public class [" +cls.getName()+"]");
		}
		
		if(!isFinal(cls)) {
			//不能从泛型参数拿到类型信息，需要把类型参数写到buffer中
			cls = obj.getClass();
			this.putType(buffer, cls);
		}
		
		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames,cls);
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		
		for(int i = 0; i < fieldNames.size(); i++){
			try {
				String fn = fieldNames.get(i);
				
				Field f = Utils.getIns().getClassField(cls,fn);
				
				boolean bf = f.isAccessible();
				if(!bf){
					f.setAccessible(true);
				}
				Object v = f.get(obj);
				if(!bf){
					f.setAccessible(false);
				}
				
				//不能根据字段类型判断值的类型做系列化，写入值的类型
				if(v.getClass().isArray()) {
					//数组需要写入元素类型，用于数组创建
					this.putType(buffer, v.getClass());
					this.putType(buffer, v.getClass().getComponentType());
				}else if(!isFinal(f.getType())) {
					this.putType(buffer, v.getClass());
				}
				
				if(isMap(f.getType())){
					 Class<?> keyType = finalParameterType((ParameterizedType)f.getGenericType(),0);
					 Class<?> valueType = finalParameterType((ParameterizedType)f.getGenericType(),1);
					 encodeMap(buffer,(Map<Object,Object>)v,keyType,valueType);
				}else if(isCollection(f.getType())){
					 Class<?> valueType = finalParameterType((ParameterizedType)f.getGenericType(),0);
					 encodeList(buffer,(Collection)v,valueType);
				}else {
					encodeObject(buffer,v,f.getType());
				}
				
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
		}
	}

	private void encodeList(ByteBuffer buffer, Collection<?> objs,Class<?> paramType) {
		if(objs == null) {
			buffer.putInt(0);
			return;
		}
		
		boolean flag = isFinalParameterType(paramType,0);
		buffer.putInt(objs.size());
		for(Object o: objs){
			if(!flag) {
				this.putType(buffer, o.getClass());
			}
			encodeObject(buffer,o,null);
		}
		
	}
	
   public static boolean isFinalParameterType(Type genericType,int index) {
	   boolean flag = false;
	   if(genericType == null) {
		   return false;
	   }
	   Class<?> type = finalParameterType(genericType,index);
	   if(type != null) {
		   flag = isFinal(type);
	   }
	   return flag;
	}
   
   public static Class<?> finalParameterType(Type genericType,int index) {
	   if(genericType == null) {
		   return null;
	   }
	   Class<?> type = null;
	   if(genericType instanceof ParameterizedType) {
			ParameterizedType ft = (ParameterizedType)genericType;
			Type[] types = ft.getActualTypeArguments();
			if(types[index] instanceof Class) {
				type = (Class<?>)types[index];
			}
		}
		return type;
	}

    public static boolean isFinal(Class<?> type) {
    	 if(type == null) {
  		   return false;
  	   }
		return Modifier.isFinal(type.getModifiers());
	}
    
    public static boolean isMap(Class<?> cls) {
    	return Map.class.isAssignableFrom(cls);
    }
    
    public static boolean isCollection(Class<?> cls) {
    	return Collection.class.isAssignableFrom(cls);
    }
    
    public static boolean isByteBuffer(Class<?> cls) {
    	return ByteBuffer.class.isAssignableFrom(cls);
    }
    
    public static boolean isArray(Class<?> cls) {
    	return cls.isArray() || cls == Array.class;
    }

	private <V> void encodeObjects(ByteBuffer buffer, Object objs){
		if(objs == null) {
			buffer.putInt(0);
			return;
		}
		//putType(buffer,objs.getClass().getComponentType());
		int len = Array.getLength(objs);
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		
		boolean nw = isFinal(objs.getClass().getComponentType());
		
		for(int i = 0; i < len; i++){
			Object v = Array.get(objs, i);
			if(!nw) {
				this.putType(buffer, v.getClass());
			}
			encodeObject(buffer,v,null);
		}
	}
	
	private <K,V> void encodeMap(ByteBuffer buffer
			,Map<K,V> map,Class<?> keyType,Class<?> valueType){
		if(map == null) {
			buffer.putInt(0);
			return;
		}
		int len = map.size();
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		
		boolean keyFlag = isFinal(keyType);
		boolean valueFlag = isFinal(valueType);
		
		for(Map.Entry<K,V> e: map.entrySet()){
			if(!keyFlag) {
				this.putType(buffer, e.getKey().getClass());
			}
			encodeObject(buffer,e.getKey(),null);
			
			if(!valueFlag) {
				this.putType(buffer, e.getValue().getClass());
			}
			encodeObject(buffer,e.getValue(),null);
		}
	}
	
	private void encodeString(ByteBuffer buffer,String str){
		if(StringUtils.isEmpty(str)){
			buffer.putInt(0);
			return;
		}
	    try {
			byte[] data = str.getBytes(Constants.CHARSET);
			buffer.putInt(data.length);
			buffer.put(data);
		} catch (UnsupportedEncodingException e) {
		}
	}

}
