package org.jmicro.example.comsumer;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.test.ISayHello;
import org.jmicro.api.test.Person;
import org.jmicro.example.api.ITestRpcService;

@Component
public class TestRpcClient {

	@Reference(required=true,namespace="testrpc",version="0.0.*")
	private ITestRpcService rpcService;
	
	@Reference(required=false,namespace="testsayhello",version="0.0.*")
	private ISayHello sayHello;
	
	public void invokeRpcService(){
		String result = sayHello.hello("Hello RPC Server");
		System.out.println("Get remote result:"+result);
	}
	
	public void invokePersonService(){
		Person p = new Person();
		p.setId(1234);
		p.setUsername("Client person Name");
		p = rpcService.getPerson(p);
		System.out.println(p.toString());
	}
	
}
