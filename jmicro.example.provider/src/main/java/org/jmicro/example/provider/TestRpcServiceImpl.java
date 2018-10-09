package org.jmicro.example.provider;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.Person;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.example.api.ITestRpcService;

@Service
@Component
public class TestRpcServiceImpl implements ITestRpcService{

	private AtomicInteger ai = new AtomicInteger();
	
	@Cfg("/limiterName")
	private String name;
	
	@Override
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	public Person getPerson(Person p) {
		p.setUsername("Server update username");
		p.setId(ai.getAndIncrement());
		System.out.println(p);
		return p;
	}

	@Override
	@SMethod(noNeedResponse=1)
	public void pushMessage(String msg) {
		System.out.println("Server Rec: "+ msg);
	}
	
}
