package org.jmicro.api.codec;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 编码：
 * 1. 编码时直接根据值的class取得对应的编码器，然后调用encode方法;
 * 2. 对于对像成员变量，首先判断成员变量声明类型是否是final类型，如果是，直接写编码数据，否则进入步聚3;
 * 3. 写入一个字节类型编码前缀类型，写入类型编码，写入编码数据；
 * 
 * 解码:
 * 1. 首先从buffer读取类型信息，然后根据类型信息取得解码器，调用decode解码;
 * 2. 对于对像成员变量，首先判断成员变量声明类型是否是final类型，如果是，则直接根据声明类型调用decode方法解码，如果不是，则进入步聚１
 * 
 * @author Yulei Ye
 * @date 2018年12月26日 上午10:53:38
 */
@Component
public class TypeCoderFactory {

	private static final Logger logger = LoggerFactory.getLogger(TypeCoderFactory.class);
	
	@SuppressWarnings("rawtypes")
	private static Map<Short,TypeCoder> code2Coder= new TreeMap<>();
	
	@SuppressWarnings("rawtypes")
	private static Map<Class,TypeCoder> clazz2Coder = new TreeMap<>();
	
	//static Short currentTypeCode = (short)(NON_ENCODE_TYPE + 1);
	
   private static IClientTransformClassLoader clazzLoader = null;
   
   private static short type = (short)0xFFFE;
   
   @SuppressWarnings("rawtypes")
	private static TypeCoder<Object> defaultCoder = new TypeCoder<Object>() {

		@Override
		public Class<Object> type() {
			return Object.class;
		}

		@Override
		public short code() {
			return (short) 0xFFFF;
		}

		@Override
		public boolean canSupport(Class<?> clazz) {
			return false;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void encode(ByteBuffer buffer, Object val, Class<?> fieldDeclareType, Type genericType) {
			if (val == null) {
				TypeCoder coder = TypeCoderFactory.getCoder(Void.class);
				coder.encode(buffer, null, Void.class,genericType);
			} else {
				TypeCoder coder = TypeCoderFactory.getCoder(val.getClass());
				if (coder != defaultCoder) {
					// 有指定类型的编码器，使用指定类型的编码器
					coder.encode(buffer, val, fieldDeclareType, genericType);
				} else {
					// 不属于任何类的成员字段时，fieldDeclareType==null
					if (fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
						AbstractTypeCoder.putStringType(buffer, Decoder.PREFIX_TYPE_STRING, val.getClass().getName());
					}
					encodeByReflect(buffer, val, fieldDeclareType,genericType);
				}
			}
		}

		@Override
		public Object decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
			Class<?> cls = fieldDeclareType;
			if(cls == null || !TypeUtils.isFinal(cls)) {
				cls = getType(buffer);
			}
			
			TypeCoder<?> coder = getCoder(cls);
			if(coder != defaultCoder) {
				return coder.decode(buffer, cls, genericType);
			} else {
				decodeByReflect(buffer,cls,genericType);
			}
			
			return null;
		}

	};
	
	private static class ArrayCoder extends AbstractTypeCoder<Object>{
		   
		   public ArrayCoder(short code) {
			  super(code,Object.class);
		   }

			@Override
			public boolean canSupport(Class<?> clazz) {
				return clazz != null && clazz.isArray();
			}

			@Override
			protected Object decodeData(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
				return decodeArray(buffer,fieldDeclareType,genericType);
			}

			@Override
			protected void encodeData(ByteBuffer buffer, Object val, Class<?> fieldDeclareType, Type genericType) {
				encodeArray(buffer,val,fieldDeclareType,genericType);
			}
	  }
	
  
  private static class ReflectTypeCoder<T> extends AbstractTypeCoder<T>{
	   
	   private short code;
	   private Class<T> clazz;
	   
	   public ReflectTypeCoder(short code,Class<T> clazz) {
		   super(code,clazz);
	   }
	   
		@Override
		public Class<T> type() {
			return clazz;
		}
	
		@Override
		public short code() {
			return code;
		}


		@Override
		public void encodeData(ByteBuffer buffer, T val,Class<?> fieldDeclareType, Type genericType) {
			encodeByReflect(buffer,val,fieldDeclareType, genericType);
		}

		@Override
		public T decodeData(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
			return null;
		}
  }
 
