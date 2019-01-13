package org.jmicro.api.classloader;

import org.jmicro.api.annotation.Service;

@Service
public interface IClassloaderRpc {
	
	public static final String NS = IClassloaderRpc.class.getName();

	/**
	 * 加载指定类的二进制形式
	 * @param clazz
	 * @return
	 */
	byte[] getClassData(String clazz);
	
	String info();
	
}
