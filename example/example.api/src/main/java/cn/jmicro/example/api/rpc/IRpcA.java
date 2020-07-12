package cn.jmicro.example.api.rpc;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IRpcA {

	String invokeRpcA(String aargs);
	
	//String invokeRpcAsy(String aargs);
	
}
