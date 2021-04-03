package cn.expjmicro.objfactory.simple.integration.test;

import org.junit.Assert;
import org.junit.Test;

import cn.expjmicro.example.api.ITestRpcService;
import cn.expjmicro.example.api.rpc.ISimpleRpc;
import cn.expjmicro.example.comsumer.TestRpcClient;
import cn.jmicro.api.mng.JmicroInstanceManager;
import cn.jmicro.api.registry.ServiceItem;
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
	
	@Test
	public void testGetInvalidComponent() {
		JmicroInstanceManager rpc = of.get(JmicroInstanceManager.class);
		Assert.assertNull(rpc);
	}
	
}
