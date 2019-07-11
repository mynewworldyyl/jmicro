package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jmicro.api.codec.typecoder.AbstractComparableTypeCoder;
import org.jmicro.api.codec.typecoder.AbstractFinalTypeCoder;
import org.jmicro.api.codec.typecoder.AbstractShortTypeCoder;
import org.jmicro.api.codec.typecoder.ArrayCoder;
import org.jmicro.api.codec.typecoder.DefaultCoder;
import org.jmicro.api.codec.typecoder.PrimitiveTypeArrayCoder;
import org.jmicro.api.codec.typecoder.PrimitiveTypeCoder;
import org.jmicro.api.codec.typecoder.ReflectTypeCoder;
import org.jmicro.api.codec.typecoder.TypeCoder;
import org.jmicro.api.codec.typecoder.VoidTypeCoder;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 编码： 1. 编码时直接根据值的class取得对应的编码器,然后调用encode方法; 2.
 * 对于对像成员变量,首先判断成员变量声明类型是否是final类型,如果是，直接写编码数据，否则进入步聚3; 3.
 * 写入一个字节类型编码前缀类型,写入类型编码，写入编码数据；
 * 
 * 解码: 1. 首先从buffer读取类型信息，然后根据类型信息取得解码器，调用decode解码; 2.
 * 对于对像成员变量，首先判断成员变量声明类型是否是final类型，如果是，则直接根据声明类型调用decode方法解码，如果不是，则进入步聚１
 * 
 * @author Yulei Ye
 * @date 2018年12月26日 上午10:53:38
 */
//@Component
public class TypeCoderFactory {

	private static final Logger logger = LoggerFactory.getLogger(TypeCoderFactory.class);

	private static final TypeCoderFactory ins = new TypeCoderFactory();

	public static TypeCoderFactory getIns() {
		return ins;
	}

	@SuppressWarnings("rawtypes")
	private static Map<Short, TypeCoder> code2Coder = new TreeMap<>();

	@SuppressWarnings("rawtypes")
	private static Map<Class, TypeCoder> clazz2Coder = new HashMap<>();
	
	@SuppressWarnings("rawtypes")
	private static Map<Short, Class<?>> code2class = new HashMap<>();
	
	@SuppressWarnings("rawtypes")
	private static Map<Class<?>,Short> class2code = new HashMap<>();

	// static Short currentTypeCode = (short)(NON_ENCODE_TYPE + 1);

	private static IClientTransformClassLoader clazzLoader = null;

	private static short type = (short) 0xFFFE;

	private TypeCoderFactory() {
	}

	@SuppressWarnings("rawtypes")
	private static TypeCoder<Object> defaultCoder = new DefaultCoder();

	private static final TypeCoder<Void> voidCoder = new VoidTypeCoder(type--);

	public static TypeCoder<Object> getDefaultCoder() {
		return defaultCoder;
	}

	public static TypeCoder<Void> getVoidCoder() {
		return voidCoder;
	}
	
	public static short type() {
		return type--;
	}

