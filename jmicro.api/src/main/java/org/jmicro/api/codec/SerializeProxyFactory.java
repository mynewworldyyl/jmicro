package org.jmicro.api.codec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.codec.typecoder.TypeCoder;
import org.jmicro.common.CommonException;
import org.jmicro.common.Utils;
import org.jmicro.common.util.ClassGenerator;
import org.jmicro.common.util.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeProxyFactory {

	public static final Logger logger = LoggerFactory.getLogger(SerializeProxyFactory.class);
	
	public static Map<Class<?>,ISerializeObject> cache = new HashMap<>();
	
	public static <T> ISerializeObject getSerializeCoder(Class<T> cls) {
		
		if(cache.containsKey(cls)) {
			return cache.get(cls);
		}
		
		synchronized(cls) {
			if(cache.containsKey(cls)) {
				return cache.get(cls);
			}
			
			createProxyClass(cls);
			
			if(cache.containsKey(cls)) {
				return cache.get(cls);
			}
		}
		return null;
	}
	
	private static <T> void createProxyClass(Class<T> cls) {

		 if(!Modifier.isPublic(cls.getModifiers())) {
			// 非公有类，不能做序列化
			throw new CommonException("should be public class [" + cls.getName() + "]");
		 }
		
		 ClassGenerator classGenerator = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
		 classGenerator.setClassName(cls.getName()+"$Serializer");
		 //classGenerator.setSuperClass(SerializeObject.class);
		 classGenerator.addInterface(ISerializeObject.class);
		 classGenerator.addDefaultConstructor();
		 
		 classGenerator.addMethod(getEncodeMethod(cls));      
		 
		 classGenerator.addMethod(getDecodeMethod(cls));      
		
		 Class<?> clazz = classGenerator.toClass();
		 
		 try {
			cache.put(cls, (ISerializeObject)clazz.newInstance());
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	
	}

	private static  String getDecodeMethod(Class cls) {

		StringBuffer sb = new StringBuffer("public Object decode(java.io.DataInput __buffer) {\n");
		sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
		
		sb.append(cls.getName() ).append(" __obj =  new ").append(cls.getName()).append("();\n");
		
		List<Field> fields = TypeCoder.loadClassFieldsFromCache(cls);
		for(int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			
			//sb.append(" java.lang.reflect.Field f = ").append(cls.getName()).append(".class.getField(\"").append(f.getName()).append("\"); \n");
			
			sb.append(" java.lang.Object __val1=");
			
			if(!Collection.class.isAssignableFrom(f.getType()) &&
					!Map.class.isAssignableFrom(f.getType())) {
				sb.append(" __coder.decode(__buffer,").append(f.getType().getName()).append(".class,").append(" null);\n");
			} else {
				sb.append("java.lang.reflect.Field f = ").append("this.getClass().getDeclaredField(\"").append(f.getName()).append("\");");
				sb.append("coder.encode(__buffer,vv,").append(f.getType().getName()).append(".class,").append(" f.getGenericType() );");
			}
			
			sb.append(" ").append(ReflectUtils.getName(f.getType())).append(" __val =").append(Utils.getIns().asArgument(f.getType(), "__val1")).append(";\n");
			
			String setMethodName = "set"+f.getName().substring(0, 1).toUpperCase()+f.getName().substring(1);
			Method setMethod = null;
			try {
				setMethod = cls.getMethod(setMethodName, new Class[] {f.getType()});
			} catch (NoSuchMethodException | SecurityException e) {
			}
			
			if(setMethod != null) {
				sb.append(" __obj.").append(setMethodName).append("(__val);\n\n");
			} else {
				if(Modifier.isPublic(f.getModifiers())) {
					sb.append("__obj.").append(f.getName()).append("=(").append(f.getType().getName()).append(")val;\n");
				} else {
					sb.append("org.jmicro.api.codec.TypeUtils.setFieldValue(__obj, val, f);\n");
				}
			}
		}
		
		sb.append("return __obj; \n}\n");
		
		return sb.toString();
	
	}

	private static String getEncodeMethod(Class cls) {
		StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer,Object obj) { \n");
		sb.append(cls.getName() ).append(" __obj =  (").append(cls.getName()).append(")").append("obj; \n");
		
		List<Field> fields = TypeCoder.loadClassFieldsFromCache(cls);
		for(int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			String getMethodName = "get"+f.getName().substring(0, 1).toUpperCase()+f.getName().substring(1);
			
			Class fieldDeclareType = f.getType();
			
			sb.append(" ").append(ReflectUtils.getName(fieldDeclareType)).append(" __val"+i).append("=");
			
			Method m = null;
			try {
				m = cls.getMethod(getMethodName, new Class[0]);
			} catch (NoSuchMethodException | SecurityException e) {
			}
			
			if(m != null) {
				sb.append(" __obj.").append(getMethodName).append("();\n");
			}else if(Modifier.isPublic(f.getModifiers())) {
				sb.append(" __obj.").append(f.getName()).append(";\n");
			} else {
				sb.append("try { \n");
				sb.append(" java.lang.reflect.Field f0 = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");\n");
				sb.append(" org.jmicro.api.codec.TypeUtils.getFieldValue(__obj,f0);\n");
				sb.append("catch(NoSuchFieldException | SecurityException e) { e.printStackTrace(); }\n");
			}
			
			if(fieldDeclareType == int.class || fieldDeclareType == Integer.TYPE || fieldDeclareType == Integer.class ) {
				sb.append(" __buffer.writeInt(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == String.class ) {
				sb.append(" __buffer.writeUTF(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == long.class || fieldDeclareType == Long.TYPE || fieldDeclareType == Long.class ) {
				sb.append(" __buffer.writeLong(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == byte.class || fieldDeclareType == Byte.TYPE || fieldDeclareType == Byte.class ) {
				sb.append(" __buffer.writeByte(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == short.class || fieldDeclareType == Short.TYPE || fieldDeclareType == Short.class ) {
				sb.append(" __buffer.writeShort(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType == float.class || fieldDeclareType == Float.TYPE || fieldDeclareType == Float.class ) {
				sb.append(" __buffer.writeFloat(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == double.class || fieldDeclareType == Double.TYPE || fieldDeclareType == Double.class ) {
				sb.append(" __buffer.writeDouble(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == boolean.class || fieldDeclareType == Boolean.TYPE || fieldDeclareType == Boolean.class ) {
				sb.append(" __buffer.writeBoolean(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == char.class || fieldDeclareType == Character.TYPE || fieldDeclareType == Character.class ) {
				sb.append(" __buffer.writeChar(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType == Date.class ) {
				sb.append(" __buffer.writeLong(").append(" __val").append(i).append(".getTime()); \n");
			} else {
				sb.append("if(__val"+i).append(" == null)  { __buffer.write(org.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);} else { ");
				sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
				   if(!Collection.class.isAssignableFrom(fieldDeclareType) &&
						!Map.class.isAssignableFrom(fieldDeclareType)) {
						sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null );\n\n");
					} else {
						sb.append("try { \n");
						sb.append(" java.lang.reflect.Field __f = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");");
						sb.append(" __coder.encode(buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" __f.getGenericType() );");
						sb.append("catch(NoSuchFieldException | SecurityException e) { e.printStackTrace(); }\n");
					}
				sb.append(" } //end else block \n");
			}
		}
		
		sb.append("}");
		
		return sb.toString();
	}

}
