package org.jmicro.pubsub.test;

import org.junit.Assert;
import org.junit.Test;

import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.pubsub.PubSubServer;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestPubSubServer extends JMicroBaseTestCase{

	protected static String[] getArgs() {
		return new String[] {"-DinstanceName=TestPubSubServer"};
	}
	
	@Test
	public void testSubscribe() {
		PubSubServer s = of.get(PubSubServer.class);
		//boolean succ = s.subcribe(TOPIC, helloTopicMethodKey(), new HashMap<String,String>());
		//Assert.assertTrue(succ);
	}
	
	@Test
	public void testunSubscribe() {
		testSubscribe();
		//Assert.assertTrue(of.get(PubSubServer.class).unsubcribe(TOPIC, helloTopicMethodKey(), new HashMap<String,String>()));
	}
	
	@Test
	public void testPublishString() {
		Assert.assertTrue(PubSubManager.PUB_OK == of.get(PubSubServer.class).publishString(TOPIC, "Hello pubsub server"));
		this.waitForReady(1);
	}
	
	@Test
	public void testPublishString100() {
		for(int i = 0; i < 100; i++ ) {
			this.waitForReady(0.2F);
			Assert.assertTrue(PubSubManager.PUB_OK == of.get(PubSubServer.class).publishString(TOPIC, "Hello pubsub server"+i));
		}
	}
	
	
}
