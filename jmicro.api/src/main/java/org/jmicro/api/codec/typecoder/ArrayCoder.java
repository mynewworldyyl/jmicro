package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.common.CommonException;

public class ArrayCoder extends AbstractShortTypeCoder<Object[]>{
	   
	   public ArrayCoder(short code) {
		  super(code,Object[].class);
	   }

		@Override
		public boolean canSupport(Class<?> clazz) {
			return clazz != null && clazz.isArray()
					&& Object.class.isAssignableFrom(clazz.getComponentType());
		}
		
		protected void checkType(Class<?> declareFieldType) {
		}

		@Override
		protected Object[] decodeData(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
			buffer.get();
			Class<?> eltType = TypeCoder.getType(buffer);
			return (Object[])TypeCoder.decodeArray(buffer,eltType,genericType);
		}

		@Override
		protected void encodeData(ByteBuffer buffer, Object[] val, Class<?> fieldDeclareType, Type genericType) {
			Class<?> eltType = val.getClass().getComponentType();
			if(val.length > 0) {
				eltType = val[0].getClass();
			}
			TypeCoder.putStringType(buffer, eltType.getName());
			TypeCoder.encodeArray(buffer,val,eltType,genericType);
		}
}
