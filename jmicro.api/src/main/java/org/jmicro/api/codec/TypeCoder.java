package org.jmicro.api.codec;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public interface TypeCoder<T> {
	
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

	/**
	 * 
	 * @param buffer
	 * @param val
	 * @param type
	 */
	void encode(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType);
	
	T decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType);
	
	boolean canSupport(Class<?> clazz);
}
