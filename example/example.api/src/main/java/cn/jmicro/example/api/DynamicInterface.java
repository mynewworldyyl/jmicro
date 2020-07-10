package cn.jmicro.example.api;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface DynamicInterface {

	void run(String data);
	
}
