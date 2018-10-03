package org.jmicro.example.provider;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Service;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.example.api.Person;

@Service
public class TestRpcServiceImpl implements ITestRpcService{

	@Cfg("/name")
	private String name;
	
	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	public Person getPerson(Person p) {
		System.out.println(p);
		p.setUsername("Server update username");
		p.setId(2222);
		return p;
	}

	
}
