package org.jmicro.api.codec.typecoder;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.jmicro.api.codec.TypeUtils;

@SuppressWarnings({"rawtypes","unchecked"})
public class PrimitiveTypeCoder extends AbstractFinalTypeCoder<Object> {

	public PrimitiveTypeCoder(short code, Class clazz) {
		super(code, clazz);
	}

	@Override
	public void encodeData(ByteBuffer buffer, Object obj,Class<?> fieldDeclareType,Type genericType) {

		Class<Object> cls = type();
		if (TypeUtils.isPrimitiveInt(cls) || TypeUtils.isInt(cls)) {
			if (obj == null) {
				buffer.putInt(0);
			} else {
				buffer.putInt((Integer) obj);
			}
		} else if (TypeUtils.isPrimitiveByte(cls) || TypeUtils.isByte(cls)) {
			if (obj == null) {
				buffer.put((byte) 0);
			} else {
				buffer.put((Byte) obj);
			}
		} else if (TypeUtils.isPrimitiveShort(cls) || TypeUtils.isShort(cls)) {
			if (obj == null) {
				buffer.putShort((short) 0);
			} else {
				buffer.putShort((Short) obj);
			}
		} else if (TypeUtils.isPrimitiveLong(cls) || TypeUtils.isLong(cls)) {
			if (obj == null) {
				buffer.putLong(0);
			} else {
				buffer.putLong((Long) obj);
			}
		} else if (TypeUtils.isPrimitiveFloat(cls) || TypeUtils.isFloat(cls)) {
			if (obj == null) {
				buffer.putFloat(0);
			} else {
				buffer.putFloat((Float) obj);
			}
		} else if (TypeUtils.isPrimitiveDouble(cls) || TypeUtils.isDouble(cls)) {
			if (obj == null) {
				buffer.putDouble(0);
			} else {
				buffer.putDouble((Double) obj);
			}
		} else if (TypeUtils.isPrimitiveBoolean(cls) || TypeUtils.isBoolean(cls)) {
			if (obj == null) {
				buffer.put((byte) 0);
			} else {
				boolean b = (Boolean) obj;
				buffer.put(b ? (byte) 1 : (byte) 0);
			}
		} else if (TypeUtils.isPrimitiveChar(cls) || TypeUtils.isChar(cls)) {
			if (obj == null) {
				buffer.putChar((char) 0);
			} else {
				buffer.putChar((Character) obj);
			}
		}
	}

	@Override
	public Object decode(ByteBuffer buffer, Class<?> fieldDeclareType,Type genericType) {
		Class<Object> cls = type();
		Object val = null;
		if (TypeUtils.isPrimitiveInt(cls) || TypeUtils.isInt(cls)) {
			val = buffer.getInt();
		} else if (TypeUtils.isPrimitiveByte(cls) || TypeUtils.isByte(cls)) {
			val = buffer.get();
		} else if (TypeUtils.isPrimitiveShort(cls) || TypeUtils.isShort(cls)) {
			val = buffer.getShort();
		} else if (TypeUtils.isPrimitiveLong(cls) || TypeUtils.isLong(cls)) {
			val = buffer.getLong();
		} else if (TypeUtils.isPrimitiveFloat(cls) || TypeUtils.isFloat(cls)) {
			val = buffer.getFloat();
		} else if (TypeUtils.isPrimitiveDouble(cls) || TypeUtils.isDouble(cls)) {
			val = buffer.getDouble();
		} else if (TypeUtils.isPrimitiveBoolean(cls) || TypeUtils.isBoolean(cls)) {
			val = buffer.get() == 1;
		} else if (TypeUtils.isPrimitiveChar(cls) || TypeUtils.isChar(cls)) {
			val = buffer.getChar();
		}
		return val;
	}

}
