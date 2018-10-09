package org.jmicro.example.comsumer;

import org.jmicro.api.Person;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.example.api.ITestRpcService;

@Component
public class TestRpcClient {

	@Reference(required=false)
	private ITestRpcService rpcService;
	
	public void invokeRpcService(){
		String result = rpcService.hello("Hello RPC Server");
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
