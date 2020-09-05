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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:20:22
 *
 */
public class TypeUtils {

	private static final Logger logger = LoggerFactory.getLogger(TypeUtils.class);
	
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
	
	public static void finalParameterType(Type type,Set<Class<?>> clses) {
		if (type == null) {
			return;
		}
		
		if (type instanceof ParameterizedType) {
			ParameterizedType ft = (ParameterizedType) type;
			Type[] types = ft.getActualTypeArguments();
			if(types == null || types.length == 0) {
				return;
			}
			
			for(Type t : types) {
				finalParameterType(t,clses);
			}
		}else if(type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			clses.add(c);
			if(c.isArray()) {
				clses.add(c.getComponentType());
			}
			
			finalParameterType(c.getGenericSuperclass(),clses);
		}
	}
	
	
	public static Object getFieldValue(Object obj,Field f) {
		if(obj == null) {
			return null;
		}
		
		Object v = null;
		
		//boolean bf = f.isAccessible();
		if(!f.isAccessible())
			f.setAccessible(true);
		try {
			 v = f.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		//f.setAccessible(false);
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
				e.printStackTrace();
			}
		}
		
		/*if(!bf){
			f.setAccessible(false);
		}*/
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
		return  cls == Byte.class ;
	}
	
	public static boolean isShort(Class<?> cls) {
		return  cls == Short.class;
	}
	
	public static boolean isInt(Class<?> cls) {
		return cls == Integer.class ;
	}
	
	public static boolean isLong(Class<?> cls) {
		return  cls == Long.class;
	}
	
	public static boolean isDouble(Class<?> cls) {
		return  cls == Double.class ;
	}
	
	public static boolean isFloat(Class<?> cls) {
		return  cls == Float.class ;
	}
	
	public static boolean isBoolean(Class<?> cls) {
		return  cls == Boolean.class ;
	}
	
	public static boolean isChar(Class<?> cls) {
		return cls == Character.class ;
	}
	
	public static boolean isDate(Class<?> cls) {
		return cls == java.util.Date.class;
	}
	
	public static boolean isPrimitive(Class<?> cls) {
		return isPrimitiveByte(cls) || isPrimitiveShort(cls)
				||isPrimitiveInt(cls) || isPrimitiveLong(cls)
				||isPrimitiveDouble(cls) || isPrimitiveFloat(cls)
				||isPrimitiveBoolean(cls) || isPrimitiveChar(cls)
				;
	}
	
	public static boolean isPrimitiveByte(Class<?> cls) {
		return cls == byte.class || cls == Byte.TYPE;
	}
	
	public static boolean isPrimitiveShort(Class<?> cls) {
		return cls == short.class || cls == Short.TYPE;
	}
	
	public static boolean isPrimitiveInt(Class<?> cls) {
		return cls == int.class || cls == Integer.TYPE;
	}
	
	public static boolean isPrimitiveLong(Class<?> cls) {
		return cls == long.class || cls == Long.TYPE;
	}
	
	public static boolean isPrimitiveDouble(Class<?> cls) {
		return cls == double.class || cls == Double.TYPE;
	}
	
	public static boolean isPrimitiveFloat(Class<?> cls) {
		return cls == float.class || cls == Float.TYPE;
	}
	
	public static boolean isPrimitiveBoolean(Class<?> cls) {
		return cls == boolean.class || cls == Boolean.TYPE;
	}
	
	public static boolean isPrimitiveChar(Class<?> cls) {
		return cls == char.class || cls == Character.TYPE;
	}

}
