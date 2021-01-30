package cn.expjmicro.example.api.rpc;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAsyncRpcCallback {
	void callback(String name);
}