	/**
	 * Primitive类型肯定是final类型，不需要类型前缀标识
	 * @author Yulei Ye
	 * @date 2018年12月26日 上午10:31:35
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	private static class PrimitiveTypeCoder extends AbstractTypeCoder<Object> {

		public PrimitiveTypeCoder(short code, Class clazz) {
			super(code, clazz);
		}

		@Override
		public void encodeData(ByteBuffer buffer, Object obj,Class<?> fieldDeclareType,Type genericType) {

			Class<Object> cls = type();
			if (TypeUtils.isPrimitiveInt(cls) || TypeUtils.isInt(cls)) {
				if (obj == null) {
					buffer.putInt(0);
				} else {
					buffer.putInt((Integer) obj);
				}
			} else if (TypeUtils.isPrimitiveByte(cls) || TypeUtils.isByte(cls)) {
				if (obj == null) {
					buffer.put((byte) 0);
				} else {
					buffer.put((Byte) obj);
				}
			} else if (TypeUtils.isPrimitiveShort(cls) || TypeUtils.isShort(cls)) {
				if (obj == null) {
					buffer.putShort((short) 0);
				} else {
					buffer.putShort((Short) obj);
				}
			} else if (TypeUtils.isPrimitiveLong(cls) || TypeUtils.isLong(cls)) {
				if (obj == null) {
					buffer.putLong(0);
				} else {
					buffer.putLong((Long) obj);
				}
			} else if (TypeUtils.isPrimitiveFloat(cls) || TypeUtils.isFloat(cls)) {
				if (obj == null) {
					buffer.putFloat(0);
				} else {
					buffer.putFloat((Float) obj);
				}
			} else if (TypeUtils.isPrimitiveDouble(cls) || TypeUtils.isDouble(cls)) {
				if (obj == null) {
					buffer.putDouble(0);
				} else {
					buffer.putDouble((Double) obj);
				}
			} else if (TypeUtils.isPrimitiveBoolean(cls) || TypeUtils.isBoolean(cls)) {
				if (obj == null) {
					buffer.put((byte) 0);
				} else {
					boolean b = (Boolean) obj;
					buffer.put(b ? (byte) 1 : (byte) 0);
				}
			} else if (TypeUtils.isPrimitiveChar(cls) || TypeUtils.isChar(cls)) {
				if (obj == null) {
					buffer.putChar((char) 0);
				} else {
					buffer.putChar((Character) obj);
				}
			}
		}

		@Override
		public Object decodeData(ByteBuffer buffer, Class<?> fieldDeclareType,Type genericType) {
			Class<Object> cls = type();
			Object val = null;
			if (TypeUtils.isPrimitiveInt(cls) || TypeUtils.isInt(cls)) {
				val = buffer.getInt();
			} else if (TypeUtils.isPrimitiveByte(cls) || TypeUtils.isByte(cls)) {
				val = buffer.get();
			} else if (TypeUtils.isPrimitiveShort(cls) || TypeUtils.isShort(cls)) {
				val = buffer.getShort();
			} else if (TypeUtils.isPrimitiveLong(cls) || TypeUtils.isLong(cls)) {
				val = buffer.getLong();
			} else if (TypeUtils.isPrimitiveFloat(cls) || TypeUtils.isFloat(cls)) {
				val = buffer.getFloat();
			} else if (TypeUtils.isPrimitiveDouble(cls) || TypeUtils.isDouble(cls)) {
				val = buffer.getDouble();
			} else if (TypeUtils.isPrimitiveBoolean(cls) || TypeUtils.isBoolean(cls)) {
				val = buffer.get() == 1;
			} else if (TypeUtils.isPrimitiveChar(cls) || TypeUtils.isChar(cls)) {
				val = buffer.getChar();
			}
			return val;
		}
	}
	
   private static final TypeCoder<Void> voidCoder = new VoidTypeCoder(type--);
	 
   static {
	   
	    registCoder(defaultCoder);
	    
	    registCoder(voidCoder);
	    //registCoder(new VoidTypeCoder<Void>(type--,Void.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Byte.class));
	    registCoder(new PrimitiveTypeCoder(type--,Byte.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Short.class));
	    registCoder(new PrimitiveTypeCoder(type--,Short.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Integer.class));
	    registCoder(new PrimitiveTypeCoder(type--,Integer.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Long.class));
	    registCoder(new PrimitiveTypeCoder(type--,Long.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Double.class));
	    registCoder(new PrimitiveTypeCoder(type--,Double.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Float.class));
	    registCoder(new PrimitiveTypeCoder(type--,Float.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Boolean.class));
	    registCoder(new PrimitiveTypeCoder(type--,Boolean.TYPE));
	    
	    registCoder(new PrimitiveTypeCoder(type--,Character.class));
	    registCoder(new PrimitiveTypeCoder(type--,Character.TYPE));
	    registCoder(new ArrayCoder(type--));
	    
	    
	    registCoder(new AbstractTypeCoder<String>(type--,String.class) {
			@Override
			public void encodeData(ByteBuffer buffer, String val,Class<?> fieldDeclareType,Type genericType) {
				OnePrefixTypeEncoder.encodeString(buffer, (String)val);
			}

			@Override
			public String decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				return OnePrefixDecoder.decodeString(buffer);
			}
	    	
	    });
	    
	    registCoder(new AbstractTypeCoder<ByteBuffer>(type--,ByteBuffer.class) {
			@Override
			public void encodeData(ByteBuffer buffer, ByteBuffer val, Class<?> fieldDeclareType,Type genericType) {
				putLength(buffer,val.remaining());
				buffer.put(val);
			}

			@Override
			public ByteBuffer decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				int len = buffer.getShort();
				byte[] data = new byte[len];
				buffer.get(data, 0, len);
				return ByteBuffer.wrap(data);
			}
	    	
	    });
	    
	    registCoder(new ReflectTypeCoder<RpcRequest>(type--,RpcRequest.class));
	    registCoder(new ReflectTypeCoder<RpcResponse>(type--,RpcResponse.class));
	    registCoder(new ReflectTypeCoder<ApiRequest>(type--,ApiRequest.class));
	    registCoder(new ReflectTypeCoder<ApiResponse>(type--,ApiResponse.class));
	    registCoder(new ReflectTypeCoder<SubmitItem>(type--,SubmitItem.class));
	    registCoder(new ReflectTypeCoder<RpcRequest>(type--,RpcRequest.class));
	    registCoder(new ReflectTypeCoder<RpcRequest>(type--,RpcRequest.class));
	    
	    registCoder(new AbstractTypeCoder<java.util.Date>(type--,java.util.Date.class) {
			@Override
			public void encodeData(ByteBuffer buffer, java.util.Date val, Class<?> fieldDeclareType,Type genericType) {
				buffer.putLong(val.getTime());
			}

			@Override
			public java.util.Date decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				return new java.util.Date(buffer.getLong());
			}
	    	
	    });
	    
	    registCoder(new AbstractTypeCoder<java.sql.Date>(type--,java.sql.Date.class) {
			@Override
			public void encodeData(ByteBuffer buffer, java.sql.Date val, Class<?> fieldDeclareType,Type genericType) {
				buffer.putLong(val.getTime());
			}

			@Override
			public java.sql.Date decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				return new java.sql.Date(buffer.getLong());
			}
	    	
	    });
	    
	    registCoder(new AbstractTypeCoder<Map>(type--,Map.class) {
			@Override
			public void encodeData(ByteBuffer buffer, Map val, Class<?> fieldDeclareType,Type genericType) {
				 if(genericType == null) {
					 genericType = val.getClass().getGenericSuperclass();
				 }
				 
				 Class<?> keyType = null;
				 Class<?> valueType = null;
				 
				 if(genericType != null) {
					 keyType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
					 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,1);
				 }
				 
				 encodeMap(buffer,(Map<Object,Object>)val,keyType,valueType);
			}

			@Override
			public Map decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				decodeMap(buffer,(ParameterizedType)genericType);
				return null;
			}
	    	
	    });
	    
	    registCoder(new AbstractTypeCoder<Set>(type--,Set.class) {
			@Override
			public void encodeData(ByteBuffer buffer, Set val, Class<?> fieldDeclareType, Type genericType) {
				 encodeCollection(buffer,val,fieldDeclareType,genericType);
			}

			@Override
			public Set decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				 Set result = new HashSet();
				 decodeCollection(buffer,result,declareFieldType,genericType);
				return result ;
			}
	    });
	    
	    registCoder(new AbstractTypeCoder<List>(type--,List.class) {
			@Override
			public void encodeData(ByteBuffer buffer, List val, Class<?> fieldDeclareType, Type genericType) {
				 encodeCollection(buffer,val,fieldDeclareType,genericType);
			}

			@Override
			public List decodeData(ByteBuffer buffer, Class<?> declareFieldType,Type genericType) {
				List result = new ArrayList();
				 decodeCollection(buffer,result,declareFieldType,genericType);
				return result ;
			}
	    	
	    });
		 
  }
   
   private static class VoidTypeCoder implements TypeCoder<Void>{
	   
	   private short code;
	   private Class<Void> clazz;
	   
	   public VoidTypeCoder(short code) {
		   this.code = code;
		   this.clazz = Void.class;
	   }
	   
	   @Override
		public boolean canSupport(Class<?> clazz) {
			return false;
		}
	   
		@Override
		public Class<Void> type() {
			return clazz;
		}
	
		@Override
		public short code() {
			return code;
		}

		@Override
		public void encode(ByteBuffer buffer, Void val, Class<?> declareFieldType, Type genericType) {
			//空前缀类型，没有任何数据
			buffer.put(Decoder.PREFIX_TYPE_NULL);
		}

		@Override
		public Void decode(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
			return null;
		}
   }
   
   public static void setTransformClazzLoader(IClientTransformClassLoader l) {
	   if(clazzLoader != null) {
		   throw new CommonException(clazzLoader.getClass().getName()+" have been set before "+l.getClass().getName());
	   }
	   clazzLoader = l;
   }
   
   public static void putLength(ByteBuffer buffer,int len) {
		buffer.putShort((short)len);
	}
   
    public static Class<?> getClassByProvider(Short type) {
		if(clazzLoader != null) {
			return clazzLoader.getClazz(type);
		}
		return null;
	}
	
	public static synchronized void registCoder(TypeCoder<?> coder){
		if(clazz2Coder.containsKey(coder.type())){
			logger.error("clazz["+coder.type().getName()+"], code ["+coder.code()+"] REregist");
			return;
		}
		clazz2Coder.put(coder.type(),coder);
		code2Coder.put(coder.code(), coder);
	}
	
	 @SuppressWarnings("unchecked")
	public static  <T>  TypeCoder<T> getCoder(Class<T> cls){
		 TypeCoder<T> c = clazz2Coder.get(cls);
		 
		 if(c == null) {
			 for(TypeCoder<T> e : code2Coder.values()) {
				if(e.canSupport(cls)) {
					return e;
				}
			}
		 }
		 
		 return c != null? c : (TypeCoder<T>)defaultCoder;
	}
	
	 /**
	  * 解码时从code取解码器
	  * @param code
	  * @return
	  */
	@SuppressWarnings("unchecked")
	public static  <T>  TypeCoder<T> getCoder(Short code){
		TypeCoder<T> c = code2Coder.get(code);
		return c != null? c : (TypeCoder<T>)defaultCoder;
	}
	
