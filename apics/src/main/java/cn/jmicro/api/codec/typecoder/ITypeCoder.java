package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

public interface ITypeCoder<T> extends Comparable<ITypeCoder<T>>{
	
	/**
	 * 指定值的类型编码
	 * 编码的时间，value.getClass()即为值的类型
	 * 
	 * 解码的时候，从ByteBuffer读取的前缀类型即为此类型。
	 * 
	 * 获取到类型后，即可取得对应类型的编码解码器
	 * @return
	 */
	Class<T> type();
	
	short code();
	
	byte prefixCode();

	/**
	 * 
	 * @param buffer
	 * @param val
	 * @param type
	 */
	void encode(DataOutput buffer, T val, Class<?> fieldDeclareType, Type genericType) 
			throws IOException;
	
	T decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType);
	
	boolean canSupport(Class<?> clazz);
	
}
