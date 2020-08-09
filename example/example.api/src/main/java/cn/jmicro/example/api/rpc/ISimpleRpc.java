package cn.jmicro.example.api.rpc;

import cn.jmicro.api.test.Person;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISimpleRpc {
	
	String hello(String name);
	
	String hi(Person p);
	
	String linkRpc(String msg);
	
}
