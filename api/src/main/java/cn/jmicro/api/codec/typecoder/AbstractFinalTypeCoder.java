package cn.jmicro.api.codec.typecoder;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import cn.jmicro.api.codec.Decoder;

public abstract class AbstractFinalTypeCoder<T> extends AbstractComparableTypeCoder<T> {

	public AbstractFinalTypeCoder(short code,Class<T> clazz) {
		super(Decoder.PREFIX_TYPE_FINAL,code,clazz);
	}

	@Override
	public void encode(DataOutput buffer, T val, Class<?> fieldDeclareType, Type genericType) throws IOException {
		super.encode(buffer, val, fieldDeclareType, genericType);
		encodeData(buffer, val, fieldDeclareType, genericType);
	}

	protected abstract void encodeData(DataOutput buffer, T val, 
			Class<?> fieldDeclareType, Type genericType)throws IOException;

	@Override
	public boolean canSupport(Class<?> clazz) {
		return type() == clazz;
	}
}
