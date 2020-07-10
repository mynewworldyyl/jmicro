package cn.jmicro.example.api.rpc;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.test.Person;
import cn.jmicro.codegenerator.AsyncClientProxy;

@Service
@AsyncClientProxy
public interface ISimpleRpc {
	
	String hello(String name);
	
	//IPromise<String> helloAsync(String name);
	
	String hi(Person p);
	
	String linkRpc(String msg);
}
