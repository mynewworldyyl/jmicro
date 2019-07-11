package org.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;

public class ArrayCoder extends AbstractShortTypeCoder<Object[]>{
	   
	   public ArrayCoder(short code) {
		  super(code,Object[].class);
	   }

		@Override
		public boolean canSupport(Class<?> clazz) {
			return clazz != null && clazz.isArray()
					&& Object.class.isAssignableFrom(clazz.getComponentType());
		}
		
		protected void checkType(Class<?> declareFieldType) {
		}

		@Override
		protected Object[] decodeData(DataInput buffer, Class<?> fieldDeclareType, Type genericType) {
			/*try {
				buffer.readByte();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Class<?> eltType = TypeCoder.getType(buffer);*/
			return (Object[])TypeCoder.decodeArray(buffer,null,null);
		}

		@Override
		protected void encodeData(DataOutput buffer, Object[] val, 
				Class<?> fieldDeclareType, Type genericType) throws IOException {
			//Class<?> eltType = val.getClass().getComponentType();
			/*if(val.length > 0) {
				eltType = val[0].getClass();
			}*/
			//TypeCoder.putStringType(buffer, eltType.getName());
			TypeCoder.encodeArray(buffer,val,null,null);
		}
}
