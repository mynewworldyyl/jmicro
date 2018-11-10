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
	
	@SuppressWarnings("unchecked")
	@Override
	public ByteBuffer encode(Object obj) {
		if(obj == null) {
			//对空值编码没有意义
			throw new CommonException("Encode must not be NULL");
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(encodeBufferSize);
		//记录最外层类型信息
		putType(buffer,obj.getClass());
		
		Class<?> cls = obj.getClass();
		
		if(TypeUtils.isMap(cls)){
			/* TypeVariable[] typeVars = cls.getTypeParameters();
			 Class<?> keyType = typeVars[0].getClass();
			 Class<?> valueType = typeVars[1].getClass();*/
			 encodeMap(buffer,(Map<Object,Object>)obj,null,null);
		}else if(TypeUtils.isCollection(cls)){
			/* Type type = cls.getGenericSuperclass();
			 Class<?> valueType = finalParameterType((ParameterizedType)type,0);*/
			 encodeCollection(buffer,(Collection<?>)obj,null);
		}else {
			if(!TypeUtils.isFinal(cls)) {
				throw new CommonException("class {} must by final class for encode",cls.getName());
			}
			if(cls.isArray()) {
				this.putType(buffer, cls.getComponentType());
				this.encodeObjects(buffer, obj);
			}else {
				encodeObject(buffer,obj,cls);
			}
		}
		return buffer;
	}
	
	public Class<?> putType(ByteBuffer buffer,Class<?> cls) {
        Short type = Decoder.getType(cls);
        
		if(ByteBuffer.class.isAssignableFrom(cls)){
			cls = ByteBuffer.class;
		}
		
		if(TypeUtils.isMap(cls)) {
			cls = Map.class;
		}else if(TypeUtils.isCollection(cls)) {
			cls = Collection.class;
		}else if(TypeUtils.isByteBuffer(cls)) {
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
	
    /**
     * 
     * @param buffer
     * @param obj
     * @param genericType 成员变量字段声明的类型
     */
	private void encodeObject(ByteBuffer buffer,Object obj, Class<?> genericType){
	
		Class<?> cls = null;
		if( obj == null ) {
			cls = genericType;
		} else {
			cls = obj.getClass();
		}
		
		if(TypeUtils.isByteBuffer(cls)){
			 encodeByteBuffer(buffer,(ByteBuffer)obj);
		}else if(TypeUtils.isArray(cls)){
			encodeObjects(buffer,obj);
		}else if(cls == String.class) {
			encodeString(buffer,(String)obj);
		}else if(TypeUtils.isVoid(cls)) {
		
		}else if(TypeUtils.isInt(cls)){
			if(obj == null) {
				buffer.putInt(0);
			}else {
				buffer.putInt((Integer)obj);
			}
		}else if(TypeUtils.isByte(cls)){
			if(obj == null) {
				buffer.put((byte)0);
			}else {
				buffer.put((Byte)obj);
			}
		}else if(TypeUtils.isShort(cls)){
			if(obj == null) {
				buffer.putShort((short)0);
			}else {
				buffer.putShort((Short)obj);
			}
		}else if(TypeUtils.isLong(cls)){
			if(obj == null) {
				buffer.putLong(0);
			}else {
				buffer.putLong((Long)obj);
			}
		}else if(TypeUtils.isFloat(cls)){
			if(obj == null) {
				buffer.putFloat(0);
			}else {
				buffer.putFloat((Float)obj);
			}
		}else if(TypeUtils.isDouble(cls)){
			if(obj == null) {
				buffer.putDouble(0);
			}else {
				buffer.putDouble((Double)obj);
			}
		}else if(TypeUtils.isBoolean(cls)){
			if(obj == null) {
				buffer.put((byte)0);
			}else {
				boolean b = (Boolean)obj;
				buffer.put(b?(byte)1:(byte)0);
			}
		}else if(TypeUtils.isChar(cls)){
			if(obj == null) {
				buffer.putChar((char)0);
			}else {
				buffer.putChar((Character)obj);
			}
		} else {
			encodeByReflect(buffer,obj,cls);
		}
	}
	
	private void encodeByteBuffer(ByteBuffer buffer, ByteBuffer obj) {
		buffer.putInt(obj.remaining());
		buffer.put(obj);
	}

	private void encodeByReflect(ByteBuffer buffer, Object obj, Class<?> cls) {
		
		if(!Modifier.isPublic(cls.getModifiers())) {
			throw new CommonException("should be public class [" +cls.getName()+"]");
		}
		
		if(!TypeUtils.isFinal(cls)) {
			//不能从泛型参数拿到类型信息，需要把类型参数写到buffer中
			if(obj == null) {
				this.putType(buffer, Void.class);
				return;
			} else {
				cls = obj.getClass();
				this.putType(buffer, cls);
			}
		}
		
		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames,cls);
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		
		for(int i = 0; i < fieldNames.size(); i++){

			String fn = fieldNames.get(i);
			
			Field f = Utils.getIns().getClassField(cls,fn);
			
			Object v = TypeUtils.getFieldValue(obj, f);
			
			if(!TypeUtils.isFinal(f.getType())) {
				//不能根据字段类型判断值的类型做系列化，写入值的类型
				if(v != null) {
					this.putType(buffer, v.getClass());
				}else {
					this.putType(buffer, Void.class);
					continue;
				}
			}
			
			if(TypeUtils.isMap(f.getType())){
				 Class<?> keyType = TypeUtils.finalParameterType((ParameterizedType)f.getGenericType(),0);
				 Class<?> valueType = TypeUtils.finalParameterType((ParameterizedType)f.getGenericType(),1);
				 encodeMap(buffer,(Map<Object,Object>)v,keyType,valueType);
			}else if(TypeUtils.isCollection(f.getType())){
				 Class<?> valueType = TypeUtils.finalParameterType((ParameterizedType)f.getGenericType(),0);
				 encodeCollection(buffer,(Collection)v,valueType);
			}else {
				if(f.getType().isArray()) {
					this.putType(buffer, f.getType().getComponentType());
					this.encodeObjects(buffer, v);
				}else {
					encodeObject(buffer,v,f.getType());
				}
				
			}
		}
	}

	private void encodeCollection(ByteBuffer buffer, Collection<?> objs,Class<?> paramType) {
		if(objs == null || objs.isEmpty()) {
			buffer.putInt(0);
			return;
		}
		
		boolean flag = TypeUtils.isFinal(paramType);
		buffer.putInt(objs.size());
		for(Object o: objs){
			if(!flag) {
				if(o == null) {
					this.putType(buffer, Void.class);
				}else {
					this.putType(buffer, o.getClass());
				}
			}
			encodeObject(buffer,o,null);
		}
		
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
		
		boolean nw = TypeUtils.isFinal(objs.getClass().getComponentType());
		
		for(int i = 0; i < len; i++){
			Object v = Array.get(objs, i);
			if(!nw) {
				if(v == null) {
					this.putType(buffer, Void.class);
				}else {
					this.putType(buffer, v.getClass());
				}
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
		
		boolean keyFlag = TypeUtils.isFinal(keyType);
		boolean valueFlag = TypeUtils.isFinal(valueType);
		
		for(Map.Entry<K,V> e: map.entrySet()){
			if(!keyFlag) {
				if(e.getKey() == null) {
					this.putType(buffer, Void.class);
				}else {
					this.putType(buffer, e.getKey().getClass());
				}
			}
			encodeObject(buffer,e.getKey(),null);
			
			if(!valueFlag) {
				if(e.getValue() == null) {
					this.putType(buffer, Void.class);
				}else {
					this.putType(buffer, e.getValue().getClass());
				}
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
