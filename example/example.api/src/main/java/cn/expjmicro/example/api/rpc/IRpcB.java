package cn.expjmicro.example.api.rpc;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IRpcB {

	String invokeRpcB(String bargs);
}
