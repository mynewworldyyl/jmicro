package org.jmicro.example.api;

import org.jmicro.api.Person;
import org.jmicro.api.annotation.Service;

@Service
public interface ITestRpcService {
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	void subscrite(String msg);
	
	String hello(String name);
	
}
