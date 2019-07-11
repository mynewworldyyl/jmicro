
package org.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import org.jmicro.common.CommonException;

/**
 * Primitive类型肯定是final类型，不需要类型前缀标识
 * @author Yulei Ye
 * @date 2018年12月26日 上午10:31:35
 */
public class PrimitiveTypeArrayCoder extends AbstractFinalTypeCoder<Object>{

	   @SuppressWarnings({ "rawtypes", "unchecked" })
	   public PrimitiveTypeArrayCoder(short code,Class primitiveCls) {
		  super(code,primitiveCls);
	   }

		@Override
		public Object decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
			//return TypeCoder.decodeArray(buffer,type().getComponentType(),genericType);
			short len;
			try {
				len = buffer.readShort();
			} catch (IOException e) {
				throw new CommonException("decode",e);
			}
			
			Class cls = clazz.getComponentType();
			if(len == 0) {
				return Array.newInstance(clazz.getComponentType(), 0);
			}
			
			Object arr = Array.newInstance(clazz.getComponentType(), len);
			for(int i = 0; i < len; i++){
				try {
					Object v = null;
					if(cls == Byte.TYPE) {
						v = buffer.readByte();
					}else if(cls == Short.TYPE) {
						v = buffer.readShort();
					}else if(cls == Integer.TYPE) {
						v = buffer.readInt();
					}else if(cls == Long.TYPE) {
						v = buffer.readLong();
					}else if(cls == Float.TYPE) {
						v = buffer.readFloat();
					}else if(cls == Double.TYPE) {
						v = buffer.readDouble();
					}else if(cls == Boolean.TYPE) {
						v = buffer.readBoolean();
					}else if(cls == Character.TYPE) {
						v = buffer.readChar();
					}
					Array.set(arr, i, v);
				} catch (IOException e) {
					throw new CommonException("read",e);
				}
			}
			
			return arr;
		}

		@Override
		protected void encodeData(DataOutput buffer, Object objs, Class<?> fieldDeclareType
				, Type genericType) throws IOException {
			//TypeCoder.encodeArray(buffer,val,type().getComponentType(),genericType);
			
			if(objs == null) {
				buffer.writeShort(0);
				return;
			}
			int len = Array.getLength(objs);
			buffer.writeShort(len);
			
			if(len <=0) {
				return;
			}
			
			Class cls = clazz.getComponentType();
			
			for(int i = 0; i < len; i++){
				Object v = Array.get(objs, i);
				if(cls == Byte.TYPE) {
					buffer.writeByte(((Byte)v).byteValue());
				}else if(cls == Short.TYPE) {
					buffer.writeShort(((Short)v).shortValue());
				}else if(cls == Integer.TYPE) {
					buffer.writeInt(((Integer)v).intValue());
				}else if(cls == Long.TYPE) {
					buffer.writeLong(((Long)v).longValue());
				}else if(cls == Float.TYPE) {
					buffer.writeFloat(((Float)v).floatValue());
				}else if(cls == Double.TYPE) {
					buffer.writeDouble(((Double)v).doubleValue());
				}else if(cls == Boolean.TYPE) {
					buffer.writeBoolean(((Boolean)v).booleanValue());
				}else if(cls == Character.TYPE) {
					buffer.writeChar(((Character)v).charValue());
				}
			}
			
		}

		
}
