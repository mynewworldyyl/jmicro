package org.jmicro.example.test;

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
	public void testPublishStringMessage() {
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(new HashMap<String,Object>(), "/jmicro/test/topic01", "test pubsub server");
	}
	
	@Test
	public void testPubSubServerMessage() {
		IInternalSubRpc psm = of.getRemoteServie(IInternalSubRpc.class.getName(), "org.jmicro.pubsub.DefaultPubSubServer", "0.0.1",null);
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic("/jmicro/test/topic01");
		psm.publishData(psd);
	}

	@Test
	public void testPresurePublish() {
		
		final Random ran = new Random();
		
		PubSubManager psm = of.get(PubSubManager.class);
		
		AtomicInteger id = new AtomicInteger(0);
		
		Runnable r = ()->{
			while(true) {
				try {
					try {
						psm.publish(new HashMap<String,Object>(), TOPIC, 
								"test pubsub server id: "+id.getAndIncrement());
						//Thread.sleep(500000000);
						Thread.sleep(ran.nextInt(100));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r).start();
		new Thread(r).start();
		/*new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();*/
		
		JMicro.waitForShutdown();
	}
}
