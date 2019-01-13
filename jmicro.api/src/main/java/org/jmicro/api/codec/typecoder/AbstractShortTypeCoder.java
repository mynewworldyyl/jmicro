package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;
import org.jmicro.common.CommonException;

public abstract class AbstractShortTypeCoder<T>  extends AbstractComparableTypeCoder<T> {

	public AbstractShortTypeCoder(short code, Class<T> clazz){
		super(Decoder.PREFIX_TYPE_SHORT,code,clazz);
	}

	protected void checkType(Class<?> declareFieldType) {
		/*if(!type().isAssignableFrom(declareFieldType)) {
			throw new CommonException("Missmatch type: coder type["+this.type().getName()+"], target type ["+declareFieldType.toString()+"]");
		}*/
	}
	
	@Override
	public void encode(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType) {
		//super.encode(buffer, val, fieldDeclareType, genericType);
		if(fieldDeclareType != null) {
			checkType(fieldDeclareType);
		}
		buffer.put(prefixCode);
		buffer.putShort(code());
		encodeData(buffer,val,fieldDeclareType,genericType);
	}

	@Override
	public T decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
		//确保declareFieldType==String.class
		if(fieldDeclareType != null) {
			checkType(fieldDeclareType);
		}
		
		/*if(valCls == null) {
			valCls = getType(buffer,fieldDeclareType,genericType);
		}*/
		return decodeData(buffer,type(),genericType);
	}
	
	protected abstract T decodeData(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType);
	protected abstract void encodeData(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType);

	@Override
	public boolean canSupport(Class<?> clazz) {
		if(clazz == type()) {
			//class实例相同，肯定可以支持
			return true;
		}
		
		if(clazz == Void.class || clazz == Object.class || clazz == null) {
			//应用直接取得相应实现，不用在这里匹配
			return false;
		}
		
		if(type().isAssignableFrom(clazz)) {
			return true;
		}
		
		return false;
	}
	
	
}
