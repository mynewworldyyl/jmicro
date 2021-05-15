package cn.jmicro.common.util;

import java.lang.reflect.Field;

public class Unsafe {

	public static Unsafe getUnsafe() {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			Unsafe unsafe = (Unsafe) f.get(null);
			return unsafe;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
