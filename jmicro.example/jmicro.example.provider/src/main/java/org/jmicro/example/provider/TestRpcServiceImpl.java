package org.jmicro.example.provider;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.net.IWriteCallback;
import org.jmicro.api.test.Person;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.example.api.ITestRpcService;

@Service(timeout=10*60*1000,maxSpeed=100,baseTimeUnit=Constants.TIME_SECONDS,namespace="testrpc",version="0.0.1")
@Component
public class TestRpcServiceImpl implements ITestRpcService{

	private AtomicInteger ai = new AtomicInteger();
	
	@Cfg("/defaultLimiterName")
	private String name;
	
	@Override
	@SMethod(monitorEnable=0,maxSpeed=100,baseTimeUnit=Constants.TIME_SECONDS,timeout=0)
	public String hello(String name) {
		System.out.println("Hello and welcome :" + name);
		return "Rpc server return : "+name;
	}

	@Override
	@SMethod(monitorEnable=0,maxSpeed=100,baseTimeUnit=Constants.TIME_SECONDS,timeout=0)
	public Person getPerson(Person p) {
		System.out.println(p);
		p.setUsername("Server update username");
		p.setId(ai.getAndIncrement());
		return p;
	}

	@Override
	@SMethod(needResponse=false)
	public void pushMessage(String msg) {
		System.out.println("Server Rec: "+ msg);
	}
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@Override
	@SMethod(monitorEnable=1,stream=true)
	public void subscrite(String msg) {
		IWriteCallback sender = JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK_SERVICE, null);
		if(sender == null){
			throw new CommonException("Not in async context");
		}
		for(int i = 100; i > 0; i++) {
			try {
				Thread.sleep(1000*2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String msg1 = "Server return: "+ count.getAndIncrement()+",msg: " +msg;
			System.out.println(msg1);
			if(!sender.send(msg1)) {
				break;
			}
		}
	}
}
