package cn.jmicro.example.api.rpc;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.test.Person;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
@Service(namespace="simpleRpc", version="0.0.1")
public interface ISimpleRpc {
	
	String hello(String name);
	
	String hi(Person p);
	
	 IPromise<String> linkRpc(String msg);
	
	IPromise<String> linkRpcAs(String msg);
}
