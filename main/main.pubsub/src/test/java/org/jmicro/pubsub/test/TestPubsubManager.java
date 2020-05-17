package org.jmicro.pubsub.test;

import org.junit.BeforeClass;
import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.pubsub.ISubsListener;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.pubsub.PubSubServer;
import cn.jmicro.test.JMicroBaseTestCase;

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
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(d));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishString() {
		PubSubManager m = of.get(PubSubManager.class);
		PSData d = new PSData();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(null,TOPIC,"testPubsubManagerPublishString",PSData.FLAG_PUBSUB));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishBytes() {
		PubSubManager m = of.get(PubSubManager.class);
		PSData d = new PSData();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(null,TOPIC,"testPubsubManagerPublishString".getBytes(),PSData.FLAG_PUBSUB));
		//JMicro.waitForShutdown();
	}
	
	/*@Test
	public void testPubsubAddSubListener() {
		PubSubServer m = of.get(PubSubServer.class);
		m.addSubsListener((type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		});
	}
	
	@Test
	public void testPubsubAddUnSubListener() {
		PubSubServer m = of.get(PubSubServer.class);
		ISubsListener sub = (type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		};
		m.addSubsListener(sub);
		m.removeSubsListener(sub);
	}*/
	
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
