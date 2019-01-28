package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TestClassloader {

    private static final Logger logger = LoggerFactory.getLogger(TestClassloader.class);
    
    @Test
	public void testGetClassFromRpc() throws ClassNotFoundException {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {
				"-DinstanceName=testGetClassFromRpc"});
		
		//IIdServer idServer = of.getServie(IIdServer.class.getName(), "RedisBaseIdServer", "0.0.1");
		//AbstractClientClassLoader cl = of.get(AbstractClientClassLoader.class);
		
		RpcClassLoader cl = of.get(RpcClassLoader.class);
		
		Class<?> clazz = cl.loadClass("org.jmicro.example.api.IRemoteInterface");
		org.junit.Assert.assertNotNull(clazz);
		
		Class<?> clazz1 = cl.loadClass("org.jmicro.example.api.IRemoteInterface");
		org.junit.Assert.assertNotNull(clazz1);
		
		org.junit.Assert.assertTrue(clazz == clazz1);
		
		System.out.println(clazz);

	}
    
}
