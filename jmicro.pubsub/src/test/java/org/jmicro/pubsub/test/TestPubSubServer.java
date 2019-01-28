package org.jmicro.pubsub.test;

import java.util.HashMap;

import org.jmicro.api.JMicro;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.pubsub.PubSubServer;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPubSubServer extends JMicroBaseTestCase{

	@BeforeClass //
	public static void setupTestClass() {
		of = JMicro.getObjectFactoryAndStart(getArgs());
		of.start();
		registry = of.get(IRegistry.class);
	}
	
	protected static String[] getArgs() {
		return new String[] {"-DinstanceName=TestPubSubServer"};
	}
	
	@Test
	public void testSubscribe() {
		PubSubServer s = of.get(PubSubServer.class);
		boolean succ = s.subcribe(TOPIC, helloTopicMethodKey(), new HashMap<String,String>());
		Assert.assertTrue(succ);
	}
	
	@Test
	public void testunSubscribe() {
		testSubscribe();
		Assert.assertTrue(of.get(PubSubServer.class).unsubcribe(TOPIC, helloTopicMethodKey(), new HashMap<String,String>()));
	}
	
	@Test
	public void testPublishString() {
		Assert.assertTrue(of.get(PubSubServer.class).publishString(TOPIC, "Hello pubsub server"));
		this.waitForReady(1);
	}
	
	@Test
	public void testPublishString100() {
		for(int i = 0; i < 100; i++ ) {
			this.waitForReady(0.2F);
			Assert.assertTrue(of.get(PubSubServer.class).publishString(TOPIC, "Hello pubsub server"+i));
		}
	}
	
	
}
