package org.jmicro.api.config;

public interface IConfigChangeListener {

	void configChange(String path,String value);
}
