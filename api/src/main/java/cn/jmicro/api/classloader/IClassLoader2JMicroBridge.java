package cn.jmicro.api.classloader;

import java.io.InputStream;

interface IClassLoader2JMicroBridge {

	InputStream loadByteData(String clsName);
	
	Class<?> findClass(String className);
	
}
