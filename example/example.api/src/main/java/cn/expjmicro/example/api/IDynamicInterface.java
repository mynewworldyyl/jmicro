package cn.expjmicro.example.api;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IDynamicInterface {

	void run(String data);
	
}
