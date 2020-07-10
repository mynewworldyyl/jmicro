package cn.jmicro.example.api.rpc;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAsyncRpcCallback {
	void callback(String name);
}
