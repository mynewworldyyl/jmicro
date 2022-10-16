package cn.jmicro.api.codec;

import cn.jmicro.api.codec.typecoder.ITypeCoder;

public class TypeCoderFactoryUtils {
	
	public ITypeCoder<Object> getDefaultCoder() {
		return defaultCoder;
	}

	@SuppressWarnings("rawtypes")
	private ITypeCoder<Object> defaultCoder = null;
	
	public void setDefaultCoder(ITypeCoder<Object> defaultCoder) {
		this.defaultCoder = defaultCoder;
	}

	private static final TypeCoderFactoryUtils ins = new TypeCoderFactoryUtils();

	public static TypeCoderFactoryUtils getIns() {
		return ins;
	}
	
	
}
