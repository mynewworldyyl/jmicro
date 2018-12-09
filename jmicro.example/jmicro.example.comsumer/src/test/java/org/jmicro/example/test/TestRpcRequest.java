package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.monitor.IServiceMonitorData;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.service.ICheckable;
import org.jmicro.api.test.Person;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.example.comsumer.TestRpcClient;
import org.junit.Test;

public class TestRpcRequest {

	/*@Test
	public void testDynamicProxy() {
		ITestRpcService src = SimpleObjectFactory.createDynamicServiceProxy(ITestRpcService.class
				,Constants.DEFAULT_NAMESPACE,Constants.DEFAULT_VERSION);
		AbstractServiceProxy asp = (AbstractServiceProxy)src;
		asp.setHandler(new ServiceInvocationHandler());
		System.out.println(src.hello("Hello"));
		System.out.println("testDynamicProxy");
	}*/
	
	@Test
	public void testRpcClient() {
		TestRpcClient src = JMicro.getObjectFactoryAndStart(new String[0]).get(TestRpcClient.class);
		src.invokeRpcService();	
	}
	
	@Test
	public void testInvokePersonService() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{
				"-DinstanceName=testInvokePersonService","-Dclient=true"});
		of.start();
		TestRpcClient src = of.get(TestRpcClient.class);
		src.invokePersonService();
	}
	
	@Test
	public void testNoNeedRespService() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		src.pushMessage("Hello Server");
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testStreamService() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		
		IMessageCallback<String> msgReceiver = (msg)->{
			System.out.println(msg);
			return true;
		};
		JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_CLIENT, msgReceiver);
		
		src.subscrite("Hello ");
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testCheckable() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		ICheckable c = (ICheckable)src;
		String msg = c.wayd("How are you");
		System.out.println(msg);
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testSubscriteResponseTimeMonitor() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		
		IMessageCallback<String> msgReceiver = (msg)->{
			System.out.println(msg);
			return true;
		};
		
		JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_CLIENT, msgReceiver);
		
		IServiceMonitorData src = of.get(IServiceMonitorData.class);
		String sn = null;
		/*
		ServiceItem.serviceName("org.jmicro.example.api.ITestRpcService", "testrpc", "0.0.1");
		sn = ServiceItem.methodKey(sn, "getPerson", "org.jmicro.api.Person");
		*/
		
		Integer id = src.subsicribe(sn);
		
		Utils.getIns().waitForShutdown();
		
		src.unsubsicribe(id,sn);
	}
	
	
	public static void main(String[] args) {
		ITestRpcService srv = JMicro.getRpcServiceTestingArgs(ITestRpcService.class);
		srv.getPerson(new Person());
	}
	
}
