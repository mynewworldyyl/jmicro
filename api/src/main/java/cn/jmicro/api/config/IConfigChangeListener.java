package cn.jmicro.api.config;

public interface IConfigChangeListener {

	void configChange(String path,String value);
}
