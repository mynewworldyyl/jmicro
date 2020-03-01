package org.jmicro.example.api;

import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.test.Person;

@Service(namespace="testrpc",version="0.0.*")
public interface ITestRpcService {
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	@SMethod(timeout=10*60*1000)
	void subscrite(String msg);
	
	String hello(String name);
	
	int testReturnPrimitiveResult();
	
	int[] testReturnPrimitiveArrayResult();
	
	Boolean testReturnBooleanResult();
	
	boolean testReturnPrimitiveBooleanResult();
}
