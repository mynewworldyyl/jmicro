package cn.jmicro.breaker.test.integration;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestBreakerSubscriberImpl extends JMicroBaseTestCase{
	
	@Test
	public void testPresurePublish() {
		
		final Random ran = new Random();
		
		PubSubManager psm = of.get(PubSubManager.class);
		
		AtomicInteger id = new AtomicInteger(0);
		
		Runnable r = ()->{
			while(true) {
				try {
					//Thread.sleep(2000);
					Thread.sleep(ran.nextInt(100));
					psm.publish(new HashMap<String,Object>(), TOPIC, 
							"test pubsub server id: "+id.getAndIncrement(),PSData.FLAG_PUBSUB);
				} catch (Throwable e) {
					System.out.println(e.getMessage());;
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
