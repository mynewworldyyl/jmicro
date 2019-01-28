package org.jmicro.api.codec.typecoder;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ReflectTypeCoder<T> extends AbstractShortTypeCoder<T>{
	   
	   public ReflectTypeCoder(short code,Class<T> clazz) {
		   super(code,clazz);
	   }

		@Override
		public void encodeData(DataOutput buffer, T val,Class<?> fieldDeclareType,
				Type genericType) throws IOException {
			TypeCoder.encodeByReflect(buffer,val,fieldDeclareType, genericType);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T decodeData(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
			return (T)TypeCoder.decodeByReflect(buffer,declareFieldType,genericType);
		}
}
