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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:20:22
 *
 */
public class TypeUtils {

	private TypeUtils() {
	}

	public static boolean isFinalParameterType(Type genericType, int index) {
		boolean flag = false;
		if (genericType == null) {
			return false;
		}
		Class<?> type = finalParameterType(genericType, index);
		if (type != null) {
			flag = isFinal(type);
		}
		return flag;
	}

	public static Class<?> finalParameterType(Type genericType, int index) {
		if (genericType == null) {
			return null;
		}
		Class<?> type = null;
		if (genericType instanceof ParameterizedType) {
			ParameterizedType ft = (ParameterizedType) genericType;
			Type[] types = ft.getActualTypeArguments();
			if (types[index] instanceof Class) {
				type = (Class<?>) types[index];
			}
		}
		return type;
	}
	
	public static Object getFieldValue(Object obj,Field f) {
		if(obj == null) {
			return null;
		}
		
		Object v = null;
		boolean bf = f.isAccessible();
		if(!bf){
			f.setAccessible(true);
		}
		try {
			 v = f.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		if(!bf){
			f.setAccessible(false);
		}
		return v;
	}
	
	public static void setFieldValue(Object obj,Object v,Field f) {
		if(obj == null) {
			return ;
		}
		boolean bf = f.isAccessible();
		if(!bf){
			f.setAccessible(true);
		}
		if(v != null){
			try {
				f.set(obj, v);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(!bf){
			f.setAccessible(false);
		}
	}

	public static boolean isFinal(Class<?> type) {
		if (type == null) {
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
	
	public static boolean isVoid(Class<?> cls) {
		return cls == void.class || cls == Void.class  || cls == Void.TYPE;
	}

	public static boolean isByte(Class<?> cls) {
		return cls == byte.class || cls == Byte.class || cls == Byte.TYPE;
	}
	
	public static boolean isShort(Class<?> cls) {
		return cls == short.class || cls == Short.class || cls == Short.TYPE;
	}
	
	public static boolean isInt(Class<?> cls) {
		return cls == int.class || cls == Integer.class || cls == Integer.TYPE;
	}
	
	public static boolean isLong(Class<?> cls) {
		return cls == long.class || cls == Long.class || cls == Long.TYPE;
	}
	
	public static boolean isDouble(Class<?> cls) {
		return cls == double.class || cls == Double.class || cls == Double.TYPE;
	}
	
	public static boolean isFloat(Class<?> cls) {
		return cls == float.class || cls == Float.class || cls == Float.TYPE;
	}
	
	public static boolean isBoolean(Class<?> cls) {
		return cls == boolean.class || cls == Boolean.class || cls == Boolean.TYPE;
	}
	
	public static boolean isChar(Class<?> cls) {
		return cls == char.class || cls == Character.class || cls == Character.TYPE;
	}
	
	public static boolean isDate(Class<?> cls) {
		return cls == java.util.Date.class;
	}
	
}
