package org.jmicro.example.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.jmicro.api.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
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
		/*Config cfg = new Config();
		cfg.setBindIp("localhost");
		cfg.setPort(9801);;
		cfg.setBasePackages(new String[]{"org.jmicro","org.jmtest"});
		cfg.setRegistryUrl(new URL("zookeeper","localhost",2180));
		JMicroContext.setCfg(cfg);*/
		
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
	
	
}
