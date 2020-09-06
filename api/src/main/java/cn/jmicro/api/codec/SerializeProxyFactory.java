package cn.jmicro.api.codec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.codec.typecoder.TypeCoder;
import cn.jmicro.common.Utils;

public class SerializeProxyFactory {

	//public static final Logger logger = LoggerFactory.getLogger(SerializeProxyFactory.class);
	
	public static Map<Class<?>,ISerializer> cache = new HashMap<>();
	
	public static <T> ISerializer getSerializeCoder(Class<T> cls) {
		
		if(cache.containsKey(cls)) {
			return cache.get(cls);
		}
		
		synchronized(cls) {
			if(cache.containsKey(cls)) {
				return cache.get(cls);
			}
			
			//createProxyClass(cls);
			
			if(cache.containsKey(cls)) {
				return cache.get(cls);
			}
		}
		return null;
	}
	
	/*private static <T> void createProxyClass(Class<T> cls) {

		 if(!Modifier.isPublic(cls.getModifiers())) {
			// 非公有类，不能做序列化
			throw new CommonException("should be public class [" + cls.getName() + "]");
		 }
		
		 ClassGenerator classGenerator = ClassGenerator.newInstance(cls.getClassLoader());
		 classGenerator.setClassName(cls.getName()+"$Serializer");
		 //classGenerator.setSuperClass(SerializeObject.class);
		 classGenerator.addInterface(ISerializer.class);
		 classGenerator.addDefaultConstructor();
		 
		 classGenerator.addMethod(getEncodeMethod(cls));      
		 
		 classGenerator.addMethod(getDecodeMethod(cls));      
		
		 Class<?> clazz = classGenerator.toClass();
		 
		 try {
			cache.put(cls, (ISerializer)clazz.newInstance());
		} catch (InstantiationException | IllegalAccessException e) {
			//logger.error("",e);
			e.printStackTrace();
		} finally {
			classGenerator.release();
		}
	
	}*/

	private static  String getDecodeMethod(Class<?> cls) {

		StringBuffer sb = new StringBuffer("public Object decode(java.io.DataInput __buffer)   throws java.io.IOException {\n");
		sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
		
		sb.append(cls.getName() ).append(" __obj =  new ").append(cls.getName()).append("();\n");
		
		sb.append(" java.lang.reflect.Field f = null; ");
		
		List<Field> fields = TypeCoder.loadClassFieldsFromCache(cls);
		for(int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			
			sb.append(" f = null;");
			
			//sb.append(" java.lang.reflect.Field f = ").append(cls.getName()).append(".class.getField(\"").append(f.getName()).append("\"); \n");
			
			String valStr = "val"+i;
			
			if(!Collection.class.isAssignableFrom(f.getType()) &&
					!Map.class.isAssignableFrom(f.getType())) {
				sb.append(" java.lang.Object __").append(valStr)
				.append(" = __coder.decode(__buffer,").append(f.getType().getName()).append(".class,").append(" null);\n");
			} else {
				sb.append(" f = ").append(" __obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");");
				sb.append(" java.lang.Object __").append(valStr)
				.append(" coder.encode(__buffer,vv,").append(f.getType().getName()).append(".class,").append(" f.getGenericType() );");
			}
			
			sb.append(" ").append(getName(f.getType())).append(" _"+valStr+" =").append(asArgument(f.getType(), "__"+valStr)).append(";\n");
			
			String setMethodName = "set"+f.getName().substring(0, 1).toUpperCase()+f.getName().substring(1);
			Method setMethod = null;
			try {
				setMethod = cls.getMethod(setMethodName, new Class[] {f.getType()});
			} catch (NoSuchMethodException | SecurityException e) {
			}
			
			if(setMethod != null) {
				sb.append("  __obj.").append(setMethodName).append("(_"+valStr+");\n\n");
			} else {
				if(Modifier.isPublic(f.getModifiers())) {
					sb.append(" __obj.").append(f.getName()).append("=(").append(f.getType().getName()).append(")_"+valStr+";\n");
				} else {
					sb.append(" if(f == null ) { f =  __obj.getClass().getDeclaredField(\"" +f.getName()+ "\");}");
					sb.append(" cn.jmicro.api.codec.TypeUtils.setFieldValue(__obj, __"+valStr+", f);\n");
				}
			}
		}
		
		sb.append("return __obj; \n}\n");
		
		return sb.toString();
	
	}

	private static String getEncodeMethod(Class<?> cls) {
		StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer,Object obj)   throws java.io.IOException { \n");
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
			
			Method m = null;
			try {
				m = cls.getMethod(getMethodName, new Class[0]);
			} catch (NoSuchMethodException | SecurityException e) {
			}
			
			if(m != null) {
				sb.append(" ").append(getName(fieldDeclareType)).append(" __val"+i).append("=");
				sb.append(" __obj.").append(getMethodName).append("();\n");
			}else if(Modifier.isPublic(f.getModifiers())) {
				sb.append(" ").append(getName(fieldDeclareType)).append(" __val"+i).append("=");
				sb.append(" __obj.").append(f.getName()).append(";\n");
			} else {
				sb.append(" ").append(getName(fieldDeclareType)).append(" __val"+i).append(" = ").append(Utils.getIns().defaultVal(fieldDeclareType)).append("; \n");
				sb.append("try { \n");
				sb.append(" java.lang.reflect.Field f0 = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");\n");
				sb.append(" Object v = cn.jmicro.api.codec.TypeUtils.getFieldValue(__obj,f0);\n");
				sb.append(" __val"+i).append("=").append(asArgument(fieldDeclareType, "v")).append(";\n");
				sb.append(" } catch(Exception e) { e.printStackTrace(); }\n");
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
				sb.append("if(__val"+i).append(" == null)  { __buffer.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);} else { ");
				sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
				   if(!Collection.class.isAssignableFrom(fieldDeclareType) &&
						!Map.class.isAssignableFrom(fieldDeclareType)) {
						sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null );\n\n");
					} else {
						sb.append("try { \n");
						sb.append(" java.lang.reflect.Field __f = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");");
						sb.append(" __coder.encode(buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" __f.getGenericType() );");
						sb.append(" catch(NoSuchFieldException | SecurityException e) { e.printStackTrace(); }\n");
					}
				sb.append(" } //end else block \n");
			}
			sb.append("\n\n");
		}
		
		sb.append("}");
		
		return sb.toString();
	}
	
	public static String asArgument(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (Boolean.TYPE == cl)
                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
            if (Byte.TYPE == cl)
                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
            if (Character.TYPE == cl)
                return name + "==null?(char)0:((Character)" + name + ").charValue()";
            if (Double.TYPE == cl)
                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
            if (Float.TYPE == cl)
                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
            if (Integer.TYPE == cl)
                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
            if (Long.TYPE == cl)
                return name + "==null?(long)0:((Long)" + name + ").longValue()";
            if (Short.TYPE == cl)
                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
            throw new RuntimeException(name + " is unknown primitive type.");
        }
        return "(" + getName(cl) + ")" + name;
    }

	public static String getName(Class<?> c) {
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append("[]");
                c = c.getComponentType();
            }
            while (c.isArray());

            return c.getName() + sb.toString();
        }
        return c.getName();
    }
	
}
