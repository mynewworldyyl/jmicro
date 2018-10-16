package org.jmicro.example.test;

import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.service.ICheckable;
import org.jmicro.api.servicemanager.ComponentManager;
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
		TestRpcClient src = ComponentManager.getObjectFactory().get(TestRpcClient.class);
		src.invokeRpcService();	
	}
	
	@Test
	public void testInvokePersonService() {
		Config.parseArgs(new String[0]);
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		TestRpcClient src = of.get(TestRpcClient.class);
		src.invokePersonService();
	}
	
	@Test
	public void testNoNeedRespService() {
		Config.parseArgs(new String[0]);
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		src.pushMessage("Hello Server");
		//Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testStreamService() {
		Config.parseArgs(new String[0]);
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		src.subscrite("Hello ");;
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testCheckable() {
		Config.parseArgs(new String[0]);
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		ITestRpcService src = of.get(ITestRpcService.class);
		ICheckable c = (ICheckable)src;
		String msg = c.wayd("How are you");
		System.out.println(msg);
		//Utils.getIns().waitForShutdown();
	}
}
