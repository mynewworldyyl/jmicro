package cn.jmicro.api.codec.typecoder;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.common.CommonException;

public abstract class PrefixCodeTypeCoder<T> implements TypeCoder<T>{
	
	protected final byte prefixCode;
	
	protected final Class<T> clazz;
	
	public PrefixCodeTypeCoder(byte prefixCode,Class<T> clazz) {
		if(clazz == null) {
			throw new CommonException("clazz cannot be NULL");
		}
		this.prefixCode = prefixCode;
		this.clazz = clazz;
	}

	@Override
	public int compareTo(TypeCoder<T> o) {
		if(o == null) {
			return 1;
		}
		if(o == this) {
			return 0;
		}
		return prefixCode > o.prefixCode() ? 1 : (prefixCode == o.prefixCode()? 0: -1);
	}
	
	@Override
	public void encode(DataOutput buffer, T val, Class<?> fieldDeclareType, Type genericType)
			throws IOException {
		buffer.write(prefixCode);
	}

	@Override
	public boolean canSupport(Class<?> clazz) {
	   //支持子类编码
		return type().isAssignableFrom(clazz);
	}
	
	@Override
	public Class<T> type() {
		return clazz;
	}

	@Override
	public short code() {
		return prefixCode;
	}

	public byte prefixCode() {
		return prefixCode;
	}
	
	
}
