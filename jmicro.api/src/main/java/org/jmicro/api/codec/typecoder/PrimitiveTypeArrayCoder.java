
package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.common.CommonException;

/**
 * Primitive类型肯定是final类型，不需要类型前缀标识
 * @author Yulei Ye
 * @date 2018年12月26日 上午10:31:35
 */
public class PrimitiveTypeArrayCoder extends AbstractFinalTypeCoder<Object>{

	   @SuppressWarnings({ "rawtypes", "unchecked" })
	   public PrimitiveTypeArrayCoder(short code,Class primitiveCls) {
		  super(code,primitiveCls);
	   }

		@Override
		public Object decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType) {
			return TypeCoder.decodeArray(buffer,type().getComponentType(),genericType);
		}

		@Override
		protected void encodeData(ByteBuffer buffer, Object val, Class<?> fieldDeclareType, Type genericType) {
			TypeCoder.encodeArray(buffer,val,type().getComponentType(),genericType);
		}

		
}
