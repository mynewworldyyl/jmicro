package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.monitor.IServiceMonitorData;
import org.jmicro.api.service.ICheckable;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.example.api.DynamicInterface;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.example.api.rpc.ISimpleRpc;
import org.jmicro.example.comsumer.TestRpcClient;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestRpcRequest extends JMicroBaseTestCase{

	public static void main(String[] args) {
		ISimpleRpc sayHelloSrv = JMicro.getRpcServiceTestingArgs(ISimpleRpc.class);
		sayHelloSrv.hello("Are you OK");
	}
	
	@Test
	public void testRpcClient() {
		of.get(TestRpcClient.class).invokeRpcService();	
	}
	
	@Test
	public void testInvokePersonService() {
		of.get(TestRpcClient.class).invokePersonService();
	}
	
	@Test
	public void testNoNeedRespService() {
		ITestRpcService src = of.get(ITestRpcService.class);
		src.pushMessage("Hello Server");
	}
	
	@Test
	public void testStreamService() {
		
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
		
		ITestRpcService src = of.get(ITestRpcService.class);
		ICheckable c = (ICheckable)src;
		String msg = c.wayd("How are you");
		System.out.println(msg);
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testSubscriteResponseTimeMonitor() {
		
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
	
	@Test
	public void testPrimitiveParamService() {
		ITestRpcService src = of.get(ITestRpcService.class);
		System.out.println(src.testReturnPrimitiveResult());
		System.out.println(src.testReturnPrimitiveArrayResult());
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testBooleanParamService() {
		ITestRpcService src = of.get(ITestRpcService.class);
		System.out.println(src.testReturnPrimitiveBooleanResult());
		System.out.println(src.testReturnBooleanResult());
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testCallDynamicRegistRpc() {
		DynamicInterface r = of.getRemoteServie(DynamicInterface.class.getName(), "JMicroBaseTestCase_DynamicRegistryService",
				"0.0.1", null,null);	
		//r.run();
	}
	
	@Test
	public void testAsyncCallRpc() {
		of.get(TestRpcClient.class).testCallAsyncRpc();
		Utils.getIns().waitForShutdown();
	}
	
}
