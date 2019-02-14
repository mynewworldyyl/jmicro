package org.jmicro.breaker.test.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicro;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.breaker.api.IBreakerSubscriber;
import org.jmicro.common.Constants;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestBreakerSubscriberImpl extends JMicroBaseTestCase{

	@Test
	public void testOnStatics() {
		
		final Random ran = new Random();
		
		IBreakerSubscriber bs = of.get(IBreakerSubscriber.class);
		Map<Integer,Double> data = new HashMap<>();
		for(Integer type : AbstractMonitorDataSubscriber.YTPES) {
			data.put(type, ran.nextDouble());
		}
		
		data.put(MonitorConstant.STATIS_TOTAL_RESP, 90D);
		data.put(MonitorConstant.STATIS_QPS,  22D);
		data.put(MonitorConstant.STATIS_SUCCESS_PERCENT,  80D);
		data.put(MonitorConstant.STATIS_FAIL_PERCENT,  60D);
		
		PSData psData = new PSData();
		psData.setData(data);
		psData.setTopic(MonitorConstant.TEST_SERVICE_METHOD_TOPIC);
		psData.put(Constants.SERVICE_METHOD_KEY, sayHelloServiceMethod());
	
		bs.onStatics(psData);
		
		JMicro.waitForShutdown();
	}
	
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
							"test pubsub server id: "+id.getAndIncrement());
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
