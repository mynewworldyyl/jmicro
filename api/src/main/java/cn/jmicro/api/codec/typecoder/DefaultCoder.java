package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.codec.Decoder;
import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.codec.ISerializer;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.codec.SerializeProxyFactory;
import cn.jmicro.api.codec.TypeCoderFactory;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.common.CommonException;


/**
 * 从最外层进来，fieldDeclareType和genericType都是空值，此时必须有类型信息
 * 
 * 从反射类型进来时，fieldDeclareType和genericType为非空值，此时判断fieldDeclareType是否是final类型，如果是，
 * 只需要写入类型前缀信息，不需要写入类型编码信息，否则需写入类型信息。
 * 
 * 类型信息包括前缀码和类型编码，针对高频使用的类，如Map，Set，List等，前缀码和类型码合并，直接由前缀码确定解码器
 *
 * 前缀码分为空值前缀码，表示一个空值，解码时直接返回NULL即可
 *
 * 类型编码前缀，表示接下来读取一个类型编码，由类型编码确定类型信息，并依据类型信息读取编码解码器
 *
 * 字符串类型编码前缀，表示接下来读取一个字符串表示类型全名称，由全名称加载类对像，然后由类对像获取类型编码器
 * 
 * 最高率的编码方式是使用final类作为传输类型
 * 
 * 编码时，类型前缀码，类开编码，由具体类型编码器写入，DefaultCoder只负责获取类型编码器然后编码。
 * 规则：
 * 1. 如果是NULL值，直接写入Decoder.PREFIX_TYPE_NULL，编码结束；
 * 2. 如果值为非空值，由val.getClass()获取编码器，调用编码方法编码类型前缀码，类型编码，数据
 *    2.1 如果获取到的编码器是DefaultCoder编码器，调用TypeCoder.encodeByReflect方法对数据进行反射编码
 *        写入Decoder.PREFIX_TYPE_STRING，写入类全路径名，然后通过反射编码每个非transient字段
 *    2.2  如果获取非DefaultCoder编码器， 调用编码器编码方法，并写入类型编码，
 *          如果fieldDeclareType非空，判断fieldDeclareType是否是final,如果是，则写入PREFIX_TYPE_FINAL，并编码数据；
 *          如果fieldDeclareType不是final或是空值，在编码器中写入Decoder.PREFIX_TYPE_SHORT前缀编码，类型编码和数据
 * 
 * 解码时，DefaultCoder读取类型前缀码，由类型前缀码获取类，最后获取解码器，从需解码。
 * 1. 如果前缀编码是Decoder.PREFIX_TYPE_NULL，直接返回空值
 * 2. 如果前缀编码是Decoder.PREFIX_TYPE_FINAL，直接由fieldDeclareType确定解码器并解码数据；
 * 3. 如果前缀编码是Decoder.PREFIX_TYPE_STRING，则由TypeCoder.decodeByReflect解码数据
 * 4. 如果前缀编码是Decoder.PREFIX_TYPE_SHORT,则读取类型编码，获取解码器，直接解码数据
 * 5. 如果前缀编码是Decoder.PREFIX_TYPE_LIST,PREFIX_TYPE_SET,PREFIX_TYPE_MAP,则直接使用List，Set，Map解码器解码
 * 
 * @author Yulei Ye
 * @date 2018年12月28日 下午10:57:11
 */
public class DefaultCoder implements TypeCoder<Object> {
	
