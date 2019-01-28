package org.jmicro.api.service.integration;

import java.util.Arrays;
import java.util.Random;

import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TestRedis extends JMicroBaseTestCase{

    private static final Logger logger = LoggerFactory.getLogger(TestRedis.class);
    
    @Test
	public void testIdTestRedisIdServer() {
		//IIdServer idServer = of.getServie(IIdServer.class.getName(), "RedisBaseIdServer", "0.0.1");
		ComponentIdServer idServer = of.get(ComponentIdServer.class);
		String longId = idServer.getStringId(IRequest.class);
		System.out.println(longId);

	}
    
	@Test
	public void testGetIdGenerator() {
		
		final Random ran = new Random();
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {
				"-DinstanceName=testGetIdGenerator",
				"-Dclient=true"});
		
		//IIdServer idServer = of.getServie(IIdServer.class.getName(), "RedisBaseIdServer", "0.0.1");
		ComponentIdServer idServer = of.get(ComponentIdServer.class);
		
		Runnable r = ()->{
			while(true) {
				try {
					long longId = idServer.getLongId(IRequest.class);
					System.out.println(longId);
					try {
						//Thread.sleep(500000000);
						Thread.sleep(ran.nextInt(60));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//System.out.println("one loop");
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r,"rwork1").start();
		new Thread(r,"rwork2").start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		
		JMicro.waitForShutdown();
		
	}
	

}
