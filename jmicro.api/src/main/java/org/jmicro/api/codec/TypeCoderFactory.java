package org.jmicro.api.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

	static {

		registCoder(new ArrayCoder(type--));

		registCoder(new AbstractFinalTypeCoder<String>(type--, String.class) {
			@Override
			public String decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
				return OnePrefixDecoder.decodeString(buffer);
			}

			@Override
			public void encodeData(ByteBuffer buffer, String val, Class<?> fieldDeclareType, Type genericType) {
				OnePrefixTypeEncoder.encodeString(buffer, (String) val);
			}

		});

		// registCoder(new VoidTypeCoder<Void>(type--,Void.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Byte.class));
		registCoder(new PrimitiveTypeCoder(type--, Byte.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Short.class));
		registCoder(new PrimitiveTypeCoder(type--, Short.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Integer.class));
		registCoder(new PrimitiveTypeCoder(type--, Integer.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Long.class));
		registCoder(new PrimitiveTypeCoder(type--, Long.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Double.class));
		registCoder(new PrimitiveTypeCoder(type--, Double.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Float.class));
		registCoder(new PrimitiveTypeCoder(type--, Float.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Boolean.class));
		registCoder(new PrimitiveTypeCoder(type--, Boolean.TYPE));

		registCoder(new PrimitiveTypeCoder(type--, Character.class));
		registCoder(new PrimitiveTypeCoder(type--, Character.TYPE));

		registCoder(new PrimitiveTypeArrayCoder(type--, byte[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, short[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, int[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, long[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, float[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, double[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, boolean[].class));
		registCoder(new PrimitiveTypeArrayCoder(type--, char[].class));

		registCoder(new AbstractShortTypeCoder<java.util.Date>(type--, java.util.Date.class) {
			@Override
			public void encodeData(ByteBuffer buffer, java.util.Date val, Class<?> fieldDeclareType, Type genericType) {
				buffer.putLong(val.getTime());
			}

			@Override
			public java.util.Date decodeData(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
				return new java.util.Date(buffer.getLong());
			}

		});

		registCoder(new AbstractShortTypeCoder<java.sql.Date>(type--, java.sql.Date.class) {
			@Override
			public void encodeData(ByteBuffer buffer, java.sql.Date val, Class<?> fieldDeclareType, Type genericType) {
				buffer.putLong(val.getTime());
			}

			@Override
			public java.sql.Date decodeData(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
				return new java.sql.Date(buffer.getLong());
			}
		});

		registCoder(new AbstractComparableTypeCoder<Map>(Decoder.PREFIX_TYPE_MAP,type--,Map.class) {
			
			@Override
			public Map decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
				return TypeCoder.decodeMap(buffer, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void encode(ByteBuffer buffer, Map val, Class<?> fieldDeclareType, Type genericType) {
				buffer.put(Decoder.PREFIX_TYPE_MAP);
				TypeCoder.encodeMap(buffer, (Map) val, TypeCoder.genericType(genericType));
			}
		});

		registCoder(new AbstractComparableTypeCoder<Set>(Decoder.PREFIX_TYPE_SET,type--, Set.class) {
			
			@Override
			public void encode(ByteBuffer buffer, Set val, Class<?> fieldDeclareType, Type genericType) {
				buffer.put(Decoder.PREFIX_TYPE_SET);
				TypeCoder.encodeCollection(buffer, val, fieldDeclareType, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings("rawtypes")
			@Override
			public Set decode(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
				Set result = new HashSet();
				TypeCoder.decodeCollection(buffer, result, declareFieldType, TypeCoder.genericType(genericType));
				return result;
			}
			
		});

		registCoder(new AbstractComparableTypeCoder<List>(Decoder.PREFIX_TYPE_LIST,type--, List.class) {
			@SuppressWarnings("rawtypes")
			@Override
			public void encode(ByteBuffer buffer, List val, Class<?> fieldDeclareType, Type genericType) {
				buffer.put(Decoder.PREFIX_TYPE_LIST);
				TypeCoder.encodeCollection(buffer, val, fieldDeclareType, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings("rawtypes")
			@Override
			public List decode(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
				List result = new ArrayList();
				TypeCoder.decodeCollection(buffer, result, declareFieldType, TypeCoder.genericType(genericType));
				return result;
			}
			
		});

		registCoder(new AbstractShortTypeCoder<ByteBuffer>(type--, ByteBuffer.class) {
			@Override
			public void encodeData(ByteBuffer buffer, ByteBuffer val, Class<?> fieldDeclareType, Type genericType) {
				TypeCoder.putLength(buffer, val.remaining());
				buffer.put(val);
			}

			@Override
			public ByteBuffer decodeData(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
				int len = buffer.getShort();
				byte[] data = new byte[len];
				buffer.get(data, 0, len);
				return ByteBuffer.wrap(data);
			}

		});

		registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		registCoder(new ReflectTypeCoder<RpcResponse>(type--, RpcResponse.class));
		registCoder(new ReflectTypeCoder<ApiRequest>(type--, ApiRequest.class));
		registCoder(new ReflectTypeCoder<ApiResponse>(type--, ApiResponse.class));
		registCoder(new ReflectTypeCoder<SubmitItem>(type--, SubmitItem.class));
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));

		registCoder(getDefaultCoder());
		registCoder(getVoidCoder());

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
	}
}
