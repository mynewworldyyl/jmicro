package org.jmicro.main.test;

import org.jmicro.api.annotation.Service;
import org.jmicro.main.ITestRpcService;
import org.jmicro.main.Persion;

@Service
public class TestRpcServiceImpl implements ITestRpcService{

	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	public Persion getPerson(Persion p) {
		System.out.println(p);
		p.setUsername("Server update username");
		p.setId(2222);
		return p;
	}

	
}
