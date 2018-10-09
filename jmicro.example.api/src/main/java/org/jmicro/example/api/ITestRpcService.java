package org.jmicro.example.api;

import org.jmicro.api.Person;

public interface ITestRpcService {

	String hello(String name);
	
	Person getPerson(Person p);
}
