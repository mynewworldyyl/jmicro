package cn.jmicro.example.test.classloader;

import org.junit.Test;

import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestClassloaderService extends JMicroBaseTestCase{
	
	@Test
	public void testGetMngI18NServiceClass() {
		/*
		IClassloaderRpc$JMAsyncClient rpcLlassloader = of.getRemoteServie(IClassloaderRpc$JMAsyncClient.class.getName(),
				"*", "*",null);
		IPromise<byte[]> p = rpcLlassloader.getClassDataJMAsync("cn.jmicro.mng.api.II8NService");
		byte[] bytes = p.getResult();
		*/
		
		RpcClassLoader rpccl = of.get(RpcClassLoader.class);
		Class<?> cls = rpccl.findClass("cn.jmicro.mng.api.II8NService");
		System.out.println(cls.getName());
		
	}
}
