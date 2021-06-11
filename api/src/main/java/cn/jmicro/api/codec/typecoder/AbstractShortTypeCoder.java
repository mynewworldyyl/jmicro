package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import cn.jmicro.api.codec.DecoderConstant;

public abstract class AbstractShortTypeCoder<T>  extends AbstractComparableTypeCoder<T> {

	public AbstractShortTypeCoder(short code, Class<T> clazz){
		super(DecoderConstant.PREFIX_TYPE_SHORT,code,clazz);
	}

	protected void checkType(Class<?> declareFieldType) {
		/*if(!type().isAssignableFrom(declareFieldType)) {
			throw new CommonException("Missmatch type: coder type["+this.type().getName()+"], target type ["+declareFieldType.toString()+"]");
		}*/
	}
	
	@Override
	public void encode(DataOutput buffer, T val, Class<?> fieldDeclareType, Type genericType) throws IOException {
		//super.encode(buffer, val, fieldDeclareType, genericType);
		if(fieldDeclareType != null) {
			checkType(fieldDeclareType);
		}
		buffer.write(prefixCode);
		buffer.writeShort(code());
		encodeData(buffer,val,fieldDeclareType,genericType);
	}

	@Override
	public T decode(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
		//确保declareFieldType==String.class
		if(fieldDeclareType != null) {
			checkType(fieldDeclareType);
		}
		
		/*if(valCls == null) {
			valCls = getType(buffer,fieldDeclareType,genericType);
		}*/
		return decodeData(buffer,type(),genericType);
	}
	
	protected abstract T decodeData(DataInput buffer, Class<?> fieldDeclareType, Type genericType);
	protected abstract void encodeData(DataOutput buffer, T val, Class<?> fieldDeclareType
			, Type genericType) throws IOException;

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
