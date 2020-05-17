package cn.jmicro.example.api.rpc;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.test.Person;

@Service
public interface ISimpleRpc {
	String hello(String name);
	
	String hi(Person p);
}