	public DefaultCoder() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void encode(DataOutput buffer, Object val, Class<?> fieldDeclareType
			, Type genericType) throws IOException {
		//val impossible to be null
		if(fieldDeclareType == null) {
			if(val == null) {
				buffer.write(Decoder.PREFIX_TYPE_NULL);
				return;
			}
			Class valCls = val.getClass();
			if(valCls == byte.class || valCls == Byte.TYPE || valCls == Byte.class ) {
				buffer.write(Decoder.PREFIX_TYPE_BYTE);
				buffer.writeByte((byte)val);
				return;
			}else if(valCls == short.class || valCls == Short.TYPE || valCls == Short.class ) {
				buffer.write(Decoder.PREFIX_TYPE_SHORTT);
				buffer.writeShort((short)val);
				return;
			}else if(valCls == int.class || valCls == Integer.TYPE || valCls == Integer.class ) {
				buffer.write(Decoder.PREFIX_TYPE_INT);
				buffer.writeInt((int)val);
				return;
			}else if(valCls == long.class || valCls == Long.TYPE || valCls == Long.class ) {
				buffer.write(Decoder.PREFIX_TYPE_LONG);
				buffer.writeLong((long)val);
				return;
			}else if(valCls == float.class || valCls == Float.TYPE || valCls == Float.class ) {
				buffer.write(Decoder.PREFIX_TYPE_FLOAT);
				buffer.writeFloat((float)val);
				return;
			}else if(valCls == double.class || valCls == Double.TYPE || valCls == Double.class ) {
				buffer.write(Decoder.PREFIX_TYPE_DOUBLE);
				buffer.writeDouble((double)val);
				return;
			}else if(valCls == boolean.class || valCls == Boolean.TYPE || valCls == Boolean.class ) {
				buffer.write(Decoder.PREFIX_TYPE_BOOLEAN);
				buffer.writeBoolean((boolean)val);
				return;
			}else if(valCls == char.class || valCls == Character.TYPE || valCls == Character.class ) {
				buffer.write(Decoder.PREFIX_TYPE_CHAR);
				buffer.writeChar((char)val);
				return;
			}else if(valCls == String.class ) {
				buffer.write(Decoder.PREFIX_TYPE_STRINGG);
				buffer.writeUTF((String)val);
				return;
			}else if(valCls == Date.class ) {
				buffer.write(Decoder.PREFIX_TYPE_DATE);
				buffer.writeLong(((Date)val).getTime());
				return;
			}
		} else {
			
			if(fieldDeclareType == byte.class || fieldDeclareType == Byte.TYPE || fieldDeclareType == Byte.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeByte((byte)val);
				return;
			}else if(fieldDeclareType == short.class || fieldDeclareType == Short.TYPE || fieldDeclareType == Short.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeShort((short)val);
				return;
			}else if(fieldDeclareType == int.class || fieldDeclareType == Integer.TYPE || fieldDeclareType == Integer.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeInt((int)val);
				return;
			}else if(fieldDeclareType == long.class || fieldDeclareType == Long.TYPE || fieldDeclareType == Long.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeLong((long)val);
				return;
			}else if(fieldDeclareType == float.class || fieldDeclareType == Float.TYPE || fieldDeclareType == Float.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeFloat((float)val);
				return;
			}else if(fieldDeclareType == double.class || fieldDeclareType == Double.TYPE || fieldDeclareType == Double.class ) {
				if(val == null) {
					val = 0;
				}
				buffer.writeDouble((double)val);
				return;
			}else if(fieldDeclareType == boolean.class || fieldDeclareType == Boolean.TYPE || fieldDeclareType == Boolean.class ) {
				if(val == null) {
					val = false;
				}
				buffer.writeBoolean((boolean)val);
				return;
			}else if(fieldDeclareType == char.class || fieldDeclareType == Character.TYPE || fieldDeclareType == Character.class ) {
				if(val == null) {
					buffer.writeChar(0);
				}else {
					buffer.writeChar((char)val);
				}
				return;
			}else if(fieldDeclareType == String.class ) {
				if(val == null) {
					val = "";
				}
				buffer.writeUTF((String)val);
				return;
			}else if(fieldDeclareType == Date.class ) {
				if(val == null) {
					val = 0L;
				}
				buffer.writeLong(((Date)val).getTime());
				return;
			}
		}
		
		if(val == null) {
			buffer.write(Decoder.PREFIX_TYPE_NULL);
			return;
		}
		
		Class<?> valCls = val.getClass();
		
		if(Collection.class.isAssignableFrom(valCls) ||
				Map.class.isAssignableFrom(valCls) || valCls.isArray()) {
			//val已经非空值，肯定能获取到一个编码器，至少是DefaultCoder
			TypeCoder coder = null;
			if(fieldDeclareType != null && TypeUtils.isFinal(fieldDeclareType)) {
				//基本数据类型的非引用类型
				coder = TypeCoderFactory.getIns().getCoder(fieldDeclareType);
			} else {
				coder = TypeCoderFactory.getIns().getCoder(val.getClass());
			}
			
			if (coder != this) {
				// 有指定类型的编码器，使用指定类型的编码器
				coder.encode(buffer, val, fieldDeclareType, genericType);
			} else {
				// 不属于任何类的成员字段时，fieldDeclareType==null
				if (fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
					//写入类型前缀码Decoder.PREFIX_TYPE_STRING，类型编码信息
					TypeCoder.putStringType(buffer, val.getClass().getName());
				} else {
					buffer.write(Decoder.PREFIX_TYPE_FINAL);
				}
				//默认编码器通过反射编码数据
				TypeCoder.encodeByReflect(buffer, val, fieldDeclareType,genericType);
			}
		
		} else {
			JDataOutput jo = (JDataOutput)buffer;
			int pos = jo.position();
			buffer.write(Decoder.PREFIX_TYPE_PROXY);
			buffer.writeUTF(val.getClass().getName());
			
			//buffer.writeShort(code);
			if(val instanceof ISerializeObject) {
				//System.out.println("Use Instance "+valCls.getName());
				((ISerializeObject)val).encode(buffer);
			} else {
				//System.out.println("Use Encoder "+valCls.getName());
				ISerializer so = null;
				Throwable e1 = null;
				try {
					so = SerializeProxyFactory.getSerializeCoder(valCls);
				} catch (Throwable e) {
					e1 = e;
				}
				if(so != null) {
					so.encode(buffer,val);
				}else {
					TypeCoder coder = TypeCoderFactory.getIns().getCoder(valCls);
					if (coder != this) {
						//有指定类型的编码器，使用指定类型的编码器
						jo.position(pos);//具体编码器写其对应的前缀码及编码
						coder.encode(buffer, val, fieldDeclareType, genericType);
					} else {
						/*if (fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
							//写入类型前缀码Decoder.PREFIX_TYPE_STRING，类型编码信息
							TypeCoder.putStringType(buffer, val.getClass().getName());
						} else {
							buffer.write(Decoder.PREFIX_TYPE_FINAL);
						}*/
						//默认编码器通过反射编码数据
						TypeCoder.encodeByReflect(buffer, val, fieldDeclareType,genericType);
					}
				}
			}
		}
	
	}

