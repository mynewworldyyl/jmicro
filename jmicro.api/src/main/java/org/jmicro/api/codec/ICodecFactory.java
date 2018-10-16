package org.jmicro.api.codec;

import java.nio.ByteBuffer;

public interface ICodecFactory {

	<T> IDecoder getDecoder(Class<T> clazz);
	<T> IEncoder getEncoder(Class<T> clazz);
	
	<T> void registDecoder(Class<T> clazz,IDecoder decoder);
	
	<T> void registEncoder(Class<T> clazz,IEncoder encoder);
	
	public static ByteBuffer encode(ICodecFactory f,Object obj){
		return f.getEncoder(obj.getClass()).encode(obj);
	}
	
	public static <T> T decode(ICodecFactory f,ByteBuffer buffer){
		return f.getDecoder(null).decode(buffer);
	}
}
