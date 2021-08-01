package cn.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.codec.typecoder.AbstractComparableTypeCoder;
import cn.jmicro.api.codec.typecoder.AbstractFinalTypeCoder;
import cn.jmicro.api.codec.typecoder.AbstractShortTypeCoder;
import cn.jmicro.api.codec.typecoder.ArrayCoder;
import cn.jmicro.api.codec.typecoder.DefaultCoder;
import cn.jmicro.api.codec.typecoder.PrimitiveTypeArrayCoder;
import cn.jmicro.api.codec.typecoder.PrimitiveTypeCoder;
import cn.jmicro.api.codec.typecoder.TypeCoder;
import cn.jmicro.api.codec.typecoder.VoidTypeCoder;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;

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
	private Map<Short, TypeCoder> code2Coder = new TreeMap<>();

	@SuppressWarnings("rawtypes")
	private Map<Class, TypeCoder> clazz2Coder = new HashMap<>();
	
	//@SuppressWarnings("rawtypes")
	private Map<Short, Class<?>> code2class = new HashMap<>();
	
	//@SuppressWarnings("rawtypes")
	private Map<Class<?>,Short> class2code = new HashMap<>();

	private ITypeCodeProducer tcp;

	private TypeCoderFactory() {
	}

	@SuppressWarnings("rawtypes")
	private TypeCoder<Object> defaultCoder = new DefaultCoder();

	private TypeCoder<Void> voidCoder = null;
	
	public synchronized void  setTypeCodeProducer(ITypeCodeProducer tc) {
		if(tcp != null) {
			throw new CommonException("ITypeCodeProducer has been set with: " + tcp.getClass().getName());
		}
		tcp = tc;
		create();
	}

	public TypeCoder<Object> getDefaultCoder() {
		checkTcp();
		return defaultCoder;
	}

	public TypeCoder<Void> getVoidCoder() {
		checkTcp();
		return voidCoder;
	}
	
	private void checkTcp() {
		if(tcp == null) {
			throw new CommonException("ITypeCodeProducer not set yet: ");
		}
	}
	
	private void create() {
		voidCoder = new VoidTypeCoder(tcp.getTypeCode(Void.class.getName()));

		registClass(ArrayList.class);
		registClass(LinkedList.class);
		registClass(HashSet.class);
		registClass(HashMap.class);
		registClass(Hashtable.class);
		
		registCoder(new ArrayCoder(tcp.getTypeCode(Object[].class.getName())));

		registCoder(new AbstractFinalTypeCoder<String>(tcp.getTypeCode(String.class.getName()), String.class) {
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
		registClass(String.class,tcp.getTypeCode(String.class.getName()));

		// registCoder(new VoidTypeCoder<Void>(type--,Void.TYPE));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Byte.class.getName()), Byte.class));
		registClass(Byte.class, tcp.getTypeCode(Byte.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Byte.TYPE.getName()), Byte.TYPE));
		registClass(Byte.TYPE, tcp.getTypeCode(Byte.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Short.class.getName()), Short.class));
		registClass(Short.class,tcp.getTypeCode(Short.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Short.TYPE.getName()), Short.TYPE));
		registClass(Short.TYPE,tcp.getTypeCode(Short.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Integer.class.getName()), Integer.class));
		registClass(Integer.class,tcp.getTypeCode(Integer.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Integer.TYPE.getName()), Integer.TYPE));
		registClass(Integer.TYPE,tcp.getTypeCode(Integer.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Long.class.getName()), Long.class));
		registClass(Long.class,tcp.getTypeCode(Long.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Long.TYPE.getName()), Long.TYPE));
		registClass(Long.TYPE,tcp.getTypeCode(Long.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Double.class.getName()), Double.class));
		registClass(Double.class,tcp.getTypeCode(Double.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Double.TYPE.getName()), Double.TYPE));
		registClass(Double.TYPE,tcp.getTypeCode(Double.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Float.class.getName()), Float.class));
		registClass(Float.class,tcp.getTypeCode(Float.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Float.TYPE.getName()), Float.TYPE));
		registClass(Float.TYPE,tcp.getTypeCode(Float.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Boolean.class.getName()), Boolean.class));
		registClass(Boolean.class,tcp.getTypeCode(Boolean.class.getName()));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Boolean.TYPE.getName()), Boolean.TYPE));
		registClass(Boolean.TYPE,tcp.getTypeCode(Boolean.TYPE.getName()));

		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Character.class.getName()), Character.class));
		registCoder(new PrimitiveTypeCoder(tcp.getTypeCode(Character.TYPE.getName()), Character.TYPE));

		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(byte[].class.getName()), byte[].class));
		registClass(byte[].class,tcp.getTypeCode(byte[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(short[].class.getName()), short[].class));
		registClass(short[].class,tcp.getTypeCode(short[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(int[].class.getName()), int[].class));
		registClass(int[].class,tcp.getTypeCode(int[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(long[].class.getName()), long[].class));
		registClass(long[].class,tcp.getTypeCode(long[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(float[].class.getName()), float[].class));
		registClass(float[].class,tcp.getTypeCode(float[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(double[].class.getName()), double[].class));
		registClass(double[].class,tcp.getTypeCode(double[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(boolean[].class.getName()), boolean[].class));
		registClass(boolean[].class,tcp.getTypeCode(boolean[].class.getName()));
		registCoder(new PrimitiveTypeArrayCoder(tcp.getTypeCode(char[].class.getName()), char[].class));
		registClass(char[].class,tcp.getTypeCode(char[].class.getName()));

		registCoder(new AbstractShortTypeCoder<java.util.Date>(tcp.getTypeCode(java.util.Date.class.getName()), java.util.Date.class) {
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
		registClass(java.util.Date.class,tcp.getTypeCode(java.util.Date.class.getName()));
		
		registCoder(new AbstractShortTypeCoder<java.time.LocalDate>(tcp.getTypeCode(java.time.LocalDate.class.getName()), java.time.LocalDate.class) {
			@Override
			public void encodeData(DataOutput buffer, java.time.LocalDate val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.writeShort(val.getYear());
				buffer.write(val.getMonthValue());
				buffer.write(val.getDayOfMonth());
			}

			@Override
			public java.time.LocalDate decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					int year = buffer.readShort();
					int month = buffer.readByte();
					int dayOfMonth = buffer.readByte();
					return java.time.LocalDate.of(year, month, dayOfMonth);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

		});
		registClass(java.time.LocalDate.class,tcp.getTypeCode(java.time.LocalDate.class.getName()));

		registCoder(new AbstractShortTypeCoder<java.time.LocalTime>(tcp.getTypeCode(java.time.LocalTime.class.getName()), java.time.LocalTime.class) {
			@Override
			public void encodeData(DataOutput buffer, java.time.LocalTime val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.write(val.getHour());
				buffer.write(val.getMinute());
				buffer.write(val.getSecond());
				buffer.writeInt(val.getNano());
			}

			@Override
			public java.time.LocalTime decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					int h = buffer.readByte();
					int m = buffer.readByte();
					int s = buffer.readByte();
					int n = buffer.readInt();
					return java.time.LocalTime.of(h, m, s, n);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

		});
		registClass(java.time.LocalTime.class,tcp.getTypeCode(java.time.LocalTime.class.getName()));

		
		registCoder(new AbstractShortTypeCoder<java.time.LocalDateTime>(tcp.getTypeCode(java.time.LocalDateTime.class.getName()), java.time.LocalDateTime.class) {
			@Override
			public void encodeData(DataOutput buffer, java.time.LocalDateTime val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				
				buffer.writeShort(val.getYear());
				buffer.write(val.getMonthValue());
				buffer.write(val.getDayOfMonth());
				
				buffer.write(val.getHour());
				buffer.write(val.getMinute());
				buffer.write(val.getSecond());
				buffer.writeInt(val.getNano());
				
			}

			@Override
			public java.time.LocalDateTime decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
				try {
					int year = buffer.readShort();
					int month = buffer.readByte();
					int dayOfMonth = buffer.readByte();
					java.time.LocalDate d = java.time.LocalDate.of(year, month, dayOfMonth);
					
					int h = buffer.readByte();
					int m = buffer.readByte();
					int s = buffer.readByte();
					int n = buffer.readInt();
					java.time.LocalTime t = java.time.LocalTime.of(h, m, s, n);
					
					return LocalDateTime.of(d, t);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

		});
		registClass(java.time.LocalDateTime.class,tcp.getTypeCode(java.time.LocalDateTime.class.getName()));

		
		
		registCoder(new AbstractShortTypeCoder<java.sql.Date>(tcp.getTypeCode(java.sql.Date.class.getName()), java.sql.Date.class) {
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
		registClass(java.sql.Date.class,tcp.getTypeCode(java.sql.Date.class.getName()));

		registCoder(new AbstractComparableTypeCoder<Map>(DecoderConstant.PREFIX_TYPE_MAP,tcp.getTypeCode(Map.class.getName()),Map.class) {
			
			@Override
			public Map decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
				return TypeCoder.decodeMap(buffer, fieldDeclareType, TypeCoder.genericType(genericType));
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void encode(DataOutput buffer, Map val, Class<?> fieldDeclareType, 
					Type genericType) throws IOException {
				buffer.write(DecoderConstant.PREFIX_TYPE_MAP);
				TypeCoder.encodeMap(buffer, (Map) val, TypeCoder.genericType(genericType));
			}
		});
		registClass(Map.class,tcp.getTypeCode(Map.class.getName()));

		registCoder(new AbstractComparableTypeCoder<Set>(DecoderConstant.PREFIX_TYPE_SET,tcp.getTypeCode(Set.class.getName()), Set.class) {
			
			@Override
			public void encode(DataOutput buffer, Set val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.write(DecoderConstant.PREFIX_TYPE_SET);
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
		registClass(Set.class,tcp.getTypeCode(Set.class.getName()));

		registCoder(new AbstractComparableTypeCoder<List>(DecoderConstant.PREFIX_TYPE_LIST,tcp.getTypeCode(List.class.getName()), List.class) {
			@SuppressWarnings("rawtypes")
			@Override
			public void encode(DataOutput buffer, List val, Class<?> fieldDeclareType,
					Type genericType) throws IOException {
				buffer.write(DecoderConstant.PREFIX_TYPE_LIST);
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
		registClass(List.class,tcp.getTypeCode(List.class.getName()));

		registCoder(new AbstractShortTypeCoder<ByteBuffer>(tcp.getTypeCode(ByteBuffer.class.getName()), ByteBuffer.class) {
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
		registClass(ByteBuffer.class,tcp.getTypeCode(ByteBuffer.class.getName()));

		/*registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		registClass(RpcRequest.class,type);
		
		registCoder(new ReflectTypeCoder<RpcResponse>(type--, RpcResponse.class));
		registClass(RpcResponse.class,type);
		
		registCoder(new ReflectTypeCoder<ApiRequest>(type--, ApiRequest.class));
		registClass(ApiRequest.class,type);
		
		registCoder(new ReflectTypeCoder<ApiResponse>(type--, ApiResponse.class));
		registClass(ApiResponse.class,type);
		
		registCoder(new ReflectTypeCoder<MRpcItem>(type--, MRpcItem.class));
		registClass(MRpcItem.class,type);
		
		registCoder(new ReflectTypeCoder<AsyncConfig>(type--, AsyncConfig.class));
		registClass(AsyncConfig.class,type);
		
		registCoder(new ReflectTypeCoder<PSData>(type--, PSData.class));
		registClass(PSData.class,type);
		
		registCoder(new ReflectTypeCoder<ServiceMethod>(type--, ServiceMethod.class));
		registClass(ServiceMethod.class,type);*/
		
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));
		//registCoder(new ReflectTypeCoder<RpcRequest>(type--, RpcRequest.class));

		registCoder(getDefaultCoder());
		registCoder(getVoidCoder());
		
		registClass(Void.class,voidCoder.code());
		
		registerSOClass();
	
	}
	
	private void registerSOClass() {
		Set<Class<?>> sos = ClassScannerUtils.getIns().loadClassesByAnno(SO.class);
		if(sos != null && !sos.isEmpty()) {
			for(Class<?> c : sos){
				this.registClass(c);
			}
		}
	}

	public <T> TypeCoder<T> getByCode(short code) {
		checkTcp();
		return getCoder(code);
	}

	public <T> TypeCoder<T> getByClass(Class<T> clazz) {
		checkTcp();
		return getCoder(clazz);
	}

	@SuppressWarnings("unchecked")
	public <T> TypeCoder<T> getCoder(Class<T> cls) {
		checkTcp();
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
	public <T> TypeCoder<T> getCoder(Short code) {
		checkTcp();
		checkTcp();
		TypeCoder<T> c = code2Coder.get(code);
		return c != null ? c : (TypeCoder<T>) defaultCoder;
	}

	public synchronized void registCoder(TypeCoder<?> coder) {
		checkTcp();
		if (clazz2Coder.containsKey(coder.type())) {
			System.out.println("clazz[" + coder.type().getName() + "], code [" + coder.code() + "] REregist");
			return;
		}
		clazz2Coder.put(coder.type(), coder);
		code2Coder.put(coder.code(), coder);
		//registClass(coder.code(), coder.type());
	}
	
	private synchronized void registClass(Class<?> cls,Short t) {
		checkTcp();
		if(class2code.containsKey(cls)) {
			return;
		}
		code2class.put(t,cls);
		class2code.put(cls, t);
	}
	
	public synchronized void registClass(Class<?> cls) {
		checkTcp();
		if(class2code.containsKey(cls)) {
			return;
		}
		Short t = tcp.getTypeCode(cls.getName());
		code2class.put(t,cls);
		class2code.put(cls, t);
	}
	
	public synchronized Class<?> getClassByCode(Short type) {
		checkTcp();
		if(code2class.containsKey(type)) {
			return code2class.get(type);
		}
		
		String name = tcp.getNameByCode(type);
		if(Utils.isEmpty(name)) {
			return null;
		}
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = TypeCoderFactory.class.getClassLoader();
		}
		
		try {
			Class<?> c = cl.loadClass(name);
			registClass(c,type);
			return c;
		} catch (ClassNotFoundException e) {
			logger.error(name,e);
			return null;
		}
		
	}
	
	public synchronized Short getCodeByClass(Class<?> cls) {
		checkTcp();
		return class2code.get(cls);
	}
}
