package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

public class ReflectTypeCoder<T> extends AbstractShortTypeCoder<T>{
	   
	   public ReflectTypeCoder(short code,Class<T> clazz) {
		   super(code,clazz);
	   }

		@Override
		public void encodeData(DataOutput buffer, T val,Class<?> fieldDeclareType,
				Type genericType) throws IOException {
			TypeCoderUtils.encodeByReflect(buffer,val,fieldDeclareType, genericType);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T decodeData(DataInput buffer, Class<?> declareFieldType, Type genericType) {
			return (T)TypeCoderUtils.decodeByReflect(buffer,declareFieldType,genericType);
		}
}
