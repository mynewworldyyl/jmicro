package org.jmicro.api.codec;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;

public abstract class AbstractTypeCoder<T> implements TypeCoder<T>{

	private final short code;
	private final Class<T> clazz;
	
	AbstractTypeCoder(short code, Class<T> clazz){
		if(clazz == null) {
			throw new CommonException("clazz cannot be NULL");
		}
		this.code = code;
		this.clazz = clazz;
	}

	@Override
	public Class<T> type() {
		return clazz;
	}

	@Override
	public short code() {
		return code;
	}
	
	protected void checkType(Class<?> declareFieldType) {
		if(!type().isAssignableFrom(declareFieldType)) {
			throw new CommonException("Missmatch type: coder type["+this.type().getName()+"], target type ["+declareFieldType.toString()+"]");
		}
	}
	
	/**
	 *  需不需要加类型前缀取决于字段声明类型
	 *  如果declareType是final类型，说明解码时能直接从declareType得到值的类型信息，则不需要写类型信息，否则需要写类型信息
	 *  类型信息可以是类全路径，也可以是类short编码。当代码走到这里时，说明类的编码即是此编码器的code，直接写即可
	 * @param buffer
	 * @param valClazz
	 * @param declareType
	 */
	protected void putType(ByteBuffer buffer, Class<?> valClazz, Class<?> fieldDeclareType) {
		if(fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
			//代码走到这里，肯定有类型编码，所以写类型编码即可
			putType(buffer);
		}
	}
	
	protected void putType(ByteBuffer buffer) {
		putCodeType(buffer,Decoder.PREFIX_TYPE_SHORT,code);
	}

	@Override
	public void encode(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType) {
		checkType(fieldDeclareType);
		if(fieldDeclareType == null || !TypeUtils.isFinal(fieldDeclareType)) {
			putType(buffer);
		}
		encodeData(buffer,val,fieldDeclareType,genericType);
	}

	@Override
	public T decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
		//确保declareFieldType==String.class
		checkType(fieldDeclareType);
		return decodeData(buffer,fieldDeclareType,genericType);
	}

	public static void putCodeType(ByteBuffer buffer,byte prefixType,short code) {
		//类型前缀类型
		buffer.put(prefixType);
		//类型前缀编码
		buffer.putShort(code);
	}
	
	public static void putStringType(ByteBuffer buffer,byte prefixType,String clazz) {
		//类型前缀类型
		buffer.put(prefixType);
		//类名称
		encodeString(buffer, clazz);
	}
	
	public static void putLength(ByteBuffer buffer,int len) {
		buffer.putShort((short)len);
	}
	
	public static void encodeString(ByteBuffer buffer,String str){
		if(StringUtils.isEmpty(str)){
			putLength(buffer,0);
			return;
		}
	    try {
	    	/*ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY,null);
			if(sm != null && "intrest".equals(sm.getKey().getMethod()) && str.startsWith("[L")) {
				logger.debug("eltType: {}",str);
			}*/
			byte[] data = str.getBytes(Constants.CHARSET);
			putLength(buffer,data.length);
			buffer.put(data);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException("encodeString error: "+str);
		}
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
