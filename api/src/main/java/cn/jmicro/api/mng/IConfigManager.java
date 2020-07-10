package cn.jmicro.api.mng;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IConfigManager {

	ConfigNode[] getChildren(String path,Boolean getAll);
	
	boolean update(String path, String val);
	
	boolean delete(String path);
	
	boolean add(String path, String val,Boolean isDir);
	
	
}