	static {

		registClass(ArrayList.class);
		registClass(LinkedList.class);
		registClass(HashSet.class);
		registClass(HashMap.class);
		registClass(Hashtable.class);
		
		registCoder(new ArrayCoder(type--));

		registCoder(new AbstractFinalTypeCoder<String>(type--, String.class) {
			@Override
			public String decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
				try {
					return buffer.readUTF();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public void encodeData(DataOutput buffer, String val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.writeUTF(val);
			}

		});
		registClass(String.class,type);

		// registCoder(new VoidTypeCoder<Void>(type--,Void.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Byte.class));
		registClass(Byte.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Byte.TYPE));
		registClass(Byte.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Short.class));
		registClass(Short.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Short.TYPE));
		registClass(Short.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Integer.class));
		registClass(Integer.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Integer.TYPE));
		registClass(Integer.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Long.class));
		registClass(Long.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Long.TYPE));
		registClass(Long.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Double.class));
		registClass(Double.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Double.TYPE));
		registClass(Double.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Float.class));
		registClass(Float.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Float.TYPE));
		registClass(Float.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Boolean.class));
		registClass(Boolean.class,type);
		registCoder(new PrimitiveTypeCoder(type--, Boolean.TYPE));
		registClass(Boolean.TYPE,type);

		registCoder(new PrimitiveTypeCoder(type--, Character.class));
		registCoder(new PrimitiveTypeCoder(type--, Character.TYPE));

		registCoder(new PrimitiveTypeArrayCoder(type--, byte[].class));
		registClass(byte[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, short[].class));
		registClass(short[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, int[].class));
		registClass(int[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, long[].class));
		registClass(long[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, float[].class));
		registClass(float[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, double[].class));
		registClass(double[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, boolean[].class));
		registClass(boolean[].class,type);
		registCoder(new PrimitiveTypeArrayCoder(type--, char[].class));
		registClass(char[].class,type);

		registCoder(new AbstractShortTypeCoder<java.util.Date>(type--, java.util.Date.class) {
			@Override
			public void encodeData(DataOutput buffer, java.util.Date val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.writeLong(val.getTime());
			}

			@Override
			public java.util.Date decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					return new java.util.Date(buffer.readLong());
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

		});
		registClass(java.util.Date.class,type);

		registCoder(new AbstractShortTypeCoder<java.sql.Date>(type--, java.sql.Date.class) {
			@Override
			public void encodeData(DataOutput buffer, java.sql.Date val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.writeLong(val.getTime());
			}

			@Override
			public java.sql.Date decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					return new java.sql.Date(buffer.readLong());
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
		});
		registClass(java.sql.Date.class,type);

		registCoder(new AbstractComparableTypeCoder<Map>(Decoder.PREFIX_TYPE_MAP,type--,Map.class) {
			
			@Override
			public Map decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
				return TypeCoder.decodeMap(buffer, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void encode(DataOutput buffer, Map val, Class<?> fieldDeclareType, 
					Type genericType) throws IOException {
				buffer.write(Decoder.PREFIX_TYPE_MAP);
				TypeCoder.encodeMap(buffer, (Map) val, TypeCoder.genericType(genericType));
			}
		});
		registClass(Map.class,type);

		registCoder(new AbstractComparableTypeCoder<Set>(Decoder.PREFIX_TYPE_SET,type--, Set.class) {
			
			@Override
			public void encode(DataOutput buffer, Set val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.write(Decoder.PREFIX_TYPE_SET);
				TypeCoder.encodeCollection(buffer, val, fieldDeclareType, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings("rawtypes")
			@Override
			public Set decode(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				Set result = new HashSet();
				TypeCoder.decodeCollection(buffer, result, declareFieldType, TypeCoder.genericType(genericType));
				return result;
			}
			
		});
		registClass(Set.class,type);

		registCoder(new AbstractComparableTypeCoder<List>(Decoder.PREFIX_TYPE_LIST,type--, List.class) {
			@SuppressWarnings("rawtypes")
			@Override
			public void encode(DataOutput buffer, List val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.write(Decoder.PREFIX_TYPE_LIST);
				TypeCoder.encodeCollection(buffer, val, fieldDeclareType, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings("rawtypes")
			@Override
			public List decode(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				List result = new ArrayList();
				TypeCoder.decodeCollection(buffer, result, declareFieldType, TypeCoder.genericType(genericType));
				return result;
			}
			
		});
		registClass(List.class,type);

		registCoder(new AbstractShortTypeCoder<ByteBuffer>(type--, ByteBuffer.class) {
			@Override
			public void encodeData(DataOutput buffer, ByteBuffer val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				TypeCoder.putLength(buffer, val.remaining());
				byte[] data = new byte[val.remaining()];
				val.get(data);
				buffer.write(data);
			}

			@Override
			public ByteBuffer decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					int len = buffer.readShort();
					byte[] data = new byte[len];
					buffer.readFully(data, 0, len);
					return ByteBuffer.wrap(data);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				
			}

		});
		registClass(ByteBuffer.class,type);

		registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		registClass(RpcRequest.class,type);
		registCoder(new ReflectTypeCoder<RpcResponse>(type--, RpcResponse.class));
		registClass(RpcResponse.class,type);
		registCoder(new ReflectTypeCoder<ApiRequest>(type--, ApiRequest.class));
		registClass(ApiRequest.class,type);
		registCoder(new ReflectTypeCoder<ApiResponse>(type--, ApiResponse.class));
		registClass(ApiResponse.class,type);
		registCoder(new ReflectTypeCoder<SubmitItem>(type--, SubmitItem.class));
		registClass(SubmitItem.class,type);
		
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));

		registCoder(getDefaultCoder());
		registCoder(getVoidCoder());
		
		registClass(Void.class,voidCoder.code());
		

	}

	public static void setTransformClazzLoader(IClientTransformClassLoader l) {
		if (clazzLoader != null) {
			throw new CommonException(
					clazzLoader.getClass().getName() + " have been set before " + l.getClass().getName());
		}
		clazzLoader = l;
	}

	public <T> TypeCoder<T> getByCode(short code) {
		return getCoder(code);
	}

	public <T> TypeCoder<T> getByClass(Class<T> clazz) {
		return getCoder(clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> TypeCoder<T> getCoder(Class<T> cls) {
		TypeCoder<T> c = clazz2Coder.get(cls);

		if (c == null) {
			for (TypeCoder<T> e : code2Coder.values()) {
				if (e.canSupport(cls)) {
					return e;
				}
			}
		}

		return c != null ? c : (TypeCoder<T>) defaultCoder;
	}

	/**
	 * 解码时从code取解码器
	 * 
	 * @param code
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> TypeCoder<T> getCoder(Short code) {
		TypeCoder<T> c = code2Coder.get(code);
		return c != null ? c : (TypeCoder<T>) defaultCoder;
	}

	public static Class<?> getClassByProvider(Short type) {
		if (clazzLoader != null) {
			return clazzLoader.getClazz(type);
		}
		return null;
	}

	public static synchronized void registCoder(TypeCoder<?> coder) {
		if (clazz2Coder.containsKey(coder.type())) {
			logger.error("clazz[" + coder.type().getName() + "], code [" + coder.code() + "] REregist");
			return;
		}
		clazz2Coder.put(coder.type(), coder);
		code2Coder.put(coder.code(), coder);
		//registClass(coder.code(), coder.type());
	}
	
	private static synchronized void registClass(Class<?> cls,Short t) {
		if(class2code.containsKey(cls)) {
			return;
		}
		code2class.put(t,cls);
		class2code.put(cls, t);
	}
	
	public static synchronized void registClass(Class<?> cls) {
		if(class2code.containsKey(cls)) {
			return;
		}
		Short t = type--;
		code2class.put(t,cls);
		class2code.put(cls, t);
	}
	
	public static synchronized Class<?> getClassByCode(Short type) {
		return code2class.get(type);
	}
	
	public static synchronized Short getCodeByClass(Class<?> cls) {
		return class2code.get(cls);
	}
}
