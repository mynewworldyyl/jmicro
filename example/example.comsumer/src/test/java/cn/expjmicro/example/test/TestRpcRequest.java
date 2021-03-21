package cn.expjmicro.example.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.JMicro;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.service.ICheckable;
import cn.expjmicro.example.api.IDynamicInterface;
import cn.expjmicro.example.api.ITestRpcService;
import cn.expjmicro.example.api.rpc.ISimpleRpc;
import cn.expjmicro.example.comsumer.TestRpcClient;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestRpcRequest extends JMicroBaseTestCase{

	public static void main(String[] args) {
		Map<String,String> result = new HashMap<>();
		ISimpleRpc sayHelloSrv = EnterMain.getRpcServiceTestingArgs(ISimpleRpc.class,result,Message.PROTOCOL_BIN);
		sayHelloSrv.hello("Are you OK");
		System.out.println(result);
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
	public void testCheckable() {
		
		ITestRpcService src = of.get(ITestRpcService.class);
		ICheckable c = (ICheckable)src;
		String msg = c.wayd("How are you");
		System.out.println(msg);
		//Utils.getIns().waitForShutdown();
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
		IDynamicInterface r = of.getRemoteServie(IDynamicInterface.class.getName(), "JMicroBaseTestCase_DynamicRegistryService",
				"0.0.1",null);	
		//r.run();
	}
	
	@Test
	public void testClientAsyncRpc() {
		ISimpleRpc sayHelloSrv = of.getRemoteServie(ISimpleRpc.class.getName(),"simpleRpc","0.0.1",null);
		
		IPromise<String> p = PromiseUtils.callService(sayHelloSrv, "hello",null, "hello async rpc");
		
		p.then((msg,fail,ctx) -> {
			System.out.println(msg);
		});
		
		this.waitForReady(1000);

	}
	
	@Test
	public void testLinkRpcAsync() {
		ISimpleRpc sayHelloSrv = of.getRemoteServie(ISimpleRpc.class.getName(),"simpleRpc","0.0.1",null);
		
		IPromise<String> p = PromiseUtils.callService(sayHelloSrv, "linkRpc",null, "linkRpc async rpc");
		
		p.then((msg,fail,ctx) -> {
			System.out.println(msg);
		});
		
		this.waitForReady(1000);

	}
	
}
