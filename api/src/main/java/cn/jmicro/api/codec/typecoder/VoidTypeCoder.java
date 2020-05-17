package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

import cn.jmicro.api.codec.Decoder;

public class VoidTypeCoder extends AbstractComparableTypeCoder<Void>{
	   
	   public VoidTypeCoder(short code) {
		   super(Decoder.PREFIX_TYPE_NULL,code,Void.class);
	   }
	   
		@Override
		public void encode(DataOutput buffer, Void val, Class<?> declareFieldType,
				Type genericType) throws IOException {
			super.encode(buffer, val, declareFieldType, genericType);
		}

		@Override
		public Void decode(DataInput buffer, Class<?> declareFieldType, Type genericType) {
			return null;
		}
}