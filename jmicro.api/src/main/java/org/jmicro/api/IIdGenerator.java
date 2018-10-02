package org.jmicro.api;


public interface IIdGenerator {

	long getLongId(Class<?> idType);
	
	String getStringId(Class<?> idType);
	
}
