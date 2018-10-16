package org.jmicro.codec;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;

@Component(value=Constants.DEFAULT_CODEC_FACTORY)
public class SimpleCodecFactory implements ICodecFactory{

	private Map<Class<?>,IDecoder> decoders = new HashMap<>();
	
	private Map<Class<?>,IEncoder> encoders = new HashMap<>();
	
	private IDecoder defaultDecoder = new IDecoder(){
		@Override
		public <T> T decode(ByteBuffer data) {
			return Decoder.decodeObject(data);
		}
	};
	
	@Cfg(value="respBufferSize")
	private int defaultEncodeBufferSize = 1024*4;
	
	private IEncoder defaultEncoder = new IEncoder(){
		@Override
		public <T> ByteBuffer encode(T obj) {
			ByteBuffer bb = ByteBuffer.allocate(defaultEncodeBufferSize);
			Encoder.encodeObject(bb,obj);
			bb.flip();
			return bb;
		}
		
	};
	
	@Override
	public <T> IDecoder getDecoder(Class<T> clazz) {
		if(decoders.containsKey(clazz)){
			return decoders.get(clazz);
		}
		return defaultDecoder;
	}

	@Override
	public <T> IEncoder getEncoder(Class<T> clazz) {
		if(encoders.containsKey(clazz)){
			return encoders.get(clazz);
		}
		
		return defaultEncoder;
	}

	@Override
	public  <T> void registDecoder(Class<T> clazz,IDecoder decoder) {
		if(decoders.containsKey(clazz)){
			IDecoder d = decoders.get(clazz);
			throw new CommonException("class ["+clazz.getName()+
					" have exists decoder [" + d.getClass().getName() + "]" );
		}
		decoders.put(clazz, decoder);
	}

	@Override
	public <T> void registEncoder(Class<T> clazz,IEncoder encoder) {
		if(encoders.containsKey(clazz)){
			IEncoder e = encoders.get(clazz);
			throw new CommonException("class ["+clazz.getName()+
					" have exists decoder [" + e.getClass().getName() + "]" );
		}
		encoders.put(clazz, encoder);
	}
	
}
