package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;

public class VoidTypeCoder extends AbstractComparableTypeCoder<Void>{
	   
	   public VoidTypeCoder(short code) {
		   super(Decoder.PREFIX_TYPE_NULL,code,Void.class);
	   }
	   
		@Override
		public void encode(ByteBuffer buffer, Void val, Class<?> declareFieldType, Type genericType) {
			super.encode(buffer, val, declareFieldType, genericType);
		}

		@Override
		public Void decode(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
			return null;
		}
}