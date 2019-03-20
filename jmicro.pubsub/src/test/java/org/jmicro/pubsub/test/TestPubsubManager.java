package org.jmicro.pubsub.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.pubsub.ISubsListener;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPubsubManager extends JMicroBaseTestCase{

	@BeforeClass //
	public static void setupTestClass() {
		of = JMicro.getObjectFactoryAndStart(getArgs());
		registry = of.get(IRegistry.class);
	}
	
	protected static String[] getArgs() {
		return new String[] {"-DinstanceName=TestPubSubServer"};
	}
	
	@Test
	public void testPubsubManagerPublish() {
		PubSubManager m = of.get(PubSubManager.class);
		PSData d = new PSData();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(m.publish(d));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishString() {
		PubSubManager m = of.get(PubSubManager.class);
		PSData d = new PSData();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(m.publish(null,TOPIC,"testPubsubManagerPublishString"));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishBytes() {
		PubSubManager m = of.get(PubSubManager.class);
		PSData d = new PSData();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(m.publish(null,TOPIC,"testPubsubManagerPublishString".getBytes()));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubAddSubListener() {
		PubSubManager m = of.get(PubSubManager.class);
		m.addSubsListener((type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		});
	}
	
	@Test
	public void testPubsubAddUnSubListener() {
		PubSubManager m = of.get(PubSubManager.class);
		ISubsListener sub = (type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		};
		m.addSubsListener(sub);
		m.removeSubsListener(sub);
	}
	
	/*@Test
	public void testSubcribe() {
		PubSubManager m = of.get(PubSubManager.class);
		Assert.assertTrue(m.subscribe(null, "/jmicro/test/topic01", 
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic"));
		
		this.waitForReady(1);
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testUnsubcribe() {
		PubSubManager m = of.get(PubSubManager.class);
		Assert.assertTrue(m.subscribe(null, "/jmicro/test/topic01", 
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic"));
		
		this.waitForReady(3);
		
		m.unsubcribe(null, "/jmicro/test/topic01",
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic");
		
		//JMicro.waitForShutdown();
	}*/
	
	
	
}