	@Override
	public Object decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
		
		try {
			
			if(fieldDeclareType != null) {
				if(fieldDeclareType == byte.class || fieldDeclareType == Byte.TYPE || fieldDeclareType == Byte.class ) {
					return buffer.readByte();
				}else if(fieldDeclareType == short.class || fieldDeclareType == Short.TYPE || fieldDeclareType == Short.class ) {
					return buffer.readShort();
				}else if(fieldDeclareType == int.class || fieldDeclareType == Integer.TYPE || fieldDeclareType == Integer.class ) {
					return buffer.readInt();
				}else if(fieldDeclareType == long.class || fieldDeclareType == Long.TYPE || fieldDeclareType == Long.class ) {
					return buffer.readLong();
				}else if(fieldDeclareType == float.class || fieldDeclareType == Float.TYPE || fieldDeclareType == Float.class ) {
					return buffer.readFloat();
				}else if(fieldDeclareType == double.class || fieldDeclareType == Double.TYPE || fieldDeclareType == Double.class ) {
					return buffer.readDouble();
				}else if(fieldDeclareType == boolean.class || fieldDeclareType == Boolean.TYPE || fieldDeclareType == Boolean.class ) {
					return buffer.readBoolean();
				}else if(fieldDeclareType == char.class || fieldDeclareType == Character.TYPE || fieldDeclareType == Character.class ) {
					return buffer.readChar();
				}else if(fieldDeclareType == String.class ) {
					return buffer.readUTF();
				}else if(fieldDeclareType == Date.class ) {
					return new Date(buffer.readLong());
				}
			}
			
			byte prefixCodeType = buffer.readByte();
			if(prefixCodeType == Decoder.PREFIX_TYPE_NULL) {
				return null;
			}
			
			if(Decoder.PREFIX_TYPE_BYTE == prefixCodeType) {
				return buffer.readByte();
			}else if(Decoder.PREFIX_TYPE_SHORTT == prefixCodeType) {
				return buffer.readShort();
			}else if(Decoder.PREFIX_TYPE_INT == prefixCodeType) {
				return buffer.readInt();
			}else if(Decoder.PREFIX_TYPE_LONG == prefixCodeType) {
				return buffer.readLong();
			}else if(Decoder.PREFIX_TYPE_FLOAT == prefixCodeType) {
				return buffer.readFloat();
			}else if(Decoder.PREFIX_TYPE_DOUBLE == prefixCodeType) {
				return buffer.readDouble();
			}else if(Decoder.PREFIX_TYPE_BOOLEAN == prefixCodeType) {
				return buffer.readBoolean();
			}else if(Decoder.PREFIX_TYPE_CHAR == prefixCodeType) {
				return buffer.readChar();
			}else if(Decoder.PREFIX_TYPE_STRINGG == prefixCodeType) {
				return buffer.readUTF();
			}else if(Decoder.PREFIX_TYPE_DATE == prefixCodeType) {
				return new Date(buffer.readLong());
			}else if(Decoder.PREFIX_TYPE_PROXY == prefixCodeType) {
				Class<?> cls = TypeCoder.getType(buffer);
				if(ISerializeObject.class.isAssignableFrom(cls)) {
					try {
						ISerializeObject obj = (ISerializeObject)cls.newInstance();
						obj.decode(buffer);
						return  obj;
					} catch (InstantiationException | IllegalAccessException e) {
						throw new CommonException("Create instance of: " + cls.getName() + " error!",e);
					}
				} else {
					
					ISerializer so = null;
					Throwable e1 = null;
					try {
						so = SerializeProxyFactory.getSerializeCoder(cls);
					} catch (Throwable e) {
						e1 = e;
					}
					if(so != null) {
						 Object o = so.decode(buffer);
						 return o;
					} else {
						TypeCoder cd = TypeCoderFactory.getIns().getCoder(cls);
						if(cd != null && cd != this) {
							return cd.decode(buffer,cls,null);
						} else {
							return TypeCoder.decodeByReflect(buffer, cls, genericType);
						}
					}
				}
			}else if(Decoder.PREFIX_TYPE_STRING == prefixCodeType) {
				fieldDeclareType = TypeCoder.getType(buffer);
				if(fieldDeclareType == null) {
					throw new CommonException("Invalid class data buffer: "+buffer.toString());
				}
				return TypeCoder.decodeByReflect(buffer, fieldDeclareType, genericType);
			}else if(Decoder.PREFIX_TYPE_FINAL == prefixCodeType) {
				TypeCoder<?> coder = null;
				if(fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
					coder = TypeCoderFactory.getIns().getCoder(buffer.readShort());
				} else {
					coder = TypeCoderFactory.getIns().getCoder(fieldDeclareType);
				}
				
				if(coder != this) {
					return coder.decode(buffer, fieldDeclareType, genericType);
				} else {
					return TypeCoder.decodeByReflect(buffer, fieldDeclareType, genericType);
				}
			} else if(Decoder.PREFIX_TYPE_SHORT == prefixCodeType) {
				Short code = buffer.readShort();
				Class<?> cls = TypeCoderFactory.getIns().getClassByCode(code);
				TypeCoder<?> coder = TypeCoderFactory.getIns().getCoder(code);
				if(coder != this) {
					return coder.decode(buffer, fieldDeclareType, genericType);
				}else {
					throw new CommonException("Invalid type code: "+code);
				}
			} else if(Decoder.PREFIX_TYPE_LIST == prefixCodeType) {
				TypeCoder<?> coder = TypeCoderFactory.getIns().getCoder(List.class);
				if(coder != this) {
					return coder.decode(buffer, fieldDeclareType, genericType);
				}else {
					throw new CommonException("Invalid List type coder: " + coder.type().getName());
				}
				
			}  else if(Decoder.PREFIX_TYPE_SET == prefixCodeType) {
				TypeCoder<?> coder = TypeCoderFactory.getIns().getCoder(Set.class);
				if(coder != this) {
					return coder.decode(buffer, fieldDeclareType, genericType);
				}else {
					throw new CommonException("Invalid Set type coder: " + coder.type().getName());
				}
			}  else if(Decoder.PREFIX_TYPE_MAP == prefixCodeType) {
				TypeCoder<?> coder = TypeCoderFactory.getIns().getCoder(Map.class);
				if(coder != this) {
					return coder.decode(buffer, fieldDeclareType, genericType);
				}else {
					throw new CommonException("Invalid Map type coder: " + coder.type().getName());
				}
			}  else {
				throw new CommonException(0,"not support prefix type:" 
			+ prefixCodeType+", fieldDeclareType:"+(fieldDeclareType==null?"":fieldDeclareType.getName()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	@Override
	public int compareTo(TypeCoder<Object> o) {
		return -1;
	}

	@Override
	public Class<Object> type() {
		return Object.class;
	}

	@Override
	public short code() {
		return Short.MAX_VALUE;
	}
	
	@Override
	public byte prefixCode() {
		return Byte.MAX_VALUE;
	}

	@Override
	public boolean canSupport(Class<?> clazz) {
		//在列表中只能通过精确查询获取clazz = Object.class
		return false;
	}
}