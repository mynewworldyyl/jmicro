package org.jmicro.objfactory.simple.integration.test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicro;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestPubSubServer extends JMicroBaseTestCase{

	@Test
	public void testPubSubServerMessage() {
		IInternalSubRpc psm = of.getRemoteServie(IInternalSubRpc.class.getName(), "org.jmicro.pubsub.DefaultPubSubServer", "0.0.1",null);
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic(TOPIC);
		psm.publishData(psd);
	}
	
	@Test
	public void testPublishArgs() {
		PubSubManager psm = of.get(PubSubManager.class);
		Object[] args = new String[] {"test publish args"};
		long msgid = psm.publish("/jmicro/test/topic02",PSData.FLAG_PUBSUB,args);
		System.out.println("pubsub msgID:"+msgid);
	}
	

	@Test
	public void testPresurePublish() {
		
		final Random ran = new Random();
		
		PubSubManager psm = of.get(PubSubManager.class);
		
		AtomicInteger id = new AtomicInteger(0);
		
		Runnable r = ()->{
			while(true) {
				try {
					/*
					long msgid = psm.publish(new HashMap<String,Object>(), TOPIC, 
							"test pubsub server id: "+id.getAndIncrement(),PSData.FLAG_QUEUE);*/
					

					long msgid = psm.publish("/jmicro/test/topic02",PSData.FLAG_QUEUE,new String[] {"test pubsub server id: "+id.getAndIncrement()});
					
					System.out.println("pubsub msgID:"+msgid);
					
					//Thread.sleep(2000);
					Thread.sleep(ran.nextInt(50));
					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishStringMessage() {
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(new HashMap<String,Object>(), TOPIC, "test pubsub server",PSData.FLAG_PUBSUB);
		JMicro.waitForShutdown();
	}
}
