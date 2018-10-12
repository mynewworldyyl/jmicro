package org.jmicro.example.api;

import org.jmicro.api.Person;
import org.jmicro.api.annotation.Service;

@Service
public interface ITestRpcService {

	String hello(String name);
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	void subscrite(String msg);
	
}
