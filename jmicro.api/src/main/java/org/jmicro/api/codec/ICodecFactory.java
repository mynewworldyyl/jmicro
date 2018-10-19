package org.jmicro.api.codec;

public interface ICodecFactory {

	IDecoder getDecoder(Byte protocol);
	IEncoder getEncoder(Byte protocol);
	
	void registDecoder(Byte protocol,IDecoder<?> decoder);
	
	void registEncoder(Byte protocol,IEncoder<?> encoder);
	
	public static <R> R encode(ICodecFactory f,Object obj,Byte protocol){
		return (R)f.getEncoder(protocol).encode(obj);
	}
	
	public static <T,R> R decode(ICodecFactory f, T src,Class<R> clazz,Byte protocol){
		return (R)f.getDecoder(protocol).decode(src,clazz);
	}
}
