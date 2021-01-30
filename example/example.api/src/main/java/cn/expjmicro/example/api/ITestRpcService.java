package cn.expjmicro.example.api;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.test.Person;
import cn.jmicro.codegenerator.AsyncClientProxy;

@Service(version="0.0.*")
@AsyncClientProxy
public interface ITestRpcService {
	
	Person getPerson(Person p);
	
	void pushMessage(String msg);
	
	String hello(String name);
	
	int testReturnPrimitiveResult();
	
	int[] testReturnPrimitiveArrayResult();
	
	Boolean testReturnBooleanResult();
	
	boolean testReturnPrimitiveBooleanResult();
}