	/**
    * 对那些没有指定编码器的类做编码，此类前缀肯定是全类名
    * 
    * @param buffer
    * @param obj
    * @param fieldDeclareType
    */
	public static void encodeByReflect(ByteBuffer buffer, Object obj, Class<?> fieldDeclareType,Type genericType) {

		// 进入此方法，obj必须不能为NULL
		Class<?> cls = obj.getClass();

		if(!Modifier.isPublic(cls.getModifiers())) {
			// 非公有类，不能做序列化
			throw new CommonException("should be public class [" + cls.getName() + "]");
		}

		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames, cls);
		fieldNames.sort((v1, v2) -> v1.compareTo(v2));

		for (int i = 0; i < fieldNames.size(); i++) {

			String fn = fieldNames.get(i);

			Field f = Utils.getIns().getClassField(cls, fn);

			Object v = TypeUtils.getFieldValue(obj, f);

			if (v == null) {
				voidCoder.encode(buffer, null, f.getType(),null);
			} else {
				defaultCoder.encode(buffer, v, f.getType(),f.getGenericType());
			}

		}
	}
	
	public static void encodeCollection(ByteBuffer buffer, Collection objs, Class<?> declareFieldType, Type genericType) {
		if(objs == null || objs.isEmpty()) {
			putLength(buffer,0);
			return;
		}
		
		if(genericType == null) {
			 genericType = objs.getClass().getGenericSuperclass();
		 }
		 
		 Class<?> valueType = null;
		 
		 if(genericType != null) {
			 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
		 }
		
		putLength(buffer,objs.size());
		for(Object o: objs){
			defaultCoder.encode(buffer, o, valueType, genericType);
		}
		
	}
	
	public static <K,V> void encodeMap(ByteBuffer buffer,Map<K,V> map, Class<?> keyType, Class<?> valueType){
		if(map == null) {
			putLength(buffer,0);
			return;
		}
		int len = map.size();
		putLength(buffer,len);
		
		if(len <=0) {
			return;
		}
		
		for(Map.Entry<K,V> e: map.entrySet()){
			
			if(e.getKey() == null) {
				voidCoder.encode(buffer, null, Void.class, null);
			} else {
				defaultCoder.encode(buffer, e.getKey(), keyType, null);
			}

			if(e.getValue() == null) {
				voidCoder.encode(buffer, null, Void.class, null);
			} else {
				defaultCoder.encode(buffer, e.getValue(), valueType, null);
			}
		
		}
	}
	
	public static <V> void encodeArray(ByteBuffer buffer, Object objs, Class<?> fieldDeclareType, Type genericType){
		if(objs == null) {
			putLength(buffer,0);
			return;
		}
		int len = Array.getLength(objs);
		putLength(buffer,len);
		
		if(len <=0) {
			return;
		}
		
		//boolean nw = TypeUtils.isFinal(objs.getClass().getComponentType());
		
		for(int i = 0; i < len; i++){
			Object v = Array.get(objs, i);
			defaultCoder.encode(buffer, v, objs.getClass().getComponentType(), genericType);
		}
	}
	
	public  <T> TypeCoder<T> getByCode(short code) {
		return getCoder(code);
	}
	
	public <T> TypeCoder<T> getByClass(Class<T> clazz) {
		return getCoder(clazz);
	}
	
	
	/**********************************解码相关代码开始**************************************/
	
	public static Class<?> getType(ByteBuffer buffer) {

		String clsName = decodeString(buffer);
		if(clsName == null) {
			throw new CommonException("Decode invalid class full name!");
		}
		Class<?> cls = null;
		try {
			if(clsName.startsWith("[L")) {
				clsName = clsName.substring(2,clsName.length()-1);
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
				cls = Array.newInstance(cls, 0).getClass();
			} else {
				cls = Thread.currentThread().getContextClassLoader()
						.loadClass(clsName);
			}
		} catch (ClassNotFoundException e) {
			throw new CommonException("class not found:" + clsName,e);
		}
	    return cls;
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
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object decodeByReflect(ByteBuffer buffer, Class<?> cls,Type genericType) {
		
		int m = cls.getModifiers();
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m) || !Modifier.isPublic(m)){
			logger.warn("decodeByReflect class [{}] not support decode",cls.getName());
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

			Field f = Utils.getIns().getClassField(cls, fieldNames.get(i));

			Object v = null;
			TypeCoder<?> coder = null;
			
			if(!TypeUtils.isFinal(f.getType())) {
				byte prefixCodeType = buffer.get();
				if(prefixCodeType == Decoder.PREFIX_TYPE_NULL){
					TypeUtils.setFieldValue(obj, null, f);
					continue;
				}else if(Decoder.PREFIX_TYPE_STRING == prefixCodeType) {
					coder = defaultCoder;
				}else if(Decoder.PREFIX_TYPE_SHORT == prefixCodeType) {
					Short code = buffer.getShort();
					coder = TypeCoderFactory.getCoder(code);
				} else {
					throw new CommonException("not support prefix type:" + prefixCodeType);
				}
			} else {
				coder = TypeCoderFactory.getCoder(f.getType());
			}
			
			v = coder.decode(buffer, f.getType(), f.getGenericType());
			TypeUtils.setFieldValue(obj, v, f);
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	public static Map<Object,Object> decodeMap(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getShort();
		if(len <= 0) {
			return Collections.EMPTY_MAP;
		}
		
		Class<?> keyType = TypeUtils.finalParameterType(paramType,0);
		Class<?> valType = TypeUtils.finalParameterType(paramType,1);
		
		Map<Object,Object> map = new HashMap<>();
		for(; len > 0; len--) {
			Object key = defaultCoder.decode(buffer, keyType, null);
			Object value = defaultCoder.decode(buffer, valType, null);
			map.put(key, value);
		}
		
		return map;
	}
	
	public static void decodeCollection(ByteBuffer buffer, Collection coll, Class<?> declareType,Type genericType){
		int len = buffer.getShort();
		if(len <= 0) {
			return ;
		}
		
		Class<?> valueType = null;
		 if(genericType != null) {
			 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
		 }
		 
		for(int i =0; i <len; i++){
			Object v = defaultCoder.decode(buffer, valueType, genericType);
			if(v != null) {
				coll.add(v);
			}
		}
	}
	
	public static Object decodeArray(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType){

		int len = buffer.getShort();
		if(len <= 0) {
			return null;
		}
		
		Object objs = Array.newInstance(Object.class, len);
		for(int i = 0; i < len; i++){
			Array.set(objs, i, defaultCoder.decode(buffer, fieldDeclareType, genericType));
		}
		
		return objs;
	}
	
	/**********************************解码相关代码结束**************************************/
}
