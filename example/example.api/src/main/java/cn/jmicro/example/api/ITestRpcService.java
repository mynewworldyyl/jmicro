package cn.jmicro.example.api;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.test.Person;

@Service(namespace="testrpc",version="0.0.*")
public interface ITestRpcService {
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	String hello(String name);
	
	int testReturnPrimitiveResult();
	
	int[] testReturnPrimitiveArrayResult();
	
	Boolean testReturnBooleanResult();
	
	boolean testReturnPrimitiveBooleanResult();
}
