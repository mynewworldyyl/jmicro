package cn.jmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.example.api.ITestRpcService;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.example.comsumer.TestRpcClient;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestObjectFactory extends JMicroBaseTestCase{
	
	@Test
	public void testGetRemoteServie0() {
		ServiceItem si = sayHelloServiceItem();
		ISimpleRpc sayHello = of.getRemoteServie(si.getKey().getServiceName(), si.getKey().getNamespace()
				, si.getKey().getVersion(),null);
		System.out.println(sayHello.hello("testGetRemoteServie0"));
	}
	
	@Test
	public void testGetRemoteServie1() {
		ServiceItem si = sayHelloServiceItem();
		ISimpleRpc sayHello = of.getRemoteServie(si,null);
		System.out.println(sayHello.hello("testGetRemoteServie0"));
	}
	
	@Test
	public void testGetRemoteServie3() {
		ITestRpcService rpc = of.get(ITestRpcService.class);
		System.out.println(rpc.hello("testGetRemoteServie3"));
	}
	
	@Test
	public void testTestRpcClient() {
		TestRpcClient rpc = of.get(TestRpcClient.class);
		rpc.invokePersonService();
	}

	@Test
	public void testGetSetServices() {
		TestRpcClient rpc = of.get(TestRpcClient.class);
		System.out.println(rpc.testSetServices());
	}
	
	@Test
	public void testPresureGetSetServices() {
		TestRpcClient rpc = of.get(TestRpcClient.class);
		
		System.out.println(rpc.testSetServices());
	}
	
}
