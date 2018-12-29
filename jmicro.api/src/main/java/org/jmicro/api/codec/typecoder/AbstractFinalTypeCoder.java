package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;

public abstract class AbstractFinalTypeCoder<T> extends AbstractComparableTypeCoder<T> {

	public AbstractFinalTypeCoder(short code,Class<T> clazz) {
		super(Decoder.PREFIX_TYPE_FINAL,code,clazz);
	}

	@Override
	public void encode(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType) {
		super.encode(buffer, val, fieldDeclareType, genericType);
		encodeData(buffer, val, fieldDeclareType, genericType);
	}

	protected abstract void encodeData(ByteBuffer buffer, T val, 
			Class<?> fieldDeclareType, Type genericType);

	@Override
	public boolean canSupport(Class<?> clazz) {
		return type() == clazz;
	}
}
