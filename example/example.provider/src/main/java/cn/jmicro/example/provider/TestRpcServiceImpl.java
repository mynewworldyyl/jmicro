package cn.jmicro.example.provider;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.test.Person;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.ITestRpcService;

@Service(timeout=10*60*1000,maxSpeed=100,baseTimeUnit=Constants.TIME_SECONDS,namespace="testrpc",
version="0.0.1", clientId=1000)
@Component
public class TestRpcServiceImpl implements ITestRpcService{

	private AtomicInteger ai = new AtomicInteger();
	
	@Cfg("/TestRpcServiceImpl/defaultLimiterName")
	private String name="defaultLimit";
	
	@Cfg(value="/TestRpcServiceImpl/MapParams-*")
	public Map<String,String> params = null;
	
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
	public int testReturnPrimitiveResult() {
		return 66;
	}

	public int[] testReturnPrimitiveArrayResult() {
		return new int[] {22,33};
	}

	@Override
	public Boolean testReturnBooleanResult() {
		return true;
	}

	@Override
	public boolean testReturnPrimitiveBooleanResult() {
		return false;
	}
	
	
	
}
