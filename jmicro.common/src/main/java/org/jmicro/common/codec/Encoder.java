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
package org.jmicro.common.codec;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:25
 */
public class Encoder{

	public byte[] encode(Object obj) {
		ByteBuffer bb = ByteBuffer.allocate(1024*8);
		encodeObject(bb,obj);
		bb.flip();
		return bb.array();
	}

	public static <V> void encodeObject(ByteBuffer buffer,V obj){
	
		if(obj == null){
			buffer.put(Decoder.PREFIX_TYPE_NULL);
			return;
		}
		Class<?> cls = obj.getClass();
		Integer type = Decoder.getType(cls);
		
		if(type == null || type <= 0) {
			buffer.put(Decoder.PREFIX_TYPE_STRING);
			encodeString(buffer,cls.getName());
		}else {
			cls = Decoder.getClass(type);
			buffer.put(Decoder.PREFIX_TYPE_BYTE);
			buffer.put((byte)type.intValue());
		}
		
		Object v = null;
		if(Map.class == cls){
			encodeMap(buffer,(Map<Object,Object>)obj);
		}else if(Collection.class == cls){
			 encodeList(buffer,(Collection)obj);
		}else if(cls == Array.class){
			encodeObjects(buffer,(Object[])obj);
		}else if(cls == String.class) {
			encodeString(buffer,(String)obj);
		}else if(cls == void.class || cls == Void.class  || cls == Void.TYPE) {
			v =  null;
		}else if(cls == int.class || cls == Integer.class || cls == Integer.TYPE){
			buffer.putInt((Integer)obj);
		}else if(cls == byte.class || cls == Byte.class || cls == Byte.TYPE){
			buffer.put((Byte)obj);
		}else if(cls == short.class || cls == Short.class || cls == Short.TYPE){
			buffer.putShort((Short)obj);
		}else if(cls == long.class || cls == Long.class || cls == Long.TYPE){
			buffer.putLong((Long)obj);
		}else if(cls == float.class || cls == Float.class || cls == Float.TYPE){
			buffer.putFloat((Float)obj);
		}else if(cls == double.class || cls == Double.class || cls == Double.TYPE){
			buffer.putDouble((Double)obj);
		}else if(cls == boolean.class || cls == Boolean.class || cls == Boolean.TYPE){
			boolean b = (Boolean)obj;
			buffer.put(b?(byte)1:(byte)0);
		}else if(cls == char.class || cls == Character.class || cls == Character.TYPE){
			buffer.putChar((Character)obj);
		} else {
			encodeByReflect(buffer,cls,type,obj);
		}
	
	}
	
	private static void encodeByReflect(ByteBuffer buffer, Class<?> cls, Integer type,Object obj) {
		
		int m = cls.getModifiers() ;
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m)){
			cls = obj.getClass();
			m = cls.getModifiers();
		}
		
		if(!Modifier.isPublic(m)) {
			throw new CommonException("should be public class [" +cls.getName()+"]");
		}
		
		
		List<String> fieldNames = Decoder.sortFieldNames(cls);
		
		for(int i = 0; i < fieldNames.size(); i++){
			try {
				String fn = fieldNames.get(i);
				
				/*if(fn.equals("reqArgsStr")){
					System.out.println("");
				}*/
				
				Field f = cls.getDeclaredField(fn);
				
				boolean bf = f.isAccessible();
				if(!bf){
					f.setAccessible(true);
				}
				Object v = f.get(obj);
				if(!bf){
					f.setAccessible(false);
				}
				encodeObject(buffer,v);
				
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
		}
		
	}

	private static void encodeList(ByteBuffer buffer, Collection objs) {
		buffer.putInt(objs.size());
		for(Object o: objs){
			encodeObject(buffer,o);
		}
	}

	private static <V> void encodeObjects(ByteBuffer buffer,V[] objs){
		
		int len = objs.length;
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		for(Object o : objs){
			encodeObject(buffer,o);
		}
	}
	
	private static <K,V> void encodeMap(ByteBuffer buffer,Map<K,V> map){
		
		int len = map.size();
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		
		for(Map.Entry<K,V> e: map.entrySet()){
			encodeObject(buffer,e.getKey());
			encodeObject(buffer,e.getValue());
		}
		
	}
	
	private static void encodeString(ByteBuffer buffer,String str){
		if(StringUtils.isEmpty(str)){
			buffer.putInt(0);
			return;
		}
		/*buffer.putInt(str.length());
		for(int i =0; i < str.length(); i++){
			buffer.putChar(str.charAt(i));
		}*/
	    try {
			byte[] data = str.getBytes(Constants.CHARSET);
			buffer.putInt(data.length);
			buffer.put(data);
		} catch (UnsupportedEncodingException e) {
		}
	}

}
