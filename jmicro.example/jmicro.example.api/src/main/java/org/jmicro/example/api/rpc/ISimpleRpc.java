package org.jmicro.example.api.rpc;

import org.jmicro.api.annotation.Service;
import org.jmicro.api.test.Person;

@Service
public interface ISimpleRpc {
	String hello(String name);
	
	String hi(Person p);
}
